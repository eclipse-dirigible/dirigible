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

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.store.java.outbox.OutboxEntry;
import org.eclipse.dirigible.components.data.store.java.outbox.OutboxRecordingParticipant;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Records one webhook delivery per subscription and entity event, in the transaction that records
 * the event: a rolled-back write leaves no delivery behind, and a committed one cannot lose its
 * deliveries to a broker outage or a restart. The event's outbox id becomes the event id, so every
 * attempt and every resend of it carries the same {@code Webhook-Id}.
 *
 * <p>
 * A tenant without subscriptions costs a map lookup per write: the subscriptions are cached per
 * tenant and evicted by the registry.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class WebhookFanout implements OutboxRecordingParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookFanout.class);

    private final TenantWebhookRegistryService registry;

    private final WebhookEventCatalog catalog;

    private final WebhookDeliveryStore store;

    private final TenantContext tenantContext;

    private final Tenant defaultTenant;

    /**
     * Instantiates the fan-out.
     *
     * @param registry the subscriptions
     * @param catalog the declared event types
     * @param store the deliveries
     * @param tenantContext the tenant context
     * @param defaultTenant the tenant a write outside any tenant scope belongs to
     */
    WebhookFanout(TenantWebhookRegistryService registry, WebhookEventCatalog catalog, WebhookDeliveryStore store,
            TenantContext tenantContext, @DefaultTenant Tenant defaultTenant) {
        this.registry = registry;
        this.catalog = catalog;
        this.store = store;
        this.tenantContext = tenantContext;
        this.defaultTenant = defaultTenant;
    }

    @Override
    public void prepare() {
        boolean subscribed = registry.subscribers(currentTenantId())
                                     .stream()
                                     .anyMatch(subscriber -> subscriber.status()
                                                                       .receivesEvents());
        if (!subscribed) {
            return;
        }
        try {
            store.prepare();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to prepare the webhook delivery tables", ex);
        }
    }

    @Override
    public void recorded(Connection connection, List<OutboxEntry> entries) throws SQLException {
        String tenantId = currentTenantId();
        List<WebhookSubscriber> subscribers = registry.subscribers(tenantId);
        if (subscribers.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (OutboxEntry entry : entries) {
            List<WebhookSubscriber> receiving = subscribers.stream()
                                                           .filter(subscriber -> subscriber.receives(entry.topic()))
                                                           .toList();
            if (receiving.isEmpty()) {
                continue;
            }
            Optional<WebhookEventType> type = catalog.find(entry.topic());
            if (type.isEmpty()) {
                LOGGER.warn("Tenant [{}] subscribes to [{}], which no published application declares any more - nothing is recorded",
                        LogSanitizer.sanitize(tenantId), LogSanitizer.sanitize(entry.topic()));
                continue;
            }
            if (!store.tablesExist()) {
                // A subscription registered between this write's prepare and its commit: the tables are
                // created by the next write's prepare. Never DDL inside the business transaction.
                LOGGER.warn("Tenant [{}] has no webhook delivery tables yet - event [{}] of type [{}] is not delivered",
                        LogSanitizer.sanitize(tenantId), entry.id(), LogSanitizer.sanitize(entry.topic()));
                continue;
            }
            String body = WebhookEventBody.build(entry.id(), type.get(), tenantId, now, entry.payload());
            for (WebhookSubscriber subscriber : receiving) {
                store.insert(connection, entry.id(), subscriber.subscriptionId(), entry.topic(), body, now);
            }
        }
    }

    private String currentTenantId() {
        return tenantContext.isNotInitialized() ? defaultTenant.getId()
                : tenantContext.getCurrentTenant()
                               .getId();
    }
}
