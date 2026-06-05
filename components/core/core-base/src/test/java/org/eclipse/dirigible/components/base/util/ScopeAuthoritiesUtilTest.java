/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The Class ScopeAuthoritiesUtilTest.
 */
class ScopeAuthoritiesUtilTest {

    @Test
    void extractScopeNameTakesSubstringAfterLastSlash() {
        assertEquals("ADMINISTRATOR", ScopeAuthoritiesUtil.extractScopeName("sample-resource-server-1a2b3c/ADMINISTRATOR"));
        assertEquals("sample-app.Orders.OrderFullAccess",
                ScopeAuthoritiesUtil.extractScopeName("sample-resource-server-1a2b3c/sample-app.Orders.OrderFullAccess"));
    }

    @Test
    void extractScopeNameIgnoresStandardOidcScopes() {
        assertNull(ScopeAuthoritiesUtil.extractScopeName("openid"));
        assertNull(ScopeAuthoritiesUtil.extractScopeName("email"));
        assertNull(ScopeAuthoritiesUtil.extractScopeName("profile"));
        assertNull(ScopeAuthoritiesUtil.extractScopeName("aws.cognito.signin.user.admin"));
        assertNull(ScopeAuthoritiesUtil.extractScopeName(null));
    }

    @Test
    void resolveRoleNamesMapsScopesOneToOneAndIgnoresOidcScopes() {
        List<String> rawScopes = Arrays.asList("rs-xyz/ADMINISTRATOR", "rs-xyz/sample-app.Orders.OrderFullAccess", "openid");

        Set<String> roleNames = ScopeAuthoritiesUtil.resolveRoleNames(rawScopes, Collections.emptyMap());

        assertEquals(Set.of("ADMINISTRATOR", "sample-app.Orders.OrderFullAccess"), roleNames);
    }

    @Test
    void resolveRoleNamesExpandsMappedScopeToMultipleRolesAndFallsBackForUnmapped() {
        Map<String, List<String>> mappings =
                Map.of("orders-manage", List.of("sample-app.Orders.OrderFullAccess", "sample-app.Orders.OrderReadOnly"));
        List<String> rawScopes = Arrays.asList("rs-xyz/orders-manage", "rs-xyz/ADMINISTRATOR");

        Set<String> roleNames = ScopeAuthoritiesUtil.resolveRoleNames(rawScopes, mappings);

        assertTrue(roleNames.contains("sample-app.Orders.OrderFullAccess"));
        assertTrue(roleNames.contains("sample-app.Orders.OrderReadOnly"));
        assertTrue(roleNames.contains("ADMINISTRATOR"));
        assertEquals(3, roleNames.size());
    }

    @Test
    void resolveRoleNamesReturnsEmptyForNullOrEmptyInput() {
        assertTrue(ScopeAuthoritiesUtil.resolveRoleNames(null, Collections.emptyMap())
                                       .isEmpty());
        assertTrue(ScopeAuthoritiesUtil.resolveRoleNames(Collections.emptyList(), null)
                                       .isEmpty());
    }
}
