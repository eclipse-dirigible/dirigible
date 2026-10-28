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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;
import org.quartz.CronExpression;

/**
 * A schedule's cron is refused at parse when the platform's Quartz scheduler cannot run it, and the
 * generated job carries the Quartz form of the one it can (#8235).
 *
 * <p>
 * Before: {@code "0 0 4 1 * *"} - the Spring-style first-of-the-month - parsed, generated and
 * published clean, was listed as an enabled job, and never fired, because Quartz refuses a cron
 * with day-of-month AND day-of-week set.
 */
class ScheduleCronIntentTest {

    private static String model(String scheduleCron, String folderCron) {
        return """
                name: billing
                entities:
                  - name: Bill
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string }
                      - { name: note, type: string }
                schedules:
                  - name: monthly-bills
                    cron: "%s"
                    entity: Bill
                    notify:
                      to: email
                      subject: "Bill {note}"
                      body: "still open"
                inbound:
                  - { name: billDrop, source: { folder: /data/inbox/bills, cron: "%s" }, create: Bill }
                """.formatted(scheduleCron, folderCron);
    }

    private static Map<String, Object> schedule(IntentModel model) {
        List<Map<String, Object>> schedules = GlueIntentGenerator.buildSchedulesForTest(model);
        assertEquals(1, schedules.size());
        return schedules.get(0);
    }

    @Test
    void theSpringStyleDayPairIsAcceptedAndEmittedInQuartzForm() {
        IntentModel model = IntentParser.parse(model("0 0 4 1 * *", "0 */5 * * * *"));

        assertEquals("0 0 4 1 * ?", schedule(model).get("cron"), "the generated cron() is handed to Quartz unchanged");
        assertEquals("0 */5 * * * ?", GlueIntentGenerator.buildInboundFilesForTest(model)
                                                         .get(0)
                                                         .get("cron"),
                "a polled folder's cron takes the same translation");
        assertTrue(CronExpression.isValidExpression((String) schedule(model).get("cron")));
    }

    @Test
    void aWeekdayScheduleDropsTheDayOfMonthStar() {
        IntentModel model = IntentParser.parse(model("0 0 8 * * MON", "0 0 1 * * ?"));

        assertEquals("0 0 8 ? * MON", schedule(model).get("cron"));
    }

    @Test
    void aQuartzCronIsEmittedAsWritten() {
        IntentModel model = IntentParser.parse(model("0 0 1 1 * ?", "0 0 1 * * ?"));

        assertEquals("0 0 1 1 * ?", schedule(model).get("cron"));
    }

    @Test
    void bothDayFieldsConstrainedIsRefusedNamingTheSchedule() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(model("0 0 4 1 * MON", "0 0 1 * * ?")));

        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.startsWith("schedule [monthly-bills] cron [0 0 4 1 * MON] cannot be scheduled:")
                             && issue.contains("day-of-month and day-of-week cannot both be constrained")),
                ex.getIssues()
                  .toString());
    }

    @Test
    void aUnixCronIsRefusedNamingTheSchedule() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(model("0 4 1 * *", "0 0 1 * * ?")));

        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.startsWith("schedule [monthly-bills] cron [0 4 1 * *] cannot be scheduled:")),
                ex.getIssues()
                  .toString());
    }

    @Test
    void aPolledFolderCronIsHeldToTheSameRule() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(model("0 0 4 1 * ?", "every five minutes")));

        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.startsWith("inbound [billDrop] source cron [every five minutes] cannot be scheduled:")),
                ex.getIssues()
                  .toString());
    }
}
