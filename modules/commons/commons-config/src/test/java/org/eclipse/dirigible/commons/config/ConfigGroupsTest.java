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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ConfigGroupsTest {

    /** A new catalogued key must land in a group, or the view shows it under "Other". */
    @Test
    public void everyCataloguedKeyHasAGroup() {
        List<String> ungrouped = Arrays.stream(Configuration.getConfigurationParameters())
                                       .filter(key -> ConfigGroups.resolve(key) == ConfigGroup.OTHER)
                                       .sorted()
                                       .toList();
        assertTrue("Keys without a group (add a prefix rule to ConfigGroups): " + ungrouped, ungrouped.isEmpty());
    }

    /** An explicit catalogue group must name a known group, or it would be silently ignored. */
    @Test
    public void everyExplicitGroupIsKnownAndWins() {
        for (DirigibleConfig entry : DirigibleConfig.values()) {
            if (entry.getGroup() != null) {
                assertEquals(entry.getKey(), ConfigGroup.fromId(entry.getGroup())
                                                        .orElseThrow(),
                        ConfigGroups.resolve(entry.getKey()));
            }
        }
    }

    @Test
    public void specificPrefixesWinOverGeneralOnes() {
        assertEquals(ConfigGroup.MAIL, ConfigGroups.resolve("DIRIGIBLE_FLOWABLE_MAIL_SERVER_HOST"));
        assertEquals(ConfigGroups.SUBGROUP_PROCESS, ConfigGroups.subgroup("DIRIGIBLE_FLOWABLE_MAIL_SERVER_HOST"));
        assertEquals(ConfigGroup.BPM, ConfigGroups.resolve("DIRIGIBLE_FLOWABLE_DATABASE_URL"));
        assertEquals(ConfigGroup.DEVTOOLS, ConfigGroups.resolve("DIRIGIBLE_JAVASCRIPT_GRAALVM_DEBUGGER_PORT"));
        assertEquals(ConfigGroup.RUNTIMES, ConfigGroups.resolve("DIRIGIBLE_JAVASCRIPT_HANDLER_CLASS_NAME"));
        assertEquals(ConfigGroup.WEB, ConfigGroups.resolve("DIRIGIBLE_SECURITY_FRAME_OPTIONS"));
        assertEquals(ConfigGroup.AUTH, ConfigGroups.resolve("DIRIGIBLE_SECURITY_LOGIN_PAGE"));
        assertEquals(ConfigGroup.DOCUMENTS, ConfigGroups.resolve("DIRIGIBLE_CMS_DATABASE_DATASOURCE_NAME"));
        assertEquals(ConfigGroup.JOBS, ConfigGroups.resolve("DIRIGIBLE_SCHEDULER_DATABASE_URL"));
        assertEquals(ConfigGroup.TENANCY, ConfigGroups.resolve("DIRIGIBLE_APP_ID"));
        assertEquals(ConfigGroup.INSTANCE, ConfigGroups.resolve("DIRIGIBLE_APP_BASE_URL"));
    }

    @Test
    public void thePrefixIsOptionalAndUnknownKeysFallToOther() {
        assertEquals(ConfigGroup.DATABASE, ConfigGroups.resolve("SNOWFLAKE_DEFAULT_TABLE_TYPE"));
        assertEquals(ConfigGroup.OTHER, ConfigGroups.resolve("DIRIGIBLE_NO_SUCH_THING"));
        assertEquals(ConfigGroup.OTHER, ConfigGroups.resolve(null));
        assertNull(ConfigGroups.subgroup("DIRIGIBLE_MAIL_USERNAME"));
    }

    @Test
    public void groupIdsRoundTrip() {
        for (ConfigGroup group : ConfigGroup.values()) {
            assertEquals(group, ConfigGroup.fromId(group.getId())
                                           .orElseThrow());
        }
        assertTrue(ConfigGroup.fromId("nope")
                              .isEmpty());
    }

}
