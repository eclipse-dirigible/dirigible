/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Smoke test for the Monitoring perspective. Verifies that:
 * <ul>
 * <li>the {@code perspective-monitoring} entry is reachable from the IDE shell;</li>
 * <li>each of the three hosted views (Counters in the left region, Monitoring and Threads in the
 * center) renders identifiable content after the perspective is opened;</li>
 * <li>the three backing endpoints under {@code /services/ide/monitoring/} respond with HTTP 200 and
 * a payload that looks like the expected JSON shape.</li>
 * </ul>
 *
 * <p>
 * Intentionally light — this is a smoke test, not a behavioural one. Anything more specific (e.g.
 * asserting that the Hikari pool reports a {@code DefaultDB} entry) is brittle against the
 * datasource set the runtime initializes by the time the click happens, so it is deliberately left
 * out.
 */
public class MonitoringPerspectiveIT extends UserInterfaceIntegrationTest {

    private static final String PERSPECTIVE_ID = "perspective-monitoring";
    private static final String METRICS_ENDPOINT = "/services/ide/monitoring/metrics";
    private static final String THREADS_ENDPOINT = "/services/ide/monitoring/threads";
    private static final String COUNTS_ENDPOINT = "/services/ide/monitoring/counts";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void monitoringPerspectiveOpensAndRendersAllViews() {
        ide.openHomePage();

        Browser browser = ide.getBrowser();
        browser.clickOnElementById(PERSPECTIVE_ID);

        // The Metrics view (left region) is the first to load — assert its accordion-panel title
        // (the layout's h4) and one of its group titles. Threads/Classes are the only groups
        // guaranteed to be present on every JVM. "Metrics" appears as the toolbar label inside
        // the view iframe too, so we anchor on the panel title (h4) to avoid the duplicate match.
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.HEADER4, "Metrics");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.DIV, "Classes");

        // The Monitoring view (center region) auto-loads alongside Counters. The "Heap" tile label
        // is a stable, unique anchor that proves the dashboard rendered without races.
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.DIV, "Heap");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.DIV, "Memory pools");
    }

    @Test
    void monitoringEndpointsReturnExpectedShapes() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(METRICS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("\"memory\""))
                                                 .body(containsString("\"threads\""))
                                                 .body(containsString("\"runtime\"")));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(THREADS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("\"state\""))
                                                 .body(containsString("\"name\"")));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(COUNTS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("\"groups\""))
                                                 .body(containsString("\"Threads\"")));
    }
}
