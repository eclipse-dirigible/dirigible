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
 * A to-one relation picked on create and fixed afterwards ({@code fixedAfterCreate: true}, issue
 * #7903): what the parser refuses, and the attributes the generated controllers and forms read.
 */
class FixedAfterCreateIntentTest {

    /** A project register and a timesheet placed on one of them. */
    private static final String HEAD = """
            name: timesheets
            entities:
              - name: Project
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Timesheet
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
            """;

    @Test
    void aFixedRelationCarriesTheFlagAndTheDefaultTextOfItsRefusal() {
        Map<String, Object> project = projectProperty("""
                  - { name: Project, kind: manyToOne, to: Project, required: true, fixedAfterCreate: true }
                """);
        assertEquals("true", project.get("fixedAfterCreate"));
        assertEquals("'Project' cannot change once the Timesheet is created", project.get("fixedAfterCreateMessage"));
    }

    @Test
    void anOrdinaryRelationCarriesNeither() {
        Map<String, Object> project = projectProperty("""
                  - { name: Project, kind: manyToOne, to: Project }
                """);
        assertFalse(project.containsKey("fixedAfterCreate"));
        assertFalse(project.containsKey("fixedAfterCreateMessage"));
    }

    /** The flag rides the {@code .edm} too, so the modeler's regeneration keeps it. */
    @Test
    void theFlagRidesTheEdmTwin() {
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(withRelation("""
                  - { name: Project, kind: manyToOne, to: Project, fixedAfterCreate: true }
                """)), "timesheets");
        assertTrue(edm.contains("fixedAfterCreate=\"true\""), edm);
    }

    /** The issue's own case: the employee a timesheet belongs to lives in another model. */
    @Test
    void aCrossModelRelationIsFixedTheSameWay() {
        String yaml = """
                name: timesheets
                uses:
                  - { model: employees, project: employees }
                entities:
                  - name: Timesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Employee, kind: manyToOne, to: Employee, model: employees, required: true, fixedAfterCreate: true }
                """;
        Map<String, Object> employee =
                propertyByName(entity(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "timesheets")), "Employee");
        assertEquals("true", employee.get("fixedAfterCreate"));
        assertEquals("'Employee' cannot change once the Timesheet is created", employee.get("fixedAfterCreateMessage"));
    }

    @Test
    void aCollectionHasNoFkToFix() {
        assertIssue("""
                  - { name: Projects, kind: oneToMany, to: Project, fixedAfterCreate: true }
                """, "declares fixedAfterCreate but only a manyToOne/oneToOne relation has an FK a user picks");
    }

    @Test
    void aCompositionParentIsPresetByTheLayout() {
        assertIssue("""
                  - { name: Project, kind: manyToOne, to: Project, composition: true, fixedAfterCreate: true }
                """, "is a composition parent (preset by the layout) so it cannot declare fixedAfterCreate");
    }

    @Test
    void anUpdateRecomputeAlreadyOwnsTheValue() {
        assertIssue("""
                  - { name: Project, kind: manyToOne, to: Project, fixedAfterCreate: true, calculatedActionOnUpdate: custom.KeepProject }
                """, "declares both fixedAfterCreate and calculatedActionOnUpdate");
    }

    @Test
    void aSubsetAndAManyToManyAreRefused() {
        assertIssue("""
                  - { name: Projects, kind: subset, to: Project, fixedAfterCreate: true }
                """, "is a subset relation so it cannot declare [fixedAfterCreate]");
        assertIssue("""
                  - { name: Projects, kind: manyToMany, to: Project, fixedAfterCreate: true }
                """, "is a manyToMany so it cannot declare [fixedAfterCreate]");
    }

    private static String withRelation(String relation) {
        return HEAD + relation.indent(6);
    }

    private static Map<String, Object> projectProperty(String relation) {
        return propertyByName(entity(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(withRelation(relation)), "timesheets")),
                "Project");
    }

    private static void assertIssue(String relation, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(withRelation(relation)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(expected)),
                "expected an issue containing [" + expected + "], got: " + ex.getIssues());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(Map<String, Object> modelJson) {
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        return entities.stream()
                       .filter(e -> "Timesheet".equals(e.get("name")))
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
