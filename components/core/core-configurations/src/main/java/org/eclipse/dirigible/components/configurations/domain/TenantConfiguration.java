/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.domain;

/**
 * A single tenant-scoped configuration entry as persisted in the per-tenant
 * DIRIGIBLE_CONFIGURATIONS table.
 *
 * @param key the configuration key
 * @param value the configuration value
 */
public record TenantConfiguration(String key, String value) {
}
