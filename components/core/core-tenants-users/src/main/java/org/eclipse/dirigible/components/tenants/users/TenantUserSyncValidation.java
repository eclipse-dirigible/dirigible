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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a users snapshot body in one pass, so a single 400 names every offending field - a caller
 * fixes the whole body at once instead of one field per round trip.
 */
final class TenantUserSyncValidation {

    /** The most roles one user may carry. */
    static final int ROLES_MAX = 20;

    /** The longest role name. */
    static final int ROLE_MAX = 100;

    /** The longest error code. */
    static final int ERROR_CODE_MAX = 64;

    /** The longest error message. */
    static final int ERROR_MESSAGE_MAX = 2000;

    private TenantUserSyncValidation() {}

    /**
     * What is wrong with a body, as {@code field: what} entries.
     *
     * @param body the body
     * @return the problems, empty when the body is valid
     */
    static List<String> problems(TenantUserSync body) {
        List<String> problems = new ArrayList<>();
        if (body.revision() == null) {
            problems.add("revision: is required");
        } else if (body.revision() < 0) {
            problems.add("revision: must not be negative");
        }
        if (body.users() == null) {
            problems.add("users: is required");
            return problems;
        }
        if (body.users()
                .isEmpty() && !body.isComplete()) {
            problems.add("users: must not be empty unless complete is true");
        }
        Set<String> emails = new HashSet<>();
        for (int index = 0; index < body.users()
                                        .size(); index++) {
            TenantUserSync.Snapshot user = body.users()
                                               .get(index);
            String at = "users[" + index + "]";
            if (user == null) {
                problems.add(at + ": must not be null");
                continue;
            }
            if (user.email() == null) {
                problems.add(at + ".email: is required");
            } else {
                String email = TenantUserRules.normalize(user.email());
                if (!TenantUserRules.isEmail(email)) {
                    problems.add(at + ".email: must be an email address of at most " + TenantUserRules.EMAIL_MAX + " characters");
                } else if (!emails.add(email)) {
                    problems.add(at + ".email: [" + email + "] appears more than once");
                }
            }
            if (user.revision() == null) {
                problems.add(at + ".revision: is required");
            } else if (user.revision() < 1) {
                problems.add(at + ".revision: must be at least 1");
            }
            if (user.status() == null) {
                problems.add(at + ".status: is required");
            } else if (!isEnum(TenantUserStatus.class, user.status())) {
                problems.add(at + ".status: must be one of PENDING, INVITED, ASSIGNED, FAILED, REMOVED");
            }
            checkPerson(problems, at + ".invitedBy", user.invitedBy());
            checkInstant(problems, at + ".invitedAt", user.invitedAt());
            checkPerson(problems, at + ".lastChangedBy", user.lastChangedBy());
            checkInstant(problems, at + ".lastChangedAt", user.lastChangedAt());
            checkRoles(problems, at, user.roles());
            checkLastError(problems, at, user.lastError());
        }
        return problems;
    }

    private static void checkRoles(List<String> problems, String at, List<TenantUserSync.RoleState> roles) {
        if (roles == null) {
            return;
        }
        if (roles.size() > ROLES_MAX) {
            problems.add(at + ".roles: must hold at most " + ROLES_MAX + " roles");
        }
        Set<String> names = new HashSet<>();
        for (int index = 0; index < roles.size(); index++) {
            TenantUserSync.RoleState role = roles.get(index);
            String where = at + ".roles[" + index + "]";
            if (role == null) {
                problems.add(where + ": must not be null");
                continue;
            }
            if (role.role() == null || role.role()
                                           .isBlank()) {
                problems.add(where + ".role: is required");
            } else if (role.role()
                           .length() > ROLE_MAX) {
                problems.add(where + ".role: must be at most " + ROLE_MAX + " characters");
            } else if (!names.add(role.role())) {
                problems.add(where + ".role: [" + role.role() + "] appears more than once");
            }
            if (role.state() == null) {
                problems.add(where + ".state: is required");
            } else if (!isEnum(TenantUserRoleState.class, role.state())) {
                problems.add(where + ".state: must be one of GRANTED, ADDING, REMOVING");
            } else if (TenantUserRoleState.ADDING.name()
                                                 .equals(role.state())) {
                if (role.grantedBy() != null || role.grantedAt() != null) {
                    problems.add(where + ": a role being added has no grantedBy or grantedAt yet");
                }
            } else if (role.grantedAt() == null) {
                problems.add(where + ".grantedAt: is required on a " + role.state() + " role");
            }
            checkPerson(problems, where + ".grantedBy", role.grantedBy());
            checkInstant(problems, where + ".grantedAt", role.grantedAt());
        }
    }

    private static void checkLastError(List<String> problems, String at, TenantUserSync.LastError lastError) {
        if (lastError == null) {
            return;
        }
        if (lastError.code() == null || lastError.code()
                                                 .isBlank()) {
            problems.add(at + ".lastError.code: is required");
        } else if (lastError.code()
                            .length() > ERROR_CODE_MAX) {
            problems.add(at + ".lastError.code: must be at most " + ERROR_CODE_MAX + " characters");
        }
        if (lastError.message() != null && lastError.message()
                                                    .length() > ERROR_MESSAGE_MAX) {
            problems.add(at + ".lastError.message: must be at most " + ERROR_MESSAGE_MAX + " characters");
        }
    }

    private static void checkPerson(List<String> problems, String field, String value) {
        if (value != null && value.length() > TenantUserRules.EMAIL_MAX) {
            problems.add(field + ": must be at most " + TenantUserRules.EMAIL_MAX + " characters");
        }
    }

    private static void checkInstant(List<String> problems, String field, String value) {
        if (value != null && instant(value) == null) {
            problems.add(field + ": must be an ISO-8601 instant such as 2026-09-30T10:00:00Z");
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

    private static <E extends Enum<E>> boolean isEnum(Class<E> type, String value) {
        for (E constant : type.getEnumConstants()) {
            if (constant.name()
                        .equals(value)) {
                return true;
            }
        }
        return false;
    }
}
