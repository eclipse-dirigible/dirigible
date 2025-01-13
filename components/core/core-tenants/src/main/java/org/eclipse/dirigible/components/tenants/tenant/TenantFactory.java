/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.springframework.stereotype.Component;

/**
 * A factory for creating Tenant objects.
 */
@Component
public class TenantFactory {

    /**
     * Creates a new Tenant object.
     *
     * @param tenant the tenant
     * @return the tenant
     */
    public Tenant createFromEntity(org.eclipse.dirigible.components.tenants.domain.Tenant tenant) {
        return TenantImpl.createFromEntity(tenant);
    }
}
