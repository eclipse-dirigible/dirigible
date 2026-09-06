/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * The parse-time half of a scheduled generation's natural key (issue #7070): {@code unique:} names
 * the target properties that identify ONE tick's output, so a second run of the job finds what the
 * first one created instead of duplicating it. Every way the key could be declared and still not
 * guard anything is an authoring error, because at runtime both directions of the mistake are
 * silent - a key column nothing assigns is queried as null, which matches either everything or
 * nothing.
 */
class ScheduleGenerateUniqueIntentTest {

    private static final String ENTITIES = """
            name: timesheets
            entities:
              - name: Project
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: status, type: string }
              - name: ProjectTimesheet
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: period, type: month }
                relations:
                  - { name: Project, kind: manyToOne, to: Project }
            """;

    @Test
    void theNaturalKeyParses() {
        IntentModel model = IntentParser.parse(ENTITIES + """
                schedules:
                  - name: monthly-project-timesheets
                    cron: "0 0 2 1 * ?"
                    entity: Project
                    generate:
                      to: ProjectTimesheet
                      unique: [Project, period]
                      map:
                        Project: id
                      defaults:
                        Period: now
                """);

        assertEquals(List.of("Project", "period"), model.getSchedules()
                                                        .get(0)
                                                        .getGenerate()
                                                        .getUnique());
    }

    @Test
    void aKeyTheGenerationNeverAssignsIsRefused() {
        // The guard queries the target by the values it is about to write. A column outside map /
        // defaults is queried as null, so the schedule either never generates again or generates a
        // duplicate every tick - and neither says so.
        assertRejected("""
                schedules:
                  - name: monthly-project-timesheets
                    cron: "0 0 2 1 * ?"
                    entity: Project
                    generate:
                      to: ProjectTimesheet
                      unique: [period]
                      map:
                        Project: id
                """, "generate unique [period] is not assigned by this generate's map or defaults");
    }

    @Test
    void aKeyThatIsNotAPropertyOfTheTargetIsRefused() {
        assertRejected("""
                schedules:
                  - name: monthly-project-timesheets
                    cron: "0 0 2 1 * ?"
                    entity: Project
                    generate:
                      to: ProjectTimesheet
                      unique: [quarter]
                      map:
                        Project: id
                      defaults:
                        quarter: now
                """, "generate unique [quarter] is not a field or to-one relation of [ProjectTimesheet]");
    }

    @Test
    void aRepeatedKeyEntryIsRefused() {
        assertRejected("""
                schedules:
                  - name: monthly-project-timesheets
                    cron: "0 0 2 1 * ?"
                    entity: Project
                    generate:
                      to: ProjectTimesheet
                      unique: [Project, project]
                      map:
                        Project: id
                """, "generate unique repeats [project]");
    }

    @Test
    void anOnDemandCreateFromKeepsItsEventModeAsItsCardinality() {
        // Two differently-shaped guards on one create-from would leave two answers to "may this run
        // again"; the on-demand one's answer is `mode: once` guarded by the back-reference.
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(ENTITIES + """
                generates:
                  - name: timesheetFromProject
                    from: Project
                    to: ProjectTimesheet
                    unique: [Project]
                    map:
                      Project: id
                """));
        assertTrue(failure.getMessage()
                          .contains("declares unique - the natural key is a scheduled generation's idempotency guard"),
                failure.getMessage());
    }

    private static void assertRejected(String schedules, String expected) {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(ENTITIES + schedules));
        assertTrue(failure.getMessage()
                          .contains(expected),
                failure.getMessage());
    }
}
