/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.security.service.ScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The Class ScopeRoleJwtAuthoritiesConverterTest.
 */
@ExtendWith(MockitoExtension.class)
class ScopeRoleJwtAuthoritiesConverterTest {

    @Mock
    private ScopeService scopeService;

    private Jwt jwt(String scopeClaim) {
        return Jwt.withTokenValue("token")
                  .header("alg", "none")
                  .issuedAt(Instant.EPOCH)
                  .expiresAt(Instant.EPOCH.plusSeconds(3600))
                  .claim("scope", scopeClaim)
                  .build();
    }

    private Set<String> authorityNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                          .map(GrantedAuthority::getAuthority)
                          .collect(Collectors.toSet());
    }

    @Test
    void mapsRoleNamedScopesToRoleAuthoritiesAndIgnoresOidcScopes() {
        when(scopeService.getScopeRolesMappings()).thenReturn(Collections.emptyMap());
        ScopeRoleJwtAuthoritiesConverter converter = new ScopeRoleJwtAuthoritiesConverter(scopeService);

        Collection<GrantedAuthority> authorities =
                converter.convert(jwt("rs-xyz/ADMINISTRATOR rs-xyz/sample-app.Orders.OrderFullAccess openid"));

        Set<String> names = authorityNames(authorities);
        assertEquals(Set.of("ROLE_ADMINISTRATOR", "ROLE_sample-app.Orders.OrderFullAccess"), names);
        assertFalse(names.contains("ROLE_openid"));
        assertFalse(names.contains("SCOPE_openid"));
    }

    @Test
    void expandsMappedScopeToMultipleRolesAndFallsBackForUnmapped() {
        when(scopeService.getScopeRolesMappings()).thenReturn(
                Map.of("orders-manage", Set.of("sample-app.Orders.OrderFullAccess", "sample-app.Orders.OrderReadOnly")));
        ScopeRoleJwtAuthoritiesConverter converter = new ScopeRoleJwtAuthoritiesConverter(scopeService);

        Collection<GrantedAuthority> authorities = converter.convert(jwt("rs-xyz/orders-manage rs-xyz/ADMINISTRATOR"));

        Set<String> names = authorityNames(authorities);
        assertTrue(names.contains("ROLE_sample-app.Orders.OrderFullAccess"));
        assertTrue(names.contains("ROLE_sample-app.Orders.OrderReadOnly"));
        assertTrue(names.contains("ROLE_ADMINISTRATOR"));
        assertEquals(3, names.size());
    }
}
