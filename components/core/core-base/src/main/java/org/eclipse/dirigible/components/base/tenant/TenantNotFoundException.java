/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant;

/**
 * The Class TenantNotFoundException.
 */
public class TenantNotFoundException extends RuntimeException {

    /**
     * Instantiates a new tenant not found exception.
     *
     * @param tenantId the tenant id
     */
    public TenantNotFoundException(String tenantId) {
        super("Tenant with id [" + tenantId + "] was not found.");
    }
}
