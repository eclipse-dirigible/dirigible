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

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.KeycloakUserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * Settings > Users in the application shell, driven by real people signed in through Keycloak: an
 * owner of the tenant opens the section, invites another person and sees the request waiting for an
 * answer; a plain member of the same tenant is not offered the section at all.
 */
class TenantUsersSettingsIT extends KeycloakUserInterfaceIntegrationTest {

    private static final String SECTION = "/services/web/application/index.html#/settings/tenant-users";
    private static final String OWNER = "owner.users@example.com";
    private static final String MEMBER = "member.users@example.com";

    private static DirigibleTestTenant tenant;

    @BeforeAll
    static void enableTenantUsers() {
        DirigibleConfig.TENANT_USERS_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.setStringValue("global:tenant-users-settings-it.requests");
    }

    @BeforeEach
    void provisionTheTenantOnce() {
        if (tenant == null) {
            tenant = provisionTenant("tenant-users-settings-it");
            grant(OWNER, tenant, "Owner");
            grant(MEMBER, tenant, "User");
        }
    }

    @Test
    void anOwnerInvitesAPersonAndSeesTheRequestWaiting() {
        signInAndOpen("/services/web/application/index.html", OWNER);
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));
        browser.openPath(SECTION);

        Selenide.$(By.id("tenant-users-page"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));
        Selenide.$(By.id("tenant-users-email"))
                .setValue("Invited.From.UI@Example.com");
        Selenide.$(By.id("tenant-users-invite-button"))
                .click();

        Selenide.$(By.cssSelector("#tenant-users-table tr[data-user='invited.from.ui@example.com']"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30))
                .shouldHave(Condition.text("Pending"))
                .shouldHave(Condition.text(OWNER));
    }

    @Test
    void aMemberIsNotOfferedTheSection() {
        signInAndOpen("/services/web/application/index.html", MEMBER);
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));
        browser.openPath(SECTION);

        Awaitility.await()
                  .pollInSameThread()
                  .atMost(Duration.ofSeconds(30))
                  .pollInterval(Duration.ofMillis(250))
                  .until(() -> Boolean.TRUE.equals(
                          Selenide.executeJavaScript("var s = window.Alpine && Alpine.store('tenantUsers'); return !!(s && s.context);")));
        assertEquals(Boolean.FALSE, Selenide.executeJavaScript("return Alpine.store('tenantUsers').visible;"));
        Selenide.$(By.id("tenant-users-page"))
                .shouldNot(Condition.exist);
    }
}
