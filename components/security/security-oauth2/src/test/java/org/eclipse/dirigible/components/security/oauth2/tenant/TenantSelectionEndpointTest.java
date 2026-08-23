/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantSelectionEndpoint.TenantSelectionRefusal;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantSelectionEndpoint.TenantSelectionRequest;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantSelectionEndpoint.TenantSelectionResult;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantSelectionEndpoint.TenantSelectionState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

/**
 * The endpoint is the picker's contract: it lists what the user may enter, enters one, and answers
 * a refusal with a status a client can act on. It exists only where tenants are selected at all.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantSelectionEndpointTest {

    private static final String ACME = "acme";

    @Mock
    private TenantSelectionManager tenantSelectionManager;

    private TenantSelectionEndpoint endpoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        endpoint = new TenantSelectionEndpoint(tenantSelectionManager);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
    }

    @Test
    void theStateListsTheTenantsAndTheSelection() {
        when(tenantSelectionManager.selectedTenantId(request)).thenReturn(ACME);
        when(tenantSelectionManager.availableTenants(any())).thenReturn(
                List.of(new TenantOption(ACME, "Acme Ltd", true), new TenantOption("globex", "globex", false)));

        ResponseEntity<TenantSelectionState> state = endpoint.state(request);

        assertThat(state.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(state.getBody()
                        .selectedTenantId()).isEqualTo(ACME);
        assertThat(state.getBody()
                        .tenants()).hasSize(2);
    }

    @Test
    void selectingReturnsTheTenantAndTheRoles() {
        when(tenantSelectionManager.selectTenant(any(), any(), eq(ACME))).thenReturn(Set.of("Owner", "DEVELOPER"));

        ResponseEntity<TenantSelectionResult> result = endpoint.select(new TenantSelectionRequest(ACME), request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()
                         .tenantId()).isEqualTo(ACME);
        assertThat(result.getBody()
                         .roles()).containsExactlyInAnyOrder("Owner", "DEVELOPER");
    }

    @Test
    void theTenantIdIsRequiredAndTrimmed() {
        when(tenantSelectionManager.selectTenant(any(), any(), eq(ACME))).thenReturn(Set.of("Owner"));

        assertThat(endpoint.select(new TenantSelectionRequest("  acme  "), request, response)
                           .getBody()
                           .tenantId()).isEqualTo(ACME);

        assertThatThrownBy(() -> endpoint.select(new TenantSelectionRequest("   "), request, response)).isInstanceOf(
                ResponseStatusException.class);
        assertThatThrownBy(() -> endpoint.select(new TenantSelectionRequest(null), request, response)).isInstanceOf(
                ResponseStatusException.class);
        assertThatThrownBy(() -> endpoint.select(null, request, response)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void aRefusalCarriesTheStatusOfItsReason() {
        assertThat(refusalStatus(TenantSelectionException.Reason.NOT_A_MEMBER)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refusalStatus(TenantSelectionException.Reason.NOT_PROVISIONED_HERE)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refusalStatus(TenantSelectionException.Reason.NOT_AN_INTERACTIVE_SESSION)).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<TenantSelectionRefusal> refusal = endpoint.onRefusedSelection(
                new TenantSelectionException(TenantSelectionException.Reason.NOT_PROVISIONED_HERE, ACME, "not provisioned yet"));
        assertThat(refusal.getBody()
                          .reason()).isEqualTo("NOT_PROVISIONED_HERE");
        assertThat(refusal.getBody()
                          .message()).isEqualTo("not provisioned yet");
    }

    @Test
    void theEndpointIsAbsentWhereTenantsAreNotSelected() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());

        assertThatThrownBy(() -> endpoint.state(request)).isInstanceOf(ResponseStatusException.class)
                                                         .hasMessageContaining("404");
        assertThatThrownBy(() -> endpoint.select(new TenantSelectionRequest(ACME), request, response)).isInstanceOf(
                ResponseStatusException.class);
    }

    private HttpStatus refusalStatus(TenantSelectionException.Reason reason) {
        return HttpStatus.valueOf(endpoint.onRefusedSelection(new TenantSelectionException(reason, ACME, "refused"))
                                          .getStatusCode()
                                          .value());
    }
}
