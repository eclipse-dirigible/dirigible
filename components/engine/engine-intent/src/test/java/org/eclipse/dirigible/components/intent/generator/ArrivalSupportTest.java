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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.InboundIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The arrival vocabulary: the {@code accept:} gate as one boolean expression and the {@code map:}
 * projection as a typed conversion per property, including the business-key lookups. These are pure
 * translations, so they are asserted here rather than only through the generated Java.
 */
class ArrivalSupportTest {

    private static final String MODEL = """
            name: provisioning
            entities:
              - name: Tenant
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: tenantId, type: string,  unique: true }
                  - { name: name,     type: string }
              - name: AssignmentRole
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, unique: true }
              - name: TenantUserAssignment
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: messageId, type: string, unique: true }
                  - { name: email,     type: string }
                  - { name: seats,     type: integer }
                  - { name: quota,     type: long }
                  - { name: amount,    type: decimal }
                  - { name: rate,      type: double }
                  - { name: active,    type: boolean }
                  - { name: since,     type: date }
                  - { name: at,        type: timestamp }
                relations:
                  - { name: tenant, kind: manyToOne, to: Tenant }
                  - { name: role,   kind: manyToOne, to: AssignmentRole }
            inbound:
              - name: userAssignments
                source: { queue: "global:codbex.user-assignment-requests" }
                accept: { type: user.assignment.requested, version: 1, urgent: false }
                create: TenantUserAssignment
                map:
                  messageId: messageId
                  email:     email
                  seats:     seatCount
                  quota:     seatQuota
                  amount:    total
                  rate:      unitRate
                  active:    isActive
                  since:     startedOn
                  at:        occurredAt
                  tenant:    { lookup: Tenant,         by: tenantId, from: tenantId }
                  role:      { lookup: AssignmentRole, by: name,     from: role }
              - name: rawAssignments
                path: /raw
                create: TenantUserAssignment
                map:
                  email:  email
                  tenant: tenantKey
              - name: gateOnly
                path: /gate
                accept: { type: user.assignment.requested }
                create: TenantUserAssignment
              - { name: plain, path: /plain, create: TenantUserAssignment }
            """;

    @Test
    void theGateIsOneBooleanExpressionOverTheEnvelope() {
        ArrivalSupport.Plan plan = plan("userAssignments");

        assertEquals(
                "\"user.assignment.requested\".equals(envelope.get(\"type\"))"
                        + " && (envelope.get(\"version\") instanceof Number && ((Number) envelope.get(\"version\")).doubleValue() == 1)"
                        + " && Boolean.FALSE.equals(envelope.get(\"urgent\"))",
                plan.acceptExpression(),
                "a string compares by equals, a number as a double (every number in a parsed envelope is one) and a boolean by identity");
        assertEquals("type=user.assignment.requested, version=1, urgent=false", plan.acceptSummary());
    }

    @Test
    void everyMappedPropertyConvertsToItsOwnType() {
        List<ArrivalSupport.MapField> fields = plan("userAssignments").fields();

        assertEquals(List.of("MessageId", "Email", "Seats", "Quota", "Amount", "Rate", "Active", "Since", "At"), fields.stream()
                                                                                                                       .map(ArrivalSupport.MapField::property)
                                                                                                                       .toList(),
                "the properties keep the authored order and are PascalCased onto the entity");
        assertEquals("\"seatCount\"", field(fields, "Seats").fromLiteral(), "an envelope key reaches the template pre-quoted");
        assertEquals("String.valueOf(raw)", field(fields, "MessageId").expression());
        // Every number in a parsed envelope is a Double, so an integral property goes through
        // BigDecimal - Integer.parseInt would reject the very "1.0" the parse produces.
        assertEquals("Integer.valueOf(new java.math.BigDecimal(String.valueOf(raw)).intValue())", field(fields, "Seats").expression());
        assertEquals("Long.valueOf(new java.math.BigDecimal(String.valueOf(raw)).longValue())", field(fields, "Quota").expression());
        assertEquals("new java.math.BigDecimal(String.valueOf(raw))", field(fields, "Amount").expression());
        // `double` is a DECIMAL column like `decimal`, so its property is a BigDecimal too.
        assertEquals("new java.math.BigDecimal(String.valueOf(raw))", field(fields, "Rate").expression());
        assertEquals("Boolean.valueOf(String.valueOf(raw))", field(fields, "Active").expression());
        assertEquals("org.eclipse.dirigible.sdk.utils.LenientJavaTime.parseLocalDate(String.valueOf(raw))",
                field(fields, "Since").expression());
        assertEquals("org.eclipse.dirigible.sdk.utils.LenientJavaTime.parseInstant(String.valueOf(raw))", field(fields, "At").expression());
    }

