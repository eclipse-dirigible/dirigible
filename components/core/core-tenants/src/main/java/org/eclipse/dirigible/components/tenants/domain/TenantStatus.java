/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.domain;

/**
 * The Enum TenantStatus.
 */
public enum TenantStatus {

    /** The initial. */
    INITIAL,
    /** The provisioned. */
    PROVISIONED,
    /**
     * Registered by an external provisioner, which owns the tenant's database user, schema and data
     * source; the platform must not act on the tenant until that provisioner activates it.
     *
     * <p>
     * The state is deliberately invisible to both halves of the built-in flow: the provisioner queries
     * {@link #INITIAL} only, so it never races the external one by creating a user and a schema of its
     * own, and {@code executeForEachTenant} queries {@link #PROVISIONED} only, so no synchronizer, job
     * or listener reaches a tenant whose data source does not exist yet. Activation moves it to
     * {@link #PROVISIONED}; until then it can also be deleted, which is the rollback path when
     * provisioning is abandoned.
     */
    PENDING_ACTIVATION,
    /**
     * Paused by an external provisioner - a subscription whose payment failed, say. Its people and
     * integration clients are refused, and nothing runs for it: its jobs are paused, its listeners
     * stopped, its process instances suspended, and {@code executeForEachTenant} - which queries
     * {@link #PROVISIONED} only - skips it, so no relay, top-up or reconciliation pass reaches it
     * either. Its data, its data source, its user replica and its client registry stay as they are.
     *
     * <p>
     * It is still maintained: {@code executeForEachMaintainedTenant} visits it, so an application
     * version published meanwhile reaches its schema. The status is the only marker - there is no
     * in-memory one - and resuming moves the tenant back to {@link #PROVISIONED}.
     */
    SUSPENDED,
    /**
     * Being removed through the tenant provisioning API's deprovisioning.
     *
     * <p>
     * Set and persisted before anything of the tenant is torn down, so it stops new work from that
     * moment and across a restart: {@code executeForEachTenant} queries {@link #PROVISIONED} only, so
     * no synchronizer, job, listener or top-up pass reaches the tenant any more, its tenant-users
     * endpoint answers 409, and an activation is refused. A deprovisioning interrupted half way leaves
     * the tenant here, and the repeated call resumes it. The tenant row is deleted at the end.
     */
    DEPROVISIONING
}
