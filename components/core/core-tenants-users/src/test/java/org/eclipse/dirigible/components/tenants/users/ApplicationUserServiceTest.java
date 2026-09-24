/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ApplicationUserServiceTest {

    private static final String TENANT = "acme";
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    private ApplicationUserRepository users;
    private ApplicationUserService service;
    private ApplicationUser stored;

    @BeforeEach
    void setUp() {
        users = mock(ApplicationUserRepository.class);
        TenantService tenants = mock(TenantService.class);
        when(tenants.findById(TENANT)).thenReturn(Optional.of(new Tenant()));
        when(tenants.findById("unknown")).thenReturn(Optional.empty());
        when(users.findByTenantIdAndEmail(any(), any())).thenAnswer(call -> Optional.ofNullable(stored));
        when(users.save(any(ApplicationUser.class))).thenAnswer(call -> {
            stored = call.getArgument(0);
            return stored;
        });
        service = new ApplicationUserService(users, tenants, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ApplicationUserUpsert upsert(String status, String role, String requestId) {
        return new ApplicationUserUpsert("New.User@Example.com ", role, status, requestId, null, "owner@example.com");
    }

    @Test
    void aSuccessForAnUnknownUserCreatesItWithTheRole() {
        ApplicationUserService.CallbackResult result = service.applyCallback(TENANT, upsert("INVITED", "User", "m1"));

        assertTrue(result.created());
        ApplicationUserState state = result.state();
        assertEquals("new.user@example.com", state.email());
        assertEquals("INVITED", state.status());
        assertEquals("COMPLETED", state.requestState());
        assertEquals("m1", state.requestId());
        assertEquals(1, state.roles()
                             .size());
        assertEquals("User", state.roles()
                                  .get(0)
                                  .role());
        assertEquals("owner@example.com", state.roles()
                                               .get(0)
                                               .grantedBy());
        assertEquals("owner@example.com", state.invitedBy());
        assertEquals(NOW.toString(), state.invitedAt());
    }

    @Test
    void aSecondRoleIsAddedAndTheFirstKeepsItsGrant() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));
        Instant firstGrant = stored.roleNamed("User")
                                   .orElseThrow()
                                   .getGrantedAt();

        ApplicationUserService.CallbackResult result = service.applyCallback(TENANT,
                new ApplicationUserUpsert("new.user@example.com", "Owner", "ASSIGNED", null, null, "other@x.io"));

        assertFalse(result.created());
        assertEquals(List.of("Owner", "User"), result.state()
                                                     .roles()
                                                     .stream()
                                                     .map(ApplicationUserState.GrantedRole::role)
                                                     .sorted()
                                                     .toList());
        assertEquals(firstGrant, stored.roleNamed("User")
                                       .orElseThrow()
                                       .getGrantedAt());
        assertEquals("owner@example.com", stored.roleNamed("User")
                                                .orElseThrow()
                                                .getGrantedBy());
    }

    @Test
    void theSameRoleTwiceAddsNoSecondRow() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));

        assertEquals(1, stored.getRoles()
                              .size());
    }

    @Test
    void anInvitedUserStaysInvitedOnAnAssignedCallback() {
        service.applyCallback(TENANT, upsert("INVITED", "User", "m1"));
        service.applyCallback(TENANT, upsert("ASSIGNED", "Owner", null));

        assertEquals(ApplicationUserStatus.INVITED, stored.getStatus());
    }

    @Test
    void anActiveUserIsNeverLowered() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));
        stored.setStatus(ApplicationUserStatus.ACTIVE);

        service.applyCallback(TENANT, upsert("INVITED", "Owner", null));

        assertEquals(ApplicationUserStatus.ACTIVE, stored.getStatus());
    }

    @Test
    void aSuccessAfterASignInMakesTheUserActive() {
        stored = new ApplicationUser(TENANT, "new.user@example.com", ApplicationUserStatus.PENDING, NOW);
        stored.setRequestId("m1");
        stored.setLastSignInAt(NOW);

        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));

        assertEquals(ApplicationUserStatus.ACTIVE, stored.getStatus());
    }

    @Test
    void aFailureForAnUnknownUserCreatesAFailedRow() {
        ApplicationUserService.CallbackResult result = service.applyCallback(TENANT,
                new ApplicationUserUpsert("x@example.com", "User", "FAILED", "m1", "the tenant is suspended", "owner@example.com"));

        assertTrue(result.created());
        assertEquals("FAILED", result.state()
                                     .status());
        assertEquals("FAILED", result.state()
                                     .requestState());
        assertEquals("the tenant is suspended", result.state()
                                                      .errorMessage());
        assertTrue(result.state()
                         .roles()
                         .isEmpty());
    }

    @Test
    void aFailureAfterAGrantKeepsTheStatus() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));

        service.applyCallback(TENANT, new ApplicationUserUpsert("new.user@example.com", "Owner", "FAILED", "m1", "boom", "o@x.io"));

        assertEquals(ApplicationUserStatus.ASSIGNED, stored.getStatus());
        assertEquals(ApplicationUserRequestState.FAILED, stored.getRequestState());
        assertEquals("boom", stored.getErrorMessage());
    }

    @Test
    void aFailureOfAnotherRequestIsIgnored() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m2"));
        Instant before = stored.getUpdatedAt();

        service.applyCallback(TENANT, new ApplicationUserUpsert("new.user@example.com", "Owner", "FAILED", "m1", "old", "o@x.io"));

        assertEquals(ApplicationUserRequestState.COMPLETED, stored.getRequestState());
        assertNull(stored.getErrorMessage());
        assertEquals(before, stored.getUpdatedAt());
    }

    @Test
    void aSuccessClearsAnEarlierFailureOfTheSameRequest() {
        service.applyCallback(TENANT, new ApplicationUserUpsert("new.user@example.com", "User", "FAILED", "m1", "boom", "o@x.io"));

        service.applyCallback(TENANT, upsert("INVITED", "User", "m1"));

        assertEquals(ApplicationUserStatus.INVITED, stored.getStatus());
        assertEquals(ApplicationUserRequestState.COMPLETED, stored.getRequestState());
        assertNull(stored.getErrorMessage());
    }

    @Test
    void anUnknownTenantIsNotFound() {
        ResponseStatusException refusal =
                assertThrows(ResponseStatusException.class, () -> service.applyCallback("unknown", upsert("INVITED", "User", "m1")));
        assertEquals(HttpStatus.NOT_FOUND, refusal.getStatusCode());
        verify(users, never()).save(any());
    }

    @Test
    void anInvalidTenantIdIsABadRequest() {
        ResponseStatusException refusal =
                assertThrows(ResponseStatusException.class, () -> service.applyCallback("-bad-", upsert("INVITED", "User", "m1")));
        assertEquals(HttpStatus.BAD_REQUEST, refusal.getStatusCode());
    }
}
