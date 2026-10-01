/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Applies the provisioning system's user snapshots to the replica - the only way it changes, apart
 * from the application's own last sign-in. The rules are the users snapshot contract's:
 *
 * <ol>
 * <li>a snapshot whose revision is not higher than the stored one is ignored;</li>
 * <li>otherwise it replaces the row completely - status, revision, who/when, the latest error and
 * the roles exactly as sent - and leaves {@code lastSignInAt} alone;</li>
 * <li>{@code REMOVED} drops the roles and keeps the row as a hidden tombstone; a newer live
 * snapshot revives it and clears {@code lastSignInAt};</li>
 * <li>a complete list turns every live row it does not name, at or below its revision, into a
 * tombstone at that revision - a row written after the provisioning system read its list is
 * kept;</li>
 * <li>an email with no row is inserted as sent, a {@code REMOVED} one as a tombstone, which still
 * guards the ordering.</li>
 * </ol>
 *
 * Every user is applied in a transaction of its own: a failure on one leaves the others applied,
 * and the provisioning system's next push or resync repairs the rest.
 */
@Service
class TenantUserReplicaService {

    private static final Logger logger = LoggerFactory.getLogger(TenantUserReplicaService.class);

    /** The replica. */
    private final TenantUserRepository users;

    /** The tenants. */
    private final TenantService tenants;

    /** One new transaction per user, whatever the caller runs in. */
    private final TransactionTemplate writes;

    /** A read-only transaction for the views, which read the lazy roles. */
    private final TransactionTemplate reads;

    /**
     * Instantiates the service.
     *
     * @param users the replica
     * @param tenants the tenants
     * @param transactionManager the SystemDB transaction manager
     */
    TenantUserReplicaService(TenantUserRepository users, TenantService tenants,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        this.users = users;
        this.tenants = tenants;
        this.writes = new TransactionTemplate(transactionManager);
        this.writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.reads = new TransactionTemplate(transactionManager);
        this.reads.setReadOnly(true);
    }

