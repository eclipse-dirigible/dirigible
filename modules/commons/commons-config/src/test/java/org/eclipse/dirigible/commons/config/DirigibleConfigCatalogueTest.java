/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 * The configuration catalogue is the {@link DirigibleConfig} enum alone (#7759): its metadata, the
 * misspelled-key aliases and the tenant allow-list derived from it.
 */
public class DirigibleConfigCatalogueTest {

    /** A key named like a credential. */
    private static final Pattern SECRET_NAME = Pattern.compile("(PASSWORD|SECRET|_TOKEN|API_KEY|SECRET_ACCESS_KEY)$");

    @Test
    public void theConfigurationParametersAreTheCatalogueKeys() {
        Set<String> catalogue = Arrays.stream(DirigibleConfig.values())
                                      .map(DirigibleConfig::getKey)
                                      .collect(Collectors.toSet());
        List<String> parameters = Arrays.asList(Configuration.getConfigurationParameters());

        assertEquals("every key is catalogued once", DirigibleConfig.values().length, catalogue.size());
        assertEquals(DirigibleConfig.values().length, parameters.size());
        assertEquals(catalogue, Set.copyOf(parameters));
        // the formerly hard-coded strings are in the enum now
        assertTrue(parameters.contains("DIRIGIBLE_BRANDING_NAME"));
        assertTrue(parameters.contains("SERVER_MAXHTTPHEADERSIZE"));
        // and so are the keys that were read but never listed
        assertTrue(parameters.contains("DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD"));
        assertTrue(parameters.contains("DIRIGIBLE_COGNITO_CLIENT_ID"));
    }

    @Test
    public void aKeyOutsideTheDirigiblePrefixCarriesAnExplicitGroup() {
        for (DirigibleConfig config : DirigibleConfig.values()) {
            if (!config.getKey()
                       .startsWith("DIRIGIBLE_")) {
                assertNotNull(config.getKey() + " has no DIRIGIBLE_ prefix for the group rule to read", config.getGroup());
            }
        }
    }

    @Test
    public void everyCredentialIsSensitive() {
        for (DirigibleConfig config : DirigibleConfig.values()) {
            if (SECRET_NAME.matcher(config.getKey())
                           .find()) {
                assertTrue(config.getKey() + " is named like a credential", config.isSensitive());
                assertEquals(ConfigType.SECRET, config.getType());
            }
        }
        assertFalse(DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.isSensitive());
        assertFalse(DirigibleConfig.INTENT_AI_MAX_TOKENS.isSensitive());
    }

    @Test
    public void theTypeIsInferredWhereItIsNotDeclared() {
        assertEquals(ConfigType.BOOLEAN, DirigibleConfig.TRIAL_ENABLED.getType());
        assertEquals(ConfigType.INT, DirigibleConfig.CSV_DATA_BATCH_SIZE.getType());
        assertEquals(ConfigType.DURATION, DirigibleConfig.ACT_AS_TTL_SECONDS.getType());
        assertEquals(ConfigType.URL, DirigibleConfig.INTENT_AI_BASE_URL.getType());
        assertEquals(ConfigType.STRING, DirigibleConfig.INSTANCE_NAME.getType());
        assertEquals(ConfigType.LIST, DirigibleConfig.CORS_ALLOWED_ORIGINS.getType());
        assertEquals(ConfigType.ENUM, DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getType());
    }

    @Test
    public void theTenantAllowListIsTodaysThirteenKeysInTodaysOrder() {
        assertEquals(List.of( //
                "DIRIGIBLE_BRANDING_NAME", //
                "DIRIGIBLE_BRANDING_SUBTITLE", //
                "DIRIGIBLE_BRANDING_BRAND", //
                "DIRIGIBLE_BRANDING_BRAND_URL", //
                "DIRIGIBLE_BRANDING_FAVICON", //
                "DIRIGIBLE_BRANDING_THEME", //
                "DIRIGIBLE_BRANDING_PREFIX", //
                "DIRIGIBLE_BRANDING_ANALYTICS", //
                "DIRIGIBLE_APPLICATION_LANGUAGES", //
                "DIRIGIBLE_APPLICATION_COUNTRY", //
                "DIRIGIBLE_DOCUMENTS_EXT_CONTENT_TYPE_MS_ENABLED", //
                "DIRIGIBLE_CMS_ROLES_ENABLED", //
                "DIRIGIBLE_APP_BASE_URL"), DirigibleConfig.tenantOverridableKeys());
        for (String key : DirigibleConfig.tenantOverridableKeys()) {
            assertFalse(key + " is read per call", DirigibleConfig.fromKey(key)
                                                                  .orElseThrow()
                                                                  .isRestartRequired());
        }
    }

    @Test
    public void theNeverReadKeysAreDeprecated() {
        // the 60 keys #7759 found read nowhere (the two misspellings among them), and the renamed tenant
        // users queue
        assertEquals(61, Arrays.stream(DirigibleConfig.values())
                               .filter(DirigibleConfig::isDeprecated)
                               .count());
        assertTrue(DirigibleConfig.OAUTH_ENABLED.isDeprecated());
        assertTrue(DirigibleConfig.JOB_EXPRESSION_JOBS.isDeprecated());
        assertFalse(DirigibleConfig.SCHEDULER_LOGS_RETENTION_PERIOD.isDeprecated());
        // renamed, not misspelled: the old value is refused, not carried over
        assertEquals("DIRIGIBLE_TENANT_USERS_CHANGE_QUEUE", DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.getDeprecatedBy());
        assertFalse(DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.isAlias());
    }

    @Test
    public void theRetentionPeriodResolvesUnderBothSpellings() {
        assertAliasResolvesBothWays("DIRIGIBLE_SCHEDULER_LOGS_RETANTION_PERIOD", "DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD");
    }

    @Test
    public void theAuthorizeUrlResolvesUnderBothSpellings() {
        assertAliasResolvesBothWays("DIRIGIBLE_OAUTH_AUTHORIZE_UR", "DIRIGIBLE_OAUTH_AUTHORIZE_URL");
    }

    @Test
    public void theCorrectSpellingWinsWhenBothAreSet() {
        try {
            Configuration.set("DIRIGIBLE_SCHEDULER_LOGS_RETANTION_PERIOD", "12");
            Configuration.set("DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD", "24");
            assertEquals("24", Configuration.get("DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD"));
            assertEquals(24, Configuration.getAsInt("DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD", 168));
        } finally {
            Configuration.remove("DIRIGIBLE_SCHEDULER_LOGS_RETANTION_PERIOD");
            Configuration.remove("DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD");
        }
    }

    @Test
    public void aRenamedKeyIsNotResolvedInPlaceOfItsReplacement() {
        try {
            Configuration.set("DIRIGIBLE_TENANT_USERS_REQUEST_QUEUE", "acme.requests");
            assertEquals(null, Configuration.get("DIRIGIBLE_TENANT_USERS_CHANGE_QUEUE"));
        } finally {
            Configuration.remove("DIRIGIBLE_TENANT_USERS_REQUEST_QUEUE");
        }
    }

    private static void assertAliasResolvesBothWays(String misspelled, String canonical) {
        DirigibleConfig alias = DirigibleConfig.fromKey(misspelled)
                                               .orElseThrow();
        assertTrue(alias.isAlias());
        assertEquals(canonical, alias.getDeprecatedBy());
        try {
            Configuration.set(misspelled, "1");
            assertEquals("1", Configuration.get(canonical));
            assertEquals("1", Configuration.get(canonical, "default"));
        } finally {
            Configuration.remove(misspelled);
        }
        try {
            Configuration.set(canonical, "2");
            assertEquals("2", Configuration.get(misspelled));
        } finally {
            Configuration.remove(canonical);
        }
        assertEquals("default", Configuration.get(canonical, "default"));
    }
}
