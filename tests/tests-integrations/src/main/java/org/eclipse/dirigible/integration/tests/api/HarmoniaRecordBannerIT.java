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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * A record-page banner (intent {@code banners:}, dirigible #8371) is evaluated once, in the shared
 * shell: {@code basePage.bannerSubject(banner, record, rowsByRegister)} answers the row the banner
 * speaks about, or null. Exercised by evaluating the shipped {@code basePage.js} in a polyglot
 * context, over an order whose shipments register answered the rows below.
 */
class HarmoniaRecordBannerIT {

    private static final String BASE_PAGE = "/META-INF/dirigible/application-core/shell/js/components/pages/basePage.js";

    /** The shipments register's key, as the page reads its rows under it. */
    private static final String REGISTER = "/api/shipment/ShipmentController|Order";

    /** "The last shipment failed": the latest row, status 3 (FAILED). */
    private static final String LATEST_FAILED = "{ key: 'Order1', when: [], related: { register: '" + REGISTER
            + "', primaryKey: 'Id', row: 'latest', when: [{ property: 'Status', equal: true, value: '3' }] } }";

    @Test
    void latestTestsOnlyTheHighestKey() {
        try (Context context = load()) {
            assertEquals("none", subject(context, LATEST_FAILED, "{ Id: 7, Status: 1 }", "[{ Id: 1, Status: 3 }, { Id: 2, Status: 2 }]"),
                    "row 2 DELIVERED is the latest - an older failure is not the news");
            assertEquals("2", subject(context, LATEST_FAILED, "{ Id: 7, Status: 1 }", "[{ Id: 2, Status: '3' }, { Id: 1, Status: 2 }]"),
                    "the highest key wins whatever the order the rows came in, and a status compares as text");
        }
    }

    @Test
    void anyMatchesAnyRow() {
        String any = LATEST_FAILED.replace("row: 'latest'", "row: 'any'");
        try (Context context = load()) {
            assertEquals("1", subject(context, any, "{ Id: 7, Status: 1 }", "[{ Id: 1, Status: 3 }, { Id: 2, Status: 2 }]"));
            assertEquals("none", subject(context, any, "{ Id: 7, Status: 1 }", "[{ Id: 1, Status: 1 }, { Id: 2, Status: 2 }]"));
        }
    }

    @Test
    void ownAndRelatedConditionsAreAnded() {
        String both = LATEST_FAILED.replace("when: [], related", "when: [{ property: 'Status', equal: false, value: '9' }], related");
        String onHold = "{ key: 'Order2', when: [{ property: 'Status', equal: true, value: '2' }], related: null }";
        try (Context context = load()) {
            assertEquals("2", subject(context, both, "{ Id: 7, Status: 1 }", "[{ Id: 2, Status: 3 }]"), "both hold");
            assertEquals("none", subject(context, both, "{ Id: 7, Status: 9 }", "[{ Id: 2, Status: 3 }]"),
                    "the record's own condition fails - no banner, whatever the rows say");
            assertEquals("7", subject(context, onHold, "{ Id: 7, Status: 2 }", "{}"), "an own-field banner's subject is the record");
            assertEquals("none", subject(context, onHold, "{ Id: 7, Status: 1 }", "{}"));
        }
    }

    @Test
    void noRowsMeansNoBanner() {
        try (Context context = load()) {
            assertEquals("none", subject(context, LATEST_FAILED, "{ Id: 7, Status: 1 }", "[]"));
            assertEquals("none", subject(context, LATEST_FAILED, "{ Id: 7, Status: 1 }", "null"), "a failed read holds no banner");
            assertEquals("none", subject(context, LATEST_FAILED, "null", "[{ Id: 2, Status: 3 }]"), "no record, no banner");
        }
    }

    /**
     * The subject's key ({@code Id}) for the banner, or {@code none}. Rows given as an array are the
     * register's.
     */
    private static String subject(Context context, String banner, String record, String rows) {
        String byRegister = rows.startsWith("[") || "null".equals(rows) ? "{ '" + REGISTER + "': " + rows + " }" : rows;
        return context.eval("js",
                "(function () { var s = basePage().bannerSubject(" + banner + ", " + record + ", " + byRegister
                        + "); return s ? String(s.Id) : 'none'; })()")
                      .asString();
    }

    private static Context load() {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", "var window = this;");
        try (InputStream in = HarmoniaRecordBannerIT.class.getResourceAsStream(BASE_PAGE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + BASE_PAGE);
            }
            context.eval("js", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to evaluate " + BASE_PAGE, ex);
        }
        return context;
    }
}
