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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Posts one signed webhook delivery. Only a 2xx within the timeout counts as delivered; a redirect
 * is not followed (it is an answer other than 2xx), and the URL is checked against the
 * {@link WebhookUrlGuard} before every attempt.
 *
 * <p>
 * Headers: {@code Webhook-Id} (the event id), {@code Webhook-Timestamp} (Unix seconds, signed with
 * the body) and {@code Webhook-Signature}, see {@link WebhookSigner}.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class WebhookSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookSender.class);

    static final String HEADER_ID = "Webhook-Id";
    static final String HEADER_TIMESTAMP = "Webhook-Timestamp";
    static final String HEADER_SIGNATURE = "Webhook-Signature";

    private static final String USER_AGENT = "Dirigible-Webhooks/1";

    /**
     * What an attempt came to.
     *
     * @param statusCode the answer's status code, {@code null} when there was no answer
     * @param error why it failed, {@code null} when it was delivered
     * @param durationMillis how long it took
     */
    record Outcome(Integer statusCode, String error, long durationMillis) {

        /**
         * Whether the receiver took the event.
         *
         * @return true for a 2xx
         */
        boolean delivered() {
            return error == null;
        }
    }

    private final HttpClient client;

    /** Instantiates the sender. */
    WebhookSender() {
        this.client = HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .connectTimeout(timeout())
                                .build();
    }

    /**
     * Posts a delivery.
     *
     * @param subscriber the subscription
     * @param eventId the event id
     * @param body the body
     * @return the outcome
     */
    Outcome send(WebhookSubscriber subscriber, String eventId, String body) {
        Instant start = Instant.now();
        Optional<String> refused = WebhookUrlGuard.problem(subscriber.url());
        if (refused.isPresent()) {
            return new Outcome(null, "The URL " + refused.get(), 0);
        }
        long timestamp = start.getEpochSecond();
        HttpRequest request = HttpRequest.newBuilder(URI.create(subscriber.url()))
                                         .timeout(timeout())
                                         .header("Content-Type", "application/json")
                                         .header("User-Agent", USER_AGENT)
                                         .header(HEADER_ID, eventId)
                                         .header(HEADER_TIMESTAMP, Long.toString(timestamp))
                                         .header(HEADER_SIGNATURE,
                                                 WebhookSigner.signatureHeader(subscriber.signingSecrets(), timestamp, body))
                                         .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                         .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long duration = Duration.between(start, Instant.now())
                                    .toMillis();
            int status = response.statusCode();
            return status >= 200 && status < 300 ? new Outcome(status, null, duration)
                    : new Outcome(status, "The receiver answered " + status, duration);
        } catch (IOException ex) {
            LOGGER.debug("Webhook delivery of event [{}] to subscription [{}] failed", eventId, subscriber.subscriptionId(), ex);
            return new Outcome(null, ex.getClass()
                                       .getSimpleName()
                    + (ex.getMessage() == null ? "" : ": " + ex.getMessage()),
                    Duration.between(start, Instant.now())
                            .toMillis());
        } catch (InterruptedException ex) {
            LOGGER.debug("Webhook delivery of event [{}] was interrupted", eventId, ex);
            Thread.currentThread()
                  .interrupt();
            return new Outcome(null, "Interrupted", Duration.between(start, Instant.now())
                                                            .toMillis());
        }
    }

    private static Duration timeout() {
        return Duration.ofSeconds(Math.max(1, DirigibleConfig.TENANT_WEBHOOKS_TIMEOUT_SECONDS.getIntValue()));
    }
}
