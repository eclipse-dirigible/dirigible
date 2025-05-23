/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
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
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"spring-boot-admin-server", "spring-boot-admin-client"})
public class SpringBootAdminIT extends UserInterfaceIntegrationTest {

    private static final String SPRING_ADMIN_BRAND_TITLE = "Eclipse Dirigible Admin";

    @Test
    void testSpringBootAdminStarts() {
        ide.openSpringBootAdmin();

        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, SPRING_ADMIN_BRAND_TITLE);
    }
}
