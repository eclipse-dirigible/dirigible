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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
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
    private static final String EMAIL = "new.user@example.com";
    private static final String OWNER = "owner@example.com";
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(600);

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
        when(users.findById(anyLong())).thenAnswer(call -> Optional.ofNullable(stored));
        when(users.save(any(ApplicationUser.class))).thenAnswer(call -> {
            stored = call.getArgument(0);
            if (stored.getId() == null) {
                Field id = ApplicationUser.class.getDeclaredField("id");
                id.setAccessible(true);
                id.set(stored, 1L);
            }
            return stored;
        });
        service = new ApplicationUserService(users, tenants, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ApplicationUserUpsert upsert(String status, String role, String requestId) {
        return new ApplicationUserUpsert("New.User@Example.com ", role, status, requestId, null, OWNER);
    }

    private static ApplicationUserUpsert failed(String role, String requestId, String errorMessage) {
        return new ApplicationUserUpsert(EMAIL, role, "FAILED", requestId, errorMessage, "o@x.io");
    }

    private ApplicationUserRole role(String role) {
        return stored.roleNamed(role)
                     .orElseThrow();
    }

    private ApplicationUserService.PreparedInvitation invite(String role, String messageId) {
        return service.prepareInvitation(TENANT, EMAIL, role, OWNER, messageId, NOW);
    }

    @Test
    void aSuccessForAnUnknownUserCreatesItWithAGrantedRole() {
        ApplicationUserService.CallbackResult result = service.applyCallback(TENANT, upsert("INVITED", "User", "m1"));

        assertTrue(result.created());
        ApplicationUserState state = result.state();
        assertEquals(EMAIL, state.email());
        assertEquals("INVITED", state.status());
        assertEquals(1, state.roles()
                             .size());
        ApplicationUserState.RoleState role = state.roles()
                                                   .get(0);
        assertEquals("User", role.role());
        assertEquals("GRANTED", role.state());
        assertEquals("m1", role.requestId());
        assertEquals(OWNER, role.grantedBy());
        assertEquals(NOW.toString(), role.grantedAt());
        assertEquals(OWNER, state.invitedBy());
        assertEquals(NOW.toString(), state.invitedAt());
    }

    @Test
    void aSecondRoleIsAddedAndTheFirstKeepsItsGrant() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));
        Instant firstGrant = role("User").getGrantedAt();

        ApplicationUserService.CallbackResult result =
                service.applyCallback(TENANT, new ApplicationUserUpsert(EMAIL, "Owner", "ASSIGNED", null, null, "other@x.io"));

        assertFalse(result.created());
        assertEquals(List.of("Owner", "User"), result.state()
                                                     .roles()
                                                     .stream()
                                                     .map(ApplicationUserState.RoleState::role)
                                                     .sorted()
                                                     .toList());
        assertEquals(firstGrant, role("User").getGrantedAt());
        assertEquals(OWNER, role("User").getGrantedBy());
    }

    @Test
    void theSameRoleTwiceAddsNoSecondRowAndKeepsTheFirstGrant() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));
        service.applyCallback(TENANT, new ApplicationUserUpsert(EMAIL, "User", "ASSIGNED", "m9", null, "other@x.io"));

        assertEquals(1, stored.getRoles()
                              .size());
        assertEquals(OWNER, role("User").getGrantedBy());
        assertEquals("m1", role("User").getRequestId());
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
        invite("User", "m1");
        stored.setLastSignInAt(NOW);

        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));

        assertEquals(ApplicationUserStatus.ACTIVE, stored.getStatus());
    }

    @Test
    void aSuccessGrantsTheRequestedRoleAndKeepsWhoAskedForIt() {
        invite("User", "m1");

        service.applyCallback(TENANT, new ApplicationUserUpsert(EMAIL, "User", "INVITED", "m1", null, "operator@x.io"));

        ApplicationUserRole user = role("User");
        assertEquals(ApplicationUserRoleState.GRANTED, user.getState());
        assertEquals(OWNER, user.getRequestedBy());
        assertEquals(NOW, user.getRequestedAt());
        assertEquals("operator@x.io", user.getGrantedBy());
        assertEquals("m1", user.getRequestId());
        assertEquals(ApplicationUserStatus.INVITED, stored.getStatus());
    }

    @Test
    void aFailureForAnUnknownUserCreatesAFailedRole() {
        ApplicationUserService.CallbackResult result = service.applyCallback(TENANT,
                new ApplicationUserUpsert("x@example.com", "User", "FAILED", "m1", "the tenant is suspended", OWNER));

        assertTrue(result.created());
        assertEquals("FAILED", result.state()
                                     .status());
        ApplicationUserState.RoleState role = result.state()
                                                    .role("User")
                                                    .orElseThrow();
        assertEquals("FAILED", role.state());
        assertEquals("the tenant is suspended", role.errorMessage());
        assertNull(role.grantedAt());
    }

    @Test
    void aFailureOfAnotherRoleKeepsTheStatusAndTheGrant() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));

        service.applyCallback(TENANT, failed("Owner", "m2", "boom"));

        assertEquals(ApplicationUserStatus.ASSIGNED, stored.getStatus());
        assertEquals(ApplicationUserRoleState.FAILED, role("Owner").getState());
        assertEquals("boom", role("Owner").getErrorMessage());
        assertEquals(ApplicationUserRoleState.GRANTED, role("User").getState());
    }

    @Test
    void aFailureOfAGrantedRoleIsIgnored() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));
        Instant before = stored.getUpdatedAt();

        service.applyCallback(TENANT, failed("User", "m1", "late"));

        assertEquals(ApplicationUserRoleState.GRANTED, role("User").getState());
        assertNull(role("User").getErrorMessage());
        assertEquals(before, stored.getUpdatedAt());
    }

    @Test
    void aFailureOfAnOlderRequestIsIgnored() {
        invite("Owner", "m2");

        service.applyCallback(TENANT, failed("Owner", "m1", "old"));

        assertEquals(ApplicationUserRoleState.REQUESTED, role("Owner").getState());
        assertNull(role("Owner").getErrorMessage());
        assertEquals(ApplicationUserStatus.PENDING, stored.getStatus());
    }

    @Test
    void aFailureWhileAnotherRoleIsRequestedKeepsTheUserPending() {
        invite("User", "m1");
        invite("Owner", "m2");

        service.applyCallback(TENANT, failed("Owner", "m2", "boom"));

        assertEquals(ApplicationUserStatus.PENDING, stored.getStatus());
        assertEquals(ApplicationUserRoleState.FAILED, role("Owner").getState());
        assertEquals(ApplicationUserRoleState.REQUESTED, role("User").getState());
    }

    @Test
    void aSuccessClearsAnEarlierFailureOfTheSameRole() {
        service.applyCallback(TENANT, failed("User", "m1", "boom"));

        service.applyCallback(TENANT, upsert("INVITED", "User", "m1"));

        assertEquals(ApplicationUserStatus.INVITED, stored.getStatus());
        assertEquals(ApplicationUserRoleState.GRANTED, role("User").getState());
        assertNull(role("User").getErrorMessage());
    }

    @Test
    void anInvitationCreatesAPendingUserWithARequestedRole() {
        ApplicationUserService.PreparedInvitation prepared = invite("User", "m1");

        assertTrue(prepared.created());
        assertEquals("PENDING", prepared.state()
                                        .status());
        ApplicationUserState.RoleState role = prepared.state()
                                                      .role("User")
                                                      .orElseThrow();
        assertEquals("REQUESTED", role.state());
        assertEquals("m1", role.requestId());
        assertEquals(OWNER, role.requestedBy());
        assertEquals(NOW.toString(), role.requestedAt());
        assertNull(role.grantedAt());
    }

    @Test
    void aGrantedRoleIsNotAskedForAgain() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));

        TenantUsersException refusal = assertThrows(TenantUsersException.class, () -> invite("User", "m2"));

        assertEquals(HttpStatus.CONFLICT, refusal.status());
        assertEquals("ROLE_ALREADY_GRANTED", refusal.reason());
    }

    @Test
    void aRequestedRoleIsNotAskedForTwice() {
        invite("User", "m1");

        TenantUsersException refusal = assertThrows(TenantUsersException.class, () -> invite("User", "m2"));

        assertEquals("REQUEST_PENDING", refusal.reason());
    }

    @Test
    void differentRolesMayBeRequestedSideBySide() {
        invite("User", "m1");

        invite("Owner", "m2");

        assertEquals(ApplicationUserRoleState.REQUESTED, role("User").getState());
        assertEquals(ApplicationUserRoleState.REQUESTED, role("Owner").getState());
        assertEquals("m2", role("Owner").getRequestId());
    }

    @Test
    void aFailedRoleIsAskedForAgainWithANewRequest() {
        service.applyCallback(TENANT, failed("User", "m1", "boom"));

        invite("User", "m2");

        assertEquals(ApplicationUserStatus.PENDING, stored.getStatus());
        assertEquals(ApplicationUserRoleState.REQUESTED, role("User").getState());
        assertEquals("m2", role("User").getRequestId());
        assertNull(role("User").getErrorMessage());
    }

    @Test
    void undoingAnInvitationForANewRoleRemovesTheRoleRow() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));

        service.undoInvitation(invite("Owner", "m2"));

        assertTrue(stored.roleNamed("Owner")
                         .isEmpty());
        assertEquals(ApplicationUserStatus.ASSIGNED, stored.getStatus());
    }

    @Test
    void undoingAnInvitationForAFailedRoleRestoresTheFailure() {
        service.applyCallback(TENANT, failed("User", "m1", "boom"));

        service.undoInvitation(invite("User", "m2"));

        assertEquals(ApplicationUserStatus.FAILED, stored.getStatus());
        assertEquals(ApplicationUserRoleState.FAILED, role("User").getState());
        assertEquals("m1", role("User").getRequestId());
        assertEquals("boom", role("User").getErrorMessage());
    }

    @Test
    void undoingTheInvitationOfANewUserRemovesIt() {
        service.undoInvitation(invite("User", "m1"));

        verify(users).deleteById(1L);
    }

    @Test
    void aResendRefreshesTheRequestTimeAndKeepsItsId() {
        invite("User", "m1");

        ApplicationUserService.PreparedInvitation prepared = service.prepareResend(TENANT, 1L, "User", "other@x.io", LATER);

        assertEquals(LATER, role("User").getRequestedAt());
        assertEquals("m1", prepared.state()
                                   .role("User")
                                   .orElseThrow()
                                   .requestId());
        assertEquals(OWNER, role("User").getRequestedBy());
    }

    @Test
    void onlyARequestedRoleIsResent() {
        service.applyCallback(TENANT, upsert("ASSIGNED", "User", "m1"));

        TenantUsersException granted =
                assertThrows(TenantUsersException.class, () -> service.prepareResend(TENANT, 1L, "User", OWNER, LATER));
        TenantUsersException absent =
                assertThrows(TenantUsersException.class, () -> service.prepareResend(TENANT, 1L, "Owner", OWNER, LATER));

        assertEquals("NOT_PENDING", granted.reason());
        assertEquals("NOT_PENDING", absent.reason());
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
