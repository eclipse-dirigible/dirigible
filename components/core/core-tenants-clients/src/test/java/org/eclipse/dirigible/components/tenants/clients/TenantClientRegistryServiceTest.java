/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.base.tenant.clients.IntegrationClient;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

/**
 * The apply rules of the integration client registry, and the lookup the security layer makes.
 */
class TenantClientRegistryServiceTest {

    private static final String TENANT = "acme";
    private static final String READ_LEAVE = "base-vacations.Vacations.VacationRequestReadOnly";
    private static final String READ_EMPLOYEES = "base-employees.Employees.EmployeeReadOnly";

    private TenantClientRepository clients;
    private TenantService tenants;
    private TenantClientRegistryService service;
    private List<TenantClient> stored;

    @BeforeEach
    void setUp() {
        clients = mock(TenantClientRepository.class);
        tenants = mock(TenantService.class);
        stored = new ArrayList<>();
        when(tenants.findById(TENANT)).thenReturn(Optional.of(tenant(TenantStatus.PROVISIONED)));
        when(clients.findForUpdateByTenantIdOrderByClientId(TENANT)).thenAnswer(invocation -> new ArrayList<>(stored));
        when(clients.findByClientId(anyString())).thenReturn(Optional.empty());
        // A mock manager runs every TransactionTemplate callback inline.
        service = new TenantClientRegistryService(clients, tenants, mock(PlatformTransactionManager.class));
    }

    @Test
    void aFirstRegistryInsertsItsClientsWithTheirRoles() {
        TenantClientSync.Result result = service.apply(TENANT, registry(1, client("partner", READ_LEAVE, READ_EMPLOYEES)));

        assertTrue(result.applied());
        assertEquals(1L, result.revision());
        assertEquals(1, result.active());
        assertEquals(0, result.revoked());
        TenantClient partner = saved("partner");
        assertEquals(TENANT, partner.getTenantId());
        assertEquals("Partner system", partner.getName());
        assertEquals(Set.of(READ_LEAVE, READ_EMPLOYEES), partner.roleNames());
        assertEquals("staff@codbex.com", partner.getCreatedBy());
        assertEquals(Instant.parse("2026-11-02T10:00:00Z"), partner.getCreatedAt());
        assertEquals(1, partner.getRevision());
        assertFalse(partner.isRevoked());
    }

    @Test
    void aRegistryThatIsNotNewerChangesNothing() {
        TenantClient partner = stored("partner", 5, READ_LEAVE);

        TenantClientSync.Result result = service.apply(TENANT, registry(5, client("partner", READ_EMPLOYEES)));

        assertFalse(result.applied());
        assertEquals(5L, result.revision());
        assertEquals(Set.of(READ_LEAVE), partner.roleNames());
        verify(clients, never()).saveAll(any());
    }

    @Test
    void aNewerRegistryReplacesTheRolesExactlyAsSent() {
        TenantClient partner = stored("partner", 5, READ_LEAVE, READ_EMPLOYEES);

        service.apply(TENANT, registry(6, client("partner", READ_EMPLOYEES)));

        assertEquals(Set.of(READ_EMPLOYEES), partner.roleNames());
        assertEquals(6, partner.getRevision());
    }

    @Test
    void aClientTheRegistryNoLongerNamesIsRevoked() {
        TenantClient partner = stored("partner", 5, READ_LEAVE);

        TenantClientSync.Result result = service.apply(TENANT, registry(6));

        assertTrue(partner.isRevoked());
        assertNull(partner.getRevokedBy());
        assertEquals(6, partner.getRevision());
        assertEquals(0, result.active());
        assertEquals(1, result.revoked());
    }

    @Test
    void aRevocationSentIsKeptWithWhoAndWhen() {
        TenantClient partner = stored("partner", 5, READ_LEAVE);

        service.apply(TENANT, registry(6, revoked(client("partner", READ_LEAVE))));

        assertTrue(partner.isRevoked());
        assertEquals("staff@codbex.com", partner.getRevokedBy());
        assertEquals(Instant.parse("2026-11-03T09:00:00Z"), partner.getRevokedAt());
    }

    @Test
    void aRevokedClientStaysRevokedWhateverALaterRegistrySays() {
        TenantClient partner = stored("partner", 5, READ_LEAVE);
        partner.revoke("staff@codbex.com", Instant.parse("2026-11-03T09:00:00Z"));

        TenantClientSync.Result result = service.apply(TENANT, registry(7, client("partner", READ_LEAVE)));

        assertTrue(partner.isRevoked());
        assertEquals(Instant.parse("2026-11-03T09:00:00Z"), partner.getRevokedAt());
        assertEquals(0, result.active());
    }

