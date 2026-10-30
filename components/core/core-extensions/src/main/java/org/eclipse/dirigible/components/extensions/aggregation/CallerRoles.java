/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.http.access.UserRequestVerifier;
import org.eclipse.dirigible.components.base.http.roles.Roles;

/**
 * The {@code roles} of a contribution, checked against the caller - so a gated entry is never
 * shipped to the browser. That is in addition to the {@code role} of the extension artefact itself,
 * which the extension service applies.
 */
final class CallerRoles {

    private CallerRoles() {}

    /**
     * Whether the caller is offered the contribution: it names no {@code roles}, or the caller holds at
     * least one of them.
     *
     * @param contribution the contribution
     * @return whether it is offered
     */
    static boolean offered(Map<String, Object> contribution) {
        if (!(contribution.get("roles") instanceof List<?> roles) || roles.isEmpty()) {
            return true;
        }
        return roles.stream()
                    .anyMatch(role -> role != null && isUserInRole(role.toString()));
    }

    /**
     * Whether the caller holds at least one role of a comma-separated list, as a generated controller's
     * role gate decides it; a blank list gates nothing.
     *
     * @param roles the comma-separated roles, may be null
     * @return whether the caller passes the gate
     */
    static boolean holdsAnyOf(String roles) {
        if (roles == null || roles.isBlank()) {
            return true;
        }
        return Arrays.stream(roles.split(","))
                     .map(String::trim)
                     .anyMatch(role -> !role.isEmpty() && isUserInRole(role));
    }

    /**
     * Whether the current user holds the role, as the script API's {@code user.isInRole} answered: in
     * an anonymous mode everybody does, a developer or an administrator holds every role, and outside a
     * request nobody does.
     */
    private static boolean isUserInRole(String role) {
        if (Configuration.isAnonymousModeEnabled() || Configuration.isAnonymousUserEnabled()) {
            return true;
        }
        if (!UserRequestVerifier.isValid()) {
            return false;
        }
        return UserRequestVerifier.isUserInRole(Roles.DEVELOPER.getRoleName())
                || UserRequestVerifier.isUserInRole(Roles.ADMINISTRATOR.getRoleName()) || UserRequestVerifier.isUserInRole(role);
    }
}
