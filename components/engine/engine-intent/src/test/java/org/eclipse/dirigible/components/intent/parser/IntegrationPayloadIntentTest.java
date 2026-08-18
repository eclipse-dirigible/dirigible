/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * The parse-time half of a declared {@code payload:} on an integration: the value forms are checked
 * before anything is generated, and the transport rule (a payload needs a request body) is checked
 * here rather than left to produce a listener that builds an envelope and discards it.
 */
class IntegrationPayloadIntentTest {

    private static final String ENTITIES = """
            name: provisioning
            entities:
              - name: Role
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: UserInvitation
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
                relations:
                  - { name: role, kind: manyToOne, to: Role }
            integrations:
              - name: requestUserAssignment
                event: { onCreate: UserInvitation }
                method: %s
                url: "@config:ASSIGNMENT_URL"
                payload:
            %s
            """;

    @Test
    void aDeclaredPayloadParsesAndIsCarriedOnTheIntegration() {
        IntentModel model = IntentParser.parse(intent("POST", """
                      type: "user.assignment.requested"
                      version: 1
                      messageId: "{uuid}"
                      tenantId: "{tenant}"
                      appId: "@config:APP_ID"
                      email: email
                      role: role.name
                      requestedAt: "{now}"
                """));

        var payload = model.getIntegrations()
                           .get(0)
                           .getPayload();
        assertEquals(8, payload.size());
        assertEquals("user.assignment.requested", payload.get("type"));
        assertEquals(1L, payload.get("version"), "a YAML integer stays integral through the mapping");
        assertEquals("role.name", payload.get("role"));
    }

    @Test
    void aPayloadNeedsAMethodThatCarriesABody() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(intent("GET", "      email: email\n")));
        assertTrue(failure.getMessage()
                          .contains("sends no request body"),
                failure.getMessage());
    }

    @Test
    void anUnknownContextTokenFailsTheParse() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(intent("POST", "      stamp: \"{today}\"\n")));
        assertTrue(failure.getMessage()
                          .contains("neither a context token"),
                failure.getMessage());
    }

    @Test
    void aMultiHopValueFailsTheParse() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(intent("POST", "      owner: role.owner.name\n")));
        assertTrue(failure.getMessage()
                          .contains("walks more than one relation"),
                failure.getMessage());
    }

    private static String intent(String method, String payload) {
        return ENTITIES.formatted(method, payload);
    }
}
