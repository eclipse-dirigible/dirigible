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

/**
 * Who may call the tenant provisioning API.
 *
 * <p>
 * {@link #TENANT_PROVISIONER} is deliberately NOT a value of
 * {@code org.eclipse.dirigible.components.base.http.roles.Roles}: trial mode grants every role of
 * that enum to every user, and a role that provisions tenants and accepts database credentials must
 * never be handed out by a convenience switch. It is carried by a machine-to-machine token - a
 * resource-server scope whose bare name is {@code TENANT_PROVISIONER}, which
 * {@code ScopeRoleJwtAuthoritiesConverter} maps one to one.
 *
 * <p>
 * {@code ADMINISTRATOR} and {@code OPERATOR} are admitted as well, so an operator can drive the
 * same sequence by hand when a provisioning run has to be repaired.
 */
final class TenantProvisioningRoles {

    /** The role a machine-to-machine provisioning client presents. */
    static final String TENANT_PROVISIONER = "TENANT_PROVISIONER";

    /** The role of a platform administrator - break-glass access to the same API. */
    static final String ADMINISTRATOR = "ADMINISTRATOR";

    /** The role of a platform operator - break-glass access to the same API. */
    static final String OPERATOR = "OPERATOR";

    private TenantProvisioningRoles() {}
}
