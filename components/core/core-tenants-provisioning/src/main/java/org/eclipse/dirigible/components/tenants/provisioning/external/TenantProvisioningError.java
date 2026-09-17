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
 * What a refused call answers with.
 *
 * @param status the HTTP status code
 * @param error the HTTP status name
 * @param message why the call was refused - the part a caller can act on
 */
public record TenantProvisioningError(int status, String error, String message) {
}
