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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The body of {@code PUT tenants/{tenantId}/webhooks}: the tenant's complete webhook registry as
 * the external provisioning system holds it, and the revision of that registry, one sequence per
 * tenant. {@code GET} answers the same shape, without the secrets.
 *
 * @param revision the revision of the registry
 * @param subscriptions the subscriptions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TenantWebhookSync(Long revision, List<Subscription> subscriptions) {

    /**
     * One subscription.
     *
     * @param subscriptionId the provisioning system's id of the subscription
     * @param name a human-readable name
     * @param url the HTTPS URL the events are posted to
     * @param events the event types it receives, see {@code GET webhook-events}
     * @param status {@code ACTIVE} or {@code DISABLED} when sent; {@code GET} answers {@code FAILING}
     *        too
     * @param secret the current signing secret - required for a new subscription; absent keeps the
     *        stored secrets. Never answered.
     * @param previousSecret the secret being rotated out, signed with as well until a registry leaves
     *        it out; only read when {@code secret} is sent. Never answered.
     * @param createdBy who asked for the subscription
     * @param createdAt when, ISO-8601
     * @param failingAt when the subscription was marked {@code FAILING} - answered only
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Subscription(String subscriptionId, String name, String url, List<String> events, String status, String secret,
            String previousSecret, String createdBy, String createdAt, String failingAt) {

        /**
         * The view of a stored subscription, without its secrets.
         *
         * @param webhook the subscription
         * @return the view
         */
        static Subscription of(TenantWebhook webhook) {
            return new Subscription(
                    webhook.getSubscriptionId(), webhook.getName(), webhook.getUrl(), List.copyOf(webhook.eventTypes()), webhook.getStatus()
                                                                                                                                .name(),
                    null, null, webhook.getCreatedBy(), text(webhook.getCreatedAt()), text(webhook.getFailingAt()));
        }

        private static String text(Instant instant) {
            return instant == null ? null : instant.toString();
        }
    }

    /**
     * What an apply did.
     *
     * @param applied false when the registry was not newer than the stored one, and nothing changed
     * @param revision the revision stored now
     * @param subscriptions how many subscriptions the tenant holds now
     */
    record Result(boolean applied, Long revision, int subscriptions) {
    }
}
