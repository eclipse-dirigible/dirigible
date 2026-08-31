/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Derives how far a tenant's initialization has got.
 *
 * <p>
 * Derived, never tracked: there is no run registry to consult, so the answer is the same on every
 * node of a cluster and survives a restart.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantInitializationStatusCalculator {

    /**
     * Calculate the initialization state of a tenant.
     *
     * @param tenant the tenant
     * @return the state
     */
    TenantInitializationState calculate(Tenant tenant) {
        if (TenantStatus.PROVISIONED != tenant.getStatus()) {
            return TenantInitializationState.of(InitializationStatus.NOT_STARTED);
        }
        return TenantInitializationState.of(InitializationStatus.COMPLETED);
    }
}
