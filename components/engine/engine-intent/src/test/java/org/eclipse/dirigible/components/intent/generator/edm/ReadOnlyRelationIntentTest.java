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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A to-one only the system sets ({@code readOnly: true} on a relation, issue #8662): the storno
 * link of a cash order, written by a posting delegate. What the parser refuses, and the flag the
 * generated controllers and forms read.
 */
class ReadOnlyRelationIntentTest {

    private static final String HEAD = """
            name: cashdesk
            entities:
              - name: Employee
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: CashOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
            """;

    @Test
    void aReadOnlyRelationCarriesTheReadOnlyFlag() {
        Map<String, Object> storno = property("""
                  - { name: StornoOf, kind: manyToOne, to: CashOrder, inlineCreate: false, readOnly: true }
                """, "StornoOf");
        assertEquals("true", storno.get("isReadOnlyProperty"));
        // Still the relation it was: a dropdown keyed by the target, so a set value reads as its label.
        assertEquals("DROPDOWN", storno.get("widgetType"));
        assertEquals("CashOrder", storno.get("relationshipEntityName"));
    }

    @Test
    void anOrdinaryRelationIsNotReadOnly() {
        Map<String, Object> storno = property("""
                  - { name: StornoOf, kind: manyToOne, to: CashOrder }
                """, "StornoOf");
        assertFalse(storno.containsKey("isReadOnlyProperty"));
    }

    /** The flag rides the {@code .edm} too, so the modeler's regeneration keeps it. */
    @Test
    void theFlagRidesTheEdmTwin() {
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(withRelation("""
                  - { name: Employee, kind: oneToOne, to: Employee, readOnly: true }
                """)), "cashdesk");
        assertTrue(edm.contains("isReadOnlyProperty=\"true\""), edm);
    }

    @Test
    void aCrossModelRelationIsReadOnlyTheSameWay() {
        String yaml = """
                name: cashdesk
                uses:
                  - { model: employees, project: employees }
                entities:
                  - name: CashOrder
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: PostedBy, kind: manyToOne, to: Employee, model: employees, readOnly: true }
                """;
        Map<String, Object> postedBy = propertyByName(
                entityNamed(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "cashdesk"), "CashOrder"), "PostedBy");
        assertEquals("true", postedBy.get("isReadOnlyProperty"));
    }

    @Test
    void aCollectionSubsetAndManyToManyAreRefused() {
        assertIssue("""
                  - { name: Orders, kind: oneToMany, to: CashOrder, readOnly: true }
                """, "declares readOnly but only a manyToOne/oneToOne relation has an FK the system sets");
        assertIssue("""
                  - { name: Employees, kind: subset, to: Employee, readOnly: true }
                """, "is a subset relation so it cannot declare [readOnly]");
        assertIssue("""
                  - { name: Employees, kind: manyToMany, to: Employee, readOnly: true }
                """, "is a manyToMany so it cannot declare [readOnly]");
    }

    @Test
    void aValueTheUserOrThePlatformChoosesIsRefused() {
        assertIssue("""
                  - { name: StornoOf, kind: manyToOne, to: CashOrder, readOnly: true, fixedAfterCreate: true }
                """, "declares readOnly (only the system sets it) beside fixedAfterCreate");
        assertIssue("""
                  - { name: StornoOf, kind: manyToOne, to: CashOrder, readOnly: true, calculatedActionOnCreate: custom.Pick }
                """, "beside calculatedAction");
        assertIssue("""
                  - { name: Employee, kind: manyToOne, to: Employee, readOnly: true, composition: true }
                """, "beside composition");
        assertIssue("""
                  - { name: Employee, kind: manyToOne, to: Employee, readOnly: true, personal: true }
                """, "beside personal");
    }

    /**
     * {@code required} beside {@code readOnly} is a NOT NULL column no user create can fill (#8751) -
     * refused, unless only the system creates the entity's rows ({@code systemWritten: true}).
     */
    @Test
    void requiredBesideReadOnlyIsRefusedUnlessTheEntityIsSystemWritten() {
        assertIssue("""
                  - { name: StornoOf, kind: manyToOne, to: CashOrder, readOnly: true, required: true }
                """, "declares both required and readOnly");
        String systemWritten = HEAD.replace("  - name: CashOrder\n", "  - name: CashOrder\n    systemWritten: true\n") + """
                  - { name: StornoOf, kind: manyToOne, to: CashOrder, readOnly: true, required: true }
                """.indent(6);
        Map<String, Object> storno = propertyByName(
                entityNamed(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(systemWritten), "cashdesk"), "CashOrder"),
                "StornoOf");
        assertEquals("true", storno.get("isReadOnlyProperty"));
    }

    private static String withRelation(String relation) {
        return HEAD + relation.indent(6);
    }

    private static Map<String, Object> property(String relation, String name) {
        return propertyByName(
                entityNamed(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(withRelation(relation)), "cashdesk"), "CashOrder"),
                name);
    }

    private static void assertIssue(String relation, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(withRelation(relation)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(expected)),
                "expected an issue containing [" + expected + "], got: " + ex.getIssues());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entityNamed(Map<String, Object> modelJson, String name) {
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        return entities.stream()
                       .filter(e -> name.equals(e.get("name")))
                       .findFirst()
                       .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> propertyByName(Map<String, Object> entity, String name) {
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.get("properties");
        Map<String, Object> property = properties.stream()
                                                 .filter(p -> name.equals(p.get("name")))
                                                 .findFirst()
                                                 .orElse(null);
        assertFalse(property == null, "property [" + name + "] not found");
        return property;
    }
}
