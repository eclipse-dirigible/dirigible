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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

class TenantUserReplicaServiceTest {

    private static final String TENANT = "acme";
    private static final String SIGN_IN = "2026-09-29T08:00:00Z";
    private static final String GRANTED_AT = "2026-09-30T10:00:00Z";

    private TenantUserRepository users;
    private TenantService tenants;
    private TenantUserReplicaService service;

    @BeforeEach
    void setUp() {
        users = mock(TenantUserRepository.class);
        tenants = mock(TenantService.class);
        when(tenants.findById(TENANT)).thenReturn(Optional.of(mock(Tenant.class)));
        when(users.findForUpdateByTenantIdAndEmail(any(), any())).thenReturn(Optional.empty());
        when(users.findByTenantIdAndStatusNotOrderByEmail(TENANT, TenantUserStatus.REMOVED)).thenReturn(List.of());
        // A mock manager runs every TransactionTemplate callback inline.
        service = new TenantUserReplicaService(users, tenants, mock(PlatformTransactionManager.class));
    }

    @Test
    void aLowerRevisionIsIgnored() {
        TenantUser ann = stored("ann@example.com", 5, TenantUserStatus.ASSIGNED);

        TenantUserSync.Result result = service.apply(TENANT, changed(user("ann@example.com", 4, "INVITED")));

        assertEquals(0, result.applied());
        assertEquals(1, result.ignored());
        assertEquals(TenantUserStatus.ASSIGNED, ann.getStatus());
        assertEquals(5, ann.getRevision());
    }

    @Test
    void anEqualRevisionIsIgnored() {
        TenantUser ann = stored("ann@example.com", 5, TenantUserStatus.ASSIGNED);

        TenantUserSync.Result result = service.apply(TENANT, changed(user("ann@example.com", 5, "INVITED")));

        assertEquals(1, result.ignored());
        assertEquals(TenantUserStatus.ASSIGNED, ann.getStatus());
    }

