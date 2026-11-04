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
 * The body of a tenant status change: the status the tenant is to have. Only
 * {@link TenantStatus#SUSPENDED} (suspend) and {@link TenantStatus#PROVISIONED} (resume) are
 * accepted; {@link TenantSuspensionService} answers any other value with 400.
 *
 * @param status the requested status
 */
record TenantStatusParameter(TenantStatus status) {
}
