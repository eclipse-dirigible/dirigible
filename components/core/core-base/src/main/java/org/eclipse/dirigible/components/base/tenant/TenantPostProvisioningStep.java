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
 * A step which will be executed once a tenant provisioning process is completed (all
 * {@link TenantProvisioningStep} have completed).
 */
public interface TenantPostProvisioningStep {

    /**
     * Will be called once provisioning of all tenants is completed.<br>
     * If there are no tenants in INITIAL status, post provisioning step will not be called.
     *
     * @throws TenantProvisioningException the tenant provisioning exception
     */
    void execute() throws TenantProvisioningException;

}
