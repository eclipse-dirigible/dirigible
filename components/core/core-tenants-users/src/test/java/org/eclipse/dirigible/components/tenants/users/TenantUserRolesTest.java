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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The catalog of the roles a person of a tenant may be given (#8728).
 */
class TenantUserRolesTest {

    private final List<Role> registered = new ArrayList<>();
    private TenantUserRoles roles;

    @BeforeEach
    void setUp() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.getAll()).thenReturn(registered);
        roles = new TenantUserRoles(roleService);
    }

    @Test
    void anApplicationThatMarksNothingOffersOwnerAndUser() {
        registered.add(role("/acme/acme.roles", "acme.Invoices.InvoiceReadOnly", null, null, false));
        registered.add(role("/acme/acme.roles", "acme.Invoices.InvoiceFullAccess", null, null, false));

        assertEquals(List.of(offer("Owner"), offer("User")), roles.catalog());
        assertEquals(List.of("Owner", "User"), roles.names());
    }

    @Test
    void theAssignableRolesFollowOwnerAndUserByNameWithTheirLabelAndDescription() {
        registered.add(role("/acme/acme.roles", "Warehouse", "Warehouse clerk", "Reads the stock.", true));
        registered.add(role("/acme/acme.roles", "Accountant", " ", "Enters invoices.", true));
        registered.add(role("/acme/acme.roles", "acme.Invoices.InvoiceReadOnly", null, null, false));

        assertEquals(List.of(offer("Owner"), offer("User"), new TenantUserRoles.Offer("Accountant", null, "Enters invoices."),
                new TenantUserRoles.Offer("Warehouse", "Warehouse clerk", "Reads the stock.")), roles.catalog());
    }

    @Test
    void ownerAndUserKeepTheirPlaceAndARoleDeclaredTwiceIsOfferedOnce() {
        registered.add(role("/acme/acme.roles", "User", null, "Admitted by every generated gate.", true));
        registered.add(role("/acme/acme.roles", "Accountant", "Accountant", null, true));
        registered.add(role("/billing/billing.roles", "Accountant", "Bookkeeper", null, true));

        assertEquals(List.of("Owner", "User", "Accountant"), roles.names());
        assertEquals(offer("User"), roles.catalog()
                                         .get(1));
    }

    @Test
    void aRoleOfTheInstanceTheProvisioningRoleAndAnOverlongNameAreNeverOffered() {
        registered.add(role("/acme/acme.roles", "ADMINISTRATOR", null, null, true));
        registered.add(role("/acme/acme.roles", "DEVELOPER", null, null, true));
        registered.add(role("/acme/acme.roles", "TENANT_PROVISIONER", null, null, true));
        registered.add(role("/acme/acme.roles", "R".repeat(TenantUserSyncValidation.ROLE_MAX + 1), null, null, true));
        registered.add(role("/acme/acme.roles", "R".repeat(TenantUserSyncValidation.ROLE_MAX), null, null, true));

        assertEquals(List.of("Owner", "User", "R".repeat(TenantUserSyncValidation.ROLE_MAX)), roles.names());
    }

    private static TenantUserRoles.Offer offer(String name) {
        return new TenantUserRoles.Offer(name, null, null);
    }

    private static Role role(String location, String name, String label, String description, boolean assignable) {
        Role role = new Role(location, name, description);
        role.setLabel(label);
        role.setAssignable(assignable);
        return role;
    }
}