    @Test
    void aLookupResolvesTheBusinessKeyAgainstTheRelationsOwnTarget() {
        List<ArrivalSupport.Lookup> lookups = plan("userAssignments").lookups();

        assertEquals(2, lookups.size());
        ArrivalSupport.Lookup tenant = lookups.get(0);
        assertEquals("Tenant", tenant.property());
        assertEquals("lookupTenant", tenant.local());
        assertEquals("Tenant", tenant.targetEntity());
        assertEquals("Tenant", tenant.targetPerspective());
        assertEquals("TenantId", tenant.byProperty());
        assertEquals("\"tenantId\"", tenant.fromLiteral());
        assertEquals("String.valueOf(lookupTenantKey)", tenant.byValueExpression(),
                "the by-value converts to the by-field's type, off the block's own local");
        assertEquals("Id", tenant.targetKeyProperty(), "what is stored is the target's primary key");

        // A setting entity's artifacts live under the shared Settings perspective, so the lookup's
        // import must resolve through it - a settings-unaware walk names a package that does not exist.
        assertEquals("Settings", lookups.get(1)
                                        .targetPerspective());
    }

    @Test
    void aRelationMappedFromAPlainKeyCarriesTheTargetsRawIdentifier() {
        List<ArrivalSupport.MapField> fields = plan("rawAssignments").fields();

        assertTrue(plan("rawAssignments").lookups()
                                         .isEmpty(),
                "a plain envelope key is not a lookup");
        assertEquals("Integer.valueOf(new java.math.BigDecimal(String.valueOf(raw)).intValue())", field(fields, "Tenant").expression(),
                "a raw foreign key converts to the target primary key's own type, not to a string");
    }

    @Test
    void theGlueKeysAreAlwaysPresentSoATemplateCanBranchOnThem() {
        Map<String, Object> mapped = ArrivalSupport.arrivalFields(plan("userAssignments"));
        assertEquals(Boolean.TRUE, mapped.get("hasEnvelope"));
        assertEquals(Boolean.TRUE, mapped.get("hasAccept"));
        assertEquals(Boolean.TRUE, mapped.get("hasMap"));

        Map<String, Object> gate = ArrivalSupport.arrivalFields(plan("gateOnly"));
        assertEquals(Boolean.TRUE, gate.get("hasEnvelope"), "a gate alone still reads the payload as an envelope");
        assertEquals(Boolean.TRUE, gate.get("hasAccept"));
        assertEquals(Boolean.FALSE, gate.get("hasMap"));

        // An arrival declaring neither key must generate byte for byte as it did before the feature.
        assertNull(plan("plain"), "no accept and no map is no plan at all");
        Map<String, Object> plain = ArrivalSupport.arrivalFields(null);
        assertEquals(Boolean.FALSE, plain.get("hasEnvelope"));
        assertEquals(Boolean.FALSE, plain.get("hasAccept"));
        assertEquals(Boolean.FALSE, plain.get("hasMap"));
        assertTrue(((List<?>) plain.get("mapFields")).isEmpty());
        assertTrue(((List<?>) plain.get("lookups")).isEmpty());
        // A Java string literal even with no gate: the template interpolates it into a log call, so a
        // null or a bare empty string would emit source that does not compile.
        assertEquals("\"\"", plain.get("acceptSummaryLiteral"));
    }

    private static ArrivalSupport.MapField field(List<ArrivalSupport.MapField> fields, String property) {
        return fields.stream()
                     .filter(field -> property.equals(field.property()))
                     .findFirst()
                     .orElseThrow(() -> new AssertionError("no mapped property [" + property + "]"));
    }

    private static ArrivalSupport.Plan plan(String name) {
        IntentModel model = IntentParser.parse(MODEL);
        InboundIntent inbound = model.getInbound()
                                     .stream()
                                     .filter(entry -> name.equals(entry.getName()))
                                     .findFirst()
                                     .orElseThrow();
        return ArrivalSupport.plan(inbound, IntentEntities.byName(model)
                                                          .get(inbound.getCreate()),
                IntentEntities.byName(model), IntentEntities.compositionParents(model), model);
    }
}
