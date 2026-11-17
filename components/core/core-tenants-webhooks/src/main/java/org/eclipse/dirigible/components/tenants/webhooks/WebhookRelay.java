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

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Delivers the current tenant's due webhook deliveries - called by {@link WebhookRelayJob} inside
 * each running tenant's scope, so a suspended tenant's deliveries wait, and resume with it.
 *
 * <p>
 * Per delivery: claim it (so a second relay cannot take it), give it up when its subscription was
 * removed or disabled, otherwise post it and log the attempt. A 2xx ends it {@code DELIVERED} (and
 * makes a {@code FAILING} subscription {@code ACTIVE}); anything else schedules the next attempt by
 * the {@link WebhookRetrySchedule}, until a failure comes after the retry window: then the delivery
 * is given up, the subscription is marked {@code FAILING} and the provisioning system is told. No
 * ordering is promised between deliveries - the receiver re-reads the record.
 *
 * <p>
 * Once an hour per tenant the relay also drops the deliveries finished longer ago than
 * {@link DirigibleConfig#TENANT_WEBHOOKS_LOG_RETENTION_DAYS}.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class WebhookRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookRelay.class);

    /** Deliveries attempted per tick and tenant. Bounds one tick, never the total. */
    static final int BATCH_SIZE = 20;

    /** How often the retention runs per tenant. */
    private static final Duration PURGE_INTERVAL = Duration.ofHours(1);

    private final WebhookDeliveryStore store;

    private final TenantWebhookRegistryService registry;

    private final WebhookSender sender;

    private final WebhookFailingNotifier notifier;

    /** When the retention last ran, per tenant. */
    private final Map<String, Instant> purgedAt = new ConcurrentHashMap<>();

    /**
     * Instantiates the relay.
     *
     * @param store the deliveries
     * @param registry the subscriptions
     * @param sender the sender
     * @param notifier the failing notifier
     */
    WebhookRelay(WebhookDeliveryStore store, TenantWebhookRegistryService registry, WebhookSender sender, WebhookFailingNotifier notifier) {
        this.store = store;
        this.registry = registry;
        this.sender = sender;
        this.notifier = notifier;
    }

    /**
     * Delivers what is due for a tenant. Must run inside that tenant's scope.
     *
     * @param tenantId the tenant
     * @throws SQLException if the delivery tables cannot be read or written
     */
    void relay(String tenantId) throws SQLException {
        if (!store.tablesExist()) {
            return;
        }
        Instant now = Instant.now();
        WebhookRetrySchedule schedule = WebhookRetrySchedule.configured();
        // Past the attempt's own timeout: should this node die mid-attempt, the delivery is due again.
        Instant lease = now.plusSeconds(2L * Math.max(1, DirigibleConfig.TENANT_WEBHOOKS_TIMEOUT_SECONDS.getIntValue()) + 60);
        List<WebhookDeliveryStore.Delivery> due = store.findDue(now, BATCH_SIZE);
        for (WebhookDeliveryStore.Delivery delivery : due) {
            if (store.claim(delivery, lease)) {
                attempt(tenantId, delivery, schedule);
            }
        }
        purgeIfDue(tenantId, now);
    }

    private void attempt(String tenantId, WebhookDeliveryStore.Delivery delivery, WebhookRetrySchedule schedule) throws SQLException {
        Optional<WebhookSubscriber> subscriber = registry.subscriber(tenantId, delivery.subscriptionId());
        if (subscriber.isEmpty() || subscriber.get()
                                              .status() == TenantWebhookStatus.DISABLED) {
            String reason = subscriber.isEmpty() ? "removed" : "disabled";
            store.recordAttempt(delivery.deliveryId(),
                    new WebhookDeliveryStore.Attempt(Instant.now(), null, 0, "Not sent: the subscription was " + reason));
            store.finish(delivery.deliveryId(), WebhookDeliveryStore.State.GIVEN_UP, Instant.now());
            return;
        }
        WebhookSender.Outcome outcome;
        try {
            outcome = sender.send(subscriber.get(), delivery.eventId(), delivery.body());
        } catch (IllegalStateException ex) {
            // A secret that cannot be decrypted (the encryption key was changed): an attempt that failed.
            LOGGER.error("Could not sign webhook event [{}] for subscription [{}] of tenant [{}]", delivery.eventId(),
                    LogSanitizer.sanitize(delivery.subscriptionId()), LogSanitizer.sanitize(tenantId), ex);
            outcome = new WebhookSender.Outcome(null, ex.getMessage(), 0);
        }
        Instant now = Instant.now();
        store.recordAttempt(delivery.deliveryId(),
                new WebhookDeliveryStore.Attempt(now, outcome.statusCode(), outcome.durationMillis(), outcome.error()));
        if (outcome.delivered()) {
            store.finish(delivery.deliveryId(), WebhookDeliveryStore.State.DELIVERED, now);
            if (subscriber.get()
                          .status() == TenantWebhookStatus.FAILING) {
                registry.markRecovered(tenantId, delivery.subscriptionId());
            }
            return;
        }
        int failedAttempts = delivery.attempts() + 1;
        if (schedule.givesUp(delivery.createdAt(), now)) {
            store.finish(delivery.deliveryId(), WebhookDeliveryStore.State.GIVEN_UP, now);
            if (registry.markFailing(tenantId, delivery.subscriptionId())) {
                notifier.failing(tenantId, delivery.subscriptionId(), delivery.eventId(), delivery.eventType());
            }
            return;
        }
        LOGGER.info("Webhook event [{}] to subscription [{}] of tenant [{}] failed attempt [{}]: {}", delivery.eventId(),
                LogSanitizer.sanitize(delivery.subscriptionId()), LogSanitizer.sanitize(tenantId), failedAttempts,
                LogSanitizer.sanitize(outcome.error()));
        store.reschedule(delivery.deliveryId(), schedule.nextAttemptAt(failedAttempts, now));
    }

    private void purgeIfDue(String tenantId, Instant now) throws SQLException {
        Instant last = purgedAt.get(tenantId);
        if (last != null && now.isBefore(last.plus(PURGE_INTERVAL))) {
            return;
        }
        purgedAt.put(tenantId, now);
        int days = Math.max(1, DirigibleConfig.TENANT_WEBHOOKS_LOG_RETENTION_DAYS.getIntValue());
        int purged = store.purge(now.minus(Duration.ofDays(days)));
        if (purged > 0) {
            LOGGER.info("Dropped [{}] webhook deliveries of tenant [{}] finished more than [{}] days ago", purged,
                    LogSanitizer.sanitize(tenantId), days);
        }
    }

    /**
     * Forgets a tenant's bookkeeping - deprovisioning.
     *
     * @param tenantId the tenant
     */
    void forget(String tenantId) {
        purgedAt.remove(tenantId);
    }
}
