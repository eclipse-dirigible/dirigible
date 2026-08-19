/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code integrations} glue entries a declared {@code payload:} contributes: the ordered
 * key/expression list the template renders, plus the one-hop relation loads it needs - the same
 * shape a notification carries, so the generated listener resolves a related record exactly once.
 * An integration that declares no payload keeps forwarding the record and carries neither.
 */
class GlueIntegrationPayloadTest {

    private static final String YAML = """
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
                method: POST
                url: "@config:ASSIGNMENT_URL"
                payload:
                  type: "user.assignment.requested"
                  version: 1
                  messageId: "{uuid}"
                  tenantId: "{tenant}"
                  email: email
                  role: role.name
              - name: pingCatalog
                event: { onUpdate: UserInvitation }
                method: POST
                url: "https://hooks.example.com/invitations"
            """;

    @SuppressWarnings("unchecked")
    @Test
    void aDeclaredPayloadBecomesAnOrderedKeyExpressionListWithItsRelationLoads() {
        Map<String, Object> integration = integration("requestUserAssignment");

        assertEquals(true, integration.get("hasPayload"));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) integration.get("payloadFields");
        assertEquals(List.of("type", "version", "messageId", "tenantId", "email", "role"), fields.stream()
                                                                                                 .map(field -> field.get("key"))
                                                                                                 .toList(),
                "the envelope keeps its authored order - a contract is read in the order it was written");
        assertEquals("\"type\"", fields.get(0)
                                       .get("keyLiteral"),
                "the key is pre-quoted so the template never decides how it is escaped");
        assertEquals("\"user.assignment.requested\"", fields.get(0)
                                                            .get("expression"));
        assertEquals("1", fields.get(1)
                                .get("expression"));
        assertEquals("java.util.UUID.randomUUID().toString()", fields.get(2)
                                                                     .get("expression"));
        assertEquals("org.eclipse.dirigible.sdk.core.Tenant.getId()", fields.get(3)
                                                                            .get("expression"));
        assertEquals("entity.Email", fields.get(4)
                                           .get("expression"));
        assertEquals("(role == null ? null : role.Name)", fields.get(5)
                                                                .get("expression"));

        List<Map<String, Object>> loads = (List<Map<String, Object>>) integration.get("relationLoads");
        assertEquals(1, loads.size(), "loads: " + loads);
        assertEquals("role", loads.get(0)
                                  .get("local"));
        assertEquals("Role", loads.get(0)
                                  .get("targetEntity"));
        assertEquals("Settings", loads.get(0)
                                      .get("targetPerspective"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void anIntegrationWithoutAPayloadStillForwardsTheRecord() {
        Map<String, Object> integration = integration("pingCatalog");

        assertEquals(false, integration.get("hasPayload"));
        assertTrue(((List<Map<String, Object>>) integration.get("payloadFields")).isEmpty());
        assertTrue(((List<Map<String, Object>>) integration.get("relationLoads")).isEmpty());
        assertEquals("\"https://hooks.example.com/invitations\"", integration.get("urlExpression"));
    }

    private static Map<String, Object> integration(String name) {
        IntentModel model = IntentParser.parse(YAML);
        for (Map<String, Object> entry : GlueIntentGenerator.buildIntegrationsForTest(model)) {
            if (name.equals(entry.get("name"))) {
                return entry;
            }
        }
        throw new AssertionError("no integration [" + name + "] in the glue");
    }
}
