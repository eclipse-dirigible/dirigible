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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Owns every rule about application users. The endpoints stay thin; the merge rules live here once.
 *
 * <p>
 * The merge rules of a callback, which make it idempotent - a re-driven request replays the same
 * callback to the same state:
 * <ul>
 * <li>a success ({@code INVITED}/{@code ASSIGNED}) adds the role row if absent - an existing one
 * keeps its who and when - clears the failure text, and raises the status by precedence, never
 * lowering it; a row whose person has already entered the tenant becomes {@code ACTIVE};</li>
 * <li>a {@code FAILED} whose request id is not the row's is ignored - a later request that failed
 * must not touch an earlier grant; otherwise it marks the request failed, and the account
 * {@code FAILED} only while no role is granted.</li>
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
            user.setRequestId(upsert.requestId());
            user.setRequestedRole(upsert.role());
        }

        if (sent == ApplicationUserStatus.FAILED) {
            if (!created && upsert.requestId() != null && user.getRequestId() != null && !upsert.requestId()
                                                                                                .equals(user.getRequestId())) {
                LOGGER.warn("Ignored a FAILED outcome of request [{}] for [{}] in tenant [{}]: the user's latest request is [{}]",
                        upsert.requestId(), email, tenantId, user.getRequestId());
                return new CallbackResult(false, ApplicationUserState.of(user));
            }
            adoptRequestId(user, upsert);
            user.setRequestState(ApplicationUserRequestState.FAILED);
            user.setErrorMessage(upsert.errorMessage());
            if (user.getRoles()
                    .isEmpty()) {
                user.setStatus(ApplicationUserStatus.FAILED);
            }
        } else {
            if (user.roleNamed(upsert.role())
                    .isEmpty()) {
                user.grant(upsert.role(), upsert.updatedBy(), now, upsert.requestId());
            }
            user.setErrorMessage(null);
            ApplicationUserStatus target = user.getLastSignInAt() != null ? ApplicationUserStatus.ACTIVE : sent;
            if (target.rank() > user.getStatus()
                                    .rank()) {
                user.setStatus(target);
            }
            if (upsert.requestId() == null || user.getRequestId() == null || upsert.requestId()
                                                                                   .equals(user.getRequestId())) {
                adoptRequestId(user, upsert);
                user.setRequestState(ApplicationUserRequestState.COMPLETED);
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
     * Records an owner's request for a person on their user row, before it is published: a new row is
     * {@code PENDING}; an existing one keeps its status - a failed one returns to {@code PENDING} - and
     * gets the new request. One request is in flight per user, and a granted role is not asked for
     * twice.
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
        if (created) {
            user = new ApplicationUser(tenantId, email, ApplicationUserStatus.PENDING, now);
            user.setInvitedBy(requestedBy);
            user.setInvitedAt(now);
        } else {
            if (user.getRequestState() == ApplicationUserRequestState.SENT) {
                throw new TenantUsersException(HttpStatus.CONFLICT, "REQUEST_PENDING",
                        "A request for [" + email + "] is still waiting for an answer - resend it instead");
            }
            if (user.roleNamed(role)
                    .isPresent()) {
                throw new TenantUsersException(HttpStatus.CONFLICT, "ROLE_ALREADY_GRANTED",
                        "[" + email + "] already holds the role [" + role + "]");
            }
            previous = Snapshot.of(user);
            if (user.getStatus() == ApplicationUserStatus.FAILED) {
                user.setStatus(ApplicationUserStatus.PENDING);
            }
            user.setErrorMessage(null);
        }
        user.setRequestId(messageId);
        user.setRequestedRole(role);
        user.setRequestState(ApplicationUserRequestState.SENT);
        user.setRequestedAt(now);
        user.setUpdatedBy(requestedBy);
        user.setUpdatedAt(now);
        ApplicationUser saved = users.save(user);
        return new PreparedInvitation(saved.getId(), created, previous, ApplicationUserState.of(saved));
    }

    /**
     * Records that an unanswered request is published again, with the same id.
     *
     * @param tenantId the tenant
     * @param userId the user
     * @param requestedBy the owner
     * @param now the time
     * @return what was changed, so a failed publish can be undone
     */
    @Transactional
    PreparedInvitation prepareResend(String tenantId, long userId, String requestedBy, Instant now) {
        ApplicationUser user = users.findById(userId)
                                    .filter(found -> found.getTenantId()
                                                          .equals(tenantId))
                                    .orElseThrow(() -> new TenantUsersException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                                            "There is no user [" + userId + "] in this tenant"));
        if (user.getRequestState() != ApplicationUserRequestState.SENT) {
            throw new TenantUsersException(HttpStatus.CONFLICT, "NOT_PENDING",
                    "Only a request still waiting for an answer can be resent - invite again instead");
        }
        Snapshot previous = Snapshot.of(user);
        user.setRequestedAt(now);
        user.setUpdatedBy(requestedBy);
        user.setUpdatedAt(now);
        ApplicationUser saved = users.save(user);
        return new PreparedInvitation(saved.getId(), false, previous, ApplicationUserState.of(saved));
    }

    /**
     * Undoes a prepared invitation whose publish failed: a created row is removed, an existing one gets
     * its previous request back.
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
                         .restore(user);
                 users.save(user);
             });
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
     * Takes the callback's request id when the row has none yet.
     *
     * @param user the user
     * @param upsert the outcome
     */
    private static void adoptRequestId(ApplicationUser user, ApplicationUserUpsert upsert) {
        if (user.getRequestId() == null && upsert.requestId() != null) {
            user.setRequestId(upsert.requestId());
            user.setRequestedRole(upsert.role());
        }
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
     * @param created whether the row was created for it
     * @param previous the previous request of an existing row, or null
     * @param state the resulting state
     */
    record PreparedInvitation(long userId, boolean created, Snapshot previous, ApplicationUserState state) {
    }

    /**
     * The request fields of a row before an invitation changed them.
     *
     * @param status the status
     * @param errorMessage the failure text
     * @param requestId the request id
     * @param requestedRole the requested role
     * @param requestState the request state
     * @param requestedAt when it was published
     * @param updatedBy who changed the row
     * @param updatedAt when
     */
    record Snapshot(ApplicationUserStatus status, String errorMessage, String requestId, String requestedRole,
            ApplicationUserRequestState requestState, Instant requestedAt, String updatedBy, Instant updatedAt) {

        static Snapshot of(ApplicationUser user) {
            return new Snapshot(user.getStatus(), user.getErrorMessage(), user.getRequestId(), user.getRequestedRole(),
                    user.getRequestState(), user.getRequestedAt(), user.getUpdatedBy(), user.getUpdatedAt());
        }

        void restore(ApplicationUser user) {
            user.setStatus(status);
            user.setErrorMessage(errorMessage);
            user.setRequestId(requestId);
            user.setRequestedRole(requestedRole);
            user.setRequestState(requestState);
            user.setRequestedAt(requestedAt);
            user.setUpdatedBy(updatedBy);
            user.setUpdatedAt(updatedAt);
        }
    }
}
