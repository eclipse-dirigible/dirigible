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
 * #7903), and a field set on create and fixed afterwards (the same key, #8363): what the parser
 * refuses, and the attributes the generated controllers and forms read.
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

    /** A customer whose code is set on create and fixed afterwards (#8363). */
    private static final String CUSTOMER = """
            name: customers
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100, required: true }
            """;

    @Test
    void aFixedFieldCarriesTheFlagAndTheDefaultTextOfItsRefusal() {
        Map<String, Object> customer = customer(withField("""
                  - { name: code, type: string, length: 20, required: true, unique: true, normalize: upper, fixedAfterCreate: true }
                """));
        Map<String, Object> code = propertyByName(customer, "Code");
        assertEquals("true", code.get("fixedAfterCreate"));
        assertEquals("'Code' cannot change once the Customer is created", code.get("fixedAfterCreateMessage"));
        // Still a caller-written field: an input on the New form, not the read-only details block.
        assertFalse(code.containsKey("isReadOnlyProperty"));
        Map<String, Object> name = propertyByName(customer, "Name");
        assertFalse(name.containsKey("fixedAfterCreate"));
        assertFalse(name.containsKey("fixedAfterCreateMessage"));
    }

    /** The flag rides the {@code .edm} too, so the modeler's regeneration keeps it. */
    @Test
    void aFixedFieldRidesTheEdmTwin() {
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(withField("""
                  - { name: code, type: string, length: 20, fixedAfterCreate: true }
                """)), "customers");
        assertTrue(edm.contains("fixedAfterCreate=\"true\""), edm);
        assertTrue(edm.contains("fixedAfterCreateMessage=\"&apos;Code&apos; cannot change once the Customer is created\""), edm);
    }

    /**
     * A create-time default (#6696) stays caller-editable, and the other caller-facing keys combine
     * with the lock.
     */
    @Test
    void aCreateDefaultMayBeFixed() {
        Map<String, Object> customer = customer(withField("""
                  - { name: code, type: string, length: 20, calculatedActionOnCreate: custom.NextCode, fixedAfterCreate: true }
                  - { name: rate, type: decimal, defaultValue: "1", fixedAfterCreate: true }
                  - { name: since, type: date, fixedAfterCreate: true }
                  - { name: ref, type: string, length: 20, pattern: "^[A-Z]+$", unique: true, fixedAfterCreate: true }
                """));
        assertEquals("true", propertyByName(customer, "Code").get("fixedAfterCreate"));
        assertEquals("true", propertyByName(customer, "Rate").get("fixedAfterCreate"));
        assertEquals("true", propertyByName(customer, "Since").get("fixedAfterCreate"));
        assertEquals("true", propertyByName(customer, "Ref").get("fixedAfterCreate"));
    }

    @Test
    void aFieldNoCallerWritesIsRefused() {
        assertFieldIssue("""
                  - { name: key, type: integer, primaryKey: true, generated: true, fixedAfterCreate: true }
                """, "entity [Customer] field [key] declares fixedAfterCreate but the platform assigns a primary key or a generated field");
        assertFieldIssue("""
                  - { name: token, type: uuid, fixedAfterCreate: true }
                """, "field [token] declares fixedAfterCreate but a uuid is assigned by the platform");
        assertFieldIssue("""
                  - { name: number, type: string, length: 40, number: {}, fixedAfterCreate: true }
                """, "field [number] declares fixedAfterCreate but a number: field is stamped by the platform");
        assertFieldIssue("""
                  - { name: locked, type: string, readOnly: true, fixedAfterCreate: true }
                """, "field [locked] declares fixedAfterCreate but a readOnly field is never written by a caller already");
        assertFieldIssue("""
                  - { name: upper, type: string, calculatedOnCreate: "entity.Name", fixedAfterCreate: true }
                """,
                "field [upper] declares fixedAfterCreate but a calculatedOnCreate, calculatedOnUpdate or calculatedActionOnUpdate field is derived");
        assertFieldIssue("""
                  - { name: touched, type: string, calculatedOnUpdate: "entity.Name", fixedAfterCreate: true }
                """,
                "field [touched] declares fixedAfterCreate but a calculatedOnCreate, calculatedOnUpdate or calculatedActionOnUpdate field is derived");
        assertFieldIssue("""
                  - { name: kept, type: string, calculatedActionOnUpdate: custom.Keep, fixedAfterCreate: true }
                """,
                "field [kept] declares fixedAfterCreate but a calculatedOnCreate, calculatedOnUpdate or calculatedActionOnUpdate field is derived");
        assertFieldIssue("""
                  - { name: total, type: decimal, aggregate: true, fixedAfterCreate: true }
                """, "field [total] declares fixedAfterCreate but an aggregate is a derived total");
        // A status stamp is written by the repository on the status move.
        String stamped = """
                name: customers
                seeds:
                  - name: statuses
                    entity: CustomerStatus
                    rows:
                      - { id: 1, name: DRAFT }
                      - { id: 2, name: APPROVED }
                entities:
                  - name: CustomerStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 50 }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: approvedOn, type: date, stampOn: { status: APPROVED }, fixedAfterCreate: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: CustomerStatus, function: EntityStatus, init: DRAFT }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(stamped));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(
                             "field [approvedOn] declares fixedAfterCreate but a stampOn: field is stamped on a status move")),
                String.valueOf(ex.getIssues()));
    }

    @Test
    void aDependsOnOrTimestampFieldIsRefused() {
        assertFieldIssue("""
                  - { name: signedAt, type: timestamp, fixedAfterCreate: true }
                """, "field [signedAt] declares fixedAfterCreate but a timestamp does not survive the form round trip");
        String yaml = """
                name: customers
                entities:
                  - name: Country
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: code, type: string }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: countryCode, type: string, dependsOn: { relation: Country, valueFrom: code }, fixedAfterCreate: true }
                    relations:
                      - { name: Country, kind: manyToOne, to: Country }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(
                             "field [countryCode] declares fixedAfterCreate but a dependsOn field is rewritten by the form whenever its relation is re-picked")),
                String.valueOf(ex.getIssues()));
    }

    @Test
    void aTranslatableKeyOfAMultilingualEntityIsRefused() {
        String multilingual = """
                name: customers
                entities:
                  - name: Customer
                    multilingual: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 100 }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(multilingual + """
                  - { name: code, type: string, length: 20, fixedAfterCreate: true }
                """.indent(4)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("field [code] declares fixedAfterCreate but it is translatable in a multilingual entity")
                             && i.contains("add translatable: false")),
                String.valueOf(ex.getIssues()));

        Map<String, Object> customer = customer(multilingual + """
                  - { name: code, type: string, length: 20, translatable: false, fixedAfterCreate: true }
                """.indent(4));
        assertEquals("true", propertyByName(customer, "Code").get("fixedAfterCreate"));
    }

    private static String withField(String field) {
        return CUSTOMER + field.indent(4);
    }

    private static Map<String, Object> customer(String yaml) {
        return entityNamed(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "customers"), "Customer");
    }

    private static void assertFieldIssue(String field, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(withField(field)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(expected)),
                "expected an issue containing [" + expected + "], got: " + ex.getIssues());
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

    private static Map<String, Object> entity(Map<String, Object> modelJson) {
        return entityNamed(modelJson, "Timesheet");
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
