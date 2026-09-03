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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A field's authored label reaches the {@code .model} as the property's own {@code widgetLabel} -
 * the attribute every generated surface renders and the catalog is seeded from - and its
 * country-scoped variants ride alongside it as a structured value (#6424).
 */
class EdmFieldLabelTest {

    private static final String PAYROLL = """
            name: payroll
            entities:
              - name: Employee
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: nationalId
                    type: string
                    label: National ID
                    countryLabels:
                      bg: ЕГН
                      DE: Steuer-ID
                  - { name: name, type: string }
            """;

    @Test
    void theAuthoredLabelIsThePropertysWidgetLabel() {
        assertEquals("National ID", property(PAYROLL, "NationalId").get("widgetLabel"));
    }

    @Test
    void aFieldWithoutALabelCarriesNoneAtAll() {
        assertNull(property(PAYROLL, "Name").get("widgetLabel"),
                "the generation pipeline derives the humanized name, so a model without labels stays byte-identical");
    }

    @Test
    void theCountryVariantsAreCanonicalizedToUpperCaseCodes() {
        Map<String, Object> variants = variants(PAYROLL, "NationalId");

        assertEquals("ЕГН", variants.get("BG"), "the runtime compares against the configured country in upper case");
        assertEquals("Steuer-ID", variants.get("DE"));
    }

    @Test
    void aFieldWithoutVariantsCarriesNone() {
        assertNull(property(PAYROLL, "Name").get("widgetCountryLabels"));
    }

    /**
     * The {@code .edm} twin is scalar: a stringified Java map met there would be rendered as an
     * attribute value and written back on the modeler's next save.
     */
    @Test
    void theVariantsStayOutOfTheEdmXmlWhileTheLabelDoesNot() {
        String xml = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(PAYROLL), "payroll");

        assertTrue(xml.contains("widgetLabel=\"National ID\""), "the label is a plain modeler attribute: " + xml);
        assertFalse(xml.contains("widgetCountryLabels"), "a structured value is never a property attribute");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> variants(String yaml, String propertyName) {
        return (Map<String, Object>) property(yaml, propertyName).get("widgetCountryLabels");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(String yaml, String propertyName) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "payroll");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .flatMap(entity -> ((List<Map<String, Object>>) entity.get("properties")).stream())
                       .filter(property -> propertyName.equals(property.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no property [" + propertyName + "]"));
    }
}
