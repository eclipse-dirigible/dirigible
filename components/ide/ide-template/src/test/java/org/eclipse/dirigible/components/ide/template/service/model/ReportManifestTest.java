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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The report manifest a generated application carries in its configuration (#8277): the reports the
 * intent wrote into the model, which the shell's reports store lists instead of discovering them
 * through the core repository API - an API a business user may not call.
 */
class ReportManifestTest {

    private static final String TEMPLATE = "template-application-ui-harmonia-java/template/template.js";

    @Test
    void theModelsReportsAreRenderedInTheirOrderWithTheirWidgetOnlyWhereTheyDeclareOne() {
        Map<String, Object> parameters = parameters();

        ModelTemplateAdapters.prepare(TEMPLATE, model("""
                ,
                "reports": [
                  {
                    "name": "OpenOrders",
                    "label": "Open Orders",
                    "dashboard": true,
                    "tId": "openOrders",
                    "widget": { "kind": "count", "label": "Open", "icon": "inbox" },
                    "columns": [ { "alias": "Number", "type": "VARCHAR", "select": true } ],
                    "url": "/services/web/shop/gen/openorders/reports/OpenOrders/index.html",
                    "apiBase": "/services/java/shop/gen/openorders/api/reports/OpenOrdersController"
                  },
                  {
                    "name": "Revenue",
                    "label": "Revenue",
                    "description": "Revenue by month",
                    "dashboard": false,
                    "chart": "bar",
                    "tId": "revenue",
                    "descriptionTId": "revenueDescription",
                    "url": "/services/web/shop/gen/revenue/reports/Revenue/index.html",
                    "apiBase": "/services/java/shop/gen/revenue/api/reports/RevenueController"
                  }
                ]
                """), parameters);

        JsonArray reports = JsonParser.parseString((String) parameters.get("appReports"))
                                      .getAsJsonArray();
        assertEquals(2, reports.size(), "both reports, in the model's order: " + reports);
        JsonObject withWidget = reports.get(0)
                                       .getAsJsonObject();
        assertEquals("OpenOrders", withWidget.get("name")
                                             .getAsString());
        assertEquals("count", withWidget.getAsJsonObject("widget")
                                        .get("kind")
                                        .getAsString());
        assertEquals("Number", withWidget.getAsJsonArray("columns")
                                         .get(0)
                                         .getAsJsonObject()
                                         .get("alias")
                                         .getAsString());
        assertEquals("/services/java/shop/gen/openorders/api/reports/OpenOrdersController", withWidget.get("apiBase")
                                                                                                      .getAsString());
        JsonObject withoutWidget = reports.get(1)
                                          .getAsJsonObject();
        assertEquals("Revenue", withoutWidget.get("name")
                                             .getAsString());
        assertFalse(withoutWidget.has("widget"), "a report without a widget carries none: " + withoutWidget);
        assertFalse(withoutWidget.has("columns"), "nor the columns only a widget tile reads: " + withoutWidget);
        assertFalse(withoutWidget.get("dashboard")
                                 .getAsBoolean());
        assertEquals("revenueDescription", withoutWidget.get("descriptionTId")
                                                        .getAsString());
    }

    @Test
    void aModelThatListsNoReportsLeavesTheShellToDiscoverThem() {
        Map<String, Object> parameters = parameters();

        ModelTemplateAdapters.prepare(TEMPLATE, model(""), parameters);

        // Not an empty list: a model the intent did not write may still have standalone reports beside
        // it, generated from the report editor, which only the store's discovery finds.
        assertEquals("null", parameters.get("appReports"), "the configuration is valid JavaScript and tells the store to discover");
    }

    private static String model(String rootExtras) {
        return """
                {
                  "model": {
                    "entities": [
                      {
                        "name": "Order",
                        "dataName": "ORDER",
                        "properties": [
                          { "name": "Id", "dataName": "ORDER_ID", "dataType": "INTEGER", "dataPrimaryKey": "true" }
                        ]
                      }
                    ]%s
                  }
                }
                """.formatted(rootExtras.strip());
    }

    private static Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("projectName", "shop");
        parameters.put("genFolderName", "edm");
        parameters.put("filePath", "/shop/edm.model");
        return parameters;
    }

}
