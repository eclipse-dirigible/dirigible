/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.eclipse.dirigible.components.tenants.tenant.TenantResolutionConfigValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * A tenant resolution configuration that cannot work must stop the platform from starting, rather
 * than let it serve requests that silently land in the wrong tenant.
 *
 * <p>
 * The assertion is therefore about the Spring context itself: refreshing a context that contains
 * the validator has to fail. No application is booted - the validator reads the configuration in
 * its constructor, so a context holding only that bean reproduces the startup behaviour exactly, in
 * milliseconds instead of minutes.
 */
class TenantResolutionConfigValidationIT {

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
    void theDefaultConfigurationStarts() {
        assertThatCode(TenantResolutionConfigValidationIT::refreshContext).doesNotThrowAnyException();
    }

    @Test
    void aValidTokenGroupsConfigurationStarts() {
        configureTokenGroups();

        assertThatCode(TenantResolutionConfigValidationIT::refreshContext).doesNotThrowAnyException();
    }

    @Test
    void aMissingApplicationIdStopsTheStartup() {
        configureTokenGroups();
        Configuration.remove(DirigibleConfig.APP_ID.getKey());

        assertStartupFailsOn(DirigibleConfig.APP_ID);
    }

    @Test
    void aDottedApplicationIdStopsTheStartup() {
        configureTokenGroups();
        DirigibleConfig.APP_ID.setStringValue("codbex.library");

        assertStartupFailsOn(DirigibleConfig.APP_ID);
    }

    @Test
    void singleTenantModeStopsTheStartup() {
        configureTokenGroups();
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(false);

        assertStartupFailsOn(DirigibleConfig.MULTI_TENANT_MODE_ENABLED);
    }

    @Test
    void theLegacyCognitoSingleUserPoolModelStopsTheStartup() {
        configureTokenGroups();
        DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.setBooleanValue(true);

        assertStartupFailsOn(DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED);
    }

    @Test
    void aBlankGroupsClaimStopsTheStartup() {
        configureTokenGroups();
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue("   ");

        assertStartupFailsOn(DirigibleConfig.TENANT_GROUPS_CLAIM);
    }

    @Test
    void anUnknownStrategyStopsTheStartup() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("SESSION");

        assertStartupFailsOn(DirigibleConfig.TENANT_RESOLUTION_STRATEGY);
    }

    private static void configureTokenGroups() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue("library");
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.setBooleanValue(false);
    }

    private static void refreshContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TenantResolutionConfigValidator.class)) {
            assertThat(context.getBean(TenantResolutionConfigValidator.class)).isNotNull();
        }
    }

    private static void assertStartupFailsOn(DirigibleConfig expectedKey) {
        assertThatThrownBy(TenantResolutionConfigValidationIT::refreshContext).rootCause()
                                                                              .isInstanceOf(InvalidConfigException.class)
                                                                              .hasMessageContaining(expectedKey.getKey());
    }
}
