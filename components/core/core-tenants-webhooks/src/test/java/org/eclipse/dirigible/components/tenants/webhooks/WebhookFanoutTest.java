/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.store.java.outbox.OutboxEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The fan-out: one delivery per receiving subscription of the writing tenant, the outbox id as the
 * event id, nothing for a tenant, a type or a status that does not receive.
 */
class WebhookFanoutTest {

    private static final String TRANSITIONED = "hr-Leave-Leave-transitioned";
    private static final String UPDATED = "hr-Leave-Leave-updated";

    private final TenantWebhookRegistryService registry = mock(TenantWebhookRegistryService.class);
    private final WebhookEventCatalog catalog = mock(WebhookEventCatalog.class);
    private final WebhookDeliveryStore store = mock(WebhookDeliveryStore.class);
    private final TenantContext tenantContext = mock(TenantContext.class);
    private final Connection connection = mock(Connection.class);

    private WebhookFanout fanout;

    @BeforeEach
    void setUp() throws SQLException {
        Tenant acme = mock(Tenant.class);
        when(acme.getId()).thenReturn("acme");
        when(tenantContext.isNotInitialized()).thenReturn(false);
        when(tenantContext.getCurrentTenant()).thenReturn(acme);
        Tenant defaultTenant = mock(Tenant.class);
        when(defaultTenant.getId()).thenReturn("default-tenant");
        when(store.tablesExist()).thenReturn(true);
        when(catalog.find(TRANSITIONED)).thenReturn(
                Optional.of(new WebhookEventType(TRANSITIONED, "transitioned", "hr", "Leave", "Id", null, "Status")));
        fanout = new WebhookFanout(registry, catalog, store, tenantContext, defaultTenant);
    }

    private static WebhookSubscriber subscriber(String id, TenantWebhookStatus status, String... types) {
        return new WebhookSubscriber("acme", id, "https://hooks.example.com/" + id, Set.of(types), status, "v1:x", null);
    }

    @Test
    void recordsOneDeliveryPerReceivingSubscriptionWithTheOutboxIdAsTheEventId() throws SQLException {
        when(registry.subscribers("acme")).thenReturn(List.of(subscriber("s1", TenantWebhookStatus.ACTIVE, TRANSITIONED),
                subscriber("s2", TenantWebhookStatus.FAILING, TRANSITIONED), subscriber("s3", TenantWebhookStatus.DISABLED, TRANSITIONED),
                subscriber("s4", TenantWebhookStatus.ACTIVE, UPDATED)));

        fanout.recorded(connection, List.of(new OutboxEntry("evt-1", TRANSITIONED, "{\"Id\":7,\"Status\":3,\"Note\":\"private\"}"),
                new OutboxEntry("evt-2", "hr-Leave-Leave-rekeyed", "{\"Id\":7}")));

        String body = "\"data\":{\"id\":7,\"status\":3}";
        verify(store).insert(eq(connection), eq("evt-1"), eq("s1"), eq(TRANSITIONED), contains(body), any());
        verify(store).insert(eq(connection), eq("evt-1"), eq("s2"), eq(TRANSITIONED), contains(body), any());
        verify(store, never()).insert(any(), anyString(), eq("s3"), anyString(), anyString(), any());
        verify(store, never()).insert(any(), anyString(), eq("s4"), anyString(), anyString(), any());
        verify(store, never()).insert(any(), eq("evt-2"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void aTenantWithoutSubscriptionsCostsALookupOnly() throws SQLException {
        when(registry.subscribers("acme")).thenReturn(List.of());

        fanout.prepare();
        fanout.recorded(connection, List.of(new OutboxEntry("evt-1", TRANSITIONED, "{\"Id\":7}")));

        verifyNoInteractions(store, catalog);
    }

    @Test
    void preparesTheTablesOnlyForATenantThatReceives() throws SQLException {
        when(registry.subscribers("acme")).thenReturn(List.of(subscriber("s3", TenantWebhookStatus.DISABLED, TRANSITIONED)));
        fanout.prepare();
        verify(store, never()).prepare();

        when(registry.subscribers("acme")).thenReturn(List.of(subscriber("s1", TenantWebhookStatus.ACTIVE, TRANSITIONED)));
        fanout.prepare();
        verify(store).prepare();
    }
}
