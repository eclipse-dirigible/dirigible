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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TenantUserSyncValidationTest {

    private static final String AT = "2026-09-30T10:00:00Z";

    @Test
    void aValidBodyHasNoProblems() {
        TenantUserSync body = new TenantUserSync(false, 7L,
                List.of(new TenantUserSync.Snapshot("ann@example.com", 7L, "ASSIGNED",
                        List.of(new TenantUserSync.RoleState("Owner", "ADDING", null, null),
                                new TenantUserSync.RoleState("User", "GRANTED", "bob@acme.test", AT)),
                        "bob@acme.test", AT, "bob@acme.test", AT, new TenantUserSync.LastError("LAST_OWNER", null))));

        assertEquals(List.of(), TenantUserSyncValidation.problems(body));
    }

    @Test
    void oneCallNamesEveryOffendingField() {
        List<TenantUserSync.RoleState> tooMany = new ArrayList<>();
        for (int index = 0; index < 21; index++) {
            tooMany.add(new TenantUserSync.RoleState("Role" + index, "GRANTED", null, AT));
        }
        TenantUserSync body = new TenantUserSync(false, 7L, List.of(
                new TenantUserSync.Snapshot("not-an-email", 0L, "LOST",
                        List.of(new TenantUserSync.RoleState("x".repeat(101), "GRANTED", null, AT),
                                new TenantUserSync.RoleState("Owner", "ADDING", "bob@acme.test", null),
                                new TenantUserSync.RoleState("User", "GRANTED", null, null),
                                new TenantUserSync.RoleState("Auditor", "PAUSED", null, null)),
                        null, "yesterday", null, null, new TenantUserSync.LastError(null, "no code")),
                new TenantUserSync.Snapshot("ann@example.com", null, null, tooMany, null, null, null, null, null),
                new TenantUserSync.Snapshot("ANN@example.com", 3L, "INVITED", List.of(), null, null, null, null, null)));

        String all = String.join("\n", TenantUserSyncValidation.problems(body));

        for (String expected : new String[] {"users[0].email: must be an email address", "users[0].revision: must be at least 1",
                "users[0].status: must be one of", "users[0].roles[0].role: must be at most 100", "users[0].roles[1]: a role being added",
                "users[0].roles[2].grantedAt: is required on a GRANTED role", "users[0].roles[3].state: must be one of",
                "users[0].invitedAt: must be an ISO-8601 instant", "users[0].lastError.code: is required", "users[1].revision: is required",
                "users[1].status: is required", "users[1].roles: must hold at most 20",
                "users[2].email: [ann@example.com] appears more than once"}) {
            assertTrue(all.contains(expected), "missing [" + expected + "] in:\n" + all);
        }
    }

    @Test
    void anEmptyListIsOnlyAResync() {
        assertTrue(String.join("", TenantUserSyncValidation.problems(new TenantUserSync(false, 7L, List.of())))
                         .contains("users: must not be empty unless complete is true"));
        assertEquals(List.of(), TenantUserSyncValidation.problems(new TenantUserSync(true, 7L, List.of())));
    }

    @Test
    void theBodyNeedsItsRevisionAndItsUsers() {
        List<String> problems = TenantUserSyncValidation.problems(new TenantUserSync(null, null, null));

        assertTrue(problems.contains("revision: is required"));
        assertTrue(problems.contains("users: is required"));
    }

    @Test
    void aRoleNamedTwiceIsRefused() {
        TenantUserSync body = new TenantUserSync(false, 7L, List
                                                                .of(new TenantUserSync.Snapshot("ann@example.com", 7L, "ASSIGNED",
                                                                        List.of(new TenantUserSync.RoleState("User", "GRANTED", null, AT),
                                                                                new TenantUserSync.RoleState("User", "REMOVING", null, AT)),
                                                                        null, null, null, null, null)));

        assertTrue(String.join("", TenantUserSyncValidation.problems(body))
                         .contains("users[0].roles[1].role: [User] appears more than once"));
    }
}
