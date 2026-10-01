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

import static org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport.drainQueue;
import static org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport.pushSnapshot;
import static org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport.receive;
import static org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.KeycloakUserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Settings > Users, in a browser, against a real Keycloak: an owner invites a person with two
 * roles, follows the change while the provisioning system works on it, edits the person's roles in
 * one save, reads why a change was not applied, removes the person, and is refused - locally - when
 * they would leave the tenant without an owner. The provisioning system is played by the test: it
 * reads the change requests off the queue and pushes the snapshots back through the users snapshot
 * endpoint.
 */
class TenantUsersSettingsIT extends KeycloakUserInterfaceIntegrationTest {

    private static final String SECTION = "/services/web/application/index.html#/settings/tenant-users";
    private static final String QUEUE = "global:it.tenant-users.changes";
    private static final String OWNER = "owner.users@example.com";
    private static final String MEMBER = "member.users@example.com";
    private static final String ANN = "ann.invited@example.com";
    private static final Duration WAIT = Duration.ofSeconds(30);

    private static DirigibleTestTenant tenant;

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    static void enableTenantUsers() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_USERS_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.setStringValue(QUEUE);
    }

    @BeforeEach
    void provisionTheTenantOnce() throws Exception {
        if (tenant != null) {
            return;
        }
        DirigibleTestTenant created = provisionTenant("tenant-users-settings-it");
        grant(OWNER, created, "Owner");
        grant(MEMBER, created, "User");
        // the owner as the provisioning system recorded them when it provisioned the tenant
        pushSnapshot(mvc, created.getId(), 1, user(OWNER).revision(1)
                                                         .role("Owner", "GRANTED")
                                                         .role("User", "GRANTED"));
        drainQueue(QUEUE);
        tenant = created;
    }

    @Test
    void anOwnerManagesTheTenantsUsers() throws Exception {
        signInAndOpen("/services/web/application/index.html", OWNER);
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));
        browser.openPath(SECTION);
        Selenide.$(By.id("tenant-users-page"))
                .shouldBe(Condition.visible, WAIT);

        // 1. the owner's own row: the role, and "you"
        row(OWNER).shouldBe(Condition.visible, WAIT)
                  .shouldHave(Condition.text("you"));
        row(OWNER).$("[data-role='Owner']")
                  .shouldHave(Condition.attribute("data-state", "GRANTED"));

        // 2. an invite with two roles is one request, and the row reads "Sending..." until the list shows
        // it
        Selenide.$(By.id("tenant-users-email"))
                .setValue("Ann.Invited@Example.com");
        Selenide.executeJavaScript("Alpine.store('tenantUsers').invite.roles = ['Owner', 'User'];");
        Selenide.$(By.id("tenant-users-invite-button"))
                .click();
        row(ANN).shouldBe(Condition.visible, WAIT)
                .shouldHave(Condition.text("Sending…"));
        JsonNode invite = receive(QUEUE, 10_000);
        assertEquals("INVITE", invite.get("action")
                                     .asText());
        assertEquals(List.of("Owner", "User"), roles(invite));
        assertEquals(OWNER, invite.get("requestedBy")
                                  .asText());

        // 3. the provisioning system accepted it: the change is in progress, the roles being added show
        pushSnapshot(mvc, tenant.getId(), 2, user(ANN).revision(2)
                                                      .status("PENDING")
                                                      .role("Owner", "ADDING")
                                                      .role("User", "ADDING"));
        row(ANN).$("[data-pending]")
                .shouldHave(Condition.text("Change in progress"), WAIT);
        row(ANN).shouldNotHave(Condition.text("Sending…"));
        row(ANN).$("[data-role='Owner']")
                .shouldHave(Condition.attribute("data-state", "ADDING"));

        // 4. ... and carried it out
        pushSnapshot(mvc, tenant.getId(), 3, user(ANN).revision(3)
                                                      .status("INVITED")
                                                      .role("Owner", "GRANTED")
                                                      .role("User", "GRANTED"));
        row(ANN).$("[data-role='Owner']")
                .shouldHave(Condition.attribute("data-state", "GRANTED"), WAIT);
        row(ANN).shouldHave(Condition.text("Invited"));

        // 5. one save that takes a role away: the dialog shows the difference, the request carries the
        // revision
        openMenu(ANN, "edit");
        Selenide.$(By.id("tenant-users-edit-dialog"))
                .shouldBe(Condition.visible, WAIT);
        Selenide.$("label[for='tenant-users-edit-role-Owner']")
                .click();
        Selenide.$("#tenant-users-edit-dialog [data-diff]")
                .shouldHave(Condition.text("− Owner"));
        Selenide.$(By.id("tenant-users-edit-save"))
                .click();
        JsonNode change = receive(QUEUE, 10_000);
        assertEquals("UPDATE_ROLES", change.get("action")
                                           .asText());
        assertEquals(List.of("User"), roles(change));
        assertEquals(3, change.get("expectedRevision")
                              .asLong());

        // 6. the provisioning system refused it: the reason is shown under the row
        pushSnapshot(mvc, tenant.getId(), 4, user(ANN).revision(4)
                                                      .status("INVITED")
                                                      .role("Owner", "GRANTED")
                                                      .role("User", "GRANTED")
                                                      .lastError("LAST_OWNER", "refused"));
        row(ANN).$("[data-failure]")
                .shouldHave(Condition.text("The tenant would be left without an owner."), WAIT);

        // 7. a removal, confirmed: one request without roles, and the row goes once the system reports it
        openMenu(ANN, "remove");
        Selenide.$(By.id("tenant-users-remove-dialog"))
                .shouldBe(Condition.visible, WAIT);
        Selenide.$(By.id("tenant-users-remove-confirm"))
                .click();
        JsonNode removal = receive(QUEUE, 10_000);
        assertEquals("REMOVE", removal.get("action")
                                      .asText());
        assertFalse(removal.has("roles"), "a removal carries no roles");
        assertEquals(4, removal.get("expectedRevision")
                               .asLong());
        pushSnapshot(mvc, tenant.getId(), 5, user(ANN).revision(5)
                                                      .status("REMOVED"));
        row(ANN).shouldNot(Condition.exist, WAIT);

        // 8. the only owner giving up the role: warned in the dialog, refused by the platform, nothing
        // published
        openMenu(OWNER, "edit");
        Selenide.$(By.id("tenant-users-edit-dialog"))
                .shouldBe(Condition.visible, WAIT);
        Selenide.$("label[for='tenant-users-edit-role-Owner']")
                .click();
        Selenide.$("#tenant-users-edit-dialog [role='alert']")
                .shouldHave(Condition.text("without an owner"), WAIT);
        Selenide.$(By.id("tenant-users-edit-save"))
                .click();
        Awaitility.await()
                  .pollInSameThread()
                  .atMost(WAIT)
                  .pollInterval(Duration.ofMillis(250))
                  .until(() -> String.valueOf(Selenide.<Object>executeJavaScript("return Alpine.store('tenantUsers').error;"))
                                     .contains("only owner"));
        assertTrue(drainQueue(QUEUE).isEmpty(), "a refused change publishes nothing");
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
                          Selenide.executeJavaScript("var s = window.Alpine && Alpine.store('tenantUsers'); return !!(s && s.context);")));
        assertEquals(Boolean.FALSE, Selenide.executeJavaScript("return Alpine.store('tenantUsers').visible;"));
        Selenide.$(By.id("tenant-users-page"))
                .shouldNot(Condition.exist);
    }

    private static SelenideElement row(String email) {
        return Selenide.$("#tenant-users-table tr[data-user='" + email + "']");
    }

    /** Opens a row's action menu and picks an item. */
    private static void openMenu(String email, String action) {
        row(email).$("button[aria-label='Actions for " + email + "']")
                  .shouldBe(Condition.visible, WAIT)
                  .click();
        Selenide.$$("li[data-action='" + action + "']")
                .findBy(Condition.visible)
                .click();
    }

    private static List<String> roles(JsonNode envelope) {
        return java.util.stream.StreamSupport.stream(envelope.get("roles")
                                                             .spliterator(),
                false)
                                             .map(JsonNode::asText)
                                             .toList();
    }
}
