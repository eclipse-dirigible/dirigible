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

class DirigibleHomepageIT extends UserInterfaceIntegrationTest {

    private static final String ECLIPSE_DIRIGIBLE_HEADER = "Eclipse Dirigible";

    @Test
    void testOpenHomepage() {
        ide.openHomePage();

        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER5, ECLIPSE_DIRIGIBLE_HEADER);
    }
}
