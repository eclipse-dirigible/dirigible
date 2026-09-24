/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantUsersConfigValidatorTest {

    @BeforeEach
    void setUp() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.setStringValue("global:acme.requests");
    }

    @AfterEach
    void tearDown() {
        for (DirigibleConfig key : new DirigibleConfig[] {DirigibleConfig.TENANT_RESOLUTION_STRATEGY,
                DirigibleConfig.TENANT_USERS_REQUEST_QUEUE, DirigibleConfig.TENANT_USERS_OWNER_ROLE, DirigibleConfig.TENANT_USERS_ROLES}) {
            Configuration.remove(key.getKey());
        }
    }

    @Test
    void aCompleteConfigurationStarts() {
        assertDoesNotThrow(TenantUsersConfigValidator::new);
    }

    @Test
    void theSubdomainStrategyIsRefused() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("SUBDOMAIN");
        InvalidConfigException refusal = assertThrows(InvalidConfigException.class, TenantUsersConfigValidator::new);
        assertTrue(refusal.getMessage()
                          .contains(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey()));
    }

    @Test
    void aMissingQueueIsRefused() {
        DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.setStringValue("global:");
        assertThrows(InvalidConfigException.class, TenantUsersConfigValidator::new);
    }

    @Test
    void anOwnerRoleThatIsNotGrantableIsRefused() {
        DirigibleConfig.TENANT_USERS_ROLES.setStringValue("User,Reader");
        assertThrows(InvalidConfigException.class, TenantUsersConfigValidator::new);
    }
}
