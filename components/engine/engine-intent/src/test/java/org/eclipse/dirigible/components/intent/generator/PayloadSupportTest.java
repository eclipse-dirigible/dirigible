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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The value vocabulary of a declared payload: three value forms, four context tokens, and a hard
 * stop at anything richer. These are pure translations, so they are asserted here rather than only
 * through the generated Java.
 */
class PayloadSupportTest {

    private static final String MODEL = """
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
                  - { name: seats, type: integer }
                relations:
                  - { name: role, kind: manyToOne, to: Role }
            """;

    @Test
    void everyValueFormTranslatesToItsExpression() {
        PayloadSupport.Plan plan = plan(payload("""
                type=user.assignment.requested
                messageId={uuid}
                requestedAt={now}
                tenantId={tenant}
                requestedBy={user}
                appId=@config:APP_ID
                email=email
                role=role.name
                """));

        Map<String, String> byKey = expressions(plan);
        assertEquals("\"user.assignment.requested\"", byKey.get("type"), "a dotted word that names no relation is a literal");
        assertEquals("java.util.UUID.randomUUID().toString()", byKey.get("messageId"));
        assertEquals("java.time.Instant.now().toString()", byKey.get("requestedAt"));
        assertEquals("org.eclipse.dirigible.sdk.core.Tenant.getId()", byKey.get("tenantId"));
        assertEquals("org.eclipse.dirigible.sdk.security.User.getName()", byKey.get("requestedBy"));
        assertEquals("Configurations.get(\"APP_ID\")", byKey.get("appId"));
        assertEquals("entity.Email", byKey.get("email"));
        assertEquals("(role == null ? null : role.Name)", byKey.get("role"), "one hop reads the related record the listener loads");

        assertEquals(1, plan.loads()
                            .size(),
                "the one-hop value contributes exactly one relation load");
        NotificationSupport.RelationLoad load = plan.loads()
                                                    .get(0);
        assertEquals("role", load.local());
        assertEquals("Role", load.targetEntity());
        assertEquals("Settings", load.targetPerspective(), "a setting target resolves to the global Settings perspective");
    }

    @Test
    void nonStringScalarsStayLiterals() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", 1);
        payload.put("rate", 1.50);
        payload.put("draft", false);
        payload.put("note", null);

        Map<String, String> byKey = expressions(plan(payload));
        assertEquals("1", byKey.get("version"));
        assertEquals("new java.math.BigDecimal(\"1.5\")", byKey.get("rate"), "a fractional literal keeps its authored precision");
        assertEquals("false", byKey.get("draft"));
        assertEquals("null", byKey.get("note"));
    }

    @Test
    void aBracedReferenceIsCheckedWhereABareWordIsALiteral() {
        assertEquals("entity.Email", expressions(plan(payload("to={email}"))).get("to"));
        // The same word unbraced also resolves - it IS a field. A word that is not stays a literal,
        // which is the only way a payload can carry a one-word constant.
        assertEquals("\"draft\"", expressions(plan(payload("state=draft"))).get("state"));
    }

    @Test
    void anUnknownContextTokenIsRejectedNamingTheClosedSet() {
        List<String> issues = validate(payload("stamp={today}"));
        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.get(0)
                         .contains("references [today], which is neither a context token")
                && issues.get(0)
                         .contains("{uuid}, {now}, {tenant}, {user}"),
                issues.toString());
    }

    @Test
    void aMultiHopReferenceIsRejected() {
        List<String> issues = validate(payload("owner=role.owner.name"));
        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.get(0)
                         .contains("walks more than one relation"),
                issues.toString());
    }

    @Test
    void aOneHopOntoAFieldTheTargetDoesNotHaveIsRejected() {
        List<String> issues = validate(payload("role=role.code"));
        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.get(0)
                         .contains("[code] is not a field of [Role]"),
                issues.toString());
    }

    @Test
    void aBracedNonReferenceIsRejectedRatherThanShippedEmpty() {
        List<String> issues = validate(payload("who={requester}"));
        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.get(0)
                         .contains("nor a field or a to-one relation of [UserInvitation]"),
                issues.toString());
    }

    @Test
    void interpolatedTextAndNestedValuesAreRefused() {
        List<String> mixed = validate(payload("subject=Order {id} shipped"));
        assertEquals(1, mixed.size(), mixed.toString());
        assertTrue(mixed.get(0)
                        .contains("mixes braces into text"),
                mixed.toString());

        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("actor", Map.of("id", "email"));
        List<String> issues = validate(nested);
        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.get(0)
                         .contains("must be a scalar"),
                issues.toString());
    }

    @Test
    void anUnresolvableValueFailsGenerationRatherThanFallingBackToTheRecord() {
        // Braced, so the parser would have caught it; the generator still refuses to build a payload
        // with a hole in it, which is what makes the drop visible instead of the contract changing.
        assertThrows(IllegalArgumentException.class, () -> plan(payload("who={role.code}")));
    }

    private static Map<String, String> expressions(PayloadSupport.Plan plan) {
        Map<String, String> byKey = new LinkedHashMap<>();
        for (PayloadSupport.Entry entry : plan.entries()) {
            byKey.put(entry.key(), entry.expression());
        }
        return byKey;
    }

    private static PayloadSupport.Plan plan(Map<String, Object> payload) {
        IntentModel model = IntentParser.parse(MODEL);
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        return PayloadSupport.plan(payload, byName.get("UserInvitation"), byName, IntentEntities.compositionParents(model), null);
    }

    private static List<String> validate(Map<String, Object> payload) {
        IntentModel model = IntentParser.parse(MODEL);
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        List<String> issues = new ArrayList<>();
        PayloadSupport.validate(payload, byName.get("UserInvitation"), byName, "integration [x]", issues);
        return issues;
    }

    /**
     * {@code key=value} lines into an ordered payload map - YAML would strip the quoting under test.
     */
    private static Map<String, Object> payload(String lines) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (String line : lines.split("\n")) {
            if (!line.isBlank()) {
                int split = line.indexOf('=');
                payload.put(line.substring(0, split), line.substring(split + 1));
            }
        }
        return payload;
    }
}
