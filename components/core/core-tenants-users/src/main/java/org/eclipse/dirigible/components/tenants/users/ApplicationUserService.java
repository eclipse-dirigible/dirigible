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

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Owns every rule about application users. The endpoints stay thin; the merge rules live here once.
 *
 * <p>
 * Every request and outcome is recorded on the row of the role it is about. The merge rules of a
 * callback, which make it idempotent - a re-driven request replays the same callback to the same
 * state:
 * <ul>
 * <li>a success ({@code INVITED}/{@code ASSIGNED}) makes the role {@code GRANTED} - a role already
 * granted keeps its who and when - clears its failure text, and raises the account status by
 * precedence, never lowering it; a person who has already entered the tenant becomes
 * {@code ACTIVE};</li>
 * <li>a {@code FAILED} for a granted role, or whose request id is not the role's latest, is ignored
 * - a failure must never touch a grant or a newer request; otherwise it marks the role failed, and
 * the account {@code FAILED} only while no role is granted or still requested.</li>
 * </ul>
 */
@Service
class ApplicationUserService {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationUserService.class);

    /** The tenant id pattern of the tenant provisioning API. */
    private static final Pattern TENANT_ID = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$");

    /** The users. */
    private final ApplicationUserRepository users;

    /** The tenants. */
    private final TenantService tenantService;

    /** The clock. */
    private final Clock clock;

    /**
     * Instantiates the service.
     *
     * @param users the users
     * @param tenantService the tenants
     */
    @Autowired
    ApplicationUserService(ApplicationUserRepository users, TenantService tenantService) {
        this(users, tenantService, Clock.systemUTC());
    }

    /**
     * Instantiates the service with a clock - for tests.
     *
     * @param users the users
     * @param tenantService the tenants
     * @param clock the clock
     */
    ApplicationUserService(ApplicationUserRepository users, TenantService tenantService, Clock clock) {
        this.users = users;
        this.tenantService = tenantService;
        this.clock = clock;
    }

    /**
     * Records the outcome of a request.
     *
     * @param tenantId the tenant id
     * @param upsert the outcome
     * @return whether the user was created, and the resulting state
     */
    @Transactional
    CallbackResult applyCallback(String tenantId, ApplicationUserUpsert upsert) {
        requireTenant(tenantId);
        String email = normalize(upsert.email());
        ApplicationUserStatus sent = ApplicationUserStatus.valueOf(upsert.status());
        Instant now = clock.instant();

        ApplicationUser user = users.findByTenantIdAndEmail(tenantId, email)
                                    .orElse(null);
        boolean created = user == null;
        if (created) {
            user = new ApplicationUser(tenantId, email, ApplicationUserStatus.PENDING, now);
            user.setInvitedBy(upsert.updatedBy());
            user.setInvitedAt(now);
        }
        ApplicationUserRole row = user.roleNamed(upsert.role())
                                      .orElse(null);

        if (sent == ApplicationUserStatus.FAILED) {
            if (row != null && row.is(ApplicationUserRoleState.GRANTED)) {
                LOGGER.warn("Ignored a FAILED outcome of request [{}] for [{}] in tenant [{}]: the role [{}] is already granted",
                        upsert.requestId(), email, tenantId, upsert.role());
                return new CallbackResult(false, ApplicationUserState.of(user));
            }
            if (row != null && upsert.requestId() != null && row.getRequestId() != null && !upsert.requestId()
                                                                                                  .equals(row.getRequestId())) {
                LOGGER.warn(
                        "Ignored a FAILED outcome of request [{}] for [{}] in tenant [{}]: the latest request for the role [{}] is [{}]",
                        upsert.requestId(), email, tenantId, upsert.role(), row.getRequestId());
                return new CallbackResult(false, ApplicationUserState.of(user));
            }
            if (row == null) {
                row = user.addRole(upsert.role(), ApplicationUserRoleState.FAILED);
            }
            row.setState(ApplicationUserRoleState.FAILED);
            row.setErrorMessage(upsert.errorMessage());
            if (row.getRequestId() == null) {
                row.setRequestId(upsert.requestId());
            }
            if (!user.hasRoleIn(ApplicationUserRoleState.GRANTED) && !user.hasRoleIn(ApplicationUserRoleState.REQUESTED)) {
                user.setStatus(ApplicationUserStatus.FAILED);
            }
        } else {
            if (row == null) {
                row = user.addRole(upsert.role(), ApplicationUserRoleState.GRANTED);
            }
            if (row.getGrantedAt() == null) {
                row.setGrantedBy(upsert.updatedBy());
                row.setGrantedAt(now);
                if (upsert.requestId() != null) {
                    row.setRequestId(upsert.requestId());
                }
            }
            row.setState(ApplicationUserRoleState.GRANTED);
            row.setErrorMessage(null);
            ApplicationUserStatus target = user.getLastSignInAt() != null ? ApplicationUserStatus.ACTIVE : sent;
            if (target.rank() > user.getStatus()
                                    .rank()) {
                user.setStatus(target);
            }
        }
        user.setUpdatedBy(upsert.updatedBy());
        user.setUpdatedAt(now);
        ApplicationUser saved = users.save(user);
        LOGGER.info("Recorded [{}] for [{}] as [{}] in tenant [{}] - the user is [{}]", sent, email, upsert.role(), tenantId,
                saved.getStatus());
        return new CallbackResult(created, ApplicationUserState.of(saved));
    }

    /**
     * Records an owner's request for a role before it is published: a new user is {@code PENDING}; an
     * existing one keeps its status - a failed one returns to {@code PENDING}. The role row becomes
     * {@code REQUESTED} with the new request. A role is asked for once at a time, and a granted role is
     * not asked for again; different roles may be requested side by side.
     *
     * @param tenantId the tenant
     * @param email the email, normalized
     * @param role the role
     * @param requestedBy the owner
     * @param messageId the request's id
     * @param now the time
     * @return what was changed, so a failed publish can be undone
     */
    @Transactional
    PreparedInvitation prepareInvitation(String tenantId, String email, String role, String requestedBy, String messageId, Instant now) {
        requireTenant(tenantId);
        ApplicationUser user = users.findByTenantIdAndEmail(tenantId, email)
                                    .orElse(null);
        boolean created = user == null;
        Snapshot previous = null;
        ApplicationUserRole row;
        if (created) {
            user = new ApplicationUser(tenantId, email, ApplicationUserStatus.PENDING, now);
            user.setInvitedBy(requestedBy);
            user.setInvitedAt(now);
            row = user.addRole(role, ApplicationUserRoleState.REQUESTED);
        } else {
            row = user.roleNamed(role)
                      .orElse(null);
            if (row != null && row.is(ApplicationUserRoleState.GRANTED)) {
                throw new TenantUsersException(HttpStatus.CONFLICT, "ROLE_ALREADY_GRANTED",
                        "[" + email + "] already holds the role [" + role + "]");
            }
            if (row != null && row.is(ApplicationUserRoleState.REQUESTED)) {
                throw new TenantUsersException(HttpStatus.CONFLICT, "REQUEST_PENDING",
                        "A request for [" + email + "] as [" + role + "] is still waiting for an answer - resend it instead");
            }
            previous = Snapshot.of(user, row);
            if (row == null) {
                row = user.addRole(role, ApplicationUserRoleState.REQUESTED);
            }
            if (user.getStatus() == ApplicationUserStatus.FAILED) {
                user.setStatus(ApplicationUserStatus.PENDING);
            }
        }
        row.setState(ApplicationUserRoleState.REQUESTED);
        row.setRequestId(messageId);
        row.setRequestedBy(requestedBy);
        row.setRequestedAt(now);
        row.setErrorMessage(null);
        user.setUpdatedBy(requestedBy);
        user.setUpdatedAt(now);
        ApplicationUser saved = users.save(user);
        return new PreparedInvitation(saved.getId(), role, created, previous, ApplicationUserState.of(saved));
    }

    /**
     * Records that an unanswered request for a role is published again, with the same id.
     *
     * @param tenantId the tenant
     * @param userId the user
     * @param role the role
     * @param requestedBy the owner
     * @param now the time
     * @return what was changed, so a failed publish can be undone
     */
    @Transactional
    PreparedInvitation prepareResend(String tenantId, long userId, String role, String requestedBy, Instant now) {
        ApplicationUser user = users.findById(userId)
                                    .filter(found -> found.getTenantId()
                                                          .equals(tenantId))
                                    .orElseThrow(() -> new TenantUsersException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                                            "There is no user [" + userId + "] in this tenant"));
        ApplicationUserRole row = user.roleNamed(role)
                                      .filter(found -> found.is(ApplicationUserRoleState.REQUESTED))
                                      .orElseThrow(() -> new TenantUsersException(HttpStatus.CONFLICT, "NOT_PENDING",
                                              "Only a request still waiting for an answer can be resent - invite again instead"));
        Snapshot previous = Snapshot.of(user, row);
        row.setRequestedAt(now);
        user.setUpdatedBy(requestedBy);
        user.setUpdatedAt(now);
        ApplicationUser saved = users.save(user);
        return new PreparedInvitation(saved.getId(), role, false, previous, ApplicationUserState.of(saved));
    }

    /**
     * Undoes a prepared invitation whose publish failed: a created user is removed, a created role row
     * is removed, and an existing one gets its previous request back.
     *
     * @param prepared what was changed
     */
    @Transactional
    void undoInvitation(PreparedInvitation prepared) {
        if (prepared.created()) {
            users.deleteById(prepared.userId());
            return;
        }
        users.findById(prepared.userId())
             .ifPresent(user -> {
                 prepared.previous()
                         .restore(user, prepared.role());
                 users.save(user);
             });
    }

    /**
     * Records that a person entered a tenant: stamps their last sign-in and makes an invited or
     * assigned user {@code ACTIVE}. The row is found by email - the email claim, or the principal name
     * when it is an address. Its own transaction, so a failure here can never reach the selection that
     * raised it.
     *
     * @param tenantId the tenant entered
     * @param principal the principal name
     * @param emailClaim the email claim, or null
     * @param at when
     * @return whether a user was found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean recordSignIn(String tenantId, String principal, String emailClaim, Instant at) {
        String key =
                emailClaim != null && !emailClaim.isBlank() ? emailClaim : principal != null && principal.contains("@") ? principal : null;
        if (key == null) {
            LOGGER.debug("Tenant [{}] entered by [{}], who carries no email to match a user by", tenantId, principal);
            return false;
        }
        return users.findByTenantIdAndEmail(tenantId, normalize(key))
                    .map(user -> {
                        user.setLastSignInAt(at);
                        if (user.getStatus() == ApplicationUserStatus.INVITED || user.getStatus() == ApplicationUserStatus.ASSIGNED) {
                            user.setStatus(ApplicationUserStatus.ACTIVE);
                        }
                        user.setUpdatedAt(at);
                        users.save(user);
                        return true;
                    })
                    .orElse(false);
    }

    /**
     * The users of a tenant.
     *
     * @param tenantId the tenant id
     * @return the users
     */
    @Transactional(readOnly = true)
    List<ApplicationUserState> listOf(String tenantId) {
        requireTenant(tenantId);
        return users.findByTenantIdOrderByEmail(tenantId)
                    .stream()
                    .map(ApplicationUserState::of)
                    .toList();
    }

    /**
     * Normalizes an email: trimmed and lower-cased.
     *
     * @param email the email
     * @return the normalized email
     */
    static String normalize(String email) {
        return Objects.requireNonNull(email, "email")
                      .trim()
                      .toLowerCase(Locale.ROOT);
    }

    /**
     * Refuses an invalid or unknown tenant.
     *
     * @param tenantId the tenant id
     */
    private void requireTenant(String tenantId) {
        if (tenantId == null || !TENANT_ID.matcher(tenantId)
                                          .matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tenant id [" + tenantId + "] is not valid - letters, digits and hyphens, not starting or ending with a hyphen");
        }
        if (tenantService.findById(tenantId)
                         .isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no tenant with id [" + tenantId + "]");
        }
    }

    /**
     * The result of a callback.
     *
     * @param created whether the user was created
     * @param state the resulting state
     */
    record CallbackResult(boolean created, ApplicationUserState state) {
    }

    /**
     * A prepared invitation.
     *
     * @param userId the user
     * @param role the role asked for
     * @param created whether the user was created for it
     * @param previous what the invitation changed on an existing user, or null
     * @param state the resulting state
     */
    record PreparedInvitation(long userId, String role, boolean created, Snapshot previous, ApplicationUserState state) {
    }

    /**
     * An existing user and the requested role's row before an invitation changed them.
     *
     * @param status the account status
     * @param updatedBy who changed the row
     * @param updatedAt when
     * @param role the role row before, or null when the invitation created it
     */
    record Snapshot(ApplicationUserStatus status, String updatedBy, Instant updatedAt, RoleSnapshot role) {

        static Snapshot of(ApplicationUser user, ApplicationUserRole row) {
            return new Snapshot(user.getStatus(), user.getUpdatedBy(), user.getUpdatedAt(), row == null ? null : RoleSnapshot.of(row));
        }

        void restore(ApplicationUser user, String roleName) {
            user.setStatus(status);
            user.setUpdatedBy(updatedBy);
            user.setUpdatedAt(updatedAt);
            if (role == null) {
                user.removeRole(roleName);
                return;
            }
            user.roleNamed(roleName)
                .ifPresent(role::restore);
        }
    }

    /**
     * The request fields of a role row before an invitation changed them.
     *
     * @param state the state
     * @param requestId the request id
     * @param requestedBy who asked
     * @param requestedAt when it was published
     * @param errorMessage the failure text
     */
    record RoleSnapshot(ApplicationUserRoleState state, String requestId, String requestedBy, Instant requestedAt, String errorMessage) {

        static RoleSnapshot of(ApplicationUserRole row) {
            return new RoleSnapshot(row.getState(), row.getRequestId(), row.getRequestedBy(), row.getRequestedAt(), row.getErrorMessage());
        }

        void restore(ApplicationUserRole row) {
            row.setState(state);
            row.setRequestId(requestId);
            row.setRequestedBy(requestedBy);
            row.setRequestedAt(requestedAt);
            row.setErrorMessage(errorMessage);
        }
    }
}
