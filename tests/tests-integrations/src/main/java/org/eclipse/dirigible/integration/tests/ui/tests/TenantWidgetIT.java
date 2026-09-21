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

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The tenant chip in every shell header: on a multitenant instance a page names the tenant its
 * request runs in, next to the user menu.
 *
 * <p>
 * Three hosts of the chip, because it is mounted three different ways: a Harmonia shell renders it
 * from the shared {@code tenant} store through the fetched fragment, Home carries no shell runtime
 * and loads the store on its own, and the Workbench is AngularJS and renders it from
 * {@code header.html}. Under {@code SUBDOMAIN} resolution on {@code localhost} the request is in
 * the default tenant, so that is the name every host must show. The failure this guards has no DOM
 * or server-side symptom before the browser: an Alpine binding that throws inside the injected
 * fragment simply leaves the chip empty.
 */
// One Dirigible boot for the whole class: the methods only read, so the per-method context reset
// inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TenantWidgetIT extends MultitenancyUserInterfaceIntegrationTest {

    /** The one id both shapes of the chip carry, so a test finds it whichever is rendered. */
    private static final String TENANT_CHIP = "tenant-menu-trigger";

    private static final String DEFAULT_TENANT = "default-tenant";

    @Test
    void aHarmoniaShellNamesTheTenantOfTheRequest() {
        ide.openPath("/services/web/monitoring/index.html");

        browser.assertElementExistsByIdAndContainsText(TENANT_CHIP, DEFAULT_TENANT);
    }

    @Test
    void theHomePageNamesTheTenantOfTheRequest() {
        ide.openPath("/services/web/home/index.html");

        browser.assertElementExistsByIdAndContainsText(TENANT_CHIP, DEFAULT_TENANT);
    }

    @Test
    void theWorkbenchNamesTheTenantOfTheRequest() {
        ide.openIde();

        browser.assertElementExistsByIdAndContainsText(TENANT_CHIP, DEFAULT_TENANT);
    }
}
