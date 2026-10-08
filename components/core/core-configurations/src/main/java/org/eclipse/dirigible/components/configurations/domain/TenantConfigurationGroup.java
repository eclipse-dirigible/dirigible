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

import java.util.List;

/**
 * One configuration group with the tenant-overridable keys in it.
 *
 * @param group the group id
 * @param section the section id
 * @param label the default label
 * @param entries the described keys, in the policy's display order
 */
public record TenantConfigurationGroup(String group, String section, String label, List<TenantConfigurationDescriptor> entries) {
}
