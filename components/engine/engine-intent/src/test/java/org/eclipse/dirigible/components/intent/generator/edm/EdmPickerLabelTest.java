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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.apptest.AppTestIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Issue #7822: a to-one picker over a target with no {@code name} field is labelled by the field
 * the target document is titled by ({@code documentTitle}), not by whichever string field comes
 * first - and the in-model picker, the cross-model picker (resolved from the owner's .model) and
 * the app-test manifest all agree on that label.
 */
class EdmPickerLabelTest {

    private static final String RFQS =
            """
                    name: rfqs
                    entities:
                      - name: RequestForQuotation
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: reference, type: string, length: 100 }
                          - { name: number, type: string, length: 100, number: { series: Request for Quotation, stampOn: create }, documentTitle: true }
                      - name: SupplierQuote
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: amount, type: decimal }
                        relations:
                          - { name: Rfq, kind: manyToOne, to: RequestForQuotation, required: true }
                    """;

    @Test
    void aTargetWithoutANameIsLabelledByItsDocumentTitle() {
        assertEquals("Number", property(edm(RFQS), "SupplierQuote", "Rfq").get("widgetDropDownValue"),
                "the documentTitle number, not the first string field (Reference)");
    }

    @Test
    void aCrossModelPickerOverTheSameTargetResolvesTheSameLabel() {
        Map<String, Map<String, Object>> edm = edm(RFQS);
        assertEquals("Number", CrossModelSupport.labelField(properties(edm, "RequestForQuotation"), "Id"));
    }

    @Test
    void theManifestNamesTheLabelThePickerRenders() {
        assertEquals("Number", manifestLabel(RFQS));
    }

    @Test
    void aStoredLabelExpressionStillWinsEverywhere() {
        String yaml = RFQS.replace("  - name: RequestForQuotation\n", "  - name: RequestForQuotation\n    label: \"{number}\"\n");
        Map<String, Map<String, Object>> edm = edm(yaml);
        assertEquals("Name", property(edm, "SupplierQuote", "Rfq").get("widgetDropDownValue"));
        assertEquals("Name", CrossModelSupport.labelField(properties(edm, "RequestForQuotation"), "Id"));
        assertEquals("Name", manifestLabel(yaml), "the manifest used to say Number while the picker rendered Name");
    }

    @Test
    void aNameFieldStillWinsOverTheDocumentTitle() {
        String yaml = RFQS.replace("      - { name: reference, type: string, length: 100 }\n",
                "      - { name: reference, type: string, length: 100 }\n      - { name: name, type: string, length: 100 }\n");
        Map<String, Map<String, Object>> edm = edm(yaml);
        assertEquals("Name", property(edm, "SupplierQuote", "Rfq").get("widgetDropDownValue"));
        assertEquals("Name", CrossModelSupport.labelField(properties(edm, "RequestForQuotation"), "Id"));
        assertEquals("Name", manifestLabel(yaml));
    }

    @SuppressWarnings("unchecked")
    private static String manifestLabel(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("rfqs", "rfqs", model, edm(yaml));
        Map<String, Object> quote = ((List<Map<String, Object>>) manifest.get("entities")).stream()
                                                                                          .filter(e -> "SupplierQuote".equals(
                                                                                                  e.get("name")))
                                                                                          .findFirst()
                                                                                          .orElseThrow();
        return (String) ((List<Map<String, Object>>) quote.get("relations")).get(0)
                                                                            .get("labelFrom");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> edm(String yaml) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "rfqs");
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        for (Map<String, Object> entity : (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities")) {
            byName.put((String) entity.get("name"), entity);
        }
        return byName;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> properties(Map<String, Map<String, Object>> edm, String entityName) {
        return (List<Map<String, Object>>) edm.get(entityName)
                                              .get("properties");
    }

    private static Map<String, Object> property(Map<String, Map<String, Object>> edm, String entityName, String propertyName) {
        return properties(edm, entityName).stream()
                                          .filter(p -> propertyName.equals(p.get("name")))
                                          .findFirst()
                                          .orElseThrow(
                                                  () -> new AssertionError("no property [" + propertyName + "] on [" + entityName + "]"));
    }
}
