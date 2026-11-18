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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One webhook subscription of a tenant: where its events go, which event types it receives, and the
 * secrets its deliveries are signed with - stored encrypted, see {@link WebhookSecretCipher}.
 *
 * <p>
 * SystemDB, with the tenant id as a plain column, like the integration client registry: the
 * provisioning system writes it without entering the tenant.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_WEBHOOKS", uniqueConstraints = {@UniqueConstraint(name = "UK_DIRIGIBLE_TENANT_WEBHOOKS_SUBSCRIPTION",
        columnNames = {"TENWEBHOOK_TENANT_ID", "TENWEBHOOK_SUBSCRIPTION_ID"})})
public class TenantWebhook {

    /** The separator of the stored event types; an event type never contains it. */
    private static final String TYPE_SEPARATOR = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TENWEBHOOK_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    @Column(name = "TENWEBHOOK_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "TENWEBHOOK_SUBSCRIPTION_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String subscriptionId;

    @Column(name = "TENWEBHOOK_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String name;

    @Column(name = "TENWEBHOOK_URL", columnDefinition = "VARCHAR", nullable = false, length = 2000)
    private String url;

    @Column(name = "TENWEBHOOK_EVENT_TYPES", columnDefinition = "VARCHAR", nullable = false, length = 4000)
    private String eventTypes;

    @Enumerated(EnumType.STRING)
    @Column(name = "TENWEBHOOK_STATUS", columnDefinition = "VARCHAR", nullable = false, length = 16)
    private TenantWebhookStatus status;

    @Column(name = "TENWEBHOOK_SECRET", columnDefinition = "VARCHAR", nullable = false, length = 1000)
    private String secret;

    @Column(name = "TENWEBHOOK_PREVIOUS_SECRET", columnDefinition = "VARCHAR", length = 1000)
    private String previousSecret;

    @Column(name = "TENWEBHOOK_REVISION", columnDefinition = "BIGINT", nullable = false)
    private long revision;

    @Column(name = "TENWEBHOOK_CREATED_BY", columnDefinition = "VARCHAR", length = 320)
    private String createdBy;

    @Column(name = "TENWEBHOOK_CREATED_AT", columnDefinition = "TIMESTAMP")
    private Instant createdAt;

    @Column(name = "TENWEBHOOK_FAILING_AT", columnDefinition = "TIMESTAMP")
    private Instant failingAt;

    TenantWebhook() {}

    TenantWebhook(String tenantId, String subscriptionId) {
        this.tenantId = tenantId;
        this.subscriptionId = subscriptionId;
    }

    Long getId() {
        return id;
    }

    String getTenantId() {
        return tenantId;
    }

    String getSubscriptionId() {
        return subscriptionId;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    /**
     * The event types the subscription receives.
     *
     * @return the types, in the order they were registered
     */
    Set<String> eventTypes() {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(eventTypes.split(TYPE_SEPARATOR))
                     .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    void replaceEventTypes(Collection<String> types) {
        this.eventTypes = String.join(TYPE_SEPARATOR, new LinkedHashSet<>(types));
    }

    TenantWebhookStatus getStatus() {
        return status;
    }

    void setStatus(TenantWebhookStatus status) {
        this.status = status;
    }

    String getSecret() {
        return secret;
    }

    String getPreviousSecret() {
        return previousSecret;
    }

    /**
     * Replaces the signing secrets, both already encrypted.
     *
     * @param secret the current secret
     * @param previousSecret the previous one, during a rotation, or {@code null}
     */
    void replaceSecrets(String secret, String previousSecret) {
        this.secret = secret;
        this.previousSecret = previousSecret;
    }

    long getRevision() {
        return revision;
    }

    void setRevision(long revision) {
        this.revision = revision;
    }

    String getCreatedBy() {
        return createdBy;
    }

    void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    Instant getFailingAt() {
        return failingAt;
    }

    void setFailingAt(Instant failingAt) {
        this.failingAt = failingAt;
    }
}
