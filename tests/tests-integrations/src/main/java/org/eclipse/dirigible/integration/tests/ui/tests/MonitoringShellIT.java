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

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Smoke test for the Monitoring shell. It asserts what only a real browser can: that the Harmonia +
 * Alpine bootstrap completed and each page rendered content produced by its store.
 * <p>
 * This matters more than it looks - a single mistake in the markup (an Alpine binding on an
 * {@code <i x-h-lucide>}, for instance) aborts Alpine's walk with no DOM or server-side symptom,
 * and every binding below that node silently stays raw markup.
 * <p>
 * The page roots carry a stable id, so the assertions hold whether or not the instance happens to
 * have processes, jobs or queues deployed.
 */
// One Dirigible boot for the whole class: the methods are read-only or clean up after themselves,
// so the per-method context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MonitoringShellIT extends UserInterfaceIntegrationTest {

    private static final String MONITORING_PATH = "/services/web/monitoring/index.html";

    @Test
    void overviewRendersTheInstanceState() {
        ide.openPath(MONITORING_PATH);

        // The shell chrome: the sidebar entry is rendered by Alpine from the shell component.
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Overview");
        // The page itself, routed into #app by Pinecone.
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.PARAGRAPH,
                "The health of this instance and what needs attention.");
        // Tiles that only exist once the store's first poll resolved - i.e. the platform endpoints
        // answered and the bindings evaluated.
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Health");
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Artefacts in error");
    }

    @Test
    void everySectionIsReachable() {
        ide.openPath(MONITORING_PATH);

        openSection("processes", "Running");
        openSection("jobs", "Jobs");
        openSection("logs", "Live");
        openSection("messaging", "Queues");
        openSection("system", "The build this instance runs, and the Java virtual machine running it.");
    }

    /**
     * The build is the first thing asked of an instance ("what is deployed here?"), so it is asserted
     * on its own: the System page's card, filled from {@code /services/core/version}.
     */
    @Test
    void systemNamesTheDeployedBuild() {
        ide.openPath(MONITORING_PATH);

        browser.clickOnElementById("monitoring-nav-system");
        browser.assertElementExistsByIdAndContainsText("monitoring-system-version", "Product");
        browser.assertElementExistsByIdAndContainsText("monitoring-system-version", "Version");
        // The same figures condensed onto the sidebar, where every page shows them.
        browser.assertElementExistsByIdAndContainsText("monitoring-build", "Eclipse Dirigible");
    }

    /**
     * Click a sidebar entry and assert its page rendered. The entry is addressed by its id, not its
     * label: a label match would also hit page content (a "System" span in the Overview's database-pool
     * tile, for instance).
     *
     * @param section the sidebar entry / page name
     * @param expectedText text the page renders regardless of what is deployed on the instance
     */
    private void openSection(String section, String expectedText) {
        browser.clickOnElementById("monitoring-nav-" + section);
        browser.assertElementExistsByIdAndContainsText("monitoring-" + section + "-page", expectedText);
    }
}
