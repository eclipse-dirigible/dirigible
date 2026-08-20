/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The token groups strategy needs an application id and real multi-tenancy, and cannot coexist with
 * the legacy single user pool tenant model. Each of those is a startup failure rather than a
 * runtime surprise.
 */
class TenantResolutionConfigValidatorTest {

    @BeforeEach
    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
        Configuration.remove(DirigibleConfig.APP_ID.getKey());
        Configuration.remove(DirigibleConfig.TENANT_GROUPS_CLAIM.getKey());
        Configuration.remove(DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getKey());
        Configuration.remove(DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.getKey());
    }

    @Test
    void theDefaultConfigurationIsValid() {
        assertDoesNotThrow(TenantResolutionConfigValidator::new);
    }

    @Test
    void subdomainStrategyIgnoresTheTokenGroupsSettings() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("SUBDOMAIN");
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(false);
        DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.setBooleanValue(true);

        assertDoesNotThrow(TenantResolutionConfigValidator::new);
    }

    @Test
    void aValidTokenGroupsConfigurationIsAccepted() {
        configureTokenGroups();

        assertDoesNotThrow(TenantResolutionConfigValidator::new);
    }

    @Test
    void anUnknownStrategyFailsInAnyMode() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("SESSION");

        InvalidConfigException exception = assertThrows(InvalidConfigException.class, TenantResolutionConfigValidator::new);

        assertEquals(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey(), exception.getConfigKey());
    }

    @Test
    void aMissingApplicationIdFails() {
        configureTokenGroups();
        Configuration.remove(DirigibleConfig.APP_ID.getKey());

        assertFailsOn(DirigibleConfig.APP_ID);
    }

    @Test
    void aBlankApplicationIdFails() {
        configureTokenGroups();
        DirigibleConfig.APP_ID.setStringValue("   ");

        assertFailsOn(DirigibleConfig.APP_ID);
    }

    @Test
    void aDottedApplicationIdFails() {
        configureTokenGroups();
        DirigibleConfig.APP_ID.setStringValue("codbex.library");

        InvalidConfigException exception = assertFailsOn(DirigibleConfig.APP_ID);
        assertTrue(exception.getMessage()
                            .contains("codbex.library"),
                "the message must quote the offending value: " + exception.getMessage());
    }

    @Test
    void singleTenantModeFails() {
        configureTokenGroups();
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(false);

        assertFailsOn(DirigibleConfig.MULTI_TENANT_MODE_ENABLED);
    }

    @Test
    void theLegacyCognitoSingleUserPoolModelFails() {
        configureTokenGroups();
        DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.setBooleanValue(true);

        assertFailsOn(DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED);
    }

    @Test
    void aBlankGroupsClaimFails() {
        configureTokenGroups();
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue("  ");

        assertFailsOn(DirigibleConfig.TENANT_GROUPS_CLAIM);
    }

    private static void configureTokenGroups() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue("library");
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.setBooleanValue(false);
    }

    private static InvalidConfigException assertFailsOn(DirigibleConfig expectedKey) {
        InvalidConfigException exception = assertThrows(InvalidConfigException.class, TenantResolutionConfigValidator::new);

        assertEquals(expectedKey.getKey(), exception.getConfigKey());
        assertTrue(exception.getMessage()
                            .contains(expectedKey.getKey()),
                "the message must name the offending key: " + exception.getMessage());
        return exception;
    }
}
