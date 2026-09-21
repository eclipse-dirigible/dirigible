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

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;

import com.codeborne.selenide.Selenide;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * On a single-tenant instance the tenant chip is not shown at all: there is nothing to say.
 *
 * <p>
 * The counterpart of {@link TenantWidgetIT}. The page is known to have booted by its own user menu
 * being there, so the chip's absence is the store deciding not to render it and not a page that
 * never got that far.
 */
class TenantWidgetSingleTenantIT extends UserInterfaceIntegrationTest {

    @BeforeAll
    static void singleTenantInstance() {
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(false);
    }

    @Test
    void aSingleTenantInstanceShowsNoTenantChip() {
        ide.openPath("/services/web/monitoring/index.html");

        Selenide.$(By.id("user-menu-trigger"))
                .shouldBe(visible);
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldNot(exist);
    }
}
