/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * The sensitive-key classification and the masking of every value column of the global
 * configurations listing.
 */
public class SensitiveConfigurationsTest {

    private static final String SECRET = "s3cr3t-value";

    @Test
    public void classifiesCredentialKeysAsSensitive() {
        for (String key : List.of("DIRIGIBLE_DATABASE_SYSTEM_PASSWORD", "DIRIGIBLE_OAUTH_CLIENT_SECRET", "DIRIGIBLE_INTENT_AI_API_KEY",
                "DIRIGIBLE_MS_SHAREPOINT_TOKEN", "DIRIGIBLE_OAUTH_VERIFICATION_KEY", "DIRIGIBLE_CMS_MANAGED_CONFIGURATION_KEY",
                "DIRIGIBLE_BASIC_USERNAME", "DIRIGIBLE_MONGODB_CLIENT_URI", "DIRIGIBLE_MESSAGING_BROKER_URL", "dirigible_mail_password")) {
            assertTrue(SensitiveConfigurations.isSensitive(key), key);
        }
    }

    @Test
    public void leavesOrdinaryKeysReadable() {
        for (String key : List.of("DIRIGIBLE_BRANDING_NAME", "DIRIGIBLE_INTENT_AI_MAX_TOKENS", "DIRIGIBLE_OAUTH_TOKEN_URL",
                "DIRIGIBLE_OAUTH2_JWT_TOKEN_KINDS", "DIRIGIBLE_KAFKA_KEY_SERIALIZER", "DIRIGIBLE_OAUTH_VERIFICATION_KEY_EXPONENT",
                "DIRIGIBLE_DATABASE_SYSTEM_USERNAME", "DIRIGIBLE_APP_BASE_URL", "TOKEN_GROUPS_IT_TENANT_MARKER")) {
            assertFalse(SensitiveConfigurations.isSensitive(key), key);
        }
        assertFalse(SensitiveConfigurations.isSensitive(null));
    }

    @Test
    public void masksSetValuesAndKeepsUnsetNull() {
        assertEquals(SensitiveConfigurations.MASK, SensitiveConfigurations.mask("DIRIGIBLE_INTENT_AI_API_KEY", SECRET));
        assertEquals(SensitiveConfigurations.MASK, SensitiveConfigurations.mask("DIRIGIBLE_INTENT_AI_API_KEY", ""));
        assertNull(SensitiveConfigurations.mask("DIRIGIBLE_INTENT_AI_API_KEY", null));
        assertEquals("Acme", SensitiveConfigurations.mask("DIRIGIBLE_BRANDING_NAME", "Acme"));
    }

    @Test
    public void recognisesTheMaskEchoedBackOnlyForSensitiveKeys() {
        assertTrue(SensitiveConfigurations.isMaskEcho("DIRIGIBLE_INTENT_AI_API_KEY", SensitiveConfigurations.MASK));
        assertFalse(SensitiveConfigurations.isMaskEcho("DIRIGIBLE_INTENT_AI_API_KEY", "new-key"));
        assertFalse(SensitiveConfigurations.isMaskEcho("DIRIGIBLE_BRANDING_NAME", SensitiveConfigurations.MASK));
    }

    @Test
    public void masksASensitiveKeyInEveryColumn() {
        String key = "DIRIGIBLE_INTENT_AI_API_KEY";
        List<List<String>> rows = ConfigurationsService.rows(new String[] {key, "DIRIGIBLE_BRANDING_NAME", "DIRIGIBLE_MAIL_PASSWORD"},
                Map.of(key, SECRET + "-runtime", "DIRIGIBLE_BRANDING_NAME", "Acme"), Map.of(key, SECRET + "-env"),
                Map.of(key, SECRET + "-deployment"), Map.of(key, SECRET + "-module"));

        assertEquals(List.of(key, SensitiveConfigurations.MASK, SensitiveConfigurations.MASK, SensitiveConfigurations.MASK,
                SensitiveConfigurations.MASK), rows.get(0));
        assertEquals("Acme", rows.get(1)
                                 .get(1));
        // an unset sensitive key stays null in every column, so "set vs unset" is still visible
        assertEquals(java.util.Arrays.asList("DIRIGIBLE_MAIL_PASSWORD", null, null, null, null), rows.get(2));
        assertFalse(rows.toString()
                        .contains(SECRET));
    }

}
