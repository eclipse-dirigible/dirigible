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
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Invites a person: records the request on the row of the role asked for, then publishes it.
 *
 * <p>
 * The row is written first and the message second, and the two are not one transaction. A publish
 * that fails is compensated - a new user or role row is removed, an existing role row gets its
 * previous request back - and answered 503. A crash between the two leaves a request marked sent
 * that nobody answers; the page marks it stale and offers to resend it with the same id.
 */
@Service
@Conditional(TenantUsersEnabledCondition.class)
class TenantUserInvitationService {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUserInvitationService.class);

    /** An address with a local part, an at sign and a dotted domain. */
    private static final Pattern EMAIL = Pattern.compile(ApplicationUserUpsert.EMAIL);

    /** The users. */
    private final ApplicationUserService users;

    /** The producer. */
    private final MessageProducer producer;

    /** The JSON mapper - its own: this platform line exposes no shared Jackson 2 mapper bean. */
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The clock. */
    private final Clock clock;

    /**
     * Instantiates the service.
     *
     * @param users the users
     * @param producer the producer
     */
    @Autowired
    TenantUserInvitationService(ApplicationUserService users, MessageProducer producer) {
        this(users, producer, Clock.systemUTC());
    }

    /**
     * Instantiates the service with a clock - for tests.
     *
     * @param users the users
     * @param producer the producer
     * @param clock the clock
     */
    TenantUserInvitationService(ApplicationUserService users, MessageProducer producer, Clock clock) {
        this.users = users;
        this.producer = producer;
        this.clock = clock;
    }

    /**
     * Invites a person into a tenant.
     *
     * @param tenantId the tenant
     * @param request the person and role
     * @param requestedBy the owner
     * @return the user's state
     */
    ApplicationUserState invite(String tenantId, InvitationRequest request, String requestedBy) {
        if (request == null) {
            throw new TenantUsersException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "The request names no email address");
        }
        String email = request.email() == null ? "" : ApplicationUserService.normalize(request.email());
        if (email.isEmpty() || email.length() > 320 || !EMAIL.matcher(email)
                                                             .matches()) {
            throw new TenantUsersException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "[" + email + "] is not an email address");
        }
        List<String> roles = TenantUsersSettings.grantableRoles();
        String role = request.role() == null ? ""
                : request.role()
                         .trim();
        if (!roles.contains(role)) {
            throw new TenantUsersException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "[" + role + "] is not one of " + roles);
        }
        Instant now = clock.instant();
        String messageId = UUID.randomUUID()
                               .toString();
        ApplicationUserService.PreparedInvitation prepared = users.prepareInvitation(tenantId, email, role, requestedBy, messageId, now);
        try {
            publish(new UserAssignmentRequest(messageId, UserAssignmentRequest.TYPE, 1, tenantId, DirigibleConfig.APP_ID.getStringValue(),
                    email, role, requestedBy, now.toString()));
        } catch (RuntimeException e) {
            users.undoInvitation(prepared);
            throw e;
        }
        return prepared.state();
    }

    /**
     * Publishes an unanswered request for a role again, with the same id.
     *
     * @param tenantId the tenant
     * @param userId the user
     * @param role the role
     * @param requestedBy the owner
     * @return the user's state
     */
    ApplicationUserState resend(String tenantId, long userId, String role, String requestedBy) {
        Instant now = clock.instant();
        ApplicationUserService.PreparedInvitation prepared = users.prepareResend(tenantId, userId, role, requestedBy, now);
        ApplicationUserState state = prepared.state();
        String requestId = state.role(role)
                                .map(ApplicationUserState.RoleState::requestId)
                                .orElseThrow();
        try {
            publish(new UserAssignmentRequest(requestId, UserAssignmentRequest.TYPE, 1, tenantId, DirigibleConfig.APP_ID.getStringValue(),
                    state.email(), role, requestedBy, now.toString()));
        } catch (RuntimeException e) {
            users.undoInvitation(prepared);
            throw e;
        }
        return state;
    }

    /**
     * Publishes a request.
     *
     * @param request the request
     */
    private void publish(UserAssignmentRequest request) {
        String queue = TenantUsersSettings.requestQueue();
        try {
            producer.sendMessageToQueue(queue, JSON.writeValueAsString(request));
            LOGGER.info("Published request [{}] inviting [{}] as [{}] into tenant [{}] to [{}]", LogSanitizer.sanitize(request.messageId()),
                    LogSanitizer.sanitize(request.email()), LogSanitizer.sanitize(request.role()), request.tenantId(),
                    LogSanitizer.sanitize(queue));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the request [" + request.messageId() + "]", e);
        } catch (Exception e) {
            LOGGER.error("Could not publish request [{}] to [{}]", LogSanitizer.sanitize(request.messageId()), LogSanitizer.sanitize(queue),
                    e);
            throw new TenantUsersException(HttpStatus.SERVICE_UNAVAILABLE, "PUBLISH_FAILED",
                    "The invitation could not be sent - try again in a moment");
        }
    }
}
