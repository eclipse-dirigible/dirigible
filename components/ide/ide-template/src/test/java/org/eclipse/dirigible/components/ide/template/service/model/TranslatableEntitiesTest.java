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
 * The shells' Settings ask each multilingual entity of a generated application how completely its
 * data is translated (dirigible #8789), so the application's configuration lists those entities
 * with the label the report names them by and the URL that answers.
 */
class TranslatableEntitiesTest {

    private static final String TEMPLATE = "template-application-ui-harmonia-java/template/template.js";

    @Test
    void everyMultilingualEntityIsListedWithItsPluralLabelAndItsCoverageUrl() {
        Map<String, Object> parameters = parameters();

        ModelTemplateAdapters.prepare(TEMPLATE, """
                {
                  "model": {
                    "entities": [
                      { "name": "Currency", "dataName": "FIN_CURRENCY", "perspectiveName": "Settings", "multilingual": "true",
                        "menuLabel": "Currencies", "properties": [] },
                      { "name": "UnitOfMeasure", "dataName": "FIN_UOM", "perspectiveName": "Settings", "multilingual": "true",
                        "properties": [] },
                      { "name": "Invoice", "dataName": "FIN_INVOICE", "perspectiveName": "Invoice", "properties": [] },
                      { "name": "Country", "dataName": "CTR_COUNTRY", "perspectiveName": "Settings", "multilingual": "true",
                        "type": "PROJECTION", "properties": [] }
                    ]
                  }
                }
                """, parameters);

        assertEquals(
                "[{\"key\":\"finance:fin-model.t.FIN_CURRENCY_plural\",\"label\":\"Currencies\","
                        + "\"url\":\"/services/java/finance/gen/fin/api/settings/CurrencyController/translations\"},"
                        + "{\"key\":\"finance:fin-model.t.FIN_UOM_plural\",\"label\":\"Units of Measure\","
                        + "\"url\":\"/services/java/finance/gen/fin/api/settings/UnitOfMeasureController/translations\"}]",
                parameters.get("appTranslatableEntities"),
                "a non-multilingual entity and a cross-model projection (no language table, no controller here) are not listed");
    }

    @Test
    void anApplicationWithoutMultilingualEntitiesListsNone() {
        Map<String, Object> parameters = parameters();

        ModelTemplateAdapters.prepare(TEMPLATE,
                """
                        { "model": { "entities": [ { "name": "Invoice", "dataName": "FIN_INVOICE", "perspectiveName": "Invoice", "properties": [] } ] } }
                        """,
                parameters);

        assertEquals("[]", parameters.get("appTranslatableEntities"), "the configuration is always valid JavaScript");
    }

    private static Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("projectName", "finance");
        parameters.put("genFolderName", "fin");
        parameters.put("filePath", "/finance/fin.model");
        return parameters;
    }
}
