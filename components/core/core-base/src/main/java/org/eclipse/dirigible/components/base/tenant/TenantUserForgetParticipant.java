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
 * Removes what one module keeps about a person in a tenant, when the tenant is told to forget that
 * person (the tenant provisioning API's {@code DELETE tenants/{id}/users/{email}}).
 *
 * <p>
 * Every participant bean runs in the transaction that deletes the person's user rows, so the module
 * that owns the erasure depends on none of them. A participant must be <b>idempotent</b>: finding
 * nothing that names the person is success. It removes or blanks what identifies the person and
 * leaves the tenant's business data alone.
 */
public interface TenantUserForgetParticipant {

    /**
     * Forgets the person in the tenant; idempotent.
     *
     * @param tenantId the id of the tenant
     * @param email the person's email address, lower-cased
     */
    void forget(String tenantId, String email);
}
