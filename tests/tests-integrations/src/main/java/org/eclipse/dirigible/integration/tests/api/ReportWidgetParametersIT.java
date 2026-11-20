/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * The dashboard never calls a report without its parameters (dirigible #8749).
 *
 * <p>
 * A KPI tile sends no parameter of its own, so its report binds each parameter's declared initial.
 * A report with a parameter that declares none - a ledger's account, a hand-authored filter - was
 * still run by the tile on every dashboard visit, with an empty value. The reports store now learns
 * those parameters (from the platform listing, or from the {@code .report} itself) and loads no
 * value for such a tile, and the application shell leaves the tile out.
 *
 * <p>
 * The SHIPPED store is evaluated in a polyglot context over a stubbed {@code fetch}; the test reads
 * which URLs the dashboard requested.
 */
class ReportWidgetParametersIT {

    private static final String STORE = "/META-INF/dirigible/application-core/shell/js/stores/reports.js";

    /** The platform listing: a tile on a report with an initial-less parameter, and one that runs. */
    private static final String LISTING = """
            [ { name: 'AccountAnalysis', project: 'journal', url: '/services/web/journal/gen/aa/reports/AccountAnalysis/index.html',
                apiBase: '/x/AccountAnalysisController', widget: { kind: 'count' }, columns: [], requiredParameters: ['account'] },
              { name: 'Revenue', project: 'journal', url: '/services/web/journal/gen/rv/reports/Revenue/index.html',
                apiBase: '/x/RevenueController', widget: { kind: 'count' }, columns: [] } ]
            """;

    @Test
    void theApplicationShellRunsNoTileWhoseReportLacksAParameterValue() {
        try (Context context = context("App.config = { aggregateReports: true };")) {
            String requested = run(context);
            assertFalse(requested.contains("AccountAnalysisController"), "the report is never called without its account: " + requested);
            assertTrue(requested.contains("/x/RevenueController/count"), "a tile whose report runs as declared still loads: " + requested);
            assertEquals("Revenue", context.eval("js", "stores.reports.kpiReports().map(r => r.name).join(',')")
                                           .asString(),
                    "the tile that cannot run is left off the dashboard");
        }
    }

    /** A shell without a manifest reads the tile's parameters from the {@code .report} itself. */
    @Test
    void aDiscoveredReportIsReadForItsParametersToo() {
        try (Context context = context("App.config = { projectName: 'journal' };")) {
            String requested = run(context);
            assertTrue(requested.contains("/journal/AccountAnalysis.report"), "the .report is read: " + requested);
            assertFalse(requested.contains("AccountAnalysisController"), "the report is never called without its account: " + requested);
            assertEquals("", context.eval("js", "stores.reports.kpiReports().map(r => r.name).join(',')")
                                    .asString(),
                    "the tile that cannot run is left off the dashboard");
        }
    }

    private static String run(Context context) {
        context.eval("js", "var __result; stores.reports.load().then(() => stores.reports.loadWidgets())"
                + ".then(() => { __result = requested.join('\\n'); }, e => { __result = 'ERROR ' + e; });");
        String result = context.eval("js", "__result")
                               .asString();
        assertFalse(result.startsWith("ERROR"), result);
        return result;
    }

    private static Context context(String config) {
        Context context = Context.newBuilder("js")
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", """
                var window = this; var App = {}; var stores = {}; var requested = [];
                window.location = { hash: '' };
                window.addEventListener = () => {};
                var console = { warn: () => {}, error: () => {}, log: () => {} };
                var document = { addEventListener: (event, callback) => { if (event === 'alpine:init') callback(); } };
                var Alpine = { store: (name, definition) => { stores[name] = definition; } };
                var reply = (body) => ({ ok: true, status: 200, json: async () => body });
                var fetch = async (url) => {
                  requested.push(url);
                  if (url === '/services/platform/reports') return reply(""" + LISTING
                + """
                        );
                          if (url === '/services/core/repository/registry/public/journal/gen') return reply({ resources: [], collections: [
                            { resources: [ { name: 'index.html', path: '/registry/public/journal/gen/aa/reports/AccountAnalysis/index.html' } ] } ] });
                          if (url === '/services/core/repository/registry/public/journal/AccountAnalysis.report') return reply({
                            widget: { kind: 'count' }, columns: [], parameters: [ { name: 'account', initial: '' }, { name: 'fromDate', initial: '1900-01-01' } ] });
                          return reply({ count: 3 });
                        };
                        """);
        context.eval("js", config);
        context.eval("js", read(STORE));
        return context;
    }

    private static String read(String resource) {
        try (InputStream in = ReportWidgetParametersIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
