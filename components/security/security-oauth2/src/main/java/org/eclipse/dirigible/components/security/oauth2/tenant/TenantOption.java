/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

/**
 * A tenant the user may enter, as offered to the tenant picker.
 *
 * @param id the tenant id, as it appears in the user's groups
 * @param name the name this instance knows the tenant under, the id when it knows none
 * @param provisionedHere whether this instance has finished provisioning the tenant; a tenant that
 *        is not cannot be entered yet, and the picker says so instead of hiding it
 */
public record TenantOption(String id, String name, boolean provisionedHere) {
}
