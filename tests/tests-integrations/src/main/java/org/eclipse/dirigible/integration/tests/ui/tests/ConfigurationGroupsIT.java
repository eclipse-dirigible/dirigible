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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import java.time.Duration;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * The grouped configurations (#7758): the additive REST endpoints over the running application, and
 * the two pages that render them - the IDE Configurations view and the shell's Tenant Configuration
 * panel. Both pages fail silently when a binding throws (Angular / Alpine stop rendering below it),
 * so only a browser shows that they render.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConfigurationGroupsIT extends UserInterfaceIntegrationTest {

    private static final String SECRET_KEY = "DIRIGIBLE_MAIL_PASSWORD";

    private static final String SECRET = "configuration-groups-it-secret";

    private static final String PLAIN_KEY = "DIRIGIBLE_MAIL_USERNAME";

    private static final String TENANT_KEY = "DIRIGIBLE_BRANDING_NAME";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private TenantConfigurationService tenantConfigurationService;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    @DefaultTenant
    private Tenant defaultTenant;

    @AfterEach
    void cleanUp() throws Exception {
        Configuration.remove(SECRET_KEY);
        Configuration.remove(PLAIN_KEY);
        tenantContext.execute(defaultTenant, () -> {
            tenantConfigurationService.delete(TENANT_KEY);
            return null;
        });
    }

    @Test
    void theLegacyEndpointKeepsItsShapeAndFiltersByGroup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", equalTo(Configuration.getConfigurationParameters().length))
                                                 .body("[0]", hasSize(5)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations?group=branding")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("collect { it[0] }", everyItem(
                                                         anyOf(startsWith("DIRIGIBLE_BRANDING_"), startsWith("DIRIGIBLE_THEME_")))));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations?group=nope")
                                                 .then()
                                                 .statusCode(400));
    }

    @Test
    void groupsAndDescriptorsNeverCarryASecret() {
        Configuration.set(SECRET_KEY, SECRET);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations/groups")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("id", hasItem("database"))
                                                 .body("id", not(hasItem("other"))));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations/descriptors?group=mail&onlySet=true")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(not(containsString(SECRET)))
                                                 .body("[0].group", equalTo("mail"))
                                                 .body("[0].entries.find { it.key == '" + SECRET_KEY + "' }.value", equalTo("********"))
                                                 .body("[0].entries.find { it.key == '" + SECRET_KEY + "' }.source", equalTo("runtime")));
    }

    @Test
    void tenantDescriptorsCarryTheTenantAndThePlatformValue() throws Exception {
        tenantContext.execute(defaultTenant, () -> {
            tenantConfigurationService.set(TENANT_KEY, "Tenant Brand");
            return null;
        });
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations/tenant/predefined?group=branding")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("key", everyItem(startsWith("DIRIGIBLE_BRANDING_"))));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations/tenant/descriptors?group=branding")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].group", equalTo("branding"))
                                                 .body("[0].entries.find { it.key == '" + TENANT_KEY + "' }.value",
                                                         equalTo("Tenant Brand")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations/tenant/groups")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.id == 'branding' }.set", equalTo(1)));
    }

    @Test
    void theIdeViewShowsTheGroupsAndTheSetKeys() {
        Configuration.set(PLAIN_KEY, "configuration-groups-it-user");
        ide.openPath("/services/web/view-configurations/configurations.html");

        Selenide.$(By.xpath("//*[normalize-space(text())='Databases & Data Sources']"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));
        Selenide.$(By.xpath("//td[contains(normalize-space(.), '" + PLAIN_KEY + "')]"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));
        Selenide.$(By.xpath("//td[normalize-space(.)='configuration-groups-it-user']"))
                .shouldBe(Condition.visible);
    }

    @Test
    void theTenantPanelIsGrouped() {
        ide.openPath("/services/web/application/index.html");
        // The sign-in redirect drops a fragment, so route to the section once the shell is up.
        Selenide.$(By.xpath("//span[normalize-space(text())='Dashboard']"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));
        Selenide.executeJavaScript("window.location.hash = '#/settings/tenant-configuration';");

        Selenide.$(By.xpath("//span[normalize-space(text())='Branding & Theme']"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));
        Selenide.$(By.xpath("//span[normalize-space(text())='Region & Language']"))
                .shouldBe(Condition.visible);
        // A boolean key renders as a default / on / off select, not a free-text input.
        Selenide.$(By.xpath("//span[normalize-space(text())='Documents, CMS & Print']"))
                .should(Condition.exist);
        Selenide.$(By.xpath(
                "//div[@data-slot='select']//input[@aria-label='Cms Roles Enabled' or @aria-label='Document access rules enforced']"))
                .should(Condition.exist);
    }

}
