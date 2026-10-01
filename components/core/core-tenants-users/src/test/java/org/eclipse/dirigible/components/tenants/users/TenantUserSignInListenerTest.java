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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.eclipse.dirigible.components.tenants.tenant.TenantEnteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class TenantUserSignInListenerTest {

    private static final Instant AT = Instant.parse("2026-09-30T10:00:00Z");

    private TenantUserRepository users;
    private TenantUserSignInListener listener;

    @BeforeEach
    void setUp() {
        users = mock(TenantUserRepository.class);
        when(users.findByTenantIdAndEmail(any(), any())).thenReturn(Optional.empty());
        listener = new TenantUserSignInListener(users, mock(PlatformTransactionManager.class));
    }

    @Test
    void anExistingRowIsStamped() {
        TenantUser ann = row("ann@example.com", TenantUserStatus.ASSIGNED);

        listener.onTenantEntered(new TenantEnteredEvent("acme", "f3a1", "Ann@Example.com", AT));

        assertEquals(AT, ann.getLastSignInAt());
    }

    @Test
    void anEmailShapedPrincipalStandsInForAMissingClaim() {
        TenantUser ann = row("ann@example.com", TenantUserStatus.INVITED);

        listener.onTenantEntered(new TenantEnteredEvent("acme", "ann@example.com", null, AT));

        assertEquals(AT, ann.getLastSignInAt());
    }

    @Test
    void aTombstoneIsNotStamped() {
        TenantUser gone = row("gone@example.com", TenantUserStatus.REMOVED);

        listener.onTenantEntered(new TenantEnteredEvent("acme", "x", "gone@example.com", AT));

        assertNull(gone.getLastSignInAt());
    }

    @Test
    void withoutAnEmailNothingIsLookedUp() {
        listener.onTenantEntered(new TenantEnteredEvent("acme", "f3a1", null, AT));

        verify(users, never()).findByTenantIdAndEmail(any(), any());
    }

    @Test
    void aFailureNeverEscapes() {
        when(users.findByTenantIdAndEmail(any(), any())).thenThrow(new IllegalStateException("the database is down"));

        assertDoesNotThrow(() -> listener.onTenantEntered(new TenantEnteredEvent("acme", "x", "ann@example.com", AT)));
    }

    private TenantUser row(String email, TenantUserStatus status) {
        TenantUser user = new TenantUser("acme", email);
        user.setStatus(status);
        when(users.findByTenantIdAndEmail("acme", email)).thenReturn(Optional.of(user));
        return user;
    }
}
