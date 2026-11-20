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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * The standalone report page parses when a column's authored values carry an apostrophe or a
 * backslash (dirigible #8748).
 *
 * <p>
 * The page puts each column's alias, alignment, aggregate and pattern - and a drill link's alias -
 * into single-quoted JS strings. They were written raw, so an alias like {@code Owner's amount}
 * ended the string and the whole page stayed blank. The report is prepared by the generation's own
 * adapter (hence this package), rendered through the shipped template and evaluated in a polyglot
 * context: the page must parse and hand the columns back verbatim.
 */
class ReportPageColumnLiteralsIT {

    private static final String TEMPLATE_ID = "template-application-ui-harmonia-java/template/template-report-file.js";

    private static final String PAGE =
            "/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/report-file/report.js.template";

    private static final String REPORT = """
            { "name": "Assets", "query": "SELECT 1", "parameters": [],
              "columns": [ { "alias": "Owner's \\\\ amount", "type": "DECIMAL", "aggregate": "SU'M", "pattern": "#'##0.00",
                             "align": "ri'ght" },
                           { "alias": "Count", "type": "INTEGER", "aggregate": "NONE" } ],
              "drill": { "app": "assets", "route": "/Asset",
                         "columns": [ { "alias": "Owner's \\\\ amount", "property": "Owner", "by": "id" } ] } }
            """;

    @Test
    void aColumnWithAnApostropheOrABackslashKeepsThePageParseable() throws Exception {
        try (Context context = page()) {
            assertEquals("Owner's \\ amount|ri'ght|SU'M|#'##0.00",
                    context.eval("js", "var c = __page.reportColumns[0]; c.key + '|' + c.align + '|' + c.agg + '|' + c.pattern;")
                           .asString(),
                    "the column's authored values reach the page verbatim");
            assertEquals("Count|right|NONE|undefined",
                    context.eval("js", "var c = __page.reportColumns[1]; c.key + '|' + c.align + '|' + c.agg + '|' + c.pattern;")
                           .asString(),
                    "a plain column is unchanged");
            assertEquals("Owner's \\ amount", context.eval("js", "__page.drill.columns[0].alias")
                                                     .asString(),
                    "the drill link's alias reaches the page verbatim");
        }
    }

    private static Context page() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Assets");
        parameters.put("projectName", "assets");
        parameters.put("genFolderName", "assets");
        parameters.put("javaGenFolderName", "assets");
        parameters.put("javaPerspectiveName", "reports");
        Map<String, Object> model = ModelTemplateAdapters.prepare(TEMPLATE_ID, REPORT, parameters)
                                                         .model();
        Map<String, Object> velocity = new HashMap<>(parameters);
        velocity.putAll(model);
        String template;
        try (InputStream in = ReportPageColumnLiteralsIT.class.getResourceAsStream(PAGE)) {
            assertNotNull(in, "template resource not found on classpath: " + PAGE);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String rendered = new String(new VelocityGenerationEngine().generate(velocity, PAGE, template.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", """
                var __init;
                var __page;
                var document = { addEventListener: function (type, fn) { if (type === 'alpine:init') __init = fn; } };
                var Alpine = { data: function (name, factory) { __page = factory(); } };
                """);
        context.eval("js", rendered);
        context.eval("js", "__init();");
        return context;
    }
}
