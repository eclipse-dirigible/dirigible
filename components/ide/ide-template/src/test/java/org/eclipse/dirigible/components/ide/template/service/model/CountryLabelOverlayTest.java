/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The country label overlay a generated application carries in its configuration: the labels that
 * are resolved from the tenant's country rather than the reader's language (#6424). It is keyed by
 * the very translation key the generated views bind, so the runtime resolves one with a single
 * exact lookup.
 */
class CountryLabelOverlayTest {

    private static final String TEMPLATE = "template-application-ui-harmonia-java/template/template.js";

    @Test
    void aDeclaredVariantIsKeyedByCountryAndThenByTheLabelsTranslationKey() {
        Map<String, Object> parameters = parameters();

        ModelTemplateAdapters.prepare(TEMPLATE, model("""
                    "widgetCountryLabels": { "BG": "ЕГН", "DE": "Steuer-ID" }
                """), parameters);

        assertEquals(
                "{\"BG\":{\"payroll:edm-model.t.EMPLOYEE_NATIONAL_ID\":\"ЕГН\"},"
                        + "\"DE\":{\"payroll:edm-model.t.EMPLOYEE_NATIONAL_ID\":\"Steuer-ID\"}}",
                parameters.get("countryLabels"),
                "the key is the project namespace, the model's catalog prefix and the property's data name - what T() is called with");
    }

    @Test
    void anApplicationThatDeclaresNoneCarriesAnEmptyOverlay() {
        Map<String, Object> parameters = parameters();

        ModelTemplateAdapters.prepare(TEMPLATE, model(""), parameters);

        assertEquals("{}", parameters.get("countryLabels"), "the configuration is always valid JavaScript, feature used or not");
    }

    private static String model(String propertyExtras) {
        return """
                {
                  "model": {
                    "entities": [
                      {
                        "name": "Employee",
                        "dataName": "EMPLOYEE",
                        "properties": [
                          {
                            "name": "NationalId",
                            "dataName": "EMPLOYEE_NATIONAL_ID",
                            "dataType": "VARCHAR",
                            "widgetLabel": "National ID",
                        %s
                          }
                        ]
                      }
                    ]
                  }
                }
                """.formatted(propertyExtras.isBlank() ? "        \"widgetType\": \"TEXTBOX\"" : propertyExtras.strip());
    }

    private static Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("projectName", "payroll");
        parameters.put("genFolderName", "edm");
        parameters.put("filePath", "/payroll/edm.model");
        return parameters;
    }

}
