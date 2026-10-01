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
 * A to-one relation's {@code pickable:} rule (issue #7496): what the parser refuses, and the
 * {@code widgetPickable} attribute the generated pickers hand to the shared runtime.
 */
class PickableIntentTest {

    /** A customer register with the columns a completeness rule reads, and an invoice picking one. */
    private static final String HEAD = """
            name: sales
            entities:
              - name: Country
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: registrationNumber, type: string }
                  - { name: address, type: string }
                  - { name: active, type: boolean }
                  - { name: kind, type: integer }
                  - { name: creditLimit, type: decimal }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
            """;

    @Test
    void aCompletenessRuleIsEmittedAsTheRuleThePickerEvaluates() {
        Map<String, Object> customer = customerProperty("""
                  - name: Customer
                    kind: manyToOne
                    to: Customer
                    pickable:
                      when: [registrationNumber != null, address != null, Country != null, active == true, kind != 2]
                      message: The customer's registration data is incomplete
                """);
        assertEquals("{\"when\":[{\"property\":\"RegistrationNumber\",\"op\":\"present\"},"
                + "{\"property\":\"Address\",\"op\":\"present\"},{\"property\":\"Country\",\"op\":\"present\"},"
                + "{\"property\":\"Active\",\"op\":\"eq\",\"value\":\"true\"},{\"property\":\"Kind\",\"op\":\"ne\",\"value\":\"2\"}],"
                + "\"hide\":false,\"message\":\"The customer's registration data is incomplete\",\"messageKey\":\"Invoice_Customer_pickable\"}",
                customer.get("widgetPickable"));
    }

    /** The reviewer's point: a marked row must say why - absent a message, the rule itself is shown. */
    @Test
    void theRuleIsTheMessageWhenNoneIsAuthoredAndHideIsCarried() {
        Map<String, Object> customer = customerProperty("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: "registrationNumber != null", else: hide } }
                """);
        assertEquals(
                "{\"when\":[{\"property\":\"RegistrationNumber\",\"op\":\"present\"}],\"hide\":true,"
                        + "\"message\":\"registrationNumber != null\",\"messageKey\":\"Invoice_Customer_pickable\"}",
                customer.get("widgetPickable"));
    }

    @Test
    void aRelationWithoutARuleCarriesNoAttribute() {
        Map<String, Object> customer = customerProperty("""
                  - { name: Customer, kind: manyToOne, to: Customer }
                """);
        assertFalse(customer.containsKey("widgetPickable"));
    }

    /** The rule rides the {@code .edm} too, escaped as an attribute value, so the modeler keeps it. */
    @Test
    void theRuleRidesTheEdmTwin() {
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(withRelation("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: "registrationNumber != null" } }
                """)), "sales");
        assertTrue(edm.contains("widgetPickable=\"{&quot;when&quot;:[{&quot;property&quot;:&quot;RegistrationNumber&quot;"), edm);
    }

    /** A cross-model target's properties are checked at generation, against the owner's model. */
    @Test
    void aCrossModelTargetIsHeldToTheGrammarAtParse() {
        String yaml = """
                name: invoices
                uses:
                  - { model: customers, project: customers }
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, model: customers, pickable: { when: [vatNumber != null] } }
                """;
        Map<String, Object> customer =
                propertyByName(entity(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "invoices")), "Customer");
        assertEquals(
                "{\"when\":[{\"property\":\"VatNumber\",\"op\":\"present\"}],\"hide\":false,\"message\":\"vatNumber != null\",\"messageKey\":\"Invoice_Customer_pickable\"}",
                customer.get("widgetPickable"));
    }

    @Test
    void anUnknownTargetPropertyIsRefused() {
        assertIssue("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [vatNumber != null] } }
                """, "reads [vatNumber], which is not a field or to-one relation of [Customer]");
    }

    @Test
    void aValueComparisonIsTypedAgainstTheTargetProperty() {
        assertIssue("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [active == yes] } }
                """, "compares [active], a [boolean], with [yes], which is not a value of that type");
        assertIssue("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [creditLimit == 0] } }
                """, "which is a [decimal] field - a value comparison is on a string, an integer or a boolean");
    }

    /** Presence is testable on any field - a decimal is complete or not exactly like a string. */
    @Test
    void presenceIsTestableOnAnyFieldType() {
        Map<String, Object> customer = customerProperty("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [creditLimit != null] } }
                """);
        assertTrue(String.valueOf(customer.get("widgetPickable"))
                         .contains("{\"property\":\"CreditLimit\",\"op\":\"present\"}"));
    }

    /** The example in the issue wrote `&&`; the DSL's AND is the list, and the refusal says so. */
    @Test
    void anAndWrittenInOneStringIsRefusedTowardsTheList() {
        assertIssue("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: "registrationNumber != null && address != null" } }
                """, "several conditions are written as a list, which is their AND");
    }

    @Test
    void aPathIsRefused() {
        assertIssue("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [Country.name != null] } }
                """, "names a path - the rule reads the Customer row itself");
    }

    @Test
    void anUnknownElseIsRefused() {
        assertIssue("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [address != null], else: grey } }
                """, "else [grey] must be `mark`");
    }

    @Test
    void aRuleWithoutAConditionIsRefused() {
        assertIssue("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { else: hide } }
                """, "pickable requires `when`");
    }

    @Test
    void anUnknownKeyInsideTheRuleIsRefused() {
        assertIssue("""
                  - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [address != null], otherwise: hide } }
                """, "unknown key [otherwise]");
    }

    @Test
    void onlyAUserPickedToOneTakesARule() {
        assertIssue("""
                  - { name: Customers, kind: subset, to: Customer, pickable: { when: [address != null] } }
                """, "is a subset relation so it cannot declare [pickable]");
        assertIssue(
                """
                          - { name: Customer, kind: manyToOne, to: Customer, composition: true, required: true, pickable: { when: [address != null] } }
                        """,
                "is declared on a composition parent");
    }

    /**
     * The invoice's relation list with this relation in it - a text block's indentation is stripped.
     */
    private static String withRelation(String relation) {
        return HEAD + relation.indent(6);
    }

    private static Map<String, Object> customerProperty(String relation) {
        return propertyByName(entity(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(withRelation(relation)), "sales")),
                "Customer");
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
                       .filter(e -> "Invoice".equals(e.get("name")))
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
