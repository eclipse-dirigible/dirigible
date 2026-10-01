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
        DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.setStringValue("global:acme.changes");
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
    }

    @AfterEach
    void tearDown() {
        for (String key : new String[] {DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey(),
                DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey(), DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey(),
                TenantUsersConfigValidator.OLD_QUEUE_KEY}) {
            Configuration.remove(key);
        }
    }

    @Test
    void aCompleteConfigurationStarts() {
        assertDoesNotThrow(TenantUsersConfigValidator::new);
    }

    @Test
    void theSubdomainStrategyIsRefused() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("SUBDOMAIN");
        assertRefusedNaming(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
    }

    @Test
    void aMissingQueueIsRefused() {
        Configuration.remove(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey());
        assertRefusedNaming(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey());
    }

    @Test
    void aBareGlobalMarkerIsRefused() {
        DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.setStringValue("global:");
        assertRefusedNaming(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey());
    }

    @Test
    void aQueueThatIsNotGlobalIsRefused() {
        DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.setStringValue("acme.changes");
        assertRefusedNaming(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey());
    }

    @Test
    void theProvisioningApiIsRequired() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(false);
        assertRefusedNaming(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey());
    }

    @Test
    void theOldKeyAloneIsRefusedNamingIt() {
        Configuration.remove(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey());
        Configuration.set(TenantUsersConfigValidator.OLD_QUEUE_KEY, "global:acme.requests");
        InvalidConfigException refusal = assertRefusedNaming(TenantUsersConfigValidator.OLD_QUEUE_KEY);
        assertTrue(refusal.getMessage()
                          .contains(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey()),
                "the refusal says which key to set instead");
    }

    private static InvalidConfigException assertRefusedNaming(String key) {
        InvalidConfigException refusal = assertThrows(InvalidConfigException.class, TenantUsersConfigValidator::new);
        assertTrue(refusal.getMessage()
                          .contains("[" + key + "]"),
                "the refusal names [" + key + "]: " + refusal.getMessage());
        return refusal;
    }
}
