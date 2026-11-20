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

import java.time.Duration;

import org.eclipse.dirigible.tests.base.KeycloakUserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverConditions;

/**
 * The Keycloak harness itself: a person whose token names exactly one tenant signs in through a
 * real identity provider, is placed in that tenant, and the shell's tenant chip names it; a person
 * of several tenants picks among them by name.
 */
class KeycloakTokenGroupsSignInIT extends KeycloakUserInterfaceIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(60);

    @Test
    void anOwnerOfOneTenantSignsInAndIsPlacedInIt() {
        DirigibleTestTenant tenant = provisionTenant("keycloak-sign-in-it");
        grant("owner.signin@example.com", tenant, "Owner");

        signInAndOpen("/services/web/application/index.html", "owner.signin@example.com");

        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60))
                .shouldHave(Condition.text(tenant.getName()));
    }

    /**
     * The picker and the tenant menu show a tenant's name, never its id (#8724) - except where two
     * offered tenants share a name, ignoring case: then each of them shows its id, since nothing else
     * tells them apart.
     */
    @Test
    void aPersonOfSeveralTenantsTellsThemApartByName() {
        DirigibleTestTenant production = provisionTenant("Production");
        DirigibleTestTenant alsoProduction = provisionTenant("production");
        DirigibleTestTenant staging = provisionTenant("Staging");
        String email = "owner.picker@example.com";
        grant(email, production, "Owner");
        grant(email, alsoProduction, "Owner");
        grant(email, staging, "Owner");

        signInAndOpen("/services/web/application/index.html", email);

        Selenide.webdriver()
                .shouldHave(WebDriverConditions.urlContaining("/tenant-selection.html"), WAIT);
        assertIdShown(Selenide.$(".tenant-tile[data-tenant='" + production.getId() + "']"), production);
        assertIdShown(Selenide.$(".tenant-tile[data-tenant='" + alsoProduction.getId() + "']"), alsoProduction);
        assertIdHidden(Selenide.$(".tenant-tile[data-tenant='" + staging.getId() + "']"), staging);

        Selenide.$(".tenant-tile[data-tenant='" + staging.getId() + "']")
                .click();
        // the selection applies from the next request: wait for the picker to have navigated home
        Selenide.webdriver()
                .shouldNotHave(WebDriverConditions.urlContaining("/tenant-selection.html"), WAIT);
        browser.openPath("/services/web/application/index.html");
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, WAIT)
                .shouldHave(Condition.text("Staging"))
                .click();
        assertIdShown(Selenide.$("#tenant-menu [data-tenant='" + production.getId() + "']"), production);
        assertIdShown(Selenide.$("#tenant-menu [data-tenant='" + alsoProduction.getId() + "']"), alsoProduction);
        assertIdHidden(Selenide.$("#tenant-menu [data-tenant='" + staging.getId() + "']"), staging);
    }

    private static void assertIdShown(SelenideElement entry, DirigibleTestTenant tenant) {
        entry.shouldBe(Condition.visible, WAIT)
             .shouldHave(Condition.text(tenant.getName()));
        entry.$("[data-tenant-id]")
             .shouldBe(Condition.visible)
             .shouldHave(Condition.exactText(tenant.getId()));
    }

    private static void assertIdHidden(SelenideElement entry, DirigibleTestTenant tenant) {
        entry.shouldBe(Condition.visible, WAIT)
             .shouldHave(Condition.text(tenant.getName()))
             .shouldNotHave(Condition.text(tenant.getId()));
        entry.$("[data-tenant-id]")
             .shouldBe(Condition.hidden);
    }
}
