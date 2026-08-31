/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registration is what a retrying provisioner calls, possibly more than once for the same tenant,
 * so the interesting cases are all about repetition and collision rather than the happy path.
 */
class TenantRegistrationServiceTest {

    private static final String ACME = "acme";

    private final TenantService tenantService = mock(TenantService.class);
    private final TenantInitializationStatusCalculator statusCalculator = mock(TenantInitializationStatusCalculator.class);
    private final TenantRegistrationService service = new TenantRegistrationService(tenantService, statusCalculator);

    @BeforeEach
    void echoSavedTenant() {
        when(tenantService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantService.findById(any())).thenReturn(Optional.empty());
        when(tenantService.findBySubdomain(any())).thenReturn(Optional.empty());
        // the derivation itself has its own test; here it only has to distinguish the two cases
        when(statusCalculator.calculate(any())).thenAnswer(invocation -> TenantInitializationState.of(
                TenantStatus.PROVISIONED == ((Tenant) invocation.getArgument(0)).getStatus() ? InitializationStatus.COMPLETED
                        : InitializationStatus.NOT_STARTED));
    }

    @Test
    void aNewTenantIsCreatedPendingActivation() {
        TenantRegistrationService.RegistrationResult result = service.register(ACME, parameter("Acme Ltd", null));

        assertTrue(result.created());
        assertEquals(ACME, result.state()
                                 .id());
        assertEquals("Acme Ltd", result.state()
                                       .name());
        assertEquals(TenantStatus.PENDING_ACTIVATION, result.state()
                                                            .status());
        assertEquals(InitializationStatus.NOT_STARTED, result.state()
                                                             .initialization()
                                                             .status());
    }

    /** Subdomains are unused when tenants are resolved from token groups, but the column is unique. */
    @Test
    void theSubdomainDefaultsToTheTenantId() {
        assertEquals(ACME, service.register(ACME, parameter("Acme Ltd", null))
                                  .state()
                                  .subdomain());
    }

    @Test
    void anExplicitSubdomainIsKept() {
        assertEquals("acme-eu", service.register(ACME, parameter("Acme Ltd", "acme-eu"))
                                       .state()
                                       .subdomain());
    }

    @Test
    void registeringAnExistingTenantUpdatesItAndReportsItAsNotCreated() {
        Tenant existing = tenant(ACME, "Acme Ltd", ACME, TenantStatus.PENDING_ACTIVATION);
        when(tenantService.findById(ACME)).thenReturn(Optional.of(existing));

        TenantRegistrationService.RegistrationResult result = service.register(ACME, parameter("Acme Corporation", null));

        assertFalse(result.created());
        assertEquals("Acme Corporation", result.state()
                                               .name());
        verify(tenantService).save(existing);
    }

    /** The status belongs to the activation, so a re-registration must not reset an active tenant. */
    @Test
    void registeringAnExistingTenantNeverChangesItsStatus() {
        Tenant existing = tenant(ACME, "Acme Ltd", ACME, TenantStatus.PROVISIONED);
        when(tenantService.findById(ACME)).thenReturn(Optional.of(existing));

        TenantRegistrationService.RegistrationResult result = service.register(ACME, parameter("Acme Ltd", null));

        assertEquals(TenantStatus.PROVISIONED, result.state()
                                                     .status());
    }

    /** An omitted subdomain leaves the registered one alone rather than resetting it to the id. */
    @Test
    void registeringWithoutASubdomainKeepsTheRegisteredOne() {
        Tenant existing = tenant(ACME, "Acme Ltd", "acme-eu", TenantStatus.PENDING_ACTIVATION);
        when(tenantService.findById(ACME)).thenReturn(Optional.of(existing));

        assertEquals("acme-eu", service.register(ACME, parameter("Acme Ltd", null))
                                       .state()
                                       .subdomain());
    }

    @Test
    void anIdThatCannotBeAGroupSegmentIsRefused() {
        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> service.register("acme.corp", parameter("Acme Ltd", null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tenantService, never()).save(any());
    }

    /**
     * A subdomain is matched out of a request's host name under the {@code SUBDOMAIN} strategy, so
     * anything that is not a DNS label could never resolve - and, until it was refused here, arbitrary
     * text including line breaks reached the log and the tenant row.
     */
    @Test
    void aSubdomainThatIsNotADnsLabelIsRefused() {
        for (String subdomain : new String[] {"acme.eu", "acme corp", "-acme", "acme\r\nINFO fake log line"}) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.register(ACME, parameter("Acme Ltd", subdomain)), "expected [" + subdomain + "] to be refused");

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
        verify(tenantService, never()).save(any());
    }

    @Test
    void aSubdomainOwnedByAnotherTenantIsRefused() {
        when(tenantService.findBySubdomain(ACME)).thenReturn(Optional.of(tenant("globex", "Globex", ACME, TenantStatus.PROVISIONED)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(ACME, parameter("Acme Ltd", null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason()
                     .contains("globex"));
        verify(tenantService, never()).save(any());
    }

    /** Its own subdomain is not a collision - otherwise no registration could ever be repeated. */
    @Test
    void aTenantKeepingItsOwnSubdomainIsNotAConflict() {
        Tenant existing = tenant(ACME, "Acme Ltd", ACME, TenantStatus.PENDING_ACTIVATION);
        when(tenantService.findById(ACME)).thenReturn(Optional.of(existing));
        when(tenantService.findBySubdomain(ACME)).thenReturn(Optional.of(existing));

        assertFalse(service.register(ACME, parameter("Acme Ltd", null))
                           .created());
    }

    @Test
    void readingAnUnknownTenantIsANotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.read("nowhere"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private static RegisterTenantParameter parameter(String name, String subdomain) {
        RegisterTenantParameter parameter = new RegisterTenantParameter();
        parameter.setName(name);
        parameter.setSubdomain(subdomain);
        return parameter;
    }

    private static Tenant tenant(String id, String name, String subdomain, TenantStatus status) {
        Tenant tenant = new Tenant(TenantRegistrationService.LOCATION, name, "", subdomain, status);
        tenant.setId(id);
        return tenant;
    }
}
