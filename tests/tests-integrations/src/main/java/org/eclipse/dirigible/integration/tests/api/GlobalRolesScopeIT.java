/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.tenants.tenant.TenantSelectionConstants;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * With {@code DIRIGIBLE_TENANT_GLOBAL_ROLES_SCOPE=APPLICATION}, one user pool serves several
 * applications and a staff group counts only in the application it names (#8501).
 *
 * <p>
 * Runs the {@code cognito} profile against the mock identity provider: a bearer ID token that names
 * no tenant is served in the default tenant only when its groups grant a global role of this
 * application. An unscoped {@code ADMINISTRATOR} (staff of some other service in the pool) and
 * another application's {@code suite.ADMINISTRATOR} are refused like a user with no groups; a
 * {@code library.ADMINISTRATOR} enters as {@code ADMINISTRATOR}. Tenant groups work as before.
 */
@ActiveProfiles("cognito")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class GlobalRolesScopeIT extends IntegrationTest {

    private static final String APP_ID = "library";
    private static final String GROUPS_CLAIM = "cognito:groups";
    private static final String PROJECT = "global-roles-scope-it";
    private static final String WHOAMI = "/services/js/" + PROJECT + "/whoami.mjs";

    private static final String WHOAMI_SOURCE = """
            import { user } from "@aerokit/sdk/security";
            import { response } from "@aerokit/sdk/http";

            response.setContentType("application/json");
            response.println(JSON.stringify({
                name: user.getName(),
                administrator: user.isInRole("ADMINISTRATOR"),
                owner: user.isInRole("Owner")
            }));
            """;

    private static ExternalFrontendIT.MockIdentityProvider identityProvider;
    private static DirigibleTestTenant tenant;

    @LocalServerPort
    private int port;

    @Autowired
    private IRepository repository;

    @BeforeAll
    static void startIdentityProviderAndScopeGlobalRolesToTheApplication() throws Exception {
        identityProvider = ExternalFrontendIT.MockIdentityProvider.start();
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(GROUPS_CLAIM);
        DirigibleConfig.TENANT_GLOBAL_ROLES_SCOPE.setStringValue("APPLICATION");
        DirigibleConfig.OAUTH2_JWT_AUDIENCES.setStringValue(ExternalFrontendIT.CLIENT_ID);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    static void stopIdentityProvider() {
        identityProvider.stop();
        Configuration.remove(DirigibleConfig.TENANT_GLOBAL_ROLES_SCOPE.getKey());
    }

    /**
     * The cognito profile with the user pool's issuer and JWKS replaced by the mock identity provider.
     */
    @DynamicPropertySource
    static void cognitoProfile(DynamicPropertyRegistry registry) {
        registry.add("DIRIGIBLE_COGNITO_CLIENT_ID", () -> ExternalFrontendIT.CLIENT_ID);
        registry.add("DIRIGIBLE_COGNITO_CLIENT_SECRET", () -> ExternalFrontendIT.CLIENT_SECRET);
        registry.add("DIRIGIBLE_COGNITO_DOMAIN", () -> identityProvider.issuer());
        registry.add("DIRIGIBLE_COGNITO_REGION_ID", () -> "eu-central-1");
        registry.add("DIRIGIBLE_COGNITO_USER_POOL_ID", () -> "eu-central-1_it");
        registry.add("spring.security.oauth2.client.provider.cognito.issuer-uri", () -> identityProvider.issuer());
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> identityProvider.issuer() + "/protocol/openid-connect/certs");
        registry.add("DIRIGIBLE_HOST", () -> "{baseUrl}");
    }

    @BeforeEach
    void writeWhoamiAndProvisionTheTenant() {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/whoami.mjs",
                WHOAMI_SOURCE.getBytes(StandardCharsets.UTF_8), false, "application/javascript", true);
        if (tenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("global-roles-scope-it");
        createTenants(created);
        waitForTenantsProvisioning(List.of(created));
        tenant = created;
    }

    @Test
    void anUnscopedStaffGroupIsRefusedTheDefaultTenant() {
        api(idToken("ADMINISTRATOR")).when()
                                     .get(WHOAMI)
                                     .then()
                                     .statusCode(403);
    }

    @Test
    void aStaffGroupOfAnotherApplicationIsRefusedTheDefaultTenant() {
        api(idToken("suite.ADMINISTRATOR", "suite.DEVELOPER")).when()
                                                              .get(WHOAMI)
                                                              .then()
                                                              .statusCode(403);
    }

    @Test
    void aStaffGroupOfThisApplicationEntersTheDefaultTenantWithItsRole() {
        api(idToken(APP_ID + ".ADMINISTRATOR")).when()
                                               .get(WHOAMI)
                                               .then()
                                               .statusCode(200)
                                               .body("administrator", equalTo(true));
    }

    @Test
    void tenantGroupsWorkAsBeforeAndAnUnscopedStaffGroupAddsNothingInTheTenant() {
        api(idToken("ADMINISTRATOR", tenant.getId() + "." + APP_ID + ".Owner"))
                                                                               .header(TenantSelectionConstants.TENANT_HEADER,
                                                                                       tenant.getId())
                                                                               .when()
                                                                               .get(WHOAMI)
                                                                               .then()
                                                                               .statusCode(200)
                                                                               .body("owner", equalTo(true))
                                                                               .body("administrator", equalTo(false));
    }

    private static String idToken(String... groups) {
        return identityProvider.idToken("jane", List.of(), claims -> claims.claim("token_use", "id")
                                                                           .claim("typ", null)
                                                                           .claim("azp", null)
                                                                           .claim("groups", null)
                                                                           .claim(GROUPS_CLAIM, List.of(groups)));
    }

    private RequestSpecification api(String token) {
        return given().port(port)
                      .auth()
                      .oauth2(token);
    }
}
