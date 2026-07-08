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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TenantConfigurationKeyPolicy} - pure logic, no Spring context. The policy
 * is an explicit white-list; for now only the branding properties are injectable.
 */
class TenantConfigurationKeyPolicyTest {

    private final TenantConfigurationKeyPolicy policy = new TenantConfigurationKeyPolicy();

    @Test
    void brandingKeysAreInjectable() {
        assertTrue(policy.isInjectable("DIRIGIBLE_BRANDING_NAME"));
        assertTrue(policy.isInjectable("DIRIGIBLE_BRANDING_THEME"));
        assertTrue(policy.isInjectable("DIRIGIBLE_BRANDING_BRAND_URL"));
    }

    @Test
    void nonWhitelistedKeysAreNotInjectable() {
        assertFalse(policy.isInjectable("DIRIGIBLE_TENANT_FEATURE_X"));
        assertFalse(policy.isInjectable("DIRIGIBLE_MAIL_SMTPS_HOST"));
        assertFalse(policy.isInjectable("DIRIGIBLE_DATABASE_H2_URL"));
        assertFalse(policy.isInjectable("SOME_RANDOM_KEY"));
    }

    @Test
    void matchIsExactNotByPrefix() {
        // a branding-prefixed but non-predefined key is NOT injectable (no wildcards)
        assertFalse(policy.isInjectable("DIRIGIBLE_BRANDING_UNKNOWN"));
        assertFalse(policy.isInjectable("DIRIGIBLE_BRANDING_"));
        assertFalse(policy.isInjectable("DIRIGIBLE_BRANDING_NAME_SUFFIX"));
    }

    @Test
    void allowedKeysAreTheBrandingProperties() {
        assertTrue(policy.allowedKeys()
                         .contains("DIRIGIBLE_BRANDING_NAME"));
        assertTrue(policy.allowedKeys()
                         .contains("DIRIGIBLE_BRANDING_THEME"));
        assertFalse(policy.allowedKeys()
                          .isEmpty());
    }

    @Test
    void blankKeyIsNotInjectable() {
        assertFalse(policy.isInjectable(null));
        assertFalse(policy.isInjectable(""));
        assertFalse(policy.isInjectable("   "));
    }

    @Test
    void filterInjectableReturnsOnlyTheWhitelistedSubset() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("DIRIGIBLE_BRANDING_NAME", "My Brand");
        raw.put("DIRIGIBLE_DATABASE_H2_URL", "jdbc:h2:mem:hack");
        raw.put("DIRIGIBLE_BRANDING_THEME", "classic");
        raw.put("SOME_RANDOM_KEY", "x");

        Map<String, String> injectable = policy.filterInjectable(raw);

        assertEquals(2, injectable.size());
        assertEquals("My Brand", injectable.get("DIRIGIBLE_BRANDING_NAME"));
        assertEquals("classic", injectable.get("DIRIGIBLE_BRANDING_THEME"));
        assertFalse(injectable.containsKey("DIRIGIBLE_DATABASE_H2_URL"));
        assertFalse(injectable.containsKey("SOME_RANDOM_KEY"));
    }

}
