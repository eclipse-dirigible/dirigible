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

import java.time.Instant;
import java.util.UUID;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/**
 * Tells the provisioning system that a subscription went {@code FAILING}, on the existing tenant
 * change channel - {@link DirigibleConfig#TENANT_USERS_CHANGE_QUEUE}, the queue the provisioning
 * system already consumes - so it can tell the tenant's Owner.
 *
 * <p>
 * The envelope is {@code tenant.webhook.failing} v1: {@code requestId, type, version, tenantId,
 * appId, subscriptionId, eventId, eventType, failedAt}. Without a configured channel the change is
 * logged only; the subscription's status reads {@code FAILING} either way. A broker refusal is
 * logged and not retried: the status is the durable record, and the provisioning system's periodic
 * read of the registry sees it.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class WebhookFailingNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookFailingNotifier.class);

    /** The envelope's type. */
    static final String TYPE = "tenant.webhook.failing";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final MessageProducer producer;

    /**
     * Instantiates the notifier.
     *
     * @param producer the producer
     */
    WebhookFailingNotifier(MessageProducer producer) {
        this.producer = producer;
    }

    /**
     * The envelope.
     *
     * @param requestId a unique id of the notice
     * @param type {@value #TYPE}
     * @param version 1
     * @param tenantId the tenant
     * @param appId the application, {@code DIRIGIBLE_APP_ID}
     * @param subscriptionId the subscription
     * @param eventId the event that was given up
     * @param eventType its type
     * @param failedAt when
     */
    record Notice(String requestId, String type, int version, String tenantId, String appId, String subscriptionId, String eventId,
            String eventType, String failedAt) {
    }

    /**
     * Publishes the notice. Synchronized, because the JMS session behind the producer is
     * single-threaded.
     *
     * @param tenantId the tenant
     * @param subscriptionId the subscription
     * @param eventId the event that was given up
     * @param eventType its type
     */
    synchronized void failing(String tenantId, String subscriptionId, String eventId, String eventType) {
        String queue = DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getStringValue();
        LOGGER.warn("Webhook subscription [{}] of tenant [{}] is FAILING: event [{}] of type [{}] was given up after the retry window",
                LogSanitizer.sanitize(subscriptionId), LogSanitizer.sanitize(tenantId), eventId, LogSanitizer.sanitize(eventType));
        if (queue == null || queue.isBlank()) {
            return;
        }
        Notice notice = new Notice(UUID.randomUUID()
                                       .toString(),
                TYPE, 1, tenantId, DirigibleConfig.APP_ID.getStringValue(), subscriptionId, eventId, eventType, Instant.now()
                                                                                                                       .toString());
        try {
            producer.sendMessageToQueue(queue, JSON.writeValueAsString(notice));
        } catch (Exception ex) {
            LOGGER.error("Could not tell [{}] that webhook subscription [{}] of tenant [{}] is FAILING", LogSanitizer.sanitize(queue),
                    LogSanitizer.sanitize(subscriptionId), LogSanitizer.sanitize(tenantId), ex);
        }
    }
}
