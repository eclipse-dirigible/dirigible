/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.clients;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The input rules of a client registry: every offending field is named in one 400.
 */
final class TenantClientSyncValidation {

    /** The longest client id, name and role. */
    static final int TEXT_MAX = 255;

    /** The longest person. */
    static final int PERSON_MAX = 320;

    /** The most roles a client holds. */
    static final int ROLES_MAX = 100;

    /** A client id: no whitespace, so what the token names is what is stored. */
    private static final Pattern CLIENT_ID = Pattern.compile("^\\S+$");

    private TenantClientSyncValidation() {}

    /**
     * The problems of a registry.
     *
     * @param body the registry
     * @return the problems, empty when it is valid
     */
    static List<String> problems(TenantClientSync body) {
        List<String> problems = new ArrayList<>();
        if (body == null) {
            problems.add("body: is required");
            return problems;
        }
        if (body.revision() == null) {
            problems.add("revision: is required");
        } else if (body.revision() < 1) {
            problems.add("revision: must be at least 1");
        }
        if (body.clients() == null) {
            problems.add("clients: is required");
            return problems;
        }
        Set<String> clientIds = new HashSet<>();
        for (int index = 0; index < body.clients()
                                        .size(); index++) {
            TenantClientSync.Client client = body.clients()
                                                 .get(index);
            String at = "clients[" + index + "]";
            if (client == null) {
                problems.add(at + ": must not be null");
                continue;
            }
            if (client.clientId() == null || client.clientId()
                                                   .isEmpty()) {
                problems.add(at + ".clientId: is required");
            } else if (client.clientId()
                             .length() > TEXT_MAX
                    || !CLIENT_ID.matcher(client.clientId())
                                 .matches()) {
                problems.add(at + ".clientId: must be at most " + TEXT_MAX + " characters, without whitespace");
            } else if (!clientIds.add(client.clientId())) {
                problems.add(at + ".clientId: [" + client.clientId() + "] appears more than once");
            }
            if (client.name() == null || client.name()
                                               .isBlank()) {
                problems.add(at + ".name: is required");
            } else if (client.name()
                             .length() > TEXT_MAX) {
                problems.add(at + ".name: must be at most " + TEXT_MAX + " characters");
            }
            checkRoles(problems, at, client);
            checkPerson(problems, at + ".createdBy", client.createdBy());
            checkInstant(problems, at + ".createdAt", client.createdAt());
            checkPerson(problems, at + ".revokedBy", client.revokedBy());
            checkInstant(problems, at + ".revokedAt", client.revokedAt());
        }
        return problems;
    }

    private static void checkRoles(List<String> problems, String at, TenantClientSync.Client client) {
        List<String> roles = client.roles();
        if (roles == null || roles.isEmpty()) {
            if (!client.isRevoked()) {
                problems.add(at + ".roles: an active client must hold at least one role");
            }
            return;
        }
        if (roles.size() > ROLES_MAX) {
            problems.add(at + ".roles: must hold at most " + ROLES_MAX + " roles");
        }
        Set<String> names = new HashSet<>();
        for (int index = 0; index < roles.size(); index++) {
            String role = roles.get(index);
            String where = at + ".roles[" + index + "]";
            if (role == null || role.isBlank()) {
                problems.add(where + ": is required");
            } else if (role.length() > TEXT_MAX) {
                problems.add(where + ": must be at most " + TEXT_MAX + " characters");
            } else if (TenantClientRoles.isForbidden(role)) {
                problems.add(where + ": [" + role + "] is never given to a client - it is the tenant's Owner or a role of the instance");
            } else if (!names.add(role)) {
                problems.add(where + ": [" + role + "] appears more than once");
            }
        }
    }

    private static void checkPerson(List<String> problems, String field, String value) {
        if (value != null && value.length() > PERSON_MAX) {
            problems.add(field + ": must be at most " + PERSON_MAX + " characters");
        }
    }

    private static void checkInstant(List<String> problems, String field, String value) {
        if (value != null && instant(value) == null) {
            problems.add(field + ": must be an ISO-8601 instant such as 2026-11-02T10:00:00Z");
        }
    }

    /**
     * Reads an ISO-8601 instant.
     *
     * @param value the text, may be null
     * @return the instant, or null when absent or not an instant
     */
    static Instant instant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
