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

import org.eclipse.dirigible.components.intent.model.InboundIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * The parse-time half of mapping on arrival - the {@code accept:} gate and the {@code map:}
 * projection, including the business-key lookups that fill a record's relations.
 *
 * <p>
 * The rule worth the most here is the uniqueness of a lookup's {@code by:}: a lookup that could
 * match several rows would silently pick one, which is a worse outcome than failing, so it is
 * refused when the intent is read rather than discovered in production.
 */
class InboundMappingIntentTest {

    private static final String ENTITIES = """
            name: provisioning
            entities:
              - name: Tenant
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: tenantId, type: string, unique: true }
                  - { name: name,     type: string }
                  - { name: openedOn, type: date, unique: true }
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
                relations:
                  - { name: tenant, kind: manyToOne, to: Tenant }
                  - { name: role,   kind: manyToOne, to: AssignmentRole }
            """;

    @Test
    void anArrivalParsesWithItsGateAndItsMappedLookups() {
        IntentModel model = IntentParser.parse(ENTITIES + """
                inbound:
                  - name: userAssignments
                    source: { queue: "global:codbex.user-assignment-requests" }
                    accept: { type: user.assignment.requested, version: 1 }
                    create: TenantUserAssignment
                    map:
                      messageId: messageId
                      email:     email
                      tenant:    { lookup: Tenant,         by: tenantId, from: tenantId }
                      role:      { lookup: AssignmentRole, by: name,     from: role }
                """);

        InboundIntent arrival = model.getInbound()
                                     .get(0);
        assertEquals("user.assignment.requested", arrival.getAccept()
                                                         .get("type"));
        assertEquals(4, arrival.getMap()
                               .size());
        assertEquals("messageId", arrival.getMap()
                                         .get("messageId"));
        assertTrue(arrival.getMap()
                          .get("tenant") instanceof java.util.Map,
                "a lookup value stays a map the generator reads");
    }

    /** The gate and the map are about the payload, so they are valid on all three arrivals. */
    @Test
    void aWebhookAndAPolledFolderTakeTheSameMapping() {
        IntentModel model = IntentParser.parse(ENTITIES + """
                inbound:
                  - name: assignmentHook
                    path: /assignments
                    accept: { type: user.assignment.requested }
                    create: TenantUserAssignment
                    map:
                      email:  email
                      tenant: { lookup: Tenant, by: tenantId, from: tenantId }
                  - name: assignmentDrop
                    source: { folder: target/inbox, cron: "0/5 * * * * ?" }
                    accept: { type: user.assignment.requested }
                    create: TenantUserAssignment
                    map:
                      email:  email
                      tenant: { lookup: Tenant, by: tenantId, from: tenantId }
                """);

        assertEquals(2, model.getInbound()
                             .size());
    }

    @Test
    void aLookupOnANonUniqueFieldFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      tenant: { lookup: Tenant, by: name, from: tenantName }
                """, "which is not unique on [Tenant]");
    }

    /**
     * The primary key is unique by construction, so it needs no flag - lookup by id is the strict case.
     */
    @Test
    void aLookupOnThePrimaryKeyIsAccepted() {
        IntentParser.parse(ENTITIES + """
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      tenant: { lookup: Tenant, by: id, from: tenantId }
                """);
    }

    @Test
    void aLookupOnATemporalFieldFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      tenant: { lookup: Tenant, by: openedOn, from: openedOn }
                """, "a business key is a string or an integer field");
    }

    @Test
    void aLookupOfAnEntityTheRelationDoesNotTargetFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      tenant: { lookup: AssignmentRole, by: name, from: role }
                """, "looks up [AssignmentRole] but [tenant] relates to [Tenant]");
    }

    @Test
    void aLookupOnAPlainFieldFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      email: { lookup: Tenant, by: tenantId, from: tenantId }
                """, "which is not a to-one relation");
    }

    @Test
    void anIncompleteLookupFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      tenant: { lookup: Tenant, by: tenantId }
                """, "has no from");
    }

    @Test
    void anUnknownKeyInsideALookupFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      tenant: { lookup: Tenant, by: tenantId, form: tenantId }
                """, "declares unknown key [form]");
    }

    @Test
    void aMapKeyThatIsNotAPropertyFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      emial: email
                """, "map [emial] is not a field or a to-one relation of [TenantUserAssignment]");
    }

    /** A generated key filled from the envelope is a record that cannot be inserted. */
    @Test
    void mappingThePrimaryKeyFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      id: externalId
                """, "fills the primary key, which is generated on insert");
    }

    @Test
    void aNonScalarAcceptValueFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    accept: { type: [a, b] }
                    create: TenantUserAssignment
                """, "accept [type] must be a scalar");
    }

    @Test
    void anEmptyAcceptValueFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    accept: { type: }
                    create: TenantUserAssignment
                """, "accept [type] has no value to gate on");
    }

    /**
     * Gson omits a null value, so a valueless key would leave an EMPTY gate - every message accepted -
     * or a field nobody fills, with the author's key gone from the model entirely. Both are caught on
     * the raw tree, while the key still exists.
     */
    @Test
    void aValuelessMapKeyFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    map:
                      email:
                """, "map [email] has no value - name the envelope key it is filled from");
    }

    @Test
    void anUnknownKeyOnAnArrivalFailsTheParse() {
        assertFails("""
                inbound:
                  - name: userAssignments
                    source: { queue: assignments }
                    create: TenantUserAssignment
                    accepts: { type: x }
                """, "unknown key [accepts]");
    }

    private static void assertFails(String inbound, String expected) {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(ENTITIES + inbound));
        assertTrue(failure.getMessage()
                          .contains(expected),
                failure.getMessage());
    }
}
