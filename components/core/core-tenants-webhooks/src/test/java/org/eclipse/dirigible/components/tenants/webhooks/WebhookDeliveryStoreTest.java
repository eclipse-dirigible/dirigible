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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The delivery tables on a real H2: a delivery recorded on a write's connection rolls back with the
 * write, the claim is exclusive, a resend reuses the event id, and the retention drops only what is
 * finished.
 */
class WebhookDeliveryStoreTest {

    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");

    private String url;

    private WebhookDeliveryStore store;

    @BeforeEach
    void setUp() throws SQLException {
        url = "jdbc:h2:mem:webhooks-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> h2Connection());
        DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
        when(dataSourcesManager.getDefaultDataSource()).thenReturn(dataSource);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(true);
        store = new WebhookDeliveryStore(dataSourcesManager, tenantContext);
        assertFalse(store.tablesExist());
        store.prepare();
        assertTrue(store.tablesExist());
    }

    @Test
    void aDeliveryRecordedOnARolledBackWriteLeavesNothing() throws SQLException {
        try (Connection write = h2Connection()) {
            write.setAutoCommit(false);
            store.insert(write, "evt-1", "s1", "hr-Leave-Leave", "{}", NOW);
            write.rollback();
        }
        assertEquals(List.of(), store.findDue(NOW.plusSeconds(1), 10));

        try (Connection write = h2Connection()) {
            write.setAutoCommit(false);
            store.insert(write, "evt-2", "s1", "hr-Leave-Leave", "{}", NOW);
            write.commit();
        }
        assertEquals(List.of("evt-2"), store.findDue(NOW.plusSeconds(1), 10)
                                            .stream()
                                            .map(WebhookDeliveryStore.Delivery::eventId)
                                            .toList());
    }

    @Test
    void onlyOneClaimWinsAndTheLeaseHidesTheDelivery() throws SQLException {
        insert("evt-1", "s1");
        WebhookDeliveryStore.Delivery due = store.findDue(NOW, 10)
                                                 .get(0);
        assertTrue(store.claim(due, NOW.plus(Duration.ofMinutes(2))));
        assertFalse(store.claim(due, NOW.plus(Duration.ofMinutes(2))), "the attempt count moved on");
        assertEquals(List.of(), store.findDue(NOW.plusSeconds(60), 10));
        assertEquals(1, store.findDue(NOW.plus(Duration.ofMinutes(3)), 10)
                             .get(0)
                             .attempts());
    }

    @Test
    void aResendQueuesTheSameEventIdOnceMore() throws SQLException {
        insert("evt-1", "s1");
        WebhookDeliveryStore.Delivery first = store.findDue(NOW, 10)
                                                   .get(0);
        store.claim(first, NOW);
        store.recordAttempt(first.deliveryId(), new WebhookDeliveryStore.Attempt(NOW, 200, 12, null));
        store.finish(first.deliveryId(), WebhookDeliveryStore.State.DELIVERED, NOW);

        assertTrue(store.resend("s1", "evt-1"));
        assertFalse(store.resend("s1", "evt-unknown"));
        assertFalse(store.resend("s2", "evt-1"), "another subscription never received it");

        List<WebhookDeliveryStore.Delivery> log = store.log("s1", 10);
        assertEquals(2, log.size());
        assertEquals(List.of("evt-1", "evt-1"), log.stream()
                                                   .map(WebhookDeliveryStore.Delivery::eventId)
                                                   .toList());
        WebhookDeliveryStore.Delivery delivered = log.stream()
                                                     .filter(delivery -> delivery.state() == WebhookDeliveryStore.State.DELIVERED)
                                                     .findFirst()
                                                     .orElseThrow();
        WebhookDeliveryStore.Attempt attempt = store.attempts(delivered.deliveryId())
                                                    .get(0);
        assertEquals(200, attempt.statusCode());
        assertNull(attempt.error());
    }

    @Test
    void theRetentionDropsOnlyWhatFinishedBeforeTheCutoff() throws SQLException {
        insert("evt-old", "s1");
        insert("evt-pending", "s1");
        WebhookDeliveryStore.Delivery old = store.log("s1", 10)
                                                 .stream()
                                                 .filter(delivery -> delivery.eventId()
                                                                             .equals("evt-old"))
                                                 .findFirst()
                                                 .orElseThrow();
        store.recordAttempt(old.deliveryId(), new WebhookDeliveryStore.Attempt(NOW, null, 10000, "timeout"));
        store.finish(old.deliveryId(), WebhookDeliveryStore.State.GIVEN_UP, NOW.minus(Duration.ofDays(31)));

        assertEquals(1, store.purge(NOW.minus(Duration.ofDays(30))));
        assertEquals(List.of("evt-pending"), store.log("s1", 10)
                                                  .stream()
                                                  .map(WebhookDeliveryStore.Delivery::eventId)
                                                  .toList());
        assertEquals(List.of(), store.attempts(old.deliveryId()));

        store.deleteAll();
        assertEquals(List.of(), store.log("s1", 10));
    }

    private void insert(String eventId, String subscriptionId) throws SQLException {
        try (Connection connection = h2Connection()) {
            store.insert(connection, eventId, subscriptionId, "hr-Leave-Leave", "{\"id\":\"" + eventId + "\"}", NOW);
        }
    }

    private DirigibleConnection h2Connection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        return (DirigibleConnection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DirigibleConnection.class},
                (proxy, method, args) -> {
                    if ("getDatabaseSystem".equals(method.getName())) {
                        return DatabaseSystem.H2;
                    }
                    if ("isOfType".equals(method.getName())) {
                        return args[0] == DatabaseSystem.H2;
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
