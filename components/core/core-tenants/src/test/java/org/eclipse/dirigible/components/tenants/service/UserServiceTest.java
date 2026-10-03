/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserServiceTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService service = new UserService(tenantService, mock(RoleService.class), userRepository, passwordEncoder,
            mock(UserRoleAssignmentRepository.class));

    private User stored;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setId("default-tenant");
        stored = new User(tenant, "jane", passwordEncoder.encode("old-secret"));
        when(tenantService.findById("default-tenant")).thenReturn(Optional.of(tenant));
        when(userRepository.findById("u1")).thenReturn(Optional.of(stored));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** The defect: the new password was stored raw, so the user could no longer log in with it. */
    @Test
    void aNewPasswordIsStoredEncoded() {
        User updated = service.updateUser("u1", "jane", "new-secret", "default-tenant");

        assertNotEquals("new-secret", updated.getPassword());
        assertTrue(passwordEncoder.matches("new-secret", updated.getPassword()));
    }

    /** The Security view's edit dialog sends the stored hash back when the password is not changed. */
    @Test
    void theStoredHashSentBackKeepsThePassword() {
        String hash = stored.getPassword();

        User updated = service.updateUser("u1", "jane", hash, "default-tenant");

        assertEquals(hash, updated.getPassword());
        assertTrue(passwordEncoder.matches("old-secret", updated.getPassword()));
    }

    @Test
    void aBlankPasswordKeepsThePassword() {
        String hash = stored.getPassword();

        assertEquals(hash, service.updateUser("u1", "jane", null, "default-tenant")
                                  .getPassword());
        assertEquals(hash, service.updateUser("u1", "jane", "  ", "default-tenant")
                                  .getPassword());
    }
}
