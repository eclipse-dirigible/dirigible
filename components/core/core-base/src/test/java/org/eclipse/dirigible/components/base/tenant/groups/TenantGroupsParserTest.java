/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant.groups;

import org.eclipse.dirigible.components.base.tenant.GlobalRolesScope;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The group grammar is {@code <tenantId>.<appId>.<role>}: this application's groups become tenant
 * roles, other applications' groups are none of this deployment's business, and anything that is
 * not tenant-bearing stays a global role.
 */
class TenantGroupsParserTest {

    private static final String APP_ID = "library";

    @Test
    void tenantGroupsOfThisApplicationBecomeTenantRoles() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("acme.library.Owner", "acme.library.User"), APP_ID);

        assertEquals(Set.of("acme"), assignments.tenantIds());
        assertEquals(Set.of("Owner", "User"), assignments.rolesFor("acme"));
        assertTrue(assignments.globalRoles()
                              .isEmpty());
    }

    @Test
    void severalTenantsAreKeptApart() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("acme.library.Owner", "globex.library.User"), APP_ID);

        assertEquals(Set.of("acme", "globex"), assignments.tenantIds());
        assertEquals(Set.of("Owner"), assignments.rolesFor("acme"));
        assertEquals(Set.of("User"), assignments.rolesFor("globex"));
    }

    @Test
    void roleNamesMayContainDots() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("acme.library.Tenant.Owner"), APP_ID);

        assertEquals(Set.of("Tenant.Owner"), assignments.rolesFor("acme"));
    }

    @Test
    void uuidTenantIdsAreSupported() {
        String tenantId = "6597d633-b0f9-4cb2-b134-4bf2a52b654f";

        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of(tenantId + ".library.User"), APP_ID);

        assertEquals(Set.of(tenantId), assignments.tenantIds());
        assertEquals(Set.of("User"), assignments.rolesFor(tenantId));
    }

    @Test
    void groupsOfOtherApplicationsAreIgnored() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("acme.bi.Owner", "globex.crm.User"), APP_ID);

        assertTrue(assignments.hasNoTenants());
        assertTrue(assignments.globalRoles()
                              .isEmpty(),
                "a tenant group of another application is not a global role");
    }

    @Test
    void theApplicationIdIsMatchedExactly() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("acme.LIBRARY.Owner", "acme.library2.Owner"), APP_ID);

        assertTrue(assignments.hasNoTenants());
    }

    @Test
    void groupsThatAreNotTenantBearingBecomeGlobalRoles() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("DEVELOPER", "OPERATOR", "acme.library.Owner"), APP_ID);

        assertEquals(Set.of("DEVELOPER", "OPERATOR"), assignments.globalRoles());
        assertEquals(Set.of("Owner"), assignments.rolesFor("acme"));
    }

    @Test
    void malformedTenantGroupsAreTreatedAsGlobalRoles() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("acme.library", ".library.Owner", "acme..Owner"), APP_ID);

        assertTrue(assignments.hasNoTenants());
        assertEquals(Set.of("acme.library", ".library.Owner", "acme..Owner"), assignments.globalRoles());
    }

    @Test
    void duplicatesCollapse() {
        UserTenantAssignments assignments =
                TenantGroupsParser.parse(List.of("acme.library.Owner", "acme.library.Owner", "DEVELOPER", "DEVELOPER"), APP_ID);

        assertEquals(Set.of("Owner"), assignments.rolesFor("acme"));
        assertEquals(Set.of("DEVELOPER"), assignments.globalRoles());
    }

    @Test
    void surroundingWhitespaceIsTrimmedAndBlanksSkipped() {
        UserTenantAssignments assignments =
                TenantGroupsParser.parse(Arrays.asList("  acme.library.Owner  ", "   ", "", null, " DEVELOPER "), APP_ID);

        assertEquals(Set.of("Owner"), assignments.rolesFor("acme"));
        assertEquals(Set.of("DEVELOPER"), assignments.globalRoles());
    }

    @Test
    void noGroupsYieldEmptyAssignments() {
        assertTrue(TenantGroupsParser.parse(null, APP_ID)
                                     .hasNoTenants());
        assertTrue(TenantGroupsParser.parse(Collections.emptyList(), APP_ID)
                                     .globalRoles()
                                     .isEmpty());
    }

    @Test
    void aMissingApplicationIdIsAMisconfiguration() {
        List<String> groups = List.of("acme.library.Owner");

        assertThrows(IllegalArgumentException.class, () -> TenantGroupsParser.parse(groups, null));
        assertThrows(IllegalArgumentException.class, () -> TenantGroupsParser.parse(groups, "   "));
    }

    @Test
    void rolesForAnUnknownTenantAreEmptyAndTheResultIsUnmodifiable() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("acme.library.Owner"), APP_ID);

        assertTrue(assignments.rolesFor("unknown")
                              .isEmpty());
        assertFalse(assignments.hasNoTenants());
        assertThrows(UnsupportedOperationException.class, () -> assignments.rolesFor("acme")
                                                                           .add("Admin"));
        assertThrows(UnsupportedOperationException.class, () -> assignments.globalRoles()
                                                                           .add("DEVELOPER"));
    }

    @Test
    void theTwoArgumentParseUsesTheInstanceScope() {
        List<String> groups = List.of("ADMINISTRATOR", "library.DEVELOPER", "acme.library.Owner");

        assertEquals(TenantGroupsParser.parse(groups, APP_ID, GlobalRolesScope.INSTANCE)
                                       .globalRoles(),
                TenantGroupsParser.parse(groups, APP_ID)
                                  .globalRoles());
        assertEquals(Set.of("ADMINISTRATOR", "library.DEVELOPER"), TenantGroupsParser.parse(groups, APP_ID, null)
                                                                                     .globalRoles());
    }

    @Test
    void underTheApplicationScopeAnUnscopedGroupGrantsNothing() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(List.of("ADMINISTRATOR", "DEVELOPER", "eu-west-1_abc_Google"), APP_ID,
                GlobalRolesScope.APPLICATION);

        assertTrue(assignments.hasNoTenants());
        assertTrue(assignments.globalRoles()
                              .isEmpty());
    }

    @Test
    void underTheApplicationScopeAGroupNamingThisApplicationGrantsItsRole() {
        UserTenantAssignments assignments =
                TenantGroupsParser.parse(List.of("library.ADMINISTRATOR", "library.DEVELOPER"), APP_ID, GlobalRolesScope.APPLICATION);

        assertTrue(assignments.hasNoTenants());
        assertEquals(Set.of("ADMINISTRATOR", "DEVELOPER"), assignments.globalRoles());
    }

    @Test
    void underTheApplicationScopeAGroupOfAnotherApplicationGrantsNothing() {
        UserTenantAssignments assignments = TenantGroupsParser.parse(
                List.of("suite.ADMINISTRATOR", "LIBRARY.ADMINISTRATOR", "library2.DEVELOPER"), APP_ID, GlobalRolesScope.APPLICATION);

        assertTrue(assignments.globalRoles()
                              .isEmpty());
    }

    @Test
    void underTheApplicationScopeTenantGroupsWorkAsBefore() {
        List<String> groups = List.of("acme.library.Owner", "acme.library.Tenant.Owner", "globex.library.User", "acme.bi.Owner",
                "library.library.Owner", "library.ADMINISTRATOR");

        UserTenantAssignments scoped = TenantGroupsParser.parse(groups, APP_ID, GlobalRolesScope.APPLICATION);
        UserTenantAssignments unscoped = TenantGroupsParser.parse(groups, APP_ID, GlobalRolesScope.INSTANCE);

        assertEquals(Set.of("acme", "globex", "library"), scoped.tenantIds());
        assertEquals(unscoped.tenantIds(), scoped.tenantIds());
        assertEquals(Set.of("Owner", "Tenant.Owner"), scoped.rolesFor("acme"));
        assertEquals(Set.of("User"), scoped.rolesFor("globex"));
        assertEquals(Set.of("Owner"), scoped.rolesFor("library"), "a group with two dots is a tenant group, whatever its first segment");
        assertEquals(Set.of("ADMINISTRATOR"), scoped.globalRoles());
    }

    @Test
    void underTheApplicationScopeMalformedGroupsGrantNothing() {
        UserTenantAssignments assignments =
                TenantGroupsParser.parse(List.of("library.", ".ADMINISTRATOR", "library", "acme..Owner", " library.OPERATOR "), APP_ID,
                        GlobalRolesScope.APPLICATION);

        assertTrue(assignments.hasNoTenants());
        assertEquals(Set.of("OPERATOR"), assignments.globalRoles());
    }
}
