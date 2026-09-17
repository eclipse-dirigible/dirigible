/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The API is absent unless a deployment asked for it - the default has to be "off", or upgrading
 * the platform would silently expose an endpoint that accepts database credentials.
 */
class TenantProvisioningApiEnabledConditionTest {

    private final TenantProvisioningApiEnabledCondition condition = new TenantProvisioningApiEnabledCondition();

    @BeforeEach
    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey());
    }

    @Test
    void theApiIsOffByDefault() {
        assertFalse(condition.matches(null, null));
    }

    @Test
    void theApiIsOnWhenTheDeploymentOptedIn() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);

        assertTrue(condition.matches(null, null));
    }

    @Test
    void anExplicitFalseKeepsItOff() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(false);

        assertFalse(condition.matches(null, null));
    }
}
