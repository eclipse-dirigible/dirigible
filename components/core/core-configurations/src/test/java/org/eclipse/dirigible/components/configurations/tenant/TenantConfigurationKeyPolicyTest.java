/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TenantConfigurationKeyPolicy} - pure logic, no Spring context.
 */
class TenantConfigurationKeyPolicyTest {

    private static final String ALLOWED_KEYS = "DIRIGIBLE_TENANT_CONFIGURATION_ALLOWED_KEYS";

    private final TenantConfigurationKeyPolicy policy = new TenantConfigurationKeyPolicy();

    @AfterEach
    void clearAllowList() {
        Configuration.remove(ALLOWED_KEYS);
    }

    @Test
    void defaultAllowListPermitsOnlyTheTenantNamespace() {
        // default is DIRIGIBLE_TENANT_* (no override set)
        assertTrue(policy.isInjectable("DIRIGIBLE_TENANT_FEATURE_X"));
        assertFalse(policy.isInjectable("DIRIGIBLE_MAIL_SMTPS_HOST"));
        assertFalse(policy.isInjectable("SOME_RANDOM_KEY"));
    }

    @Test
    void allowListCanWidenToOtherNamespaces() {
        Configuration.set(ALLOWED_KEYS, "DIRIGIBLE_TENANT_*, DIRIGIBLE_MAIL_*");

        assertTrue(policy.isInjectable("DIRIGIBLE_TENANT_FEATURE_X"));
        assertTrue(policy.isInjectable("DIRIGIBLE_MAIL_SMTPS_HOST"));
        assertFalse(policy.isInjectable("DIRIGIBLE_KAFKA_ACKS"));
    }

    @Test
    void exactKeyMatchIsSupported() {
        Configuration.set(ALLOWED_KEYS, "DIRIGIBLE_THEME_DEFAULT");

        assertTrue(policy.isInjectable("DIRIGIBLE_THEME_DEFAULT"));
        assertFalse(policy.isInjectable("DIRIGIBLE_THEME_DEFAULTS"));
        assertFalse(policy.isInjectable("DIRIGIBLE_THEME"));
    }

    @Test
    void protectedKeysAreNeverInjectableEvenWithWildcardAllowList() {
        Configuration.set(ALLOWED_KEYS, "*");

        // a non-protected arbitrary key is now injectable
        assertTrue(policy.isInjectable("MY_APP_SETTING"));

        // ... but infrastructure keys stay protected regardless
        assertFalse(policy.isInjectable("DIRIGIBLE_DATABASE_H2_URL"));
        assertFalse(policy.isInjectable("DIRIGIBLE_MASTER_REPOSITORY_PROVIDER"));
        assertFalse(policy.isInjectable("DIRIGIBLE_MULTI_TENANT_MODE"));
        assertFalse(policy.isInjectable("DIRIGIBLE_OAUTH_CLIENT_SECRET"));
        assertFalse(policy.isInjectable("DIRIGIBLE_TENANT_CONFIGURATION_ALLOWED_KEYS"));
        assertFalse(policy.isInjectable("spring.profiles.active"));
    }

    @Test
    void emptyAllowListInjectsNothing() {
        Configuration.set(ALLOWED_KEYS, "");

        assertFalse(policy.isInjectable("DIRIGIBLE_TENANT_FEATURE_X"));
        assertFalse(policy.isInjectable("MY_APP_SETTING"));
    }

    @Test
    void filterInjectableReturnsOnlyThePermittedSubset() {
        Configuration.set(ALLOWED_KEYS, "DIRIGIBLE_TENANT_*");

        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("DIRIGIBLE_TENANT_A", "1");
        raw.put("DIRIGIBLE_DATABASE_H2_URL", "jdbc:h2:mem:hack");
        raw.put("DIRIGIBLE_TENANT_B", "2");
        raw.put("SOME_RANDOM_KEY", "3");

        Map<String, String> injectable = policy.filterInjectable(raw);

        assertEquals(2, injectable.size());
        assertEquals("1", injectable.get("DIRIGIBLE_TENANT_A"));
        assertEquals("2", injectable.get("DIRIGIBLE_TENANT_B"));
        assertFalse(injectable.containsKey("DIRIGIBLE_DATABASE_H2_URL"));
        assertFalse(injectable.containsKey("SOME_RANDOM_KEY"));
    }

}
