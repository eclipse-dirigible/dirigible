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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A live subscription as the fan-out and the relay read it: an immutable copy of the registry row,
 * cached per tenant. The secrets stay encrypted until a delivery is signed.
 *
 * @param tenantId the tenant
 * @param subscriptionId the subscription id
 * @param url the receiver
 * @param eventTypes the event types it receives
 * @param status its status, never {@link TenantWebhookStatus#REMOVED}
 * @param secret the current secret, encrypted
 * @param previousSecret the secret being rotated out, encrypted, or {@code null}
 */
record WebhookSubscriber(String tenantId, String subscriptionId, String url, Set<String> eventTypes, TenantWebhookStatus status,
        String secret, String previousSecret) {

    /**
     * Instantiates a subscriber.
     *
     * @param tenantId the tenant
     * @param subscriptionId the subscription id
     * @param url the receiver
     * @param eventTypes the event types it receives
     * @param status its status
     * @param secret the current secret, encrypted
     * @param previousSecret the secret being rotated out, encrypted, or {@code null}
     */
    WebhookSubscriber {
        eventTypes = Set.copyOf(eventTypes);
    }

    /**
     * A copy of a registry row.
     *
     * @param webhook the row
     * @return the subscriber
     */
    static WebhookSubscriber of(TenantWebhook webhook) {
        return new WebhookSubscriber(webhook.getTenantId(), webhook.getSubscriptionId(), webhook.getUrl(), webhook.eventTypes(),
                webhook.getStatus(), webhook.getSecret(), webhook.getPreviousSecret());
    }

    /**
     * Whether an event of a type is recorded for this subscription.
     *
     * @param type the event type
     * @return true when it names the type and its status receives events
     */
    boolean receives(String type) {
        return status.receivesEvents() && eventTypes.contains(type);
    }

    /**
     * The secrets a delivery is signed with, decrypted: the current one first.
     *
     * @return the secrets
     */
    List<String> signingSecrets() {
        List<String> secrets = new ArrayList<>(2);
        secrets.add(WebhookSecretCipher.decrypt(secret));
        if (previousSecret != null) {
            secrets.add(WebhookSecretCipher.decrypt(previousSecret));
        }
        return secrets;
    }

    /** Leaves the secrets out, encrypted as they are: a subscriber may end up in a log line. */
    @Override
    public String toString() {
        return "WebhookSubscriber[tenantId=" + tenantId + ", subscriptionId=" + subscriptionId + ", url=" + url + ", eventTypes="
                + eventTypes + ", status=" + status + "]";
    }
}
