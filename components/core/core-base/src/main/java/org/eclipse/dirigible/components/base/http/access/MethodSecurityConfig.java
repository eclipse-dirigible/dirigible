/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables the JSR-250 method security the platform's endpoints rely on.
 * <p>
 * The URL rules in {@link HttpSecurityURIConfigurator} guard only a few path prefixes; every other
 * endpoint under {@code /services/**} is merely authenticated there and carries its own
 * {@code jakarta.annotation.security.RolesAllowed} annotation instead. Those annotations are
 * enforced ONLY when JSR-250 method security is switched on, so this declaration must be
 * unconditional: it previously lived on the basic-authentication configuration, which every
 * single-sign-on profile ({@code keycloak}, {@code cognito}, {@code github}, {@code snowflake})
 * disables via {@code basic.enabled=false} - silently turning every {@code @RolesAllowed} on the
 * platform into a no-op and leaving administrative surfaces (SQL execution, data sources, users,
 * tenants, configurations) open to any authenticated user.
 * <p>
 * Keep it here, unconditional and profile-independent, so a newly added authentication profile
 * cannot reintroduce that gap. {@code prePostEnabled} / {@code securedEnabled} stay off: the
 * platform authorizes exclusively with {@code @RolesAllowed}.
 */
@Configuration
@EnableMethodSecurity(securedEnabled = false, jsr250Enabled = true, prePostEnabled = false)
class MethodSecurityConfig {
}
