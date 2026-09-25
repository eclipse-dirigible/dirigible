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

/**
 * The Keycloak harness itself: a person whose token names exactly one tenant signs in through a
 * real identity provider, is placed in that tenant, and the shell's tenant chip names it.
 */
class KeycloakTokenGroupsSignInIT extends KeycloakUserInterfaceIntegrationTest {

    @Test
    void anOwnerOfOneTenantSignsInAndIsPlacedInIt() {
        DirigibleTestTenant tenant = provisionTenant("keycloak-sign-in-it");
        grant("owner.signin@example.com", tenant, "Owner");

        signInAndOpen("/services/web/application/index.html", "owner.signin@example.com");

        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60))
                .shouldHave(Condition.text(tenant.getName()));
    }
}
