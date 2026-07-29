/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * The warm-up contract of {@link TenantConfigurationService#resolveInjectableForCurrentTenant()}: a
 * fresh instance's first requests may arrive before the first synchronization pass registers the
 * default datasource, so a failing load is EXPECTED then - one WARN line per tenant, no stack, no
 * ERROR. Once the tenant's configuration has loaded successfully, a failure is abnormal and logs
 * the full ERROR again. Fail-soft either way: the resolver never throws and returns an empty map.
 */
class TenantConfigurationServiceWarmupTest {

    private TenantConfigurationStore store;
    private TenantConfigurationService service;
    private TenantConfigurationCache cache;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        store = mock(TenantConfigurationStore.class);
        TenantConfigurationKeyPolicy keyPolicy = mock(TenantConfigurationKeyPolicy.class);
        when(keyPolicy.filterInjectable(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn("default-tenant");
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(false);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        cache = new TenantConfigurationCache();
        service = new TenantConfigurationService(store, cache, keyPolicy, tenantContext);

        appender = new ListAppender<>();
        appender.start();
        logger = (Logger) LoggerFactory.getLogger(TenantConfigurationService.class);
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void warmupFailuresWarnOnceThenFailuresAfterASuccessErrorAgain() throws Exception {
        when(store.readAll()).thenThrow(new IllegalArgumentException("missing artefact with name: [DefaultDB]"))
                             .thenThrow(new IllegalArgumentException("missing artefact with name: [DefaultDB]"))
                             .thenReturn(Map.of("key", "value"))
                             .thenThrow(new IllegalStateException("store broke after warm-up"));

        // Warm-up: two failing requests - fail-soft, exactly ONE warn, no error, no stack noise.
        assertEquals(Map.of(), service.resolveInjectableForCurrentTenant());
        assertEquals(Map.of(), service.resolveInjectableForCurrentTenant());
        assertEquals(1, count(Level.WARN), "warm-up must warn exactly once per tenant");
        assertEquals(0, count(Level.ERROR), "warm-up failures must not log errors");

        // The store becomes reachable - the load succeeds.
        assertEquals(Map.of("key", "value"), service.resolveInjectableForCurrentTenant());

        // A failure AFTER a successful load is abnormal - the full error returns.
        cache.invalidate("default-tenant");
        assertEquals(Map.of(), service.resolveInjectableForCurrentTenant());
        assertEquals(1, count(Level.ERROR), "a post-success failure must log the error");
        assertEquals(1, count(Level.WARN), "the warm-up warn must not repeat");
    }

    private long count(Level level) {
        return appender.list.stream()
                            .filter(event -> event.getLevel() == level)
                            .count();
    }
}
