/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.core;

import java.util.Collections;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.ConfigDescriptors;

/**
 * The grouped configuration for the {@code core/configurations} JS/TS API, as JSON: the same groups
 * and descriptors {@code GET /services/core/configurations/groups} and {@code /descriptors} answer,
 * every sensitive value masked.
 */
public final class ConfigurationsFacade {

    private ConfigurationsFacade() {}

    /**
     * The configuration groups with their key counts.
     *
     * @return the groups as a JSON array
     */
    public static String getGroups() {
        return GsonHelper.toJson(ConfigDescriptors.groups());
    }

    /**
     * The described configuration keys, grouped.
     *
     * @param groups the comma-separated group ids to include; {@code null} or blank means every group
     * @return the groups as a JSON array
     */
    public static String getDescriptors(String groups) {
        return GsonHelper.toJson(ConfigDescriptors.describe(
                ConfigDescriptors.parseGroups(groups == null ? null : Collections.singletonList(groups)), null, false));
    }

}
