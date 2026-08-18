/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A composite business key has to arrive at the layer that creates it: the {@code .model} carries
 * the constraint with its physical columns, from which the schema template emits it and the REST
 * controller recovers the authored message (#6763).
 */
class EdmEntityUniqueTest {

    private static final String PROVISIONING = """
            name: provisioning
            entities:
              - name: Tenant
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Application
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: TenantApplication
                unique:
                  - { fields: [tenant, application], message: "This application is already provisioned for the tenant" }
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: plan, type: string }
                relations:
                  - { name: tenant, kind: manyToOne, to: Tenant, required: true }
                  - { name: application, kind: manyToOne, to: Application, required: true }
            """;

    @Test
    void theConstraintCarriesItsPhysicalColumnsInTheDeclaredOrder() {
        Map<String, Object> constraint = onlyConstraint(PROVISIONING);

        assertEquals("TenantApplication_Tenant_Application", constraint.get("name"),
                "the same descriptive concatenation the foreign keys use, so a regenerated model names it identically");
        assertEquals(List.of("TENANT_APPLICATION_TENANT", "TENANT_APPLICATION_APPLICATION"), columnNames(constraint),
                "a to-one relation contributes its foreign-key column, and the order is the authored one");
        assertEquals("TENANT_APPLICATION_TENANT,TENANT_APPLICATION_APPLICATION", constraint.get("columnsCsv"));
        assertEquals("This application is already provisioned for the tenant", constraint.get("message"));
    }

    @Test
    void aFieldAndARelationShareTheSameColumnForm() {
        Map<String, Object> constraint = onlyConstraint(PROVISIONING.replace("[tenant, application]", "[tenant, plan]"));

        assertEquals(List.of("TENANT_APPLICATION_TENANT", "TENANT_APPLICATION_PLAN"), columnNames(constraint));
    }

    @Test
    void anUnauthoredMessageStillReadsAsSomethingAUserCanActOn() {
        Map<String, Object> constraint =
                onlyConstraint(PROVISIONING.replace(", message: \"This application is already provisioned for the tenant\"", ""));

        assertEquals("A tenant application with the same tenant and application already exists", constraint.get("message"),
                "a caller must be told what collided even when the author said nothing");
    }

    @Test
    void anEntityWithoutAKeyCarriesNothing() {
        assertNull(entity(PROVISIONING, "Tenant").get("uniqueConstraints"),
                "the key is absent, not an empty list the schema template would have to guard twice");
    }

    /**
     * The {@code .edm} twin is scalar: an editor that met a stringified Java collection there would
     * render it as an attribute value and write it back on the next save.
     */
    @Test
    void theStructuredValueStaysOutOfTheEntityAttributes() {
        String xml = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(PROVISIONING), "provisioning");

        assertTrue(xml.contains("<entity"), "sanity: the XML twin was rendered");
        assertFalse(xml.contains("uniqueConstraints="), "a structured value is never an entity attribute");
        assertFalse(xml.contains("columnsCsv"), "and nothing of it leaks under another name");
    }

    /**
     * The key still has to reach the {@code .edm}, as its own top-level section - otherwise opening an
     * intent-generated model in the EDM modeler and saving it for any unrelated reason regenerates the
     * {@code .model} from an {@code .edm} that never mentioned the key, and the constraint is gone with
     * nothing reported.
     */
    @Test
    void theKeyIsCarriedAsATopLevelSectionOverPropertyNames() {
        String xml = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(PROVISIONING), "provisioning");

        assertTrue(xml.contains("<constraints>"), "the .edm must carry the keys the modeler round-trips");
        assertTrue(
                xml.contains("<uniqueKey><entity>TenantApplication</entity>" + "<name>TenantApplication_Tenant_Application</name>"
                        + "<properties>Tenant,Application</properties>"
                        + "<message>This application is already provisioned for the tenant</message></uniqueKey>"),
                "the section names PROPERTIES, not columns - so a later dataName change follows the key: " + xml);
    }

    @Test
    void aModelWithoutKeysGrowsNoSection() {
        String withoutKeys = PROVISIONING.replace("    unique:\n", "")
                                         .replace(
                                                 "      - { fields: [tenant, application], message: \"This application is already provisioned for the tenant\" }\n",
                                                 "");

        String xml = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(withoutKeys), "provisioning");

        assertFalse(xml.contains("<constraints>"), "an .edm without keys stays byte-identical to one generated before they existed");
    }

    private static Map<String, Object> onlyConstraint(String yaml) {
        List<Map<String, Object>> constraints = constraints(yaml);
        assertNotNull(constraints, "the entity must carry its declared key");
        assertEquals(1, constraints.size());
        return constraints.get(0);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> constraints(String yaml) {
        return (List<Map<String, Object>>) entity(yaml, "TenantApplication").get("uniqueConstraints");
    }

    @SuppressWarnings("unchecked")
    private static List<String> columnNames(Map<String, Object> constraint) {
        return ((List<Map<String, Object>>) constraint.get("columns")).stream()
                                                                      .map(column -> String.valueOf(column.get("name")))
                                                                      .toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml, String name) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "provisioning");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }
}
