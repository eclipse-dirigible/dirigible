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

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.components.tenants.tenant.TenantSelectionConstants;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end test of the {@code TOKEN_GROUPS} tenant resolution strategy: the tenant of a request
 * is the one the user selected, kept in the HTTP session, and the host is not consulted at all -
 * which is what lets a single host serve every tenant of an application.
 *
 * <p>
 * The strategy is selected in a static {@code @BeforeAll}, which is what makes it take effect:
 * {@code TenantExtractor} reads the configuration once, when the Spring context is refreshed, and
 * that happens after every {@code @BeforeAll} has run. The context is dirtied after the class and
 * {@code IntegrationTest} reloads the configuration, so the strategy does not leak into the next
 * test class.
 *
 * <p>
 * Which tenant a request actually landed in is observed through the tenant's own configuration
 * table: {@code GET /services/core/configurations/tenant} reads the current tenant's
 * DIRIGIBLE_CONFIGURATIONS, so a marker seeded per tenant identifies the resolved tenant beyond
 * doubt. Requests go through MockMvc rather than RestAssured because the session attribute has to
 * be seeded directly - in this mode there is deliberately no host to route by.
 */
// One Dirigible boot for the whole class: the tenant provisioning it needs is expensive and the
// tests only read.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class TokenGroupsTenantResolutionIT extends IntegrationTest {

    private static final String TENANT_CONFIGURATIONS_PATH = "/services/core/configurations/tenant";

    /** Not an allow-listed key on purpose - it is stored and read back, never injected. */
    private static final String MARKER_KEY = "TOKEN_GROUPS_IT_TENANT_MARKER";

    private static final String MARKER_OF_DEFAULT_TENANT = "marker-of-the-default-tenant";
    private static final String MARKER_OF_SELECTED_TENANT = "marker-of-the-selected-tenant";

    private static final String APP_ID = "library";

    /** A host that names no registered tenant - in SUBDOMAIN mode it would be answered with a 404. */
    private static final String UNREGISTERED_HOST = "unregistered-tenant.localhost";

    private static DirigibleTestTenant provisionedTenant;
    private static DirigibleTestTenant tenantAwaitingProvisioning;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    @DefaultTenant
    private Tenant defaultTenant;

    @Autowired
    private TenantConfigurationService tenantConfigurationService;

    @Autowired
    private Scheduler scheduler;

    @BeforeAll
    static void useTokenGroupsResolution() {
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
    }

    /**
     * Provisions the tenant and seeds the markers, once for the class.
     *
     * <p>
     * The provisioning job is triggered explicitly rather than waited for: its schedule is
     * {@code DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS} wide (15 minutes by default) and the
     * only prompt firing is the one at startup.
     *
     * @throws SchedulerException if the provisioning job cannot be triggered
     * @throws SQLException if a marker cannot be written
     */
    @BeforeEach
    void provisionTenantsAndSeedMarkers() throws SchedulerException, SQLException {
        if (provisionedTenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("token-groups-resolution-it");
        createTenants(created);
        scheduler.triggerJob(JobKey.jobKey("TenantsProvisioningJob", "system"));
        waitForTenantProvisioning(created);
        provisionedTenant = created;

        // Registered after the provisioning pass, so it stays in status INITIAL for this class.
        DirigibleTestTenant awaiting = new DirigibleTestTenant("token-groups-unprovisioned-it");
        createTenants(awaiting);
        tenantAwaitingProvisioning = awaiting;

        seedMarker(defaultTenant.getId(), MARKER_OF_DEFAULT_TENANT);
        seedMarker(provisionedTenant.getId(), MARKER_OF_SELECTED_TENANT);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void theSelectedTenantOfTheSessionIsTheTenantOfTheRequest() throws Exception {
        mvc.perform(get(TENANT_CONFIGURATIONS_PATH).sessionAttr(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE,
                provisionedTenant.getId()))
           .andExpect(status().isOk())
           .andExpect(markerIs(MARKER_OF_SELECTED_TENANT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void theHostIsNotConsultedAtAll() throws Exception {
        // Same selection, a host belonging to nobody, and a host belonging to another tenant.
        mvc.perform(get(TENANT_CONFIGURATIONS_PATH).header("host", UNREGISTERED_HOST)
                                                   .sessionAttr(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE,
                                                           provisionedTenant.getId()))
           .andExpect(status().isOk())
           .andExpect(markerIs(MARKER_OF_SELECTED_TENANT));

        mvc.perform(get(TENANT_CONFIGURATIONS_PATH).header("host", tenantAwaitingProvisioning.getHost())
                                                   .sessionAttr(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE,
                                                           provisionedTenant.getId()))
           .andExpect(status().isOk())
           .andExpect(markerIs(MARKER_OF_SELECTED_TENANT));
    }

    /**
     * The counterpart of {@code EnabledMultitenantModeIT.testUnregisteredTenantResolution}, which
     * expects a 404 for this very host: in this mode the host carries no tenant, so there is nothing to
     * refuse.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void anUnknownHostIsNoLongerRefused() throws Exception {
        mvc.perform(get(TENANT_CONFIGURATIONS_PATH).header("host", UNREGISTERED_HOST))
           .andExpect(status().isOk())
           .andExpect(markerIs(MARKER_OF_DEFAULT_TENANT));
    }

    /**
     * A machine-to-machine call and an anonymous request carry no session at all, so they must land in
     * the default tenant instead of being refused.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void aRequestWithoutASelectionLandsInTheDefaultTenant() throws Exception {
        mvc.perform(get(TENANT_CONFIGURATIONS_PATH))
           .andExpect(status().isOk())
           .andExpect(markerIs(MARKER_OF_DEFAULT_TENANT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void aSelectionThatNoLongerResolvesLandsInTheDefaultTenant() throws Exception {
        mvc.perform(get(TENANT_CONFIGURATIONS_PATH).sessionAttr(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE,
                "a-tenant-that-was-never-registered"))
           .andExpect(status().isOk())
           .andExpect(markerIs(MARKER_OF_DEFAULT_TENANT));
    }

    /**
     * A tenant can be in the user's groups before this instance has finished provisioning it. Entering
     * it would mean working in a half-built schema, so the request lands in the default tenant.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMINISTRATOR"})
    void aTenantThatIsNotProvisionedYetCannotBeEntered() throws Exception {
        mvc.perform(get(TENANT_CONFIGURATIONS_PATH).sessionAttr(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE,
                tenantAwaitingProvisioning.getId()))
           .andExpect(status().isOk())
           .andExpect(markerIs(MARKER_OF_DEFAULT_TENANT));
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

    private static org.springframework.test.web.servlet.ResultMatcher markerIs(String expectedMarker) {
        return jsonPath("$[?(@.key=='" + MARKER_KEY + "')].value", contains(expectedMarker));
    }
}
