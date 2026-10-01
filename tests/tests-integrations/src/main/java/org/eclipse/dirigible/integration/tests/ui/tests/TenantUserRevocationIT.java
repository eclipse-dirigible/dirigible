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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.tests.base.KeycloakUserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * A role taken away at the identity provider stops counting at the person's next access-token
 * refresh: the session re-validates its token, and a tenant selection the new token no longer
 * grants is dropped. Nothing forces the refresh - the token's lifespan is the window - so the
 * realm's lifespan is shortened for this test and restored after it.
 */
class TenantUserRevocationIT extends KeycloakUserInterfaceIntegrationTest {

    private static final String MEMBER = "member.revoked@example.com";

    /** The selection as the browser's session sees it: the tenant id, or "none" once it is gone. */
    private static final String SELECTION = "var done = arguments[arguments.length - 1];"
            + "fetch('/services/security/tenant-selection', { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })"
            + ".then(function (r) { return r.ok ? r.json() : null; })"
            + ".then(function (state) { done(state && state.selectedTenantId ? state.selectedTenantId : 'none'); })"
            + ".catch(function () { done('error'); });";

    @Test
    void aRemovedGroupDropsTheTenantAtTheNextRefresh() {
        int lifespan = KEYCLOAK.accessTokenLifespan();
        KEYCLOAK.setAccessTokenLifespan(30);
        try {
            DirigibleTestTenant tenant = provisionTenant("tenant-user-revocation-it");
            grant(MEMBER, tenant, "User");
            signInAndOpen("/services/web/application/index.html", MEMBER);
            Selenide.$(By.id("tenant-menu-trigger"))
                    .shouldBe(Condition.visible, Duration.ofSeconds(60));
            assertTrue(tenant.getId()
                             .equals(Selenide.executeAsyncJavaScript(SELECTION)),
                    "the member is in the tenant before the role is taken away");

            KEYCLOAK.removeUserFromGroup(MEMBER, tenant.getId() + "." + APP_ID + ".User");

            // A token shorter-lived than the refresh skew is refreshed on the very next request, which
            // brings the groups without the tenant - and the selection goes.
            Awaitility.await()
                      .pollInSameThread()
                      .atMost(Duration.ofSeconds(90))
                      .pollInterval(Duration.ofSeconds(5))
                      .until(() -> !tenant.getId()
                                          .equals(Selenide.executeAsyncJavaScript(SELECTION)));
        } finally {
            KEYCLOAK.setAccessTokenLifespan(lifespan);
        }
    }
}
