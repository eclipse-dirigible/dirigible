/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.dirigible.components.tenants.tenant.TenantContextInitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import jakarta.servlet.Filter;

/**
 * The tenant scope has to be open before the tenant selection runs, in every chain that selects
 * tenants (#8458).
 */
class TenantSelectionChainValidatorTest {

    private final Filter scope = mock(TenantContextInitFilter.class);
    private final Filter selection = mock(TenantSelectionFilter.class);
    private final Filter authentication = (request, response, chain) -> chain.doFilter(request, response);

    @Test
    void aChainOpeningTheScopeBeforeTheSelectionIsAccepted() {
        assertThatCode(() -> TenantSelectionChainValidator.validate(chain(scope, authentication, selection))).doesNotThrowAnyException();
    }

    @Test
    void aChainSelectingWithoutTheScopeIsRefused() {
        assertThatThrownBy(() -> TenantSelectionChainValidator.validate(chain(authentication, selection))).isInstanceOf(
                IllegalStateException.class)
                                                                                                          .hasMessageContaining(
                                                                                                                  "TenantContextInitFilter")
                                                                                                          .hasMessageContaining(
                                                                                                                  "default tenant");
    }

    @Test
    void aChainOpeningTheScopeAfterTheSelectionIsRefused() {
        assertThatThrownBy(() -> TenantSelectionChainValidator.validate(chain(authentication, selection, scope))).isInstanceOf(
                IllegalStateException.class);
    }

    @Test
    void aChainThatSelectsNoTenantIsNotItsConcern() {
        assertThatCode(() -> TenantSelectionChainValidator.validate(chain(authentication))).doesNotThrowAnyException();
    }

    @Test
    void everyChainOfTheDeploymentIsValidated() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SecurityFilterChain> chains = mock(ObjectProvider.class);
        when(chains.orderedStream()).thenAnswer(invocation -> Stream.of(chain(scope, selection), chain(authentication, selection)));

        assertThatThrownBy(() -> new TenantSelectionChainValidator(chains).afterSingletonsInstantiated()).isInstanceOf(
                IllegalStateException.class);
    }

    private static SecurityFilterChain chain(Filter... filters) {
        return new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE, filters);
    }
}
