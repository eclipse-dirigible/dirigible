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

/**
 * Where a webhook subscription stands.
 */
public enum TenantWebhookStatus {

    /** Events are recorded and delivered. */
    ACTIVE,

    /**
     * An event was given up after the retry window: the receiver is not answering. Events are still
     * recorded and attempted, and the first delivery that succeeds makes the subscription
     * {@link #ACTIVE} again. Set by the application, never by the registry.
     */
    FAILING,

    /** Paused by the provisioning system: no event is recorded, pending ones are given up. */
    DISABLED,

    /**
     * Left out of a later registry: kept as a hidden tombstone, so the revision of the tenant's
     * registry survives the removal of its last subscription. A later registry naming the id again
     * brings it back.
     */
    REMOVED;

    /**
     * Whether events of this status are recorded for delivery.
     *
     * @return true for {@link #ACTIVE} and {@link #FAILING}
     */
    boolean receivesEvents() {
        return this == ACTIVE || this == FAILING;
    }
}
