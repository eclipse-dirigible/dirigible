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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class TenantUsersAccessTest {

    private TenantContext tenantContext;
    private Tenant tenant;
    private TenantUsersAccess access;

    @BeforeEach
    void setUp() {
        tenantContext = mock(TenantContext.class);
        tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn("acme");
        when(tenantContext.isNotInitialized()).thenReturn(false);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        access = new TenantUsersAccess(tenantContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(String... roles) {
        SecurityContextHolder.getContext()
                             .setAuthentication(new UsernamePasswordAuthenticationToken("someone@example.com", "n/a", Arrays.stream(roles)
                                                                                                                            .map(role -> new SimpleGrantedAuthority(
                                                                                                                                    "ROLE_" + role))
                                                                                                                            .toList()));
    }

    @Test
    void anOwnerManagesAndReads() {
        signIn("Owner");
        assertTrue(access.canManage());
        assertTrue(access.canRead());
        assertEquals("acme", access.requireManager());
    }

    @Test
    void aUserDoesNeither() {
        signIn("User");
        assertFalse(access.canRead());
        TenantUsersException refusal = assertThrows(TenantUsersException.class, access::requireReader);
        assertEquals(HttpStatus.FORBIDDEN, refusal.status());
    }

    @Test
    void anAdministratorReadsButDoesNotInvite() {
        signIn("ADMINISTRATOR", "DEVELOPER");
        assertTrue(access.canRead());
        assertFalse(access.canManage());
        assertThrows(TenantUsersException.class, access::requireManager);
    }

    @Test
    void theDefaultTenantHasNoUsersToManage() {
        when(tenant.isDefault()).thenReturn(true);
        signIn("Owner");
        assertFalse(access.canManage());
        TenantUsersException refusal = assertThrows(TenantUsersException.class, access::requireTenant);
        assertEquals("DEFAULT_TENANT", refusal.reason());
    }
}
