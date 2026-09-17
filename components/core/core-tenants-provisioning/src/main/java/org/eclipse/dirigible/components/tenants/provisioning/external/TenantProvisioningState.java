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

import org.eclipse.dirigible.components.tenants.domain.TenantStatus;

/**
 * A tenant as the provisioning API describes it - the same body a registration answers with and a
 * read returns, so a caller never has to learn two shapes.
 *
 * @param id the tenant id
 * @param name the tenant name
 * @param subdomain the tenant subdomain
 * @param status the platform tenant status
 * @param initialization how far the tenant's initialization has got
 */
public record TenantProvisioningState(String id, String name, String subdomain, TenantStatus status,
        TenantInitializationState initialization) {
}
