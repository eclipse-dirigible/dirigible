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
import static org.hamcrest.Matchers.nullValue;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
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
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * An external frontend on a {@code TOKEN_GROUPS} deployment reaches the tenants its user's groups
 * grant (#7446): a bearer request names its tenant in the {@code X-Tenant-Id} header and is served
 * in it with the roles of that tenant, validated exactly as a session selection is. Without the
 * header nothing changes for a deployment that does not opt in - default tenant, global roles only,
 * a user of tenants only refused.
 *
 * <p>
 * The platform runs the {@code keycloak} profile against the in-process OpenID Connect provider of
 * {@link ExternalFrontendIT}, with the strategy selected in a static {@code @BeforeAll} so that the
 * beans reading it at context refresh see it. Which tenant a request landed in is observed through
 * the tenant's own configuration table, as {@code TokenGroupsTenantResolutionIT} does: a marker
 * seeded per tenant identifies the resolved tenant beyond doubt.
 */
@ActiveProfiles("keycloak")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class ExternalFrontendTenantSelectionIT extends IntegrationTest {

    private static final String TENANT_CONFIGURATIONS_PATH = "/services/core/configurations/tenant";
    private static final String TENANT_SELECTION_PATH = "/services/security/tenant-selection";
    private static final String PROJECT = "external-frontend-tenant-it";
    private static final String WHOAMI = "/services/js/" + PROJECT + "/whoami.mjs";

    /** Not an allow-listed key on purpose - it is stored and read back, never injected. */
    private static final String MARKER_KEY = "EXTERNAL_FRONTEND_TENANT_IT_MARKER";
    private static final String MARKER_OF_DEFAULT_TENANT = "marker-of-the-default-tenant";
    private static final String MARKER_OF_ENTERED_TENANT = "marker-of-the-entered-tenant";
    private static final String MARKER_VALUE = "find { it.key == '" + MARKER_KEY + "' }.value";

    private static final String APP_ID = "library";
    private static final String GROUPS_CLAIM = "groups";

    /**
     * A group naming no tenant is a role of the whole instance. OPERATOR rather than ADMINISTRATOR,
     * because the platform's role check answers true for every role to an ADMINISTRATOR, which would
     * hide whether the tenant role was granted.
     */
    private static final String OPERATOR_GROUP = "OPERATOR";

    /** A tenant id no test ever registers: the identity provider knows it, this instance does not. */
    private static final String STRANGER_TENANT = "tenant-that-was-never-registered";

    private static final String WHOAMI_SOURCE = """
            import { user } from "@aerokit/sdk/security";
            import { response } from "@aerokit/sdk/http";

            response.setContentType("application/json");
            response.println(JSON.stringify({
                name: user.getName(),
                owner: user.isInRole("Owner"),
                operator: user.isInRole("OPERATOR")
            }));
            """;

    private static ExternalFrontendIT.MockIdentityProvider identityProvider;
    private static DirigibleTestTenant provisionedTenant;
    private static DirigibleTestTenant tenantAwaitingProvisioning;

    @LocalServerPort
    private int port;

    @Autowired
    private IRepository repository;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    @DefaultTenant
    private Tenant defaultTenant;

    @Autowired
    private TenantConfigurationService tenantConfigurationService;

    @BeforeAll
    static void startIdentityProviderAndSelectTenantsByGroups() throws Exception {
        identityProvider = ExternalFrontendIT.MockIdentityProvider.start();
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(GROUPS_CLAIM);
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

    /**
     * Provisions the tenants and seeds the markers, once for the class, and writes the script every
     * case reads its identity through.
     *
     * @throws SQLException if a marker cannot be written
     */
    @BeforeEach
    void provisionTenantsSeedMarkersAndWriteWhoami() throws SQLException {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/whoami.mjs",
                WHOAMI_SOURCE.getBytes(StandardCharsets.UTF_8), false, "application/javascript", true);
        if (provisionedTenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("external-frontend-tenant-it");
        createTenants(created);
        waitForTenantProvisioning(created);
        provisionedTenant = created;

        // Registered after the provisioning pass, so it stays in status INITIAL for this class.
        DirigibleTestTenant awaiting = new DirigibleTestTenant("external-frontend-tenant-unprovisioned-it");
        createTenants(awaiting);
        tenantAwaitingProvisioning = awaiting;

        seedMarker(defaultTenant.getId(), MARKER_OF_DEFAULT_TENANT);
        seedMarker(provisionedTenant.getId(), MARKER_OF_ENTERED_TENANT);
    }

    @Test
    void theHeaderEntersATenantTheGroupsGrantWithTheRolesOfThatTenant() {
        String token = identityProvider.idToken("jane", List.of(OPERATOR_GROUP, ownerOf(provisionedTenant)));

        api().auth()
             .oauth2(token)
             .header(TenantSelectionConstants.TENANT_HEADER, provisionedTenant.getId())
             .when()
             .get(TENANT_CONFIGURATIONS_PATH)
             .then()
             .statusCode(200)
             .body(MARKER_VALUE, equalTo(MARKER_OF_ENTERED_TENANT));

        api().auth()
             .oauth2(token)
             .header(TenantSelectionConstants.TENANT_HEADER, provisionedTenant.getId())
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("name", equalTo("jane"))
             .body("owner", equalTo(true))
             .body("operator", equalTo(true));
    }

    @Test
    void withoutTheHeaderTheRequestStaysInTheDefaultTenantWithTheGlobalRolesOnly() {
        String token = identityProvider.idToken("jane", List.of(OPERATOR_GROUP, ownerOf(provisionedTenant)));

        api().auth()
             .oauth2(token)
             .when()
             .get(TENANT_CONFIGURATIONS_PATH)
             .then()
             .statusCode(200)
             .body(MARKER_VALUE, equalTo(MARKER_OF_DEFAULT_TENANT));

        api().auth()
             .oauth2(token)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("owner", equalTo(false))
             .body("operator", equalTo(true));
    }

    @Test
    void aUserOfTenantsOnlyHasToNameOne() {
        String token = identityProvider.idToken("bob", List.of(ownerOf(provisionedTenant)));

        api().auth()
             .oauth2(token)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(403);

        api().auth()
             .oauth2(token)
             .header(TenantSelectionConstants.TENANT_HEADER, provisionedTenant.getId())
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("name", equalTo("bob"))
             .body("owner", equalTo(true))
             .body("operator", equalTo(false));
    }

    @Test
    void aTenantTheGroupsDoNotGrantIsRefused() {
        api().auth()
             .oauth2(identityProvider.idToken("jane", List.of(OPERATOR_GROUP, ownerOf(provisionedTenant))))
             .header(TenantSelectionConstants.TENANT_HEADER, STRANGER_TENANT)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(403)
             .contentType(ContentType.JSON)
             .body("reason", equalTo("NOT_A_MEMBER"));
    }

    @Test
    void aTenantThatIsNotProvisionedYetIsAnsweredWithAConflict() {
        api().auth()
             .oauth2(identityProvider.idToken("jane", List.of(ownerOf(provisionedTenant), ownerOf(tenantAwaitingProvisioning))))
             .header(TenantSelectionConstants.TENANT_HEADER, tenantAwaitingProvisioning.getId())
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(409)
             .body("reason", equalTo("NOT_PROVISIONED_HERE"));
    }

    @Test
    void aTenantThisInstanceNeverRegisteredIsRefusedWithItsOwnReason() {
        api().auth()
             .oauth2(identityProvider.idToken("jane", List.of(ownerOf(STRANGER_TENANT))))
             .header(TenantSelectionConstants.TENANT_HEADER, STRANGER_TENANT)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(409)
             .body("reason", equalTo("UNKNOWN_HERE"));
    }

    /**
     * What a client asks before it knows which tenant to name - the same answer the picker of the
     * hosted login reads.
     */
    @Test
    void theTenantsOfTheTokenAreListedWithTheirLocalState() {
        api().auth()
             .oauth2(identityProvider.idToken("jane",
                     List.of(OPERATOR_GROUP, ownerOf(provisionedTenant), ownerOf(tenantAwaitingProvisioning))))
             .when()
             .get(TENANT_SELECTION_PATH)
             .then()
             .statusCode(200)
             .body("selectedTenantId", nullValue())
             .body("tenants.find { it.id == '" + provisionedTenant.getId() + "' }.provisionedHere", equalTo(true))
             .body("tenants.find { it.id == '" + provisionedTenant.getId() + "' }.state", equalTo("READY"))
             .body("tenants.find { it.id == '" + tenantAwaitingProvisioning.getId() + "' }.provisionedHere", equalTo(false));
    }

    /**
     * A machine client's token carries no groups, so it is not held to naming a tenant: it lands in the
     * default tenant with the roles its scopes map to, as it always did.
     */
    @Test
    void anAccessTokenNamingNoTenantPassesAsBefore() {
        api().auth()
             .oauth2(identityProvider.accessToken("service-account-ops", "openid dirigible/OPERATOR"))
             .when()
             .get(TENANT_CONFIGURATIONS_PATH)
             .then()
             .statusCode(200)
             .body(MARKER_VALUE, equalTo(MARKER_OF_DEFAULT_TENANT));
    }

    private RequestSpecification api() {
        return given().port(port);
    }

    private static String ownerOf(DirigibleTestTenant tenant) {
        return ownerOf(tenant.getId());
    }

    private static String ownerOf(String tenantId) {
        return tenantId + "." + APP_ID + ".Owner";
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
