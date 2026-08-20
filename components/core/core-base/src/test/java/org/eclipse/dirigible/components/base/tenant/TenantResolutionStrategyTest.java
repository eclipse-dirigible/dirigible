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
 * The configured value is normalized, and an unrecognized one fails loudly instead of silently
 * falling back to a strategy the operator did not ask for.
 */
class TenantResolutionStrategyTest {

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
    }

    @Test
    void unsetConfigurationResolvesToSubdomain() {
        assertEquals(TenantResolutionStrategy.SUBDOMAIN, TenantResolutionStrategy.fromConfiguration());
    }

    @Test
    void blankConfigurationResolvesToSubdomain() {
        assertEquals(TenantResolutionStrategy.SUBDOMAIN, TenantResolutionStrategy.parse("   "));
        assertEquals(TenantResolutionStrategy.SUBDOMAIN, TenantResolutionStrategy.parse(null));
    }

    @Test
    void configuredStrategyIsRead() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");

        assertEquals(TenantResolutionStrategy.TOKEN_GROUPS, TenantResolutionStrategy.fromConfiguration());
    }

    @Test
    void caseAndWhitespaceAreTolerated() {
        assertEquals(TenantResolutionStrategy.TOKEN_GROUPS, TenantResolutionStrategy.parse(" token_groups "));
        assertEquals(TenantResolutionStrategy.SUBDOMAIN, TenantResolutionStrategy.parse("Subdomain"));
    }

    @Test
    void unknownStrategyFailsNamingTheKeyAndTheValidValues() {
        InvalidConfigException exception = assertThrows(InvalidConfigException.class, () -> TenantResolutionStrategy.parse("SESSION"));

        assertEquals(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey(), exception.getConfigKey());
        assertTrue(exception.getMessage()
                            .contains("SESSION"),
                "the message must quote the offending value: " + exception.getMessage());
        assertTrue(exception.getMessage()
                            .contains("TOKEN_GROUPS"),
                "the message must list the supported values: " + exception.getMessage());
    }
}
