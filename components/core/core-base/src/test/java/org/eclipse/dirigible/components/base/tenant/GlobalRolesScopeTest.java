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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Nothing configured keeps today's behaviour, the value is normalized, and an unrecognized one
 * fails loudly instead of silently widening who is staff.
 */
class GlobalRolesScopeTest {

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.TENANT_GLOBAL_ROLES_SCOPE.getKey());
    }

    @Test
    void theDefaultIsTheInstanceScope() {
        assertEquals(GlobalRolesScope.INSTANCE, GlobalRolesScope.fromConfiguration());
        assertEquals(GlobalRolesScope.INSTANCE, GlobalRolesScope.parse(null));
        assertEquals(GlobalRolesScope.INSTANCE, GlobalRolesScope.parse("  "));
    }

    @Test
    void theConfiguredValueIsNormalized() {
        DirigibleConfig.TENANT_GLOBAL_ROLES_SCOPE.setStringValue(" application ");

        assertEquals(GlobalRolesScope.APPLICATION, GlobalRolesScope.fromConfiguration());
        assertEquals(GlobalRolesScope.INSTANCE, GlobalRolesScope.parse("Instance"));
    }

    @Test
    void anUnknownValueFails() {
        InvalidConfigException exception = assertThrows(InvalidConfigException.class, () -> GlobalRolesScope.parse("tenant"));

        assertTrue(exception.getMessage()
                            .contains(DirigibleConfig.TENANT_GLOBAL_ROLES_SCOPE.getKey()));
    }
}
