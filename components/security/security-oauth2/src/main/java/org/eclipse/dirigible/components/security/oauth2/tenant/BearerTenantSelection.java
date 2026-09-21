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

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The tenant a bearer request enters for its duration, and the authentication it runs as there -
 * the token's own authorities plus the roles the user's groups grant in that tenant.
 *
 * @param tenant the tenant to run the request in
 * @param authentication the authentication to run it as
 */
public record BearerTenantSelection(Tenant tenant, JwtAuthenticationToken authentication) {
}
