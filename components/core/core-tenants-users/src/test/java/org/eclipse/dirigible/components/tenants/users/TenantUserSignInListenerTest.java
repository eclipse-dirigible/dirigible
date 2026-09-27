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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.tenant.TenantEnteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantUserSignInListenerTest {

    private static final Instant BEFORE = Instant.parse("2026-09-24T09:00:00Z");
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    private ApplicationUserRepository repository;
    private ApplicationUserService users;
    private TenantUserSignInListener listener;

    @BeforeEach
    void setUp() {
        repository = mock(ApplicationUserRepository.class);
        TenantService tenants = mock(TenantService.class);
        when(tenants.findById("acme")).thenReturn(Optional.of(new Tenant()));
        users = new ApplicationUserService(repository, tenants);
        listener = new TenantUserSignInListener(users);
    }

    private ApplicationUser stored(ApplicationUserStatus status) {
        ApplicationUser user = new ApplicationUser("acme", "new.user@example.com", status, BEFORE);
        when(repository.findByTenantIdAndEmail("acme", "new.user@example.com")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void anInvitedUserMatchedByTheEmailClaimBecomesActive() {
        ApplicationUser user = stored(ApplicationUserStatus.INVITED);

        listener.onTenantEntered(new TenantEnteredEvent("acme", "some-uuid", "New.User@Example.com", NOW));

        assertEquals(ApplicationUserStatus.ACTIVE, user.getStatus());
        assertEquals(NOW, user.getLastSignInAt());
    }

    @Test
    void anAssignedUserMatchedByAnEmailShapedPrincipalBecomesActive() {
        ApplicationUser user = stored(ApplicationUserStatus.ASSIGNED);

        listener.onTenantEntered(new TenantEnteredEvent("acme", "new.user@example.com", null, NOW));

        assertEquals(ApplicationUserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void aPendingUserIsStampedButStaysPending() {
        ApplicationUser user = stored(ApplicationUserStatus.PENDING);

        listener.onTenantEntered(new TenantEnteredEvent("acme", "new.user@example.com", null, NOW));

        assertEquals(ApplicationUserStatus.PENDING, user.getStatus());
        assertEquals(NOW, user.getLastSignInAt());
    }

    @Test
    void aPrincipalThatIsNotAnEmailWithoutAClaimMatchesNobody() {
        assertFalse(users.recordSignIn("acme", "legacy-user", null, NOW));
        verify(repository, never()).findByTenantIdAndEmail(any(), any());
    }

    @Test
    void aFailureNeverEscapesTheListener() {
        when(repository.findByTenantIdAndEmail(any(), any())).thenThrow(new IllegalStateException("database down"));

        assertDoesNotThrow(() -> listener.onTenantEntered(new TenantEnteredEvent("acme", "x@example.com", null, NOW)));
    }
}
