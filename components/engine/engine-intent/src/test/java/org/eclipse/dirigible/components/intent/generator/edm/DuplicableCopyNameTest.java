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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A Duplicate names its copy from the source (issue #8663): a {@code {field}} template in
 * {@code duplicable.defaults}, the {@code unique} fields the copy is numbered against, and the
 * {@code focus} input the copy opens with.
 */
class DuplicableCopyNameTest {

    private static String model(String duplicable) {
        return """
                name: bills
                entities:
                  - name: Company
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: BillTemplate
                    function: Document
                """ + duplicable.indent(4) + """
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 100 }
                      - { name: code, type: string, length: 20 }
                      - { name: amount, type: decimal }
                      - { name: active, type: boolean }
                    relations:
                      - { name: Company, kind: manyToOne, to: Company }
                  - name: BillTemplateItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: BillTemplate, kind: manyToOne, to: BillTemplate, composition: true, required: true }
                """;
    }

    @Test
    @SuppressWarnings("unchecked")
    void aTemplateIsReadFromTheSourceAndNumberedAgainstTheUniqueFields() {
        Map<String, Object> bill = bill("""
                duplicable:
                  defaults: { name: "{name} (copy)", code: "{code}-{name}", active: false }
                  unique: [name, Company]
                  focus: name
                """);
        List<Map<String, Object>> defaults = (List<Map<String, Object>>) bill.get("duplicateDefaults");
        assertEquals("template", defaults.get(0)
                                         .get("shape"));
        assertEquals("String(src['Name'] ?? '') + \" (copy)\"", defaults.get(0)
                                                                        .get("js"));
        assertEquals("true", defaults.get(0)
                                     .get("unique"),
                "the templated name is the value the probe numbers");
        assertEquals("String(src['Code'] ?? '') + \"-\" + String(src['Name'] ?? '')", defaults.get(1)
                                                                                              .get("js"));
        assertNull(defaults.get(1)
                           .get("unique"),
                "a template outside unique is not numbered");
        assertEquals("literal", defaults.get(2)
                                        .get("shape"));
        assertEquals("Name,Company", bill.get("duplicateUnique"));
        assertEquals("Name", bill.get("duplicateFocus"));
    }

    /** Both new keys are scalars, so the .edm carries them as it is (#6826). */
    @Test
    void theProbeAndTheFocusRideTheEdm() {
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(model("""
                duplicable:
                  defaults: { name: "{name} (copy)" }
                  unique: [name, Company]
                  focus: name
                """)), "bills");
        assertTrue(edm.contains("duplicateUnique=\"Name,Company\""), edm);
        assertTrue(edm.contains("duplicateFocus=\"Name\""), edm);
    }

    /** A model without the new keys generates exactly what it did. */
    @Test
    void aLiteralDefaultIsUnchanged() {
        Map<String, Object> bill = bill("""
                duplicable:
                  defaults: { name: "Copy" }
                """);
        assertNull(bill.get("duplicateUnique"));
        assertNull(bill.get("duplicateFocus"));
    }

    @Test
    void anUnknownPlaceholderIsRefused() {
        assertIssue("""
                duplicable:
                  defaults: { name: "{title} (copy)" }
                """, "whose {title} is not a field of entity [BillTemplate]");
    }

    @Test
    void aTemplateOnANonTextFieldIsRefused() {
        assertIssue("""
                duplicable:
                  defaults: { amount: "{amount}" }
                """, "a template builds text, so it is only a value for a string / text field");
    }

    @Test
    void uniqueNeedsATemplatedField() {
        assertIssue("""
                duplicable:
                  defaults: { name: "Copy" }
                  unique: [name, Company]
                """, ".unique names no field that defaults: assigns a {field} template");
        assertIssue("""
                duplicable:
                  defaults: { name: "{name} (copy)" }
                  unique: [name, Owner]
                """, ".unique names [Owner] which is not a field or a to-one relation");
    }

    @Test
    void focusNeedsAnInput() {
        assertIssue("""
                duplicable:
                  focus: title
                """, ".focus names [title] which is not a field of entity [BillTemplate]");
        assertIssue("""
                duplicable:
                  focus: id
                """, ".focus names [id], which the copy's form renders no input for");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> bill(String duplicable) {
        Map<String, Object> modelJson = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(model(duplicable)), "bills");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        return entities.stream()
                       .filter(e -> "BillTemplate".equals(e.get("name")))
                       .findFirst()
                       .orElseThrow();
    }

    private static void assertIssue(String duplicable, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(model(duplicable)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(expected)),
                "expected an issue containing [" + expected + "], got: " + ex.getIssues());
    }
}
