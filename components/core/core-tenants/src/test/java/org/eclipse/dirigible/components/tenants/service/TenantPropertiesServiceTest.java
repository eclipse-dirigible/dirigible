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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.TenantProperties;
import org.junit.jupiter.api.Test;

class TenantPropertiesServiceTest {

    private final TenantPropertiesRepository repository = mock(TenantPropertiesRepository.class);
    private final TenantPropertiesService service = new TenantPropertiesService(repository);

    @Test
    void aTenantWithoutARowHasNoProperties() {
        when(repository.findById("acme")).thenReturn(Optional.empty());

        assertTrue(service.find("acme")
                          .isEmpty());
    }

    @Test
    void savedPropertiesAreFound() {
        TenantProperties properties = new TenantProperties("acme");
        properties.setSeatLimit(10);
        when(repository.save(properties)).thenReturn(properties);
        when(repository.findById("acme")).thenReturn(Optional.of(properties));

        assertSame(properties, service.save(properties));
        assertEquals(10, service.find("acme")
                                .orElseThrow()
                                .getSeatLimit());
    }

    @Test
    void noSeatLimitIsSaved() {
        TenantProperties properties = new TenantProperties("acme");

        service.save(properties);

        verify(repository).save(properties);
    }

    @Test
    void aSeatLimitBelowOneIsRefused() {
        for (int refused : new int[] {0, -1}) {
            TenantProperties properties = new TenantProperties("acme");
            properties.setSeatLimit(refused);

            assertThrows(IllegalArgumentException.class, () -> service.save(properties));
        }
        verify(repository, never()).save(any());
    }

    @Test
    void deletingFindsNothingOrDeletesTheRow() {
        when(repository.findById("gone")).thenReturn(Optional.empty());
        assertFalse(service.delete("gone"));
        verify(repository, never()).delete(any());

        TenantProperties properties = new TenantProperties("acme");
        when(repository.findById("acme")).thenReturn(Optional.of(properties));
        assertTrue(service.delete("acme"));
        verify(repository).delete(properties);
    }
}
