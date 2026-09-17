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
import org.eclipse.dirigible.components.intent.model.UniqueKeyIntent;
import org.junit.jupiter.api.Test;

/**
 * The parse-time half of a scheduled generation's natural key (issues #7070 and #7106):
 * {@code unique:} names the target properties that identify ONE tick's output - or, for a target
 * with no period column of its own, the calendar period of the run ({@code { run: month }}) - so a
 * second run of the job finds what the first one created instead of duplicating it. Every way the
 * key could be declared and still not guard anything is an authoring error, because at runtime both
 * directions of the mistake are silent - a key column nothing assigns is queried as null, which
 * matches either everything or nothing, and a period read off the wrong date keys the row into the
 * wrong period.
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
              - name: Bill
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: date,    type: date }
                  - { name: dueDate, type: date }
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
                                                        .getUnique()
                                                        .stream()
                                                        .map(UniqueKeyIntent::getProperty)
                                                        .toList());
    }

    @Test
    void thePeriodOfTheRunParses() {
        // Issue #7106: the recurring-template family has no period column to name - a monthly bill is a
        // plain document with a date - so the run's own calendar period is the key's second term, and
        // it ranges over the date this generate writes rather than over storage of its own.
        List<UniqueKeyIntent> key = IntentParser.parse(ENTITIES + """
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [Project, { run: month }]
                      map:
                        Project: id
                      defaults:
                        date: now
                """)
                                                .getSchedules()
                                                .get(0)
                                                .getGenerate()
                                                .getUnique();

        assertEquals("Project", key.get(0)
                                   .getProperty());
        assertTrue(key.get(1)
                      .isRun());
        assertEquals("month", key.get(1)
                                 .getRun());
    }

    @Test
    void aRunPeriodOverOneOfSeveralNowDatesNamesIt() {
        List<UniqueKeyIntent> key = IntentParser.parse(ENTITIES + """
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [Project, { run: quarter, of: date }]
                      map:
                        Project: id
                      defaults:
                        date: now
                        dueDate: now
                """)
                                                .getSchedules()
                                                .get(0)
                                                .getGenerate()
                                                .getUnique();

        assertEquals("quarter", key.get(1)
                                   .getRun());
        assertEquals("date", key.get(1)
                                .getOf());
    }

    @Test
    void aRunPeriodWithNoDateToRangeOverIsRefused() {
        // The period is not stored anywhere: it is read off the date the run writes. Without such a
        // date there is nothing to compare, and the guard would silently key on the properties alone.
        assertRejected("""
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [Project, { run: month }]
                      map:
                        Project: id
                """, "declares run [month] but this generate assigns no date property from now");
    }

    @Test
    void anAmbiguousRunPeriodDateIsRefused() {
        // Two dates written by the same run are two different periods to range over - the guard cannot
        // pick, and picking wrong is silent (a due date a month out keys the bill into the next month).
        assertRejected("""
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [Project, { run: month }]
                      map:
                        Project: id
                      defaults:
                        date: now
                        dueDate: now
                """, "assigns more than one date from now (date, dueDate) - name the one the period ranges over with of:");
    }

    @Test
    void aRunPeriodOverADateTheRunDoesNotWriteIsRefused() {
        assertRejected("""
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [Project, { run: month, of: dueDate }]
                      map:
                        Project: id
                      defaults:
                        date: now
                """, "generate unique run of [dueDate] is not a date property this generate assigns from now");
    }

    @Test
    void anUnknownRunPeriodIsRefused() {
        assertRejected("""
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [Project, { run: fortnight }]
                      map:
                        Project: id
                      defaults:
                        date: now
                """, "generate unique run [fortnight] is not a period - one of day, week, month, quarter, year");
    }

    @Test
    void aKeyThatIsOnlyTheRunPeriodIsRefused() {
        // One target per period for the WHOLE schedule: the first matching row generates and every
        // other row is skipped as if it had already run - the silent half of the duplicate this
        // feature removes.
        assertRejected("""
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [{ run: month }]
                      map:
                        Project: id
                      defaults:
                        date: now
                """, "generate unique declares only the run period");
    }

    @Test
    void twoRunPeriodsAreRefused() {
        assertRejected("""
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [Project, { run: month }, { run: year }]
                      map:
                        Project: id
                      defaults:
                        date: now
                """, "generate unique declares run more than once");
    }

    @Test
    void anEntryThatIsBothAPropertyAndARunPeriodIsRefused() {
        assertRejected("""
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: Project
                    generate:
                      to: Bill
                      unique: [{ property: Project, run: month }]
                      map:
                        Project: id
                      defaults:
                        date: now
                """, "names both the property [Project] and the run period [month] - one term per entry");
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
