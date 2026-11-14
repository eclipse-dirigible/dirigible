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

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.KeycloakUserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

/**
 * Settings > Data export, in a browser, against a real Keycloak: an owner starts an export, sees it
 * become ready with a download link that names the tenant, is told when the tenant keeps as many
 * archives as it may, and deletes one after confirming. A member is not offered the section.
 */
class TenantExportSettingsIT extends KeycloakUserInterfaceIntegrationTest {

    private static final String SECTION = "/services/web/application/index.html#/settings/tenant-export";
    private static final String OWNER = "owner.export@example.com";
    private static final String MEMBER = "member.export@example.com";
    private static final Duration WAIT = Duration.ofSeconds(60);

    private static DirigibleTestTenant tenant;

    @BeforeAll
    static void enableTheExport() {
        DirigibleConfig.TENANT_EXPORT_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_EXPORT_MAX_ARCHIVES.setStringValue("1");
    }

    @BeforeEach
    void provisionTheTenantOnce() {
        if (tenant != null) {
            return;
        }
        DirigibleTestTenant created = provisionTenant("tenant-export-settings-it");
        grant(OWNER, created, "Owner");
        grant(MEMBER, created, "User");
        tenant = created;
    }

    @Test
    void anOwnerExportsDownloadsAndDeletes() {
        signInAndOpen("/services/web/application/index.html", OWNER);
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));
        browser.openPath(SECTION);
        Selenide.$(By.id("tenant-export-page"))
                .shouldBe(Condition.visible, WAIT);

        // 1. nothing yet, and the button is offered
        Selenide.$(By.id("tenant-export-start"))
                .shouldBe(Condition.enabled, WAIT)
                .click();

        // 2. the export is followed until it is ready
        SelenideElement row = Selenide.$("#tenant-export-table tr[data-export]");
        row.shouldHave(Condition.attribute("data-status", "COMPLETED"), WAIT);
        row.shouldHave(Condition.text(OWNER));

        // 3. the download is a link the browser fetches itself, named for the tenant
        SelenideElement download = row.$("a[data-action='download']")
                                      .shouldBe(Condition.visible, WAIT);
        String id = row.getAttribute("data-export");
        download.shouldHave(Condition.attributeMatching("href", ".*/services/data/tenant-export/" + id + "/archive"));
        download.shouldHave(Condition.attributeMatching("download", "tenant-" + tenant.getId() + "-data-export-.*\\.zip"));
        download.shouldHave(Condition.attribute("data-native"));

        // 4. the tenant keeps one archive here: a further export is not offered, and the page says why
        Selenide.$("#tenant-export-page [data-limit]")
                .shouldBe(Condition.visible, WAIT)
                .shouldHave(Condition.text("Delete one to make a new one"));
        Selenide.$(By.id("tenant-export-start"))
                .shouldBe(Condition.disabled);

        // 5. a delete asks first: cancelled, the export stays; confirmed, it goes
        row.$("button[data-action='delete']")
           .click();
        Selenide.$(By.id("tenant-export-delete-dialog"))
                .shouldBe(Condition.visible, WAIT);
        Selenide.$("#tenant-export-delete-dialog button[data-variant='outline']")
                .click();
        Selenide.$(By.id("tenant-export-delete-dialog"))
                .shouldNotBe(Condition.visible, WAIT);
        row.shouldBe(Condition.visible);

        row.$("button[data-action='delete']")
           .click();
        Selenide.$(By.id("tenant-export-delete-confirm"))
                .shouldBe(Condition.visible, WAIT)
                .click();
        Selenide.$("#tenant-export-table tr[data-export]")
                .shouldNot(Condition.exist, WAIT);
        Selenide.$(By.id("tenant-export-start"))
                .shouldBe(Condition.enabled, WAIT);
    }

    @Test
    void aMemberIsNotOfferedTheSection() {
        signInAndOpen("/services/web/application/index.html", MEMBER);
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));
        browser.openPath(SECTION);

        Awaitility.await()
                  .pollInSameThread()
                  .atMost(WAIT)
                  .pollInterval(Duration.ofMillis(250))
                  .until(() -> Boolean.TRUE.equals(
                          Selenide.executeJavaScript("var s = window.Alpine && Alpine.store('tenantExport'); return !!(s && s.context);")));
        assertEquals(Boolean.FALSE, Selenide.executeJavaScript("return Alpine.store('tenantExport').visible;"));
        Selenide.$(By.id("tenant-export-page"))
                .shouldNot(Condition.exist);
    }
}
