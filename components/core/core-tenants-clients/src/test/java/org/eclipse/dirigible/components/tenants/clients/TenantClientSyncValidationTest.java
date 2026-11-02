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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The input rules of a client registry.
 */
class TenantClientSyncValidationTest {

    @Test
    void aValidRegistryHasNoProblem() {
        assertEquals(List.of(), problems(1L, client("partner", "base-vacations.Vacations.VacationRequestReadOnly")));
        assertEquals(List.of(), problems(1L), "an empty registry revokes every client, which is valid");
    }

    @Test
    void theRevisionAndTheClientsAreRequired() {
        assertEquals(List.of("revision: is required", "clients: is required"),
                TenantClientSyncValidation.problems(new TenantClientSync(null, null)));
        assertEquals(List.of("body: is required"), TenantClientSyncValidation.problems(null));
    }

    @Test
    void noClientIsEverGivenTheOwnerOrARoleOfTheInstance() {
        for (String role : List.of("Owner", "ADMINISTRATOR", "DEVELOPER", "OPERATOR", "TENANT_PROVISIONER")) {
            List<String> problems = problems(1L, client("partner", role));
            assertEquals(1, problems.size(), role);
            assertTrue(problems.get(0)
                               .contains("[" + role + "] is never given to a client"),
                    problems.get(0));
        }
    }

    @Test
    void anActiveClientNeedsARoleARevokedOneDoesNot() {
        assertEquals(List.of("clients[0].roles: an active client must hold at least one role"), problems(1L, client("partner")));
        assertEquals(List.of(),
                problems(1L, new TenantClientSync.Client("partner", "Partner", List.of(), null, null, null, "2026-11-03T09:00:00Z")));
    }

    @Test
    void aClientIdIsUniqueAndHasNoWhitespace() {
        assertEquals(List.of("clients[1].clientId: [partner] appears more than once"),
                problems(1L, client("partner", "A.B.CReadOnly"), client("partner", "A.B.CReadOnly")));
        assertEquals(List.of("clients[0].clientId: must be at most 255 characters, without whitespace"),
                problems(1L, client("part ner", "A.B.CReadOnly")));
    }

    @Test
    void timestampsAreIsoInstants() {
        assertEquals(List.of("clients[0].createdAt: must be an ISO-8601 instant such as 2026-11-02T10:00:00Z"),
                problems(1L, new TenantClientSync.Client("partner", "Partner", List.of("A.B.CReadOnly"), null, "yesterday", null, null)));
    }

    private static List<String> problems(Long revision, TenantClientSync.Client... clients) {
        return TenantClientSyncValidation.problems(new TenantClientSync(revision, Arrays.asList(clients)));
    }

    private static TenantClientSync.Client client(String clientId, String... roles) {
        return new TenantClientSync.Client(clientId, "Partner", List.of(roles), null, null, null, null);
    }
}
