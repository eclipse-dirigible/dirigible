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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantSelectionFilter;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end test of the tenant selection: a user's groups decide which tenants they may enter,
 * entering one puts it into the session, and the very next request is served in that tenant.
 *
 * <p>
 * That last step is what makes this test worth its boot time. Which tenant a request landed in is
 * observed through the tenant's own configuration table - {@code GET
 * /services/core/configurations/tenant} reads the current tenant's DIRIGIBLE_CONFIGURATIONS - so a
 * marker seeded per tenant proves the whole chain: endpoint, session attribute, tenant resolution,
 * tenant scope.
 *
 * <p>
 * The login itself is not exercised. Booting an OIDC profile means {@code basic.enabled=false},
 * which takes away the authentication every test harness in this repo uses, and a fake identity
 * provider would prove little beyond bean wiring - so the authenticated user is fabricated, exactly
 * as the unit tests of the security module do, and the real login path is covered by the scenario
 * tests outside this repo.
 */
// One Dirigible boot for the whole class: provisioning a tenant is expensive and the tests only
// read.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class TenantSelectionIT extends IntegrationTest {

    private static final String TENANT_SELECTION_ENDPOINT = "/services/security/tenant-selection";
    private static final String TENANT_CONFIGURATIONS_PATH = "/services/core/configurations/tenant";

    private static final String MARKER_KEY = "TENANT_SELECTION_IT_MARKER";
    private static final String MARKER_OF_DEFAULT_TENANT = "marker-of-the-default-tenant";
    private static final String MARKER_OF_SELECTED_TENANT = "marker-of-the-selected-tenant";

    private static final String APP_ID = "library";
    private static final String GROUPS_CLAIM = "groups";
    private static final String USER = "owner@example.com";

    /**
     * A group that carries no tenant, so it is a global role. It has to be a group: in this mode the
     * authorities of a session are recomputed from the groups, so nothing else survives a request.
     */
    private static final String ADMINISTRATOR_GROUP = "ADMINISTRATOR";

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
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(GROUPS_CLAIM);
    }

    /**
     * Provisions the tenants and seeds the markers, once for the class.
     *
     * @throws SchedulerException if the provisioning job cannot be triggered
     * @throws SQLException if a marker cannot be written
     */
    @BeforeEach
    void provisionTenantsAndSeedMarkers() throws SchedulerException, SQLException {
        if (provisionedTenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("tenant-selection-it");
        createTenants(created);
        scheduler.triggerJob(JobKey.jobKey("TenantsProvisioningJob", "system"));
        waitForTenantProvisioning(created);
        provisionedTenant = created;

        // Registered after the provisioning pass, so it stays in status INITIAL for this class.
        DirigibleTestTenant awaiting = new DirigibleTestTenant("tenant-selection-unprovisioned-it");
        createTenants(awaiting);
        tenantAwaitingProvisioning = awaiting;

        seedMarker(defaultTenant.getId(), MARKER_OF_DEFAULT_TENANT);
        seedMarker(provisionedTenant.getId(), MARKER_OF_SELECTED_TENANT);
    }

    @Test
    void theTenantsOfTheGroupsAreOfferedWithTheirLocalState() throws Exception {
        mvc.perform(
                get(TENANT_SELECTION_ENDPOINT).with(authentication(userOf(ownerOf(provisionedTenant), userOf(tenantAwaitingProvisioning)))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.selectedTenantId").doesNotExist())
           .andExpect(jsonPath("$.tenants.length()").value(2))
           .andExpect(jsonPath("$.tenants[?(@.id=='" + provisionedTenant.getId() + "')].provisionedHere", contains(true)))
           .andExpect(jsonPath("$.tenants[?(@.id=='" + tenantAwaitingProvisioning.getId() + "')].provisionedHere", contains(false)));
    }

    /**
     * The whole point: selecting a tenant makes the next request run in it.
     *
     * <p>
     * The user carries a global {@code ADMINISTRATOR} group because in this mode the authorities are
     * recomputed from the groups on every request - a role granted any other way would be dropped
     * again, which is exactly what the selection is for.
     */
    @Test
    void aSelectedTenantIsTheTenantOfTheFollowingRequests() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Authentication user = userOf(ownerOf(provisionedTenant), ADMINISTRATOR_GROUP);

        mvc.perform(post(TENANT_SELECTION_ENDPOINT).session(session)
                                                   .with(authentication(user))
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content("{\"tenantId\":\"" + provisionedTenant.getId() + "\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.tenantId").value(provisionedTenant.getId()))
           .andExpect(jsonPath("$.roles", containsInAnyOrder(ADMINISTRATOR_GROUP, "Owner")));

        assertThat(session.getAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE)).isEqualTo(
                provisionedTenant.getId());

        mvc.perform(get(TENANT_CONFIGURATIONS_PATH).session(session)
                                                   .with(authentication(user)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[?(@.key=='" + MARKER_KEY + "')].value", contains(MARKER_OF_SELECTED_TENANT)));
    }

    /**
     * Staff of the instance - global roles, no tenant of their own - keep working in the default tenant
     * instead of being asked to pick one they do not have.
     *
     * <p>
     * Their authorities are the ones the login mapper granted, which is why this is the one case that
     * has to state them: without a selection nothing recomputes them during the request.
     */
    @Test
    void staffWithoutATenantStayInTheDefaultTenant() throws Exception {
        mvc.perform(get(TENANT_CONFIGURATIONS_PATH).with(authentication(loggedInStaff(ADMINISTRATOR_GROUP))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[?(@.key=='" + MARKER_KEY + "')].value", contains(MARKER_OF_DEFAULT_TENANT)));
    }

    @Test
    void aTenantTheGroupsDoNotGrantIsRefused() throws Exception {
        mvc.perform(post(TENANT_SELECTION_ENDPOINT).with(authentication(userOf(ownerOf(provisionedTenant))))
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content("{\"tenantId\":\"someone-elses-tenant\"}"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.reason").value("NOT_A_MEMBER"));
    }

    @Test
    void aTenantThatIsNotProvisionedYetIsRefused() throws Exception {
        mvc.perform(post(TENANT_SELECTION_ENDPOINT).with(authentication(userOf(userOf(tenantAwaitingProvisioning))))
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content("{\"tenantId\":\"" + tenantAwaitingProvisioning.getId() + "\"}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.reason").value("NOT_PROVISIONED_HERE"));
    }

    @Test
    void aBrowserWithSeveralTenantsIsSentToThePicker() throws Exception {
        mvc.perform(get("/services/web/home/index.html")
                                                        .with(authentication(
                                                                userOf(ownerOf(provisionedTenant), userOf(tenantAwaitingProvisioning))))
                                                        .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl(TenantSelectionFilter.TENANT_SELECTION_PAGE));
    }

    @Test
    void aProgrammaticCallerWithSeveralTenantsIsToldToChoose() throws Exception {
        mvc.perform(get("/services/web/home/index.html")
                                                        .with(authentication(
                                                                userOf(ownerOf(provisionedTenant), userOf(tenantAwaitingProvisioning))))
                                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.error").value("TENANT_SELECTION_REQUIRED"))
           .andExpect(jsonPath("$.tenants.length()").value(2));
    }

    @Test
    void thePickerIsReachableForALoggedInUserAndNotForAnyoneElse() throws Exception {
        mvc.perform(get(TenantSelectionFilter.TENANT_SELECTION_PAGE).with(authentication(userOf(ownerOf(provisionedTenant)))))
           .andExpect(status().isOk());

        mvc.perform(get(TenantSelectionFilter.TENANT_SELECTION_PAGE))
           .andExpect(status().is(org.springframework.http.HttpStatus.UNAUTHORIZED.value()));
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

    private static String ownerOf(DirigibleTestTenant tenant) {
        return tenant.getId() + "." + APP_ID + ".Owner";
    }

    private static String userOf(DirigibleTestTenant tenant) {
        return tenant.getId() + "." + APP_ID + ".User";
    }

    /**
     * An authenticated user whose groups are the given ones - what an OIDC login leaves behind.
     *
     * @param groups the groups of the user
     * @return the authentication
     */
    private static Authentication userOf(String... groups) {
        return authenticationOf(List.of(groups));
    }

    /**
     * A user of global groups only, carrying the authorities the login mapper grants for them.
     *
     * @param globalGroups the groups that carry no tenant
     * @return the authentication
     */
    private static Authentication loggedInStaff(String... globalGroups) {
        List<SimpleGrantedAuthority> authorities = List.of(globalGroups)
                                                       .stream()
                                                       .map(group -> new SimpleGrantedAuthority("ROLE_" + group))
                                                       .toList();
        return authenticationOf(List.of(globalGroups), authorities);
    }

    private static Authentication authenticationOf(List<String> groups) {
        return authenticationOf(groups, List.of());
    }

    private static Authentication authenticationOf(List<String> groups, List<SimpleGrantedAuthority> authorities) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", USER, GROUPS_CLAIM, groups));
        OidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        return new OAuth2AuthenticationToken(oidcUser, authorities, "keycloak");
    }
}
