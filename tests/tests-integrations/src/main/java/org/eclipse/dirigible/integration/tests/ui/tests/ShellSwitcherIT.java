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
 * The shell switcher in every Harmonia shell's user menu: the other registered shells plus Home, so
 * switching does not have to go through the launchpad.
 * <p>
 * The menu is filled by the shared {@code shells} store from the same {@code platform-shells}
 * aggregation Home renders, so this exercises two different host shells to prove the block works
 * wherever it is embedded - and that each one leaves ITSELF out of its own list.
 */
// One Dirigible boot for the whole class: the methods are read-only or clean up after themselves,
// so the per-method context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ShellSwitcherIT extends UserInterfaceIntegrationTest {

    @Test
    void aShellOffersTheOtherShellsAndHome() {
        ide.openPath("/services/web/monitoring/index.html");
        browser.clickOnElementById("user-menu-trigger");

        // The section heading is an x-h-menu-label (a div); the entries below it are spans.
        browser.assertElementExistsByTypeAndText(HtmlElementType.DIV, "Switch to");
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Home");
        // Other shells, listed from the platform registration - not hardcoded per shell.
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Applications");
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Personal");
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Workbench");
    }

    @Test
    void everyShellLeavesItselfOutAndListsTheRest() {
        ide.openPath("/services/web/personal/index.html");
        browser.clickOnElementById("user-menu-trigger");

        // The shell it is NOT: offered here, and (per the test above) absent from Monitoring's own list.
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Monitoring");
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Administration");
    }
}
