/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import java.time.Duration;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.framework.keycloak.KeycloakTestContainer;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * A browser test that signs in through a real Keycloak ({@link KeycloakTestContainer}) under
 * {@code TOKEN_GROUPS}: a user's tenants and roles come from the groups
 * {@code <tenantId>.<appId>.<role>} of their token, exactly as in a deployment. One Dirigible boot
 * per class; one Keycloak per JVM.
 *
 * <p>
 * Requires Docker. Tagged {@code ui} through the parent, so it runs in the nightly UI shard.
 */
@ActiveProfiles("keycloak")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class KeycloakUserInterfaceIntegrationTest extends UserInterfaceIntegrationTest {

    /** The application id of the group names. */
    protected static final String APP_ID = "library";

    /** The password every test user gets. */
    protected static final String PASSWORD = "Test-password-1";

    /** The Keycloak. */
    protected static final KeycloakTestContainer KEYCLOAK = KeycloakTestContainer.shared();

    /** The port Dirigible listens on. */
    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void keycloakProfile(DynamicPropertyRegistry registry) {
        registry.add("DIRIGIBLE_KEYCLOAK_AUTH_SERVER_URL", KEYCLOAK::issuer);
        registry.add("DIRIGIBLE_KEYCLOAK_CLIENT_ID", () -> KeycloakTestContainer.CLIENT_ID);
        registry.add("DIRIGIBLE_KEYCLOAK_CLIENT_SECRET", () -> KeycloakTestContainer.CLIENT_SECRET);
        // the callback template - Spring expands it to the base URL of the request that starts the login
        registry.add("DIRIGIBLE_HOST", () -> "{baseUrl}");
    }

    @BeforeAll
    static void useTokenGroups() {
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(KeycloakTestContainer.GROUPS_CLAIM);
    }

    @BeforeEach
    final void allowTheLoginCallback() {
        KEYCLOAK.allowRedirectsTo("http://localhost:" + port);
    }

    /**
     * Registers and provisions a tenant in this instance.
     *
     * @param name the tenant name
     * @return the tenant
     */
    protected DirigibleTestTenant provisionTenant(String name) {
        DirigibleTestTenant tenant = new DirigibleTestTenant(name);
        createTenants(tenant);
        waitForTenantProvisioning(tenant);
        return tenant;
    }

    /**
     * Makes a person hold a role in a tenant: a Keycloak user in the group
     * {@code <tenantId>.<appId>.<role>}.
     *
     * @param email the person
     * @param tenant the tenant
     * @param role the role
     */
    protected void grant(String email, DirigibleTestTenant tenant, String role) {
        KEYCLOAK.ensureUser(email, PASSWORD, tenant.getId() + "." + APP_ID + "." + role);
    }

    /**
     * Opens a page and signs in on the Keycloak form it redirects to.
     *
     * @param path the page
     * @param email the person
     */
    protected void signInAndOpen(String path, String email) {
        browser.openPath(path);
        Selenide.$(By.id("username"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60))
                .setValue(email);
        Selenide.$(By.id("password"))
                .setValue(PASSWORD);
        Selenide.$(By.id("kc-login"))
                .click();
    }
}
