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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

public class ConfigDescriptorsTest {

    private static final String SECRET_KEY = "DIRIGIBLE_MAIL_PASSWORD";

    private static final String PLAIN_KEY = "DIRIGIBLE_MAIL_USERNAME";

    /** Set at module level by {@code /descriptors-module.properties}, which no other test loads. */
    private static final String MODULE_KEY = "DIRIGIBLE_MAIL_DESCRIPTORS_TEST";

    @After
    public void cleanUp() {
        Configuration.remove(SECRET_KEY);
        Configuration.remove(PLAIN_KEY);
        Configuration.remove(MODULE_KEY);
        ConfigDescriptors.setTenantOverridablePolicy(null);
    }

    @Test
    public void sensitiveValuesAreMaskedEverywhere() {
        Configuration.set(SECRET_KEY, "hunter2");
        ConfigDescriptors.Entry entry = ConfigDescriptors.describe(SECRET_KEY);
        assertTrue(entry.sensitive());
        assertEquals(SensitiveConfigs.MASK, entry.value());
        assertEquals(SensitiveConfigs.MASK, entry.values()
                                                 .get(ConfigDescriptors.SOURCE_RUNTIME));
        assertFalse(entry.toString()
                         .contains("hunter2"));
        assertTrue(ConfigDescriptors.describe(List.of(ConfigGroup.MAIL), "hunter", false)
                                    .isEmpty());
    }

    @Test
    public void theSourceFollowsTheConfigurationPrecedence() {
        Configuration.loadModuleConfig("/descriptors-module.properties");
        ConfigDescriptors.Entry entry = ConfigDescriptors.describe(MODULE_KEY);
        assertEquals(ConfigDescriptors.SOURCE_MODULE, entry.source());
        assertEquals(Configuration.get(MODULE_KEY), entry.value());

        Configuration.set(MODULE_KEY, "from-runtime");
        entry = ConfigDescriptors.describe(MODULE_KEY);
        assertEquals(ConfigDescriptors.SOURCE_RUNTIME, entry.source());
        assertEquals(Configuration.get(MODULE_KEY), entry.value());
        assertEquals("from-module", entry.values()
                                         .get(ConfigDescriptors.SOURCE_MODULE));
        assertFalse(entry.sensitive());
    }

    @Test
    public void anUnsetKeyFallsBackToItsDefault() {
        ConfigDescriptors.Entry entry = ConfigDescriptors.describe(DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.getKey());
        assertEquals(ConfigDescriptors.SOURCE_DEFAULT, entry.source());
        assertEquals("smtps", entry.value());
        assertEquals("smtps", entry.defaultValue());

        entry = ConfigDescriptors.describe(PLAIN_KEY);
        assertEquals(ConfigDescriptors.SOURCE_UNSET, entry.source());
        assertNull(entry.value());
    }

    @Test
    public void filtersByGroupQueryAndSetState() {
        Configuration.set(PLAIN_KEY, "operator");
        List<ConfigDescriptors.Group> groups = ConfigDescriptors.describe(Set.of(ConfigGroup.MAIL), "username", true);
        assertEquals(1, groups.size());
        assertEquals("mail", groups.get(0)
                                   .group());
        assertTrue(groups.get(0)
                         .entries()
                         .stream()
                         .anyMatch(entry -> entry.key()
                                                 .equals(PLAIN_KEY)));
        assertTrue(groups.get(0)
                         .entries()
                         .stream()
                         .allMatch(entry -> entry.key()
                                                 .toLowerCase()
                                                 .contains("username")));
        assertTrue(ConfigDescriptors.describe(Set.of(ConfigGroup.AI), "username", false)
                                    .isEmpty());
    }

    @Test
    public void groupSummariesCountTheCatalogue() {
        List<ConfigDescriptors.GroupSummary> summaries = ConfigDescriptors.groups();
        int total = summaries.stream()
                             .mapToInt(ConfigDescriptors.GroupSummary::total)
                             .sum();
        assertEquals(Configuration.getConfigurationParameters().length, total);
        assertTrue(summaries.stream()
                            .noneMatch(summary -> summary.id()
                                                         .equals("other")));
    }

    @Test
    public void tenantOverridableComesFromThePolicy() {
        assertFalse(ConfigDescriptors.describe("DIRIGIBLE_BRANDING_NAME")
                                     .tenantOverridable());
        ConfigDescriptors.setTenantOverridablePolicy("DIRIGIBLE_BRANDING_NAME"::equals);
        assertTrue(ConfigDescriptors.describe("DIRIGIBLE_BRANDING_NAME")
                                    .tenantOverridable());
    }

    @Test
    public void parsesGroupIds() {
        assertEquals(Set.of(ConfigGroup.MAIL, ConfigGroup.DATABASE), ConfigDescriptors.parseGroups(List.of("mail,database", "nope", " ")));
        assertTrue(ConfigDescriptors.parseGroups(null)
                                    .isEmpty());
    }

    @Test
    public void classifiesSensitiveKeys() {
        for (String key : List.of("DIRIGIBLE_INTENT_AI_API_KEY", "DIRIGIBLE_MS_SHAREPOINT_TOKEN", "DIRIGIBLE_OAUTH_VERIFICATION_KEY",
                "DIRIGIBLE_CMS_MANAGED_CONFIGURATION_KEY", "DIRIGIBLE_BASIC_USERNAME", "DIRIGIBLE_MONGODB_CLIENT_URI",
                "DIRIGIBLE_MESSAGING_BROKER_URL", "DIRIGIBLE_DATABASE_SYSTEM_PASSWORD", "DIRIGIBLE_KEYCLOAK_CLIENT_SECRET")) {
            assertTrue(key, SensitiveConfigs.isSensitive(key));
        }
        assertFalse(SensitiveConfigs.isSensitive("DIRIGIBLE_BRANDING_NAME"));
        assertNull(SensitiveConfigs.mask("DIRIGIBLE_MAIL_PASSWORD", null));
        assertEquals("x", SensitiveConfigs.mask("DIRIGIBLE_BRANDING_NAME", "x"));
    }

}
