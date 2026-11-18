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

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

/**
 * The webhook subscriptions. Every lookup carries the tenant id.
 */
@Repository("tenantWebhookRepository")
public interface TenantWebhookRepository extends JpaRepository<TenantWebhook, Long> {

    /**
     * The subscriptions of a tenant, locked for the apply of a registry.
     *
     * @param tenantId the tenant id
     * @return the subscriptions, tombstones included
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TenantWebhook> findForUpdateByTenantIdOrderBySubscriptionId(String tenantId);

    /**
     * The subscriptions of a tenant.
     *
     * @param tenantId the tenant id
     * @return the subscriptions, tombstones included
     */
    List<TenantWebhook> findByTenantIdOrderBySubscriptionId(String tenantId);

    /**
     * One subscription of a tenant.
     *
     * @param tenantId the tenant id
     * @param subscriptionId the subscription id
     * @return the subscription
     */
    Optional<TenantWebhook> findByTenantIdAndSubscriptionId(String tenantId, String subscriptionId);

    /**
     * Deletes the subscriptions of a tenant.
     *
     * @param tenantId the tenant id
     * @return how many were deleted
     */
    @Transactional
    long deleteByTenantId(String tenantId);
}
