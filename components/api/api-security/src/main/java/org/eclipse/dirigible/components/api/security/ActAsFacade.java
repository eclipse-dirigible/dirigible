/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.security;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.http.HttpSessionFacade;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegated entry ("act as"): an entitled user - ADMINISTRATOR - may arm an acting identity for the
 * CURRENT session and then use the personal ("My") surfaces in that identity's name: the generated
 * personal controllers resolve their identity mapping against {@link #effectiveUser()} instead of
 * the raw login, and the Inbox's assignee-task query does the same. Built for the
 * delegated-data-entry scenario where one manager enters timesheets, leave requests or expenses for
 * workers who do not use computers.
 * <p>
 * Deliberately NOT an authentication-level impersonation:
 * <ul>
 * <li>Authentication, roles and role checks always remain the REAL user's -
 * {@link UserFacade#getName()} and {@link UserFacade#isInRole(String)} are untouched, so audit
 * columns ({@code CreatedBy}/{@code UpdatedBy}) keep stamping who really performed the write while
 * the record's owner reference carries the acting identity.</li>
 * <li>Only the personal-identity resolution and the Inbox assignee filter read the override.</li>
 * <li>The override lives in the server-side HTTP session, never in a client-supplied header, and
 * the entitlement is re-checked on EVERY read - a revoked role kills the override mid-session.</li>
 * <li>The armed state EXPIRES on its own after {@link DirigibleConfig#ACT_AS_TTL_SECONDS} (30
 * minutes by default). The window is absolute - it starts at arming and no activity renews it -
 * because the failure mode this guards against is a state that is never exited: while armed, the
 * personal surfaces and the Inbox's assignee query serve the acting identity's world instead of the
 * real user's, and a forgotten override makes the real user's own tasks look like they were never
 * raised.</li>
 * </ul>
 */
public final class ActAsFacade {

    private static final Logger logger = LoggerFactory.getLogger(ActAsFacade.class);

    /** The HTTP-session attribute carrying the acting identity's username. */
    private static final String SESSION_ATTRIBUTE = "dirigible-act-as-user";

    /** The HTTP-session attribute carrying the epoch-milliseconds the identity was armed at. */
    private static final String SESSION_ATTRIBUTE_ARMED_AT = "dirigible-act-as-armed-at";

    private ActAsFacade() {}

    /**
     * Whether the CURRENT (real) user may arm an acting identity.
     *
     * @return true when the real user carries the entitling role
     */
    public static boolean isEntitled() {
        return UserFacade.isInRole(Roles.RoleNames.ADMINISTRATOR);
    }

    /**
     * The armed acting identity, or null when none is armed, the session is not valid, the real user is
     * not (or no longer) entitled, or the delegated-entry window has elapsed. The entitlement re-check
     * on every read is what makes a mid-session role revocation effective immediately; the expiry check
     * is what makes a forgotten arming harmless.
     *
     * @return the acting username or null
     */
    public static String actingAs() {
        if (!HttpSessionFacade.isValid() || !isEntitled()) {
            return null;
        }
        String acting = HttpSessionFacade.getAttribute(SESSION_ATTRIBUTE);
        if (acting == null || acting.isBlank()) {
            return null;
        }
        if (isExpired(HttpSessionFacade.getAttribute(SESSION_ATTRIBUTE_ARMED_AT), System.currentTimeMillis())) {
            clear();
            logger.info("Act-as EXPIRED: [{}] no longer acts as [{}] - the {}s delegated-entry window has elapsed", UserFacade.getName(),
                    acting, ttlSeconds());
            return null;
        }
        return acting;
    }

    /**
     * When the currently armed state expires, or null when nothing is armed.
     *
     * @return the expiry as epoch milliseconds, or null
     */
    public static Long expiresAt() {
        if (actingAs() == null) {
            return null;
        }
        Long armedAt = armedAt(HttpSessionFacade.getAttribute(SESSION_ATTRIBUTE_ARMED_AT));
        return armedAt == null ? null : armedAt + ttlSeconds() * 1000L;
    }

    /**
     * Whether an arming stamped at the given attribute value has run out. Fails CLOSED: a missing or
     * unparsable stamp is treated as expired, because an armed state we cannot date is an armed state
     * we cannot trust to end.
     *
     * @param armedAtAttribute the raw session attribute value
     * @param now the current epoch milliseconds
     * @return true when the state must be dropped
     */
    static boolean isExpired(String armedAtAttribute, long now) {
        Long armedAt = armedAt(armedAtAttribute);
        return armedAt == null || now - armedAt >= ttlSeconds() * 1000L;
    }

    private static Long armedAt(String armedAtAttribute) {
        if (armedAtAttribute == null || armedAtAttribute.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(armedAtAttribute.trim());
        } catch (NumberFormatException e) {
            logger.warn("Act-as arming timestamp [{}] is not a number - treating the state as expired", armedAtAttribute, e);
            return null;
        }
    }

    private static int ttlSeconds() {
        return DirigibleConfig.ACT_AS_TTL_SECONDS.getIntValue();
    }

    private static void clear() {
        HttpSessionFacade.removeAttribute(SESSION_ATTRIBUTE);
        HttpSessionFacade.removeAttribute(SESSION_ATTRIBUTE_ARMED_AT);
    }

    /**
     * The identity personal surfaces should resolve against: the armed acting identity when present,
     * else the real login. This - and ONLY this - is what the generated personal controllers and the
     * Inbox assignee query consume; everything else stays on {@link UserFacade#getName()}.
     *
     * @return the effective username
     */
    public static String effectiveUser() {
        String acting = actingAs();
        return acting != null ? acting : UserFacade.getName();
    }

    /**
     * Arms the acting identity for the current session. Audit-logged.
     *
     * @param username the acting identity's username (the identity record's mapped value, e.g. the
     *        employee's e-mail)
     * @throws SecurityException when the real user is not entitled
     * @throws IllegalArgumentException on a blank username
     */
    public static void arm(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("The acting identity's username must not be blank");
        }
        if (!isEntitled()) {
            throw new SecurityException("User [" + UserFacade.getName() + "] is not entitled to act as another identity");
        }
        String acting = username.trim();
        HttpSessionFacade.setAttribute(SESSION_ATTRIBUTE, acting);
        HttpSessionFacade.setAttribute(SESSION_ATTRIBUTE_ARMED_AT, Long.toString(System.currentTimeMillis()));
        logger.info("Act-as ARMED: [{}] now acts as [{}] for the next {}s", UserFacade.getName(), acting, ttlSeconds());
    }

    /** Disarms the acting identity for the current session. Audit-logged. */
    public static void disarm() {
        if (!HttpSessionFacade.isValid()) {
            return;
        }
        String acting = HttpSessionFacade.getAttribute(SESSION_ATTRIBUTE);
        clear();
        if (acting != null && !acting.isBlank()) {
            logger.info("Act-as DISARMED: [{}] no longer acts as [{}]", UserFacade.getName(), acting);
        }
    }
}
