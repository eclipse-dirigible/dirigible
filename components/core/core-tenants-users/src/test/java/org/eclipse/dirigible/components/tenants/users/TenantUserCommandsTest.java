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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class TenantUserCommandsTest {

    private static final String TENANT = "acme";
    private static final String OWNER = "owner@acme.test";

    private TenantUserReplicaService replica;
    private TenantUserChangePublisher publisher;
    private TenantUserCommands commands;
    private final List<TenantUserView> live = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DirigibleConfig.APP_ID.setStringValue("library");
        replica = mock(TenantUserReplicaService.class);
        publisher = mock(TenantUserChangePublisher.class);
        TenantUsersAccess access = mock(TenantUsersAccess.class);
        when(access.requestedBy()).thenReturn(OWNER);
        when(replica.find(anyString(), anyString())).thenReturn(Optional.empty());
        when(replica.find(anyString(), anyLong())).thenReturn(Optional.empty());
        when(replica.list(TENANT, false)).thenReturn(live);
        commands = new TenantUserCommands(replica, publisher, access);
    }

    @AfterEach
    void tearDown() {
        Configuration.remove(DirigibleConfig.APP_ID.getKey());
    }

    @Test
    void anInvitePublishesTheDesiredRolesSortedWithoutDuplicates() throws Exception {
        TenantUserCommands.Accepted accepted =
                commands.invite(TENANT, new TenantUserCommands.Invite(" Ann@Example.com ", Arrays.asList(" User", "Owner", "User")));

        TenantUserChangeRequest request = published();
        assertEquals(accepted.requestId(), request.requestId(), "the 202 carries the published request's id");
        assertEquals(TenantUserChangeRequest.TYPE, request.type());
        assertEquals(1, request.version());
        assertEquals(TenantUserChangeRequest.Action.INVITE, request.action());
        assertEquals(TENANT, request.tenantId());
        assertEquals("library", request.appId());
        assertEquals("ann@example.com", request.email());
        assertEquals(List.of("Owner", "User"), request.roles());
        assertNull(request.expectedRevision());
        assertEquals(OWNER, request.requestedBy());
        Instant.parse(request.requestedAt());

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(request));
        assertFalse(json.has("expectedRevision"), "an invite carries no expected revision");
        List<String> order = new ArrayList<>();
        json.fieldNames()
            .forEachRemaining(order::add);
        assertEquals(List.of("requestId", "type", "version", "action", "tenantId", "appId", "email", "roles", "requestedBy", "requestedAt"),
                order, "the contract's field order");
    }

    @Test
    void aRoleChangeCarriesTheRolesAndTheExpectedRevision() {
        member(7, "ann@example.com", 4, "ASSIGNED", granted("User"));
        addOwner(8, "boss@example.com");

        commands.setRoles(TENANT, 7, new TenantUserCommands.RolesChange(List.of("Owner"), 4L));

        TenantUserChangeRequest request = published();
        assertEquals(TenantUserChangeRequest.Action.UPDATE_ROLES, request.action());
        assertEquals(List.of("Owner"), request.roles());
        assertEquals(4L, request.expectedRevision());
        assertEquals("ann@example.com", request.email(), "the email comes from the replica row, not a body");
    }

    @Test
    void aRemovalCarriesNoRoles() throws Exception {
        member(7, "ann@example.com", 4, "ASSIGNED", granted("User"));

        commands.remove(TENANT, 7, new TenantUserCommands.Removal(4L));

        TenantUserChangeRequest request = published();
        assertEquals(TenantUserChangeRequest.Action.REMOVE, request.action());
        assertNull(request.roles());
        assertEquals(4L, request.expectedRevision());
        assertFalse(new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(request))
                                      .has("roles"),
                "a removal carries no roles");
    }

    @Test
    void theInputShapeIsEnforced() {
        assertReason("INVALID_EMAIL", HttpStatus.BAD_REQUEST,
                () -> commands.invite(TENANT, new TenantUserCommands.Invite("nope", List.of("User"))));
        assertReason("INVALID_EMAIL", HttpStatus.BAD_REQUEST, () -> commands.invite(TENANT, null));
        assertReason("INVALID_ROLE", HttpStatus.BAD_REQUEST,
                () -> commands.invite(TENANT, new TenantUserCommands.Invite("ann@example.com", List.of("Auditor"))));
        assertReason("NO_ROLES", HttpStatus.BAD_REQUEST,
                () -> commands.invite(TENANT, new TenantUserCommands.Invite("ann@example.com", List.of())));
        assertReason("BAD_REQUEST", HttpStatus.BAD_REQUEST,
                () -> commands.setRoles(TENANT, 7, new TenantUserCommands.RolesChange(List.of("User"), null)));
        assertReason("BAD_REQUEST", HttpStatus.BAD_REQUEST, () -> commands.remove(TENANT, 7, null));
        verify(publisher, never()).publish(any());
    }

    @Test
    void aMemberIsNotInvitedAgainAndAPendingOneWaits() {
        when(replica.find(TENANT, "ann@example.com")).thenReturn(Optional.of(view(7, "ann@example.com", 4, "ASSIGNED", granted("User"))));
        assertReason("ALREADY_MEMBER", HttpStatus.CONFLICT,
                () -> commands.invite(TENANT, new TenantUserCommands.Invite("ann@example.com", List.of("User"))));

        when(replica.find(TENANT, "ben@example.com")).thenReturn(Optional.of(view(8, "ben@example.com", 4, "PENDING", adding("User"))));
        assertReason("REQUEST_PENDING", HttpStatus.CONFLICT,
                () -> commands.invite(TENANT, new TenantUserCommands.Invite("ben@example.com", List.of("User"))));
    }

    @Test
    void aFailedOrRemovedPersonMayBeInvitedAgain() {
        when(replica.find(TENANT, "ann@example.com")).thenReturn(Optional.of(view(7, "ann@example.com", 4, "FAILED")));
        when(replica.find(TENANT, "ben@example.com")).thenReturn(Optional.of(view(8, "ben@example.com", 9, "REMOVED")));

        commands.invite(TENANT, new TenantUserCommands.Invite("ann@example.com", List.of("User")));
        commands.invite(TENANT, new TenantUserCommands.Invite("ben@example.com", List.of("User")));

        verify(publisher, org.mockito.Mockito.times(2)).publish(any());
    }

    @Test
    void anUnknownOrRemovedUserIsNotFound() {
        assertReason("USER_NOT_FOUND", HttpStatus.NOT_FOUND,
                () -> commands.setRoles(TENANT, 99, new TenantUserCommands.RolesChange(List.of("User"), 1L)));
        when(replica.find(TENANT, 98L)).thenReturn(Optional.of(view(98, "gone@example.com", 9, "REMOVED")));
        assertReason("USER_NOT_FOUND", HttpStatus.NOT_FOUND, () -> commands.remove(TENANT, 98, new TenantUserCommands.Removal(9L)));
    }

    @Test
    void theAdvisoryRulesOfARoleChange() {
        member(7, "ann@example.com", 4, "ASSIGNED", granted("User"), adding("Owner"));
        assertReason("REQUEST_PENDING", HttpStatus.CONFLICT,
                () -> commands.setRoles(TENANT, 7, new TenantUserCommands.RolesChange(List.of("User"), 4L)));

        member(8, "failed@example.com", 2, "FAILED");
        assertReason("NOT_A_MEMBER", HttpStatus.CONFLICT,
                () -> commands.setRoles(TENANT, 8, new TenantUserCommands.RolesChange(List.of("User"), 2L)));

        member(9, "ben@example.com", 5, "ASSIGNED", granted("User"));
        assertReason("STALE_REVISION", HttpStatus.CONFLICT,
                () -> commands.setRoles(TENANT, 9, new TenantUserCommands.RolesChange(List.of("Owner"), 4L)));
        assertReason("NO_CHANGE", HttpStatus.CONFLICT,
                () -> commands.setRoles(TENANT, 9, new TenantUserCommands.RolesChange(List.of("User"), 5L)));
        verify(publisher, never()).publish(any());
    }

    @Test
    void theLastOwnerKeepsTheRole() {
        member(7, "boss@example.com", 4, "ASSIGNED", granted("Owner"), granted("User"));

        assertReason("LAST_OWNER", HttpStatus.CONFLICT,
                () -> commands.setRoles(TENANT, 7, new TenantUserCommands.RolesChange(List.of("User"), 4L)));
        assertReason("LAST_OWNER", HttpStatus.CONFLICT, () -> commands.remove(TENANT, 7, new TenantUserCommands.Removal(4L)));

        addOwner(8, "second@example.com");
        commands.setRoles(TENANT, 7, new TenantUserCommands.RolesChange(List.of("User"), 4L));
        verify(publisher).publish(any());
    }

    @Test
    void dismissingAFailedRowIsAlwaysAllowed() {
        member(7, "failed@example.com", 3, "FAILED");

        commands.remove(TENANT, 7, new TenantUserCommands.Removal(1L));

        assertEquals(TenantUserChangeRequest.Action.REMOVE, published().action());
    }

    @Test
    void aBrokerFailureIsA503AndNothingIsWritten() {
        doThrow(new TenantUsersException(HttpStatus.SERVICE_UNAVAILABLE, "PUBLISH_FAILED", "down")).when(publisher)
                                                                                                   .publish(any());

        assertReason("PUBLISH_FAILED", HttpStatus.SERVICE_UNAVAILABLE,
                () -> commands.invite(TENANT, new TenantUserCommands.Invite("ann@example.com", List.of("User"))));
        verify(replica, never()).apply(any(), any());
    }

    private TenantUserChangeRequest published() {
        ArgumentCaptor<TenantUserChangeRequest> request = ArgumentCaptor.forClass(TenantUserChangeRequest.class);
        verify(publisher).publish(request.capture());
        return request.getValue();
    }

    private void member(long id, String email, long revision, String status, TenantUserSync.RoleState... roles) {
        TenantUserView user = view(id, email, revision, status, roles);
        when(replica.find(TENANT, id)).thenReturn(Optional.of(user));
        live.add(user);
    }

    private void addOwner(long id, String email) {
        live.add(view(id, email, 1, "ASSIGNED", granted("Owner")));
    }

    private static TenantUserView view(long id, String email, long revision, String status, TenantUserSync.RoleState... roles) {
        return new TenantUserView(id, email, status, revision, List.of(roles), null, null, null, null, null, null);
    }

    private static TenantUserSync.RoleState granted(String role) {
        return new TenantUserSync.RoleState(role, "GRANTED", OWNER, "2026-09-30T10:00:00Z");
    }

    private static TenantUserSync.RoleState adding(String role) {
        return new TenantUserSync.RoleState(role, "ADDING", null, null);
    }

    private static void assertReason(String reason, HttpStatus status, Runnable command) {
        TenantUsersException refusal = assertThrows(TenantUsersException.class, command::run);
        assertEquals(reason, refusal.reason());
        assertEquals(status, refusal.status());
        assertTrue(refusal.getMessage() != null && !refusal.getMessage()
                                                           .isBlank());
    }
}
