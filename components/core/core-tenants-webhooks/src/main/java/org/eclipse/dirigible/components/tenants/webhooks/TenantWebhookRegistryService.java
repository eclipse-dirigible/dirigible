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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * The webhook registry: written by the external provisioning system, one tenant's complete list at
 * a time, and read - through a per-tenant cache - by the fan-out on every entity event and by the
 * relay.
 *
 * <p>
 * The rules of an apply, all in one transaction:
 * <ol>
 * <li>a registry whose revision is not higher than the stored one changes nothing - a late push
 * cannot undo a newer one;</li>
 * <li>every subscription it names is inserted or updated: name, URL, event types, status, and its
 * secrets when it carries them;</li>
 * <li>a stored subscription it does not name is removed (kept as a hidden tombstone, so the
 * revision survives the last one's removal); its pending deliveries are given up by the relay;</li>
 * <li>{@code FAILING} is the application's observation, not the registry's: a registry sending
 * {@code ACTIVE} leaves a failing subscription failing, and its next successful delivery makes it
 * active again. {@code DISABLED} always wins.</li>
 * </ol>
 *
 * <p>
 * The cache is evicted by every apply and every status change here; an application runs as one
 * instance, as for the rest of the tenant provisioning API.
 */
@Service
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantWebhookRegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantWebhookRegistryService.class);

    /** A tenant id - the tenant registry's own rule. */
    private static final Pattern TENANT_ID = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$");

    private final TenantWebhookRepository webhooks;

    private final TenantService tenants;

    private final WebhookEventCatalog catalog;

    /** One new transaction per write, whatever the caller runs in. */
    private final TransactionTemplate writes;

    private final TransactionTemplate reads;

    /** The live subscriptions per tenant. */
    private final Map<String, List<WebhookSubscriber>> cache = new ConcurrentHashMap<>();

    /**
     * Instantiates the service.
     *
     * @param webhooks the registry
     * @param tenants the tenants
     * @param catalog the declared event types
     * @param transactionManager the SystemDB transaction manager
     */
    TenantWebhookRegistryService(TenantWebhookRepository webhooks, TenantService tenants, WebhookEventCatalog catalog,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        this.webhooks = webhooks;
        this.tenants = tenants;
        this.catalog = catalog;
        this.writes = new TransactionTemplate(transactionManager);
        this.writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.reads = new TransactionTemplate(transactionManager);
        this.reads.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.reads.setReadOnly(true);
    }

    /**
     * The live subscriptions of a tenant - {@code ACTIVE}, {@code FAILING} and {@code DISABLED}.
     *
     * @param tenantId the tenant id
     * @return the subscriptions, never {@code null}
     */
    List<WebhookSubscriber> subscribers(String tenantId) {
        return cache.computeIfAbsent(tenantId, this::load);
    }

    /**
     * One live subscription of a tenant.
     *
     * @param tenantId the tenant id
     * @param subscriptionId the subscription id
     * @return the subscription, empty when it is unknown or removed
     */
    Optional<WebhookSubscriber> subscriber(String tenantId, String subscriptionId) {
        return subscribers(tenantId).stream()
                                    .filter(subscriber -> subscriber.subscriptionId()
                                                                    .equals(subscriptionId))
                                    .findFirst();
    }

    /**
     * Applies a tenant's complete registry.
     *
     * @param tenantId the tenant id
     * @param body the registry
     * @return what the apply did
     */
    TenantWebhookSync.Result apply(String tenantId, TenantWebhookSync body) {
        requireTenant(tenantId);
        Set<String> live = new HashSet<>();
        reads.executeWithoutResult(status -> webhooks.findByTenantIdOrderBySubscriptionId(tenantId)
                                                     .stream()
                                                     .filter(webhook -> webhook.getStatus() != TenantWebhookStatus.REMOVED)
                                                     .forEach(webhook -> live.add(webhook.getSubscriptionId())));
        List<String> problems = TenantWebhookSyncValidation.problems(body, type -> catalog.find(type)
                                                                                          .isPresent(),
                live::contains, DirigibleConfig.TENANT_WEBHOOKS_MAX_SUBSCRIPTIONS.getIntValue(), WebhookUrlGuard::problem);
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", problems));
        }
        boolean carriesSecrets = body.subscriptions() != null && body.subscriptions()
                                                                     .stream()
                                                                     .anyMatch(subscription -> subscription.secret() != null);
        if (carriesSecrets && !WebhookSecretCipher.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook signing secrets cannot be stored: "
                    + DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.getKey() + " is not set to 32 bytes of Base64");
        }
        TenantWebhookSync.Result result;
        try {
            result = writes.execute(status -> applyLocked(tenantId, body));
        } finally {
            cache.remove(tenantId);
        }
        LOGGER.info("Webhook subscriptions of tenant [{}]: revision [{}] applied [{}], subscriptions [{}], stored revision [{}]",
                LogSanitizer.sanitize(tenantId), body.revision(), result.applied(), result.subscriptions(), result.revision());
        return result;
    }

    /**
     * The registry of a tenant, without the secrets and the removed subscriptions.
     *
     * @param tenantId the tenant id
     * @return the registry
     */
    TenantWebhookSync list(String tenantId) {
        requireTenant(tenantId);
        return reads.execute(status -> {
            List<TenantWebhook> stored = webhooks.findByTenantIdOrderBySubscriptionId(tenantId);
            return new TenantWebhookSync(maxRevision(stored), stored.stream()
                                                                    .filter(webhook -> webhook.getStatus() != TenantWebhookStatus.REMOVED)
                                                                    .map(TenantWebhookSync.Subscription::of)
                                                                    .toList());
        });
    }

    /**
     * Marks a subscription {@code FAILING}: an event was given up.
     *
     * @param tenantId the tenant id
     * @param subscriptionId the subscription id
     * @return true when it was {@code ACTIVE} until now - the moment to tell the provisioning system
     */
    boolean markFailing(String tenantId, String subscriptionId) {
        return changeStatus(tenantId, subscriptionId, TenantWebhookStatus.ACTIVE, TenantWebhookStatus.FAILING);
    }

    /**
     * Marks a {@code FAILING} subscription {@code ACTIVE} again: a delivery succeeded.
     *
     * @param tenantId the tenant id
     * @param subscriptionId the subscription id
     * @return true when it was {@code FAILING} until now
     */
    boolean markRecovered(String tenantId, String subscriptionId) {
        return changeStatus(tenantId, subscriptionId, TenantWebhookStatus.FAILING, TenantWebhookStatus.ACTIVE);
    }

    /**
     * Deletes the registry of a tenant - deprovisioning.
     *
     * @param tenantId the tenant id
     * @return how many rows were deleted
     */
    long deleteTenant(String tenantId) {
        try {
            return webhooks.deleteByTenantId(tenantId);
        } finally {
            cache.remove(tenantId);
        }
    }

    /**
     * Refuses an invalid, unknown or deprovisioning tenant.
     *
     * @param tenantId the tenant id
     */
    void requireTenant(String tenantId) {
        if (tenantId == null || !TENANT_ID.matcher(tenantId)
                                          .matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tenant id [" + tenantId + "] is not valid - letters, digits and hyphens, not starting or ending with a hyphen");
        }
        Optional<Tenant> tenant = tenants.findById(tenantId);
        if (tenant.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no tenant with id [" + tenantId + "]");
        }
        if (TenantStatus.DEPROVISIONING == tenant.get()
                                                 .getStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant [" + tenantId + "] is being deprovisioned");
        }
    }

    private boolean changeStatus(String tenantId, String subscriptionId, TenantWebhookStatus from, TenantWebhookStatus to) {
        Boolean changed;
        try {
            changed = writes.execute(status -> {
                Optional<TenantWebhook> webhook = webhooks.findByTenantIdAndSubscriptionId(tenantId, subscriptionId);
                if (webhook.isEmpty() || webhook.get()
                                                .getStatus() != from) {
                    return false;
                }
                webhook.get()
                       .setStatus(to);
                webhook.get()
                       .setFailingAt(to == TenantWebhookStatus.FAILING ? Instant.now() : null);
                webhooks.save(webhook.get());
                return true;
            });
        } finally {
            cache.remove(tenantId);
        }
        if (Boolean.TRUE.equals(changed)) {
            LOGGER.info("Webhook subscription [{}] of tenant [{}] is now [{}]", LogSanitizer.sanitize(subscriptionId),
                    LogSanitizer.sanitize(tenantId), to);
            return true;
        }
        return false;
    }

    private List<WebhookSubscriber> load(String tenantId) {
        List<WebhookSubscriber> loaded = reads.execute(status -> webhooks.findByTenantIdOrderBySubscriptionId(tenantId)
                                                                         .stream()
                                                                         .filter(webhook -> webhook.getStatus() != TenantWebhookStatus.REMOVED)
                                                                         .map(WebhookSubscriber::of)
                                                                         .toList());
        return loaded == null ? List.of() : loaded;
    }

    private TenantWebhookSync.Result applyLocked(String tenantId, TenantWebhookSync body) {
        List<TenantWebhook> stored = webhooks.findForUpdateByTenantIdOrderBySubscriptionId(tenantId);
        Long storedRevision = maxRevision(stored);
        if (storedRevision != null && body.revision() <= storedRevision) {
            return result(false, storedRevision, stored);
        }
        long revision = body.revision();
        Map<String, TenantWebhook> bySubscriptionId = new LinkedHashMap<>();
        stored.forEach(webhook -> bySubscriptionId.put(webhook.getSubscriptionId(), webhook));
        Set<String> named = new HashSet<>();
        List<TenantWebhookSync.Subscription> sent = body.subscriptions() == null ? List.of() : body.subscriptions();
        for (TenantWebhookSync.Subscription subscription : sent) {
            named.add(subscription.subscriptionId());
            TenantWebhook webhook = bySubscriptionId.computeIfAbsent(subscription.subscriptionId(), id -> new TenantWebhook(tenantId, id));
            webhook.setName(subscription.name()
                                        .trim());
            webhook.setUrl(subscription.url()
                                       .trim());
            webhook.replaceEventTypes(subscription.events());
            webhook.setStatus(statusAfter(webhook.getStatus(), subscription.status()));
            if (webhook.getStatus() != TenantWebhookStatus.FAILING) {
                webhook.setFailingAt(null);
            }
            if (subscription.secret() != null) {
                webhook.replaceSecrets(WebhookSecretCipher.encrypt(subscription.secret()),
                        subscription.previousSecret() == null ? null : WebhookSecretCipher.encrypt(subscription.previousSecret()));
            }
            webhook.setCreatedBy(subscription.createdBy());
            webhook.setCreatedAt(TenantWebhookSyncValidation.instant(subscription.createdAt()));
            webhook.setRevision(revision);
        }
        for (TenantWebhook webhook : bySubscriptionId.values()) {
            if (!named.contains(webhook.getSubscriptionId())) {
                if (webhook.getStatus() != TenantWebhookStatus.REMOVED) {
                    LOGGER.info("Webhook subscription [{}] of tenant [{}] is not in revision [{}] of the registry. Removing it.",
                            LogSanitizer.sanitize(webhook.getSubscriptionId()), LogSanitizer.sanitize(tenantId), revision);
                }
                webhook.setStatus(TenantWebhookStatus.REMOVED);
                webhook.setFailingAt(null);
                webhook.setRevision(revision);
            }
        }
        List<TenantWebhook> all = new ArrayList<>(bySubscriptionId.values());
        webhooks.saveAll(all);
        return result(true, revision, all);
    }

    /**
     * The status a registry leaves a subscription in.
     *
     * @param stored the stored status, {@code null} for a new subscription
     * @param sent the status sent, {@code null} meaning {@code ACTIVE}
     * @return the new status
     */
    static TenantWebhookStatus statusAfter(TenantWebhookStatus stored, String sent) {
        if (TenantWebhookStatus.DISABLED.name()
                                        .equals(sent)) {
            return TenantWebhookStatus.DISABLED;
        }
        return stored == TenantWebhookStatus.FAILING ? TenantWebhookStatus.FAILING : TenantWebhookStatus.ACTIVE;
    }

    private static TenantWebhookSync.Result result(boolean applied, Long revision, List<TenantWebhook> all) {
        int live = (int) all.stream()
                            .filter(webhook -> webhook.getStatus() != TenantWebhookStatus.REMOVED)
                            .count();
        return new TenantWebhookSync.Result(applied, revision, live);
    }

    private static Long maxRevision(List<TenantWebhook> stored) {
        return stored.isEmpty() ? null
                : stored.stream()
                        .mapToLong(TenantWebhook::getRevision)
                        .max()
                        .getAsLong();
    }
}