    @Test
    void aClientOfAnotherTenantRefusesTheWholeRegistry() {
        TenantClient elsewhere = new TenantClient("globex", "partner");
        when(clients.findByClientId("partner")).thenReturn(Optional.of(elsewhere));

        ResponseStatusException refusal =
                assertThrows(ResponseStatusException.class, () -> service.apply(TENANT, registry(1, client("partner", READ_LEAVE))));

        assertEquals(HttpStatus.CONFLICT, refusal.getStatusCode());
        verify(clients, never()).saveAll(any());
    }

    @Test
    void anInvalidRegistryIsRefusedWithEveryProblem() {
        ResponseStatusException refusal = assertThrows(ResponseStatusException.class,
                () -> service.apply(TENANT, registry(0, client("partner", "Owner"), client("other"))));

        assertEquals(HttpStatus.BAD_REQUEST, refusal.getStatusCode());
        assertTrue(refusal.getReason()
                          .contains("revision"));
        assertTrue(refusal.getReason()
                          .contains("[Owner] is never given to a client"));
        assertTrue(refusal.getReason()
                          .contains("clients[1].roles: an active client must hold at least one role"));
    }

    @Test
    void anUnknownTenantIsNotFound() {
        when(tenants.findById("nobody")).thenReturn(Optional.empty());

        ResponseStatusException refusal = assertThrows(ResponseStatusException.class, () -> service.apply("nobody", registry(1)));

        assertEquals(HttpStatus.NOT_FOUND, refusal.getStatusCode());
    }

    @Test
    void aTenantBeingDeprovisionedTakesNoRegistry() {
        when(tenants.findById(TENANT)).thenReturn(Optional.of(tenant(TenantStatus.DEPROVISIONING)));

        ResponseStatusException refusal =
                assertThrows(ResponseStatusException.class, () -> service.apply(TENANT, registry(1, client("partner", READ_LEAVE))));

        assertEquals(HttpStatus.CONFLICT, refusal.getStatusCode());
    }

    @Test
    void theLookupFindsAnActiveClientWithItsTenantAndRoles() {
        TenantClient partner = stored("partner", 5, READ_LEAVE);
        when(clients.findByClientId("partner")).thenReturn(Optional.of(partner));

        Optional<IntegrationClient> found = service.findActive("partner");

        assertTrue(found.isPresent());
        assertEquals(TENANT, found.get()
                                  .tenantId());
        assertEquals(Set.of(READ_LEAVE), found.get()
                                              .roles());
    }

    @Test
    void theLookupDoesNotFindARevokedClient() {
        TenantClient partner = stored("partner", 5, READ_LEAVE);
        partner.revoke(null, Instant.now());
        when(clients.findByClientId("partner")).thenReturn(Optional.of(partner));

        assertTrue(service.findActive("partner")
                          .isEmpty());
        assertTrue(service.findActive("")
                          .isEmpty());
    }

    @Test
    void theRegistryIsListedWithItsRevision() {
        stored("partner", 5, READ_LEAVE);
        when(clients.findByTenantIdOrderByClientId(TENANT)).thenReturn(stored);

        TenantClientSync listed = service.list(TENANT);

        assertEquals(5L, listed.revision());
        assertEquals(List.of(READ_LEAVE), listed.clients()
                                                .get(0)
                                                .roles());
        assertNotNull(listed.clients()
                            .get(0)
                            .createdAt());
    }

    private TenantClient stored(String clientId, long revision, String... roles) {
        TenantClient client = new TenantClient(TENANT, clientId);
        client.setName("Partner system");
        client.setRevision(revision);
        client.setCreatedAt(Instant.parse("2026-11-01T10:00:00Z"));
        client.replaceRoles(Arrays.asList(roles));
        stored.add(client);
        return client;
    }

    @SuppressWarnings("unchecked")
    private TenantClient saved(String clientId) {
        org.mockito.ArgumentCaptor<List<TenantClient>> saved = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(clients).saveAll(saved.capture());
        return saved.getValue()
                    .stream()
                    .filter(client -> clientId.equals(client.getClientId()))
                    .findFirst()
                    .orElseThrow();
    }

    private static TenantClientSync registry(long revision, TenantClientSync.Client... clients) {
        return new TenantClientSync(revision, List.of(clients));
    }

    private static TenantClientSync.Client client(String clientId, String... roles) {
        return new TenantClientSync.Client(clientId, "Partner system", List.of(roles), "staff@codbex.com", "2026-11-02T10:00:00Z", null,
                null);
    }

    private static TenantClientSync.Client revoked(TenantClientSync.Client client) {
        return new TenantClientSync.Client(client.clientId(), client.name(), client.roles(), client.createdBy(), client.createdAt(),
                "staff@codbex.com", "2026-11-03T09:00:00Z");
    }

    private static Tenant tenant(TenantStatus status) {
        Tenant tenant = new Tenant("-", "Acme", "The Acme tenant", TENANT, status);
        tenant.setId(TENANT);
        return tenant;
    }
}
