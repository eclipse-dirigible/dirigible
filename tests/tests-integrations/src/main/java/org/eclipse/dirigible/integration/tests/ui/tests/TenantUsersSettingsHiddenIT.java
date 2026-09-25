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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.eclipse.dirigible.tests.base.KeycloakUserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * Without DIRIGIBLE_TENANT_USERS_ENABLED even a tenant owner is offered no Users section: the
 * context endpoint does not exist, and the store reads that as "not offered here".
 */
class TenantUsersSettingsHiddenIT extends KeycloakUserInterfaceIntegrationTest {

    @Test
    void anOwnerIsNotOfferedTheSectionWhenTheFeatureIsOff() {
        DirigibleTestTenant tenant = provisionTenant("tenant-users-hidden-it");
        grant("owner.hidden@example.com", tenant, "Owner");

        signInAndOpen("/services/web/application/index.html", "owner.hidden@example.com");
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));
        browser.openPath("/services/web/application/index.html#/settings/tenant-users");

        Awaitility.await()
                  .pollInSameThread()
                  .atMost(Duration.ofSeconds(30))
                  .pollInterval(Duration.ofMillis(250))
                  .until(() -> Boolean.TRUE.equals(
                          Selenide.executeJavaScript("var s = window.Alpine && Alpine.store('tenantUsers'); return !!(s && s.context);")));
        assertEquals(Boolean.FALSE, Selenide.executeJavaScript("return Alpine.store('tenantUsers').context.enabled;"));
        Selenide.$(By.id("tenant-users-page"))
                .shouldNot(Condition.exist);
    }
}