    @Test
    void aHigherRevisionReplacesEveryFieldAndTheRolesAndKeepsTheSignIn() {
        TenantUser ann = stored("ann@example.com", 5, TenantUserStatus.INVITED);
        ann.setLastSignInAt(Instant.parse(SIGN_IN));
        ann.setLastErrorCode("FAILED");
        ann.setLastErrorMessage("the identity provider was down");
        ann.addRole("User", TenantUserRoleState.GRANTED);

        TenantUserSync.Snapshot snapshot = new TenantUserSync.Snapshot("Ann@Example.com ", 6L, "ASSIGNED",
                List.of(role("Owner", "ADDING", null, null), role("User", "REMOVING", "bob@acme.test", GRANTED_AT)), "bob@acme.test",
                "2026-09-20T08:00:00Z", "carol@acme.test", "2026-09-30T10:15:03Z", null);
        TenantUserSync.Result result = service.apply(TENANT, changed(snapshot));

        assertEquals(1, result.applied());
        assertEquals(TenantUserStatus.ASSIGNED, ann.getStatus());
        assertEquals(6, ann.getRevision());
        assertEquals("bob@acme.test", ann.getInvitedBy());
        assertEquals(Instant.parse("2026-09-20T08:00:00Z"), ann.getInvitedAt());
        assertEquals("carol@acme.test", ann.getLastChangedBy());
        assertEquals(Instant.parse("2026-09-30T10:15:03Z"), ann.getLastChangedAt());
        assertNull(ann.getLastErrorCode(), "a null lastError clears the code");
        assertNull(ann.getLastErrorMessage(), "and the message");
        assertEquals(Instant.parse(SIGN_IN), ann.getLastSignInAt(), "the application's own column survives the apply");
        assertEquals(2, ann.getRoles()
                           .size());
        TenantUserRole owner = role(ann, "Owner");
        assertEquals(TenantUserRoleState.ADDING, owner.getState());
        assertNull(owner.getGrantedBy());
        assertNull(owner.getGrantedAt());
        TenantUserRole user = role(ann, "User");
        assertEquals(TenantUserRoleState.REMOVING, user.getState());
        assertEquals("bob@acme.test", user.getGrantedBy());
        assertEquals(Instant.parse(GRANTED_AT), user.getGrantedAt());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void theLastErrorIsStoredAsSent() {
        TenantUser ann = stored("ann@example.com", 5, TenantUserStatus.ASSIGNED);

        service.apply(TENANT, changed(new TenantUserSync.Snapshot("ann@example.com", 6L, "ASSIGNED", List.of(), null, null, null, null,
                new TenantUserSync.LastError("LAST_OWNER", "the tenant would have no owner"))));

        assertEquals("LAST_OWNER", ann.getLastErrorCode());
        assertEquals("the tenant would have no owner", ann.getLastErrorMessage());
    }

    @Test
    void removedDropsTheRolesAndKeepsTheRevision() {
        TenantUser ann = stored("ann@example.com", 5, TenantUserStatus.ASSIGNED);
        ann.addRole("User", TenantUserRoleState.GRANTED);

        service.apply(TENANT, changed(new TenantUserSync.Snapshot("ann@example.com", 7L, "REMOVED",
                List.of(role("User", "GRANTED", "bob@acme.test", GRANTED_AT)), null, null, "bob@acme.test", GRANTED_AT, null)));

        assertEquals(TenantUserStatus.REMOVED, ann.getStatus());
        assertEquals(7, ann.getRevision());
        assertTrue(ann.getRoles()
                      .isEmpty());
    }

    @Test
    void aLateOlderLiveSnapshotDoesNotReviveATombstone() {
        TenantUser ann = stored("ann@example.com", 7, TenantUserStatus.REMOVED);

        TenantUserSync.Result result = service.apply(TENANT, changed(user("ann@example.com", 6, "ASSIGNED")));

        assertEquals(1, result.ignored());
        assertEquals(TenantUserStatus.REMOVED, ann.getStatus());
    }

    @Test
    void aNewerLiveSnapshotRevivesATombstoneAndClearsTheSignIn() {
        TenantUser ann = stored("ann@example.com", 7, TenantUserStatus.REMOVED);
        ann.setLastSignInAt(Instant.parse(SIGN_IN));

        service.apply(TENANT, changed(user("ann@example.com", 8, "ASSIGNED")));

        assertEquals(TenantUserStatus.ASSIGNED, ann.getStatus());
        assertNull(ann.getLastSignInAt(), "a revived membership starts without the old sign-in");
    }

    @Test
    void aResyncTombstonesOnlyAbsentLiveRowsAtOrBelowItsRevision() {
        TenantUser below = stored("below@example.com", 3, TenantUserStatus.ASSIGNED);
        below.addRole("User", TenantUserRoleState.GRANTED);
        TenantUser above = stored("above@example.com", 12, TenantUserStatus.ASSIGNED);
        TenantUser named = stored("named@example.com", 9, TenantUserStatus.ASSIGNED);
        when(users.findByTenantIdAndStatusNotOrderByEmail(TENANT, TenantUserStatus.REMOVED)).thenReturn(List.of(above, below, named));

        TenantUserSync.Result result =
                service.apply(TENANT, new TenantUserSync(true, 10L, List.of(user("named@example.com", 10, "ASSIGNED"))));

        assertEquals(1, result.removed());
        assertEquals(TenantUserStatus.REMOVED, below.getStatus());
        assertEquals(10, below.getRevision(), "the tombstone is stamped with the resync's revision");
        assertTrue(below.getRoles()
                        .isEmpty());
        assertEquals(TenantUserStatus.ASSIGNED, above.getStatus(), "a row written after the list was read is kept");
        assertEquals(12, above.getRevision());
        assertEquals(10, named.getRevision(), "a named user is applied, not tombstoned");
    }

    @Test
    void aResyncLeavesAnAbsentTombstoneUntouchedAndUncounted() {
        // The live-rows query does not return tombstones, so an absent one is never visited.
        TenantUserSync.Result result = service.apply(TENANT, new TenantUserSync(true, 10L, List.of()));

        assertEquals(0, result.removed());
        verify(users, never()).findForUpdateByTenantIdAndEmail(any(), any());
    }

    @Test
    void anUnknownRemovedUserIsStoredAsATombstone() {
        service.apply(TENANT, changed(user("gone@example.com", 3, "REMOVED")));

        verify(users).saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                user -> user.getStatus() == TenantUserStatus.REMOVED && user.getRevision() == 3 && user.getEmail()
                                                                                                       .equals("gone@example.com")));
    }

    @Test
    void anInsertRaceIsRetriedOnceAgainstTheRowThatWon() {
        TenantUser winner = new TenantUser(TENANT, "ann@example.com");
        winner.setStatus(TenantUserStatus.ASSIGNED);
        winner.setRevision(5);
        when(users.findForUpdateByTenantIdAndEmail(TENANT, "ann@example.com")).thenReturn(Optional.empty(), Optional.of(winner));
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("UK_DIRIGIBLE_TENANT_USERS_TENANT_EMAIL"));

        TenantUserSync.Result result = service.apply(TENANT, changed(user("ann@example.com", 4, "INVITED")));

        assertEquals(1, result.ignored(), "the retry compares revisions with the row that won");
        verify(users, times(1)).saveAndFlush(any());
        verify(users, times(2)).findForUpdateByTenantIdAndEmail(TENANT, "ann@example.com");
    }

    @Test
    void aBadTenantIdIsA400() {
        ResponseStatusException refused =
                assertThrows(ResponseStatusException.class, () -> service.apply("-bad-", changed(user("ann@example.com", 1, "INVITED"))));
        assertEquals(HttpStatus.BAD_REQUEST, refused.getStatusCode());
    }

    @Test
    void anUnknownTenantIsA404() {
        ResponseStatusException refused =
                assertThrows(ResponseStatusException.class, () -> service.apply("nowhere", changed(user("ann@example.com", 1, "INVITED"))));
        assertEquals(HttpStatus.NOT_FOUND, refused.getStatusCode());
    }

    @Test
    void anInvalidBodyIsA400BeforeAnythingIsWritten() {
        ResponseStatusException refused =
                assertThrows(ResponseStatusException.class, () -> service.apply(TENANT, changed(user("not-an-email", 1, "INVITED"))));
        assertEquals(HttpStatus.BAD_REQUEST, refused.getStatusCode());
        verify(users, never()).findForUpdateByTenantIdAndEmail(any(), any());
    }

    @Test
    void theStoredRevisionIsTheRepositoryMaximum() {
        when(users.findMaxRevision(TENANT)).thenReturn(42L);

        TenantUserSync.Result result = service.apply(TENANT, changed(user("ann@example.com", 4, "INVITED")));

        assertEquals(42L, result.storedRevision());
    }

    private TenantUser stored(String email, long revision, TenantUserStatus status) {
        TenantUser user = new TenantUser(TENANT, email);
        user.setStatus(status);
        user.setRevision(revision);
        when(users.findForUpdateByTenantIdAndEmail(TENANT, email)).thenReturn(Optional.of(user));
        return user;
    }

    private static TenantUserSync changed(TenantUserSync.Snapshot... snapshots) {
        return new TenantUserSync(false, 1L, Arrays.asList(snapshots));
    }

    private static TenantUserSync.Snapshot user(String email, long revision, String status) {
        return new TenantUserSync.Snapshot(email, revision, status, List.of(), null, null, null, null, null);
    }

    private static TenantUserSync.RoleState role(String role, String state, String grantedBy, String grantedAt) {
        return new TenantUserSync.RoleState(role, state, grantedBy, grantedAt);
    }

    private static TenantUserRole role(TenantUser user, String name) {
        return user.getRoles()
                   .stream()
                   .filter(row -> row.getRole()
                                     .equals(name))
                   .findFirst()
                   .orElseThrow();
    }
}
