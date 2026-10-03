/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The {@code userTasks.candidateGroupsExtra} default contract: a settings document that predates
 * the section (or omits it) keeps the documented ADMINISTRATOR default, while an explicit block -
 * including an explicitly empty list, a deliberate opt-out - means exactly what it says. Without
 * the on-load default, every pre-existing {@code .settings} silently emitted single-group tasks
 * invisible to administrators (GH-6338).
 */
class IntentSettingsTest {

    @Test
    void aSettingsFileWithoutTheUserTasksSectionKeepsTheAdministratorDefault() {
        IntentSettings settings = IntentSettings.parse("""
                {
                  "generation": {},
                  "overrides": {}
                }
                """);
        assertEquals(List.of("ADMINISTRATOR"), settings.candidateGroupsExtra());
    }

    @Test
    void anEmptyDocumentKeepsTheAdministratorDefault() {
        assertEquals(List.of("ADMINISTRATOR"), IntentSettings.parse("{}")
                                                             .candidateGroupsExtra());
    }

    @Test
    void aNullUserTasksSectionCountsAsAbsent() {
        assertEquals(List.of("ADMINISTRATOR"), IntentSettings.parse("{\"userTasks\": null}")
                                                             .candidateGroupsExtra());
    }

    @Test
    void anExplicitlyEmptyListIsADeliberateOptOut() {
        IntentSettings settings = IntentSettings.parse("""
                {
                  "userTasks": { "candidateGroupsExtra": [] }
                }
                """);
        assertTrue(settings.candidateGroupsExtra()
                           .isEmpty());
    }

    @Test
    void anExplicitListIsKeptVerbatim() {
        IntentSettings settings = IntentSettings.parse("""
                {
                  "userTasks": { "candidateGroupsExtra": ["Supervisor"] }
                }
                """);
        assertEquals(List.of("Supervisor"), settings.candidateGroupsExtra());
    }

    @Test
    void theAccessExtraRolesAreReadTrimmedAndWithoutBlanksOrRepeats() {
        IntentSettings settings = IntentSettings.parse("""
                {
                  "access": { "extraRoles": ["Owner", " User ", "", "Owner"], "extraRolesReadOnly": ["Auditor"] }
                }
                """);
        assertEquals(List.of("Owner", "User"), settings.extraRoles());
        assertEquals(List.of("Auditor"), settings.extraRolesReadOnly());
    }

    @Test
    void noAccessExtraRolesUnlessTheSettingsDeclareThem() {
        assertTrue(IntentSettings.parse("{}")
                                 .extraRoles()
                                 .isEmpty());
        assertTrue(IntentSettings.parse("{\"access\": {\"generate\": true}}")
                                 .extraRolesReadOnly()
                                 .isEmpty());
    }
}
