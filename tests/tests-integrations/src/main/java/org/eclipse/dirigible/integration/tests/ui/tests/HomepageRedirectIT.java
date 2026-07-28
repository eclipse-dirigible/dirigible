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

import org.junit.jupiter.api.Tag;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.junit.jupiter.api.Test;

@Tag("smoke")
public class HomepageRedirectIT extends UserInterfaceIntegrationTest {

    /** Static copy on the Home landing page - deterministic, unlike the user-dependent greeting. */
    private static final String HOME_LANDING_TAGLINE = "Everything starts here. Pick where you want to work today.";

    @Test
    void testOpenHomepage() {
        ide.openHomePage();
        assertHomeRedirect("/");
        assertHomeRedirect("");
        assertHomeRedirect("/home");
    }

    private void assertHomeRedirect(String path) {
        browser.openPath(path);
        browser.assertElementExistsByTypeAndText(HtmlElementType.PARAGRAPH, HOME_LANDING_TAGLINE);
    }
}
