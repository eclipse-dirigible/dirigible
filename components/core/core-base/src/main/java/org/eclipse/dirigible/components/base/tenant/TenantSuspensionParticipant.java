/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant;

/**
 * Stops and restarts one kind of runtime work of a tenant, when the tenant is suspended and
 * resumed.
 *
 * <p>
 * Suspending (the tenant provisioning API's {@code PUT tenants/{id}/status} with {@code SUSPENDED})
 * saves the tenant {@code SUSPENDED} first and then runs every participant's {@link #suspend} in
 * {@link #order() order}. Resuming runs every {@link #resume} in reverse order while the tenant is
 * still {@code SUSPENDED}, saves it {@code PROVISIONED}, and then calls every {@link #resumed}.
 * Each module that runs something per tenant contributes its own participant, so the module that
 * owns the suspension depends on none of them.
 *
 * <p>
 * A participant <b>stops the runtime and deletes no definition</b>: a paused job, a stopped
 * consumer, a suspended process instance - never a job, a listener, a message or a row. The tenant
 * must come back exactly as it was.
 *
 * <p>
 * Both directions must be <b>idempotent</b>: a run interrupted half way is repeated from the start,
 * finding nothing to do is success, and {@link #resume} also runs on a tenant that is already
 * running. A participant that throws stops the run and leaves the tenant {@code SUSPENDED}, the
 * safe side; the repeated call finishes it.
 */
public interface TenantSuspensionParticipant {

    /**
     * Where this participant runs: lower first on suspend, higher first on resume.
     *
     * @return the order
     */
    int order();

    /**
     * Stops this participant's work of the tenant; idempotent. The tenant is already saved
     * {@code SUSPENDED}.
     *
     * @param tenantId the id of the tenant being suspended
     */
    void suspend(String tenantId);

    /**
     * Restarts this participant's work of the tenant; idempotent, and harmless on a running tenant. The
     * tenant is still {@code SUSPENDED} - nothing here may depend on its status.
     *
     * @param tenantId the id of the tenant being resumed
     */
    void resume(String tenantId);

    /**
     * Runs once the resumed tenant is {@code PROVISIONED} again, for the work that reaches tenants
     * through the provisioned fan-out only and would otherwise wait for its next pass - a reconcile of
     * every tenant, which picks the resumed one up. Nothing by default.
     */
    default void resumed() {
        // most participants restart everything in resume
    }
}
