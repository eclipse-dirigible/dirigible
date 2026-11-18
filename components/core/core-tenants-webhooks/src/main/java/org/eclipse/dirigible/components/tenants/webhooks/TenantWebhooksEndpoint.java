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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningRoles;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.annotation.security.RolesAllowed;

/**
 * The webhook subscriptions of the tenant provisioning API: the external provisioning system writes
 * a tenant's complete webhook registry here, asks which event types a subscription may name, reads
 * a subscription's delivery log and resends an event.
 *
 * <p>
 * It exists exactly when the tenant provisioning API does, behind the same roles, and its URLs are
 * already claimed by that API's security configurator.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING)
@RolesAllowed({TenantProvisioningRoles.TENANT_PROVISIONER, TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantWebhooksEndpoint extends BaseEndpoint {

    /** The most deliveries one log read answers. */
    private static final int MAX_LOG = 200;

    private final TenantWebhookRegistryService registry;

    private final WebhookEventCatalog catalog;

    private final WebhookDeliveryStore store;

    private final TenantContext tenantContext;

    /**
     * Instantiates the endpoint.
     *
     * @param registry the registry
     * @param catalog the declared event types
     * @param store the deliveries
     * @param tenantContext the tenant context
     */
    TenantWebhooksEndpoint(TenantWebhookRegistryService registry, WebhookEventCatalog catalog, WebhookDeliveryStore store,
            TenantContext tenantContext) {
        this.registry = registry;
        this.catalog = catalog;
        this.store = store;
        this.tenantContext = tenantContext;
    }

    /**
     * Applies a tenant's complete webhook registry. Answers 200 whether or not it was newer than the
     * stored one: a stale registry changes nothing, which is normal, never an error.
     *
     * @param tenantId the tenant id
     * @param body the registry
     * @return what the apply did
     */
    @PutMapping(path = "tenants/{tenantId}/webhooks", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantWebhookSync.Result> apply(@PathVariable("tenantId") String tenantId, @RequestBody TenantWebhookSync body) {
        return ResponseEntity.ok(registry.apply(tenantId, body));
    }

    /**
     * The webhook registry of a tenant, without secrets.
     *
     * @param tenantId the tenant id
     * @return the registry
     */
    @GetMapping(path = "tenants/{tenantId}/webhooks", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantWebhookSync> list(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(registry.list(tenantId));
    }

    /**
     * The event types a subscription may name - the ones the published applications declare.
     *
     * @return the event types
     */
    @GetMapping(path = "webhook-events", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EventTypes> eventTypes() {
        return ResponseEntity.ok(new EventTypes(List.copyOf(catalog.types())));
    }

    /**
     * The delivery log of a subscription: its newest deliveries, each with its attempts.
     *
     * @param tenantId the tenant id
     * @param subscriptionId the subscription id
     * @param limit how many deliveries, at most {@value #MAX_LOG}
     * @return the log
     */
    @GetMapping(path = "tenants/{tenantId}/webhooks/{subscriptionId}/deliveries", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DeliveryView>> deliveries(@PathVariable("tenantId") String tenantId,
            @PathVariable("subscriptionId") String subscriptionId, @RequestParam(name = "limit", defaultValue = "50") int limit) {
        requireSubscription(tenantId, subscriptionId);
        int bounded = Math.max(1, Math.min(limit, MAX_LOG));
        List<DeliveryView> log = inTenant(tenantId, () -> {
            if (!store.tablesExist()) {
                return List.<DeliveryView>of();
            }
            List<DeliveryView> views = new ArrayList<>();
            for (WebhookDeliveryStore.Delivery delivery : store.log(subscriptionId, bounded)) {
                views.add(DeliveryView.of(delivery, store.attempts(delivery.deliveryId())));
            }
            return views;
        });
        return ResponseEntity.ok(log);
    }

    /**
     * Queues an event once more for a subscription, with the same event id.
     *
     * @param tenantId the tenant id
     * @param subscriptionId the subscription id
     * @param eventId the event id
     * @return 202, or 404 when the subscription never received the event
     */
    @PostMapping(path = "tenants/{tenantId}/webhooks/{subscriptionId}/deliveries/{eventId}/resend",
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Resent> resend(@PathVariable("tenantId") String tenantId, @PathVariable("subscriptionId") String subscriptionId,
            @PathVariable("eventId") String eventId) {
        WebhookSubscriber subscriber = requireSubscription(tenantId, subscriptionId);
        if (!subscriber.status()
                       .receivesEvents()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription [" + subscriptionId + "] is disabled");
        }
        boolean found = inTenant(tenantId, () -> store.tablesExist() && store.resend(subscriptionId, eventId));
        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Subscription [" + subscriptionId + "] has no delivery of event [" + eventId + "]");
        }
        return ResponseEntity.accepted()
                             .body(new Resent(eventId));
    }

    private WebhookSubscriber requireSubscription(String tenantId, String subscriptionId) {
        registry.requireTenant(tenantId);
        return registry.subscriber(tenantId, subscriptionId)
                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                               "Tenant [" + tenantId + "] has no webhook subscription [" + subscriptionId + "]"));
    }

    private <T> T inTenant(String tenantId, Read<T> read) {
        try {
            return tenantContext.execute(tenantId, read::read);
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not read the webhook deliveries of tenant [" + tenantId + "]", ex);
        }
    }

    /**
     * A read of the tenant's delivery tables.
     *
     * @param <T> the result
     */
    @FunctionalInterface
    private interface Read<T> {

        /**
         * Reads.
         *
         * @return the result
         * @throws SQLException if the read fails
         */
        T read() throws SQLException;
    }

    /**
     * The event types.
     *
     * @param events the event types, sorted
     */
    record EventTypes(List<WebhookEventType> events) {
    }

    /**
     * A queued resend.
     *
     * @param eventId the event id it carries
     */
    record Resent(String eventId) {
    }

    /**
     * One delivery of the log.
     *
     * @param eventId the event id
     * @param type the event type
     * @param state {@code PENDING}, {@code DELIVERED} or {@code GIVEN_UP}
     * @param createdAt when it was recorded
     * @param finishedAt when it was delivered or given up
     * @param attempts its attempts, oldest first
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record DeliveryView(String eventId, String type, String state, String createdAt, String finishedAt, List<AttemptView> attempts) {

        static DeliveryView of(WebhookDeliveryStore.Delivery delivery, List<WebhookDeliveryStore.Attempt> attempts) {
            return new DeliveryView(delivery.eventId(), delivery.eventType(), delivery.state()
                                                                                      .name(),
                    text(delivery.createdAt()), text(delivery.finishedAt()), attempts.stream()
                                                                                     .map(AttemptView::of)
                                                                                     .toList());
        }
    }

    /**
     * One attempt of the log.
     *
     * @param at when
     * @param statusCode the answer's status code, absent when there was no answer
     * @param durationMillis how long it took
     * @param error why it failed, absent when it was delivered
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AttemptView(String at, Integer statusCode, long durationMillis, String error) {

        static AttemptView of(WebhookDeliveryStore.Attempt attempt) {
            return new AttemptView(text(attempt.attemptedAt()), attempt.statusCode(), attempt.durationMillis(), attempt.error());
        }
    }

    private static String text(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
