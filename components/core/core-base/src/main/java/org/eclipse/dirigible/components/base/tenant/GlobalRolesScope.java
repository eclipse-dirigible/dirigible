/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;

/**
 * Which identity provider groups that carry no tenant are global roles of this application, under
 * the {@link TenantResolutionStrategy#TOKEN_GROUPS} strategy.
 *
 * <p>
 * Configured through {@link DirigibleConfig#TENANT_GLOBAL_ROLES_SCOPE}. It matters when one
 * identity provider serves several applications: a plain group such as {@code ADMINISTRATOR} names
 * no application, so under {@link #INSTANCE} it is a global role in every application of that
 * provider. {@link #APPLICATION} makes a global role count only where its group names the
 * application.
 */
public enum GlobalRolesScope {

    /**
     * Every group that is not a tenant group is a global role, e.g. {@code ADMINISTRATOR}. Right for an
     * identity provider that serves one application. The default.
     */
    INSTANCE,

    /**
     * Only a group {@code <appId>.<role>} naming this application is a global role, and it grants
     * {@code <role>}: {@code suite.ADMINISTRATOR} grants {@code ADMINISTRATOR} in the application
     * {@code suite}. An unscoped {@code ADMINISTRATOR} and another application's
     * {@code library.ADMINISTRATOR} grant nothing. The role must not contain a dot.
     */
    APPLICATION;

    /**
     * Resolves the configured scope.
     *
     * @return the configured scope, {@link #INSTANCE} when nothing is configured
     * @throws InvalidConfigException if the configured value is not a known scope
     */
    public static GlobalRolesScope fromConfiguration() {
        return parse(DirigibleConfig.TENANT_GLOBAL_ROLES_SCOPE.getStringValue());
    }

    /**
     * Parses a scope name, tolerating surrounding whitespace and any letter case.
     *
     * @param value the configured value
     * @return the matching scope, {@link #INSTANCE} for a blank value
     * @throws InvalidConfigException if the value is not a known scope
     */
    static GlobalRolesScope parse(String value) {
        if (value == null || value.isBlank()) {
            return INSTANCE;
        }
        String normalizedValue = value.trim()
                                      .toUpperCase();
        for (GlobalRolesScope scope : values()) {
            if (scope.name()
                     .equals(normalizedValue)) {
                return scope;
            }
        }
        throw new InvalidConfigException(
                "Unknown global roles scope [" + value + "] configured in [" + DirigibleConfig.TENANT_GLOBAL_ROLES_SCOPE.getKey()
                        + "]. Supported values: [" + INSTANCE + ", " + APPLICATION + "]",
                DirigibleConfig.TENANT_GLOBAL_ROLES_SCOPE.getKey());
    }
}
