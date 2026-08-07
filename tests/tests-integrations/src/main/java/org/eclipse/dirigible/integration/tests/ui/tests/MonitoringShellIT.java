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

/**
 * Smoke test for the Monitoring shell. It asserts what only a real browser can: that the Harmonia +
 * Alpine bootstrap completed and the Overview page rendered content produced by the polled store.
 * <p>
 * This matters more than it looks - a single mistake in the markup (an Alpine binding on an
 * {@code <i x-h-lucide>}, for instance) aborts Alpine's walk with no DOM or server-side symptom,
 * and every binding below that node silently stays raw markup.
 */
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
        // A tile that only exists once the store's first poll resolved - i.e. the platform endpoints
        // answered and the bindings evaluated.
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Health");
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Artefacts in error");
    }
}