    /**
     * Applies a snapshot body.
     *
     * @param tenantId the tenant id
     * @param body the body
     * @return what the apply did
     */
    TenantUserSync.Result apply(String tenantId, TenantUserSync body) {
        requireTenant(tenantId);
        List<String> problems = TenantUserSyncValidation.problems(body);
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", problems));
        }
        int applied = 0;
        int ignored = 0;
        for (TenantUserSync.Snapshot snapshot : body.users()) {
            if (applyWithRetry(tenantId, snapshot)) {
                applied++;
            } else {
                ignored++;
            }
        }
        int removed = body.isComplete() ? tombstoneAbsent(tenantId, body) : 0;
        Long storedRevision = users.findMaxRevision(tenantId);
        logger.info("Tenant users snapshot for tenant [{}]: complete [{}], applied [{}], ignored [{}], removed [{}], stored revision [{}]",
                LogSanitizer.sanitize(tenantId), body.isComplete(), applied, ignored, removed, storedRevision);
        return new TenantUserSync.Result(applied, ignored, removed, storedRevision);
    }

    /**
     * The users of a tenant.
     *
     * @param tenantId the tenant id
     * @param includeRemoved whether the tombstones are included
     * @return the users, by email
     */
    List<TenantUserView> list(String tenantId, boolean includeRemoved) {
        requireTenant(tenantId);
        return reads.execute(status -> (includeRemoved ? users.findByTenantIdOrderByEmail(tenantId)
                : users.findByTenantIdAndStatusNotOrderByEmail(tenantId, TenantUserStatus.REMOVED)).stream()
                                                                                                   .map(TenantUserView::of)
                                                                                                   .toList());
    }

    /**
     * One user of a tenant by id, removed or not.
     *
     * @param tenantId the tenant id
     * @param id the id
     * @return the user
     */
    Optional<TenantUserView> find(String tenantId, long id) {
        return reads.execute(status -> users.findByIdAndTenantId(id, tenantId)
                                            .map(TenantUserView::of));
    }

    /**
     * One user of a tenant by email, removed or not.
     *
     * @param tenantId the tenant id
     * @param email the email
     * @return the user
     */
    Optional<TenantUserView> find(String tenantId, String email) {
        return reads.execute(status -> users.findByTenantIdAndEmail(tenantId, TenantUserRules.normalize(email))
                                            .map(TenantUserView::of));
    }

    /**
     * Applies one user, retrying once when another apply inserted the same user between this one's
     * lookup and its insert - the row exists then, so the retry takes the revision comparison's path.
     */
    private boolean applyWithRetry(String tenantId, TenantUserSync.Snapshot snapshot) {
        try {
            return Boolean.TRUE.equals(writes.execute(status -> applyOne(tenantId, snapshot)));
        } catch (DataIntegrityViolationException race) {
            logger.debug("Tenant users snapshot for tenant [{}]: [{}] was inserted concurrently - applying again",
                    LogSanitizer.sanitize(tenantId), LogSanitizer.sanitize(snapshot.email()));
            return Boolean.TRUE.equals(writes.execute(status -> applyOne(tenantId, snapshot)));
        }
    }

    /**
     * Rules 1, 2, 3 and 5 for one user.
     *
     * @return true when applied, false when ignored
     */
    private boolean applyOne(String tenantId, TenantUserSync.Snapshot snapshot) {
        String email = TenantUserRules.normalize(snapshot.email());
        long revision = snapshot.revision();
        Optional<TenantUser> stored = users.findForUpdateByTenantIdAndEmail(tenantId, email);
        if (stored.isPresent() && stored.get()
                                        .getRevision() >= revision) {
            logger.debug("Tenant users snapshot for tenant [{}]: [{}] ignored - stored revision [{}], snapshot revision [{}]",
                    LogSanitizer.sanitize(tenantId), LogSanitizer.sanitize(email), stored.get()
                                                                                         .getRevision(),
                    revision);
            return false;
        }
        TenantUser user = stored.orElseGet(() -> new TenantUser(tenantId, email));
        TenantUserStatus status = TenantUserStatus.valueOf(snapshot.status());
        boolean revived = stored.isPresent() && user.getStatus() == TenantUserStatus.REMOVED && status != TenantUserStatus.REMOVED;
        user.setStatus(status);
        user.setRevision(revision);
        user.setInvitedBy(snapshot.invitedBy());
        user.setInvitedAt(TenantUserSyncValidation.instant(snapshot.invitedAt()));
        user.setLastChangedBy(snapshot.lastChangedBy());
        user.setLastChangedAt(TenantUserSyncValidation.instant(snapshot.lastChangedAt()));
        TenantUserSync.LastError lastError = snapshot.lastError();
        user.setLastErrorCode(lastError == null ? null : lastError.code());
        user.setLastErrorMessage(lastError == null ? null : lastError.message());
        if (status == TenantUserStatus.REMOVED) {
            user.clearRoles();
        } else {
            replaceRoles(user, snapshot.roles() == null ? List.of() : snapshot.roles());
        }
        if (revived) {
            user.setLastSignInAt(null);
        }
        if (stored.isEmpty()) {
            // Flushed here, so a concurrent insert of the same user fails inside this transaction.
            users.saveAndFlush(user);
        }
        return true;
    }

    /**
     * Makes the role rows exactly the snapshot's. A row is updated in place rather than replaced: a
     * removed and re-added row of the same role would be inserted before the old one is deleted, and
     * collide with it on the unique key.
     */
    private static void replaceRoles(TenantUser user, List<TenantUserSync.RoleState> roles) {
        Set<String> names = roles.stream()
                                 .map(TenantUserSync.RoleState::role)
                                 .collect(Collectors.toSet());
        user.retainRoles(names);
        for (TenantUserSync.RoleState role : roles) {
            TenantUserRoleState state = TenantUserRoleState.valueOf(role.state());
            TenantUserRole row = user.roleNamed(role.role())
                                     .orElseGet(() -> user.addRole(role.role(), state));
            row.setState(state);
            row.setGrantedBy(role.grantedBy());
            row.setGrantedAt(TenantUserSyncValidation.instant(role.grantedAt()));
        }
    }

    /**
     * Rule 4: a complete list tombstones every live row it does not name, at or below its revision.
     * Each row is re-read under its lock before it is changed, so a snapshot applied meanwhile is not
     * overwritten.
     */
    private int tombstoneAbsent(String tenantId, TenantUserSync body) {
        long watermark = body.revision();
        Set<String> named = body.users()
                                .stream()
                                .map(snapshot -> TenantUserRules.normalize(snapshot.email()))
                                .collect(Collectors.toSet());
        List<String> candidates = users.findByTenantIdAndStatusNotOrderByEmail(tenantId, TenantUserStatus.REMOVED)
                                       .stream()
                                       .filter(user -> !named.contains(user.getEmail()) && user.getRevision() <= watermark)
                                       .map(TenantUser::getEmail)
                                       .toList();
        int removed = 0;
        for (String email : candidates) {
            Boolean done = writes.execute(status -> users.findForUpdateByTenantIdAndEmail(tenantId, email)
                                                         .filter(user -> user.getStatus() != TenantUserStatus.REMOVED
                                                                 && user.getRevision() <= watermark)
                                                         .map(user -> {
                                                             user.setStatus(TenantUserStatus.REMOVED);
                                                             user.setRevision(watermark);
                                                             user.clearRoles();
                                                             return Boolean.TRUE;
                                                         })
                                                         .orElse(Boolean.FALSE));
            if (Boolean.TRUE.equals(done)) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * Refuses an invalid or unknown tenant.
     *
     * @param tenantId the tenant id
     */
    private void requireTenant(String tenantId) {
        if (!TenantUserRules.isTenantId(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tenant id [" + tenantId + "] is not valid - letters, digits and hyphens, not starting or ending with a hyphen");
        }
        if (tenants.findById(tenantId)
                   .isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no tenant with id [" + tenantId + "]");
        }
    }
}
