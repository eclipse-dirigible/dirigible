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
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.components.tenants.tenant.TenantSelectionConstants;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
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
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * A bearer request on the {@code cognito} profile runs in the tenant its
 * {@link TenantSelectionConstants#TENANT_HEADER} header names (#8458).
 *
 * <p>
 * The profile's security chain left the tenant scope to the servlet filter Spring Boot registers
 * for {@code TenantContextInitFilter}, which runs after the whole chain. It opened every bearer
 * request in the default tenant again after the tenant selection had entered the one the header
 * named, so an integration client and a user's ID token were served from the default tenant with
 * the roles of the tenant they named. The tenant selection ITs run on the {@code keycloak} profile,
 * whose chain opens the scope first - so this one runs the {@code cognito} profile against the same
 * mock identity provider, with the tokens shaped as Cognito issues them: the kind in
 * {@code token_use}, an access token's client in {@code client_id}, the groups in
 * {@code cognito:groups}.
 */
@ActiveProfiles("cognito")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class CognitoBearerTenantIT extends IntegrationTest {

    private static final String PROVISIONING_PATH = "/services/tenant-provisioning/";
    private static final String CURRENT_TENANT_PATH = "/services/security/tenant-selection/current";
    private static final String TENANT_CONFIGURATIONS_PATH = "/services/core/configurations/tenant";
    private static final String PROJECT = "cognito-bearer-tenant-it";
    private static final String WHOAMI = "/services/js/" + PROJECT + "/whoami.mjs";

    /**
     * A value stored in each tenant's own configuration - which one a request reads names its tenant.
     */
    private static final String MARKER_KEY = "COGNITO_BEARER_TENANT_IT_MARKER";
    private static final String MARKER_OF_DEFAULT_TENANT = "marker-of-the-default-tenant";
    private static final String MARKER_OF_NAMED_TENANT = "marker-of-the-named-tenant";
    private static final String MARKER_VALUE = "find { it.key == '" + MARKER_KEY + "' }.value";

    private static final String APP_ID = "library";
    private static final String GROUPS_CLAIM = "cognito:groups";
    private static final String OPERATOR_GROUP = "OPERATOR";
    private static final String READ_ROLE = PROJECT + ".Books.BookReadOnly";
    private static final String CLIENT_ID = "cognito-reader-client";

    private static final String WHOAMI_SOURCE = """
            import { user } from "@aerokit/sdk/security";
            import { response } from "@aerokit/sdk/http";

            response.setContentType("application/json");
            response.println(JSON.stringify({
                name: user.getName(),
                reader: user.isInRole("%s")
            }));
            """.formatted(READ_ROLE);

    private static ExternalFrontendIT.MockIdentityProvider identityProvider;
    private static DirigibleTestTenant tenant;

    @LocalServerPort
    private int port;

    @Autowired
    private IRepository repository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    @DefaultTenant
    private Tenant defaultTenant;

    @Autowired
    private TenantConfigurationService tenantConfigurationService;

    @BeforeAll
    static void startIdentityProviderAndSelectTenantsByCognitoGroups() throws Exception {
        identityProvider = ExternalFrontendIT.MockIdentityProvider.start();
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(GROUPS_CLAIM);
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
        // as on the platform: a token is accepted from the deployment's own client, or from a registered
        // one
        DirigibleConfig.OAUTH2_JWT_AUDIENCES.setStringValue(ExternalFrontendIT.CLIENT_ID);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    static void stopIdentityProvider() {
        identityProvider.stop();
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

    /**
     * Provisions the tenant and seeds the markers, once for the class, and writes the script the client
     * reads its identity through.
     *
     * @throws SQLException if a marker cannot be written
     */
    @BeforeEach
    void provisionTenantSeedMarkersAndWriteWhoami() throws SQLException {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/whoami.mjs",
                WHOAMI_SOURCE.getBytes(StandardCharsets.UTF_8), false, "application/javascript", true);
        securityUtil.ensureRole(READ_ROLE);
        if (tenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("cognito-bearer-tenant-it");
        createTenants(created);
        waitForTenantsProvisioning(List.of(created));
        tenant = created;

        seedMarker(defaultTenant.getId(), MARKER_OF_DEFAULT_TENANT);
        seedMarker(tenant.getId(), MARKER_OF_NAMED_TENANT);
    }

    @Test
    void anIntegrationClientRunsInTheTenantItNames() {
        provisioner().contentType(ContentType.JSON)
                     .body(Map.of("revision", 1, "clients", List.of(client(CLIENT_ID, READ_ROLE))))
                     .when()
                     .put(PROVISIONING_PATH + "tenants/" + tenant.getId() + "/clients")
                     .then()
                     .statusCode(200)
                     .body("active", equalTo(1));
        String token = accessToken(CLIENT_ID, "openid dirigible/integration");

        api(token).header(TenantSelectionConstants.TENANT_HEADER, tenant.getId())
                  .when()
                  .get(CURRENT_TENANT_PATH)
                  .then()
                  .statusCode(200)
                  .body("tenant.id", equalTo(tenant.getId()));
        api(token).header(TenantSelectionConstants.TENANT_HEADER, tenant.getId())
                  .when()
                  .get(WHOAMI)
                  .then()
                  .statusCode(200)
                  .body("name", equalTo(CLIENT_ID))
                  .body("reader", equalTo(true));
    }

    @Test
    void aUsersIdTokenRunsInTheTenantItNames() {
        String token = identityProvider.idToken("jane", List.of(), claims -> claims.claim("token_use", "id")
                                                                                   .claim("typ", null)
                                                                                   .claim("azp", null)
                                                                                   .claim("groups", null)
                                                                                   .claim(GROUPS_CLAIM,
                                                                                           List.of(OPERATOR_GROUP, ownerOf(tenant))));

        api(token).header(TenantSelectionConstants.TENANT_HEADER, tenant.getId())
                  .when()
                  .get(TENANT_CONFIGURATIONS_PATH)
                  .then()
                  .statusCode(200)
                  .body(MARKER_VALUE, equalTo(MARKER_OF_NAMED_TENANT));
    }

    /** The access token of the provisioning system, a client of the deployment's own. */
    private RequestSpecification provisioner() {
        return api(accessToken(ExternalFrontendIT.CLIENT_ID, "openid dirigible/TENANT_PROVISIONER"));
    }

    /**
     * A client-credentials access token as Cognito issues it: the client in {@code client_id}, no
     * audience.
     */
    private static String accessToken(String clientId, String scope) {
        return identityProvider.accessToken("sub-" + clientId, scope, claims -> claims.claim("token_use", "access")
                                                                                      .claim("typ", null)
                                                                                      .claim("aud", null)
                                                                                      .claim("azp", null)
                                                                                      .claim("client_id", clientId));
    }

    private static Map<String, Object> client(String clientId, String... roles) {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("clientId", clientId);
        client.put("name", "The " + clientId);
        client.put("roles", List.of(roles));
        client.put("createdBy", "staff@codbex.com");
        client.put("createdAt", "2026-11-03T10:00:00Z");
        return client;
    }

    private static String ownerOf(DirigibleTestTenant tenant) {
        return tenant.getId() + "." + APP_ID + ".Owner";
    }

    private RequestSpecification api(String token) {
        return given().port(port)
                      .auth()
                      .oauth2(token);
    }

    private void seedMarker(String tenantId, String marker) throws SQLException {
        try {
            tenantContext.execute(tenantId, () -> {
                tenantConfigurationService.set(MARKER_KEY, marker);
                return null;
            });
        } catch (SQLException | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to seed the marker of tenant [" + tenantId + "]", ex);
        }
    }
}
