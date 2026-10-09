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
 * A tenant-overridable configuration key, described for the settings UI: the tenant's own value
 * next to the platform value it overrides.
 *
 * @param key the configuration key
 * @param subgroup the subgroup within the group, or {@code null}
 * @param type the value type, {@code null} until declared (#7759)
 * @param defaultValue the platform value the key has when the tenant does not set it, masked when
 *        sensitive
 * @param value the tenant's value, {@code null} when unset, masked when sensitive
 * @param sensitive whether the value is a secret
 */
public record TenantConfigurationDescriptor(String key, String subgroup, String type, String defaultValue, String value,
        boolean sensitive) {
}
