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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
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
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * Integration clients end to end, under {@code TOKEN_GROUPS} with access tokens held to the
 * deployment's own client: the provisioning system writes a tenant's client registry through the
 * tenant provisioning API, and a registered client's own access token then works in that tenant
 * only, with the roles the registry gives it there - never its scopes, never another tenant, never
 * the default tenant, and not after it was revoked.
 */
@ActiveProfiles("keycloak")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class IntegrationClientsIT extends IntegrationTest {

    private static final String PROVISIONING_PATH = "/services/tenant-provisioning/";
    private static final String CURRENT_TENANT_PATH = "/services/security/tenant-selection/current";
    private static final String TENANT_CONFIGURATIONS_PATH = "/services/core/configurations/tenant";
    private static final String PROJECT = "integration-clients-it";
    private static final String WHOAMI = "/services/js/" + PROJECT + "/whoami.mjs";

    private static final String APP_ID = "library";
    private static final String READ_ROLE = PROJECT + ".Vacations.VacationRequestReadOnly";

    /** The scopes a client's token carries - which must grant it nothing. */
    private static final String CLIENT_SCOPES = "openid dirigible/OPERATOR dirigible/integration";

    private static final String WHOAMI_SOURCE = """
            import { user } from "@aerokit/sdk/security";
            import { response } from "@aerokit/sdk/http";

            response.setContentType("application/json");
            response.println(JSON.stringify({
                name: user.getName(),
                reader: user.isInRole("%s"),
                operator: user.isInRole("OPERATOR"),
                integration: user.isInRole("integration")
            }));
            """.formatted(READ_ROLE);

    private static final AtomicLong REVISION = new AtomicLong();

    private static ExternalFrontendIT.MockIdentityProvider identityProvider;
    private static DirigibleTestTenant workspace;
    private static DirigibleTestTenant otherWorkspace;

    @LocalServerPort
    private int port;

    @Autowired
    private IRepository repository;

    @Autowired
    private SecurityUtil securityUtil;

    @BeforeAll
    static void startIdentityProviderAndAcceptOnlyOurOwnClients() throws Exception {
        identityProvider = ExternalFrontendIT.MockIdentityProvider.start();
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue("groups");
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

    @DynamicPropertySource
    static void keycloakProfile(DynamicPropertyRegistry registry) {
        registry.add("DIRIGIBLE_KEYCLOAK_AUTH_SERVER_URL", () -> identityProvider.issuer());
        registry.add("DIRIGIBLE_KEYCLOAK_CLIENT_ID", () -> ExternalFrontendIT.CLIENT_ID);
        registry.add("DIRIGIBLE_KEYCLOAK_CLIENT_SECRET", () -> ExternalFrontendIT.CLIENT_SECRET);
        registry.add("DIRIGIBLE_HOST", () -> "{baseUrl}");
    }

    @BeforeEach
    void provisionTwoWorkspacesAndWriteWhoami() {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/whoami.mjs",
                WHOAMI_SOURCE.getBytes(StandardCharsets.UTF_8), false, "application/javascript", true);
        securityUtil.ensureRole(READ_ROLE);
        if (workspace != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("integration-clients-it");
        DirigibleTestTenant other = new DirigibleTestTenant("integration-clients-other-it");
        createTenants(created, other);
        waitForTenantsProvisioning(List.of(created, other));
        workspace = created;
        otherWorkspace = other;
    }

    @Test
    void aRegisteredClientWorksInItsTenantWithTheRolesItWasGivenAndNothingFromItsScopes() {
        register(workspace, client("reader-client", READ_ROLE)).body("applied", equalTo(true))
                                                               .body("active", equalTo(1));
        String token = clientToken("reader-client");

        api(token).header(TenantSelectionConstants.TENANT_HEADER, workspace.getId())
                  .when()
                  .get(CURRENT_TENANT_PATH)
                  .then()
                  .statusCode(200)
                  .body("tenant.id", equalTo(workspace.getId()));
        api(token).header(TenantSelectionConstants.TENANT_HEADER, workspace.getId())
                  .when()
                  .get(WHOAMI)
                  .then()
                  .statusCode(200)
                  .body("name", equalTo("reader-client"))
                  .body("reader", equalTo(true))
                  .body("operator", equalTo(false))
                  .body("integration", equalTo(false));
    }

    @Test
    void aRegisteredClientNamingNoTenantOrAnotherOneIsRefused() {
        register(workspace, client("confined-client", READ_ROLE));
        String token = clientToken("confined-client");

        api(token).when()
                  .get(WHOAMI)
                  .then()
                  .statusCode(403)
                  .contentType(ContentType.JSON)
                  .body("reason", equalTo("TENANT_REQUIRED"));
        // the platform's own surfaces, which a user reaches before choosing a tenant, included
        api(token).when()
                  .get(CURRENT_TENANT_PATH)
                  .then()
                  .statusCode(403)
                  .body("reason", equalTo("TENANT_REQUIRED"));
        api(token).header(TenantSelectionConstants.TENANT_HEADER, otherWorkspace.getId())
                  .when()
                  .get(WHOAMI)
                  .then()
                  .statusCode(403)
                  .body("reason", equalTo("NOT_A_MEMBER"));
    }

    @Test
    void aRoleTheClientWasNotGivenIsForbidden() {
        register(workspace, client("limited-client", READ_ROLE));

        api(clientToken("limited-client")).header(TenantSelectionConstants.TENANT_HEADER, workspace.getId())
                                          .when()
                                          .get(TENANT_CONFIGURATIONS_PATH)
                                          .then()
                                          .statusCode(403);
    }

    @Test
    void aRevokedClientIsRefusedOnItsNextCallWhateverItsTokensLifetime() {
        register(workspace, client("revoked-client", READ_ROLE));
        String token = clientToken("revoked-client");
        api(token).header(TenantSelectionConstants.TENANT_HEADER, workspace.getId())
                  .when()
                  .get(WHOAMI)
                  .then()
                  .statusCode(200);

        register(workspace).body("applied", equalTo(true));

        api(token).header(TenantSelectionConstants.TENANT_HEADER, workspace.getId())
                  .when()
                  .get(WHOAMI)
                  .then()
                  .statusCode(401);
        // and a late registry naming it as active cannot bring it back
        register(workspace, client("revoked-client", READ_ROLE)).body("active", equalTo(0));
        api(token).header(TenantSelectionConstants.TENANT_HEADER, workspace.getId())
                  .when()
                  .get(WHOAMI)
                  .then()
                  .statusCode(401);
    }

    @Test
    void anUnregisteredClientIsRefusedAsBefore() {
        api(clientToken("stranger-client")).header(TenantSelectionConstants.TENANT_HEADER, workspace.getId())
                                           .when()
                                           .get(WHOAMI)
                                           .then()
                                           .statusCode(401);
    }

    @Test
    void theRegistryNeverGivesAClientTheOwnerOrARoleOfTheInstance() {
        provisioner().contentType(ContentType.JSON)
                     .body(registry(client("greedy-client", "Owner", "ADMINISTRATOR")))
                     .when()
                     .put(PROVISIONING_PATH + "tenants/" + workspace.getId() + "/clients")
                     .then()
                     .statusCode(400)
                     .body("message", containsString("[Owner] is never given to a client"))
                     .body("message", containsString("[ADMINISTRATOR] is never given to a client"));
    }

    @Test
    void theRolesAClientMayBeGivenAreTheApplicationsOwn() {
        provisioner().when()
                     .get(PROVISIONING_PATH + "client-roles")
                     .then()
                     .statusCode(200)
                     .body("roles", hasItem(READ_ROLE))
                     .body("roles", not(hasItem("ADMINISTRATOR")))
                     .body("roles", not(hasItem("Owner")));
    }

    @Test
    void aClientBelongsToOneTenant() {
        register(workspace, client("shared-client", READ_ROLE));

        provisioner().contentType(ContentType.JSON)
                     .body(registry(client("shared-client", READ_ROLE)))
                     .when()
                     .put(PROVISIONING_PATH + "tenants/" + otherWorkspace.getId() + "/clients")
                     .then()
                     .statusCode(409);
    }

    /**
     * Writes a tenant's complete registry at the next revision, as the provisioning system does.
     */
    @SafeVarargs
    private ValidatableResponse register(DirigibleTestTenant tenant, Map<String, Object>... clients) {
        return provisioner().contentType(ContentType.JSON)
                            .body(registry(clients))
                            .when()
                            .put(PROVISIONING_PATH + "tenants/" + tenant.getId() + "/clients")
                            .then()
                            .statusCode(200);
    }

    @SafeVarargs
    private static Map<String, Object> registry(Map<String, Object>... clients) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("revision", REVISION.incrementAndGet());
        body.put("clients", List.of(clients));
        return body;
    }

    private static Map<String, Object> client(String clientId, String... roles) {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("clientId", clientId);
        client.put("name", "The " + clientId);
        client.put("roles", List.of(roles));
        client.put("createdBy", "staff@codbex.com");
        client.put("createdAt", "2026-11-02T10:00:00Z");
        return client;
    }

    /** The access token of the provisioning system, a client of the deployment's own audience. */
    private RequestSpecification provisioner() {
        return api(identityProvider.accessToken("service-account-provisioning", "openid dirigible/TENANT_PROVISIONER",
                claims -> claims.audience(ExternalFrontendIT.CLIENT_ID)));
    }

    /** The client-credentials access token of a client other than the deployment's own. */
    private static String clientToken(String clientId) {
        return identityProvider.accessToken("service-account-" + clientId, CLIENT_SCOPES, claims -> claims.claim("azp", clientId)
                                                                                                          .claim("client_id", clientId));
    }

    private RequestSpecification api(String token) {
        return given().port(port)
                      .auth()
                      .oauth2(token);
    }
}
