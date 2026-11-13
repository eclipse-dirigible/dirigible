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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.TenantProperties;
import org.junit.jupiter.api.Test;

class TenantPropertiesTenantTeardownTest {

    private final TenantPropertiesRepository repository = mock(TenantPropertiesRepository.class);
    private final TenantPropertiesTenantTeardown teardown = new TenantPropertiesTenantTeardown(new TenantPropertiesService(repository));

    @Test
    void runsAfterTheUsersBesideTheInitialization() {
        assertEquals(410, teardown.order());
    }

    @Test
    void deletesTheTenantsRowAndNoOtherTenants() {
        TenantProperties acme = new TenantProperties("acme");
        when(repository.findById("acme")).thenReturn(Optional.of(acme));

        teardown.teardown("acme");

        verify(repository).delete(acme);
        verify(repository, never()).findById("globex");
    }

    @Test
    void aTenantWithoutARowTearsDownWithoutError() {
        when(repository.findById("acme")).thenReturn(Optional.empty());

        teardown.teardown("acme");
        teardown.teardown("acme");

        verify(repository, never()).delete(any());
    }
}
