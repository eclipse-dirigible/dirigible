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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * A report parameter's label was spelled out by the report page from the parameter's name, with no
 * catalog key, so it could not be translated (#7390). Each parameter now carries a catalog key and
 * a label, and the report's catalog holds them.
 */
class ReportParameterLabelsTest {

    @Test
    void spells_out_the_name_under_a_prefixed_key() {
        Map<String, Object> report = report(parameter("fromDate"), parameter("min_total"), parameter("Region"));

        ModelTranslations.labelReportParameters(report);

        List<Map<String, Object>> parameters = ModelValues.asMaps(report.get("parameters"));
        assertEquals("paramFromDate", parameters.get(0)
                                                .get("tId"));
        assertEquals("From Date", parameters.get(0)
                                            .get("label"));
        assertEquals("paramMintotal", parameters.get(1)
                                                .get("tId"));
        assertEquals("Min total", parameters.get(1)
                                            .get("label"));
        // A parameter named like a column alias still gets a key of its own.
        assertEquals("paramRegion", parameters.get(2)
                                              .get("tId"));
    }

    @Test
    void keeps_an_authored_key_and_label() {
        Map<String, Object> authored = parameter("fromDate");
        authored.put("tId", "periodStart");
        authored.put("label", "Period start");

        ModelTranslations.labelReportParameters(report(authored));

        assertEquals("periodStart", authored.get("tId"));
        assertEquals("Period start", authored.get("label"));
    }

    @Test
    void puts_the_labels_into_the_report_catalog() {
        Map<String, Object> report = report(parameter("fromDate"));
        report.put("tId", "Sales");
        report.put("label", "Sales");
        ModelTranslations.labelReportParameters(report);

        Map<String, Object> translations = ModelTranslations.reportTranslations(report);

        assertEquals("From Date", translations.get("paramFromDate"));
    }

    private static Map<String, Object> report(Map<String, Object>... parameters) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("parameters", new ArrayList<>(List.of(parameters)));
        return report;
    }

    private static Map<String, Object> parameter(String name) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("name", name);
        parameter.put("type", "DATE");
        return parameter;
    }
}
