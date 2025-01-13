/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.junit.jupiter.api.Test;

class HomepageRedirectIT extends UserInterfaceIntegrationTest {

    @Test
    void testOpenHomepage() {
        ide.openHomePage();
        assertHomeRedirect("/");
        assertHomeRedirect("");
        assertHomeRedirect("/home");
    }

    private void assertHomeRedirect(String path) {
        browser.openPath(path);
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Welcome");

    }
}
