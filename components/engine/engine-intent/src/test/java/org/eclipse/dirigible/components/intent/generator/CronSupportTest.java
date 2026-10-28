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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.quartz.CronExpression;

/**
 * The declared cron reaches Quartz in a form it can run (#8235): the Spring-style day pair is
 * translated, and what the translation cannot repair is reported.
 */
class CronSupportTest {

    @Test
    void bothDayFieldsUnconstrainedBecomesQuartzEveryDay() {
        assertEquals("0 0 8 * * ?", CronSupport.toQuartz("0 0 8 * * *"));
        assertTrue(CronExpression.isValidExpression(CronSupport.toQuartz("0 0 8 * * *")));
    }

    @Test
    void aDayOfMonthScheduleDropsTheDayOfWeekStar() {
        assertEquals("0 0 4 1 * ?", CronSupport.toQuartz("0 0 4 1 * *"));
        assertEquals("0 0 7 1 1 ?", CronSupport.toQuartz("0 0 7 1 1 *"));
        assertEquals("0 0 5 1 1,4,7,10 ?", CronSupport.toQuartz("0 0 5 1 1,4,7,10 *"));
    }

    @Test
    void aDayOfWeekScheduleDropsTheDayOfMonthStar() {
        assertEquals("0 0 8 ? * MON", CronSupport.toQuartz("0 0 8 * * MON"));
        assertEquals("0 0 8 ? * MON-FRI 2027", CronSupport.toQuartz("0 0 8 * * MON-FRI 2027"));
    }

    @Test
    void aQuartzExpressionIsLeftAsWritten() {
        assertEquals("0 0 4 1 * ?", CronSupport.toQuartz("0 0 4 1 * ?"));
        assertEquals("0 0 8 ? * MON", CronSupport.toQuartz("0 0 8 ? * MON"));
        assertEquals("0/5 * * * * ?", CronSupport.toQuartz("0/5 * * * * ?"));
        assertEquals("0 0 4 1 * ?", CronSupport.toQuartz("  0 0 4   1 * ?  "), "only the spacing is normalized");
    }

    @Test
    void whatCannotBeTranslatedIsReturnedForTheReport() {
        assertEquals("0 4 1 * *", CronSupport.toQuartz("0 4 1 * *"), "a five-field Unix cron has no Quartz reading");
        assertEquals("0 0 4 1 * MON", CronSupport.toQuartz("0 0 4 1 * MON"), "both day fields constrained");
        assertNull(CronSupport.toQuartz(null));
    }

    @Test
    void aRunnableCronHasNoProblem() {
        assertNull(CronSupport.problem("0 0 4 1 * *"));
        assertNull(CronSupport.problem("0 0 8 * * MON"));
        assertNull(CronSupport.problem("0 0 9 * * ?"));
        assertNull(CronSupport.problem("0 */5 * * * ?"));
    }

    @Test
    void bothDayFieldsConstrainedIsReportedInTheAuthorsTerms() {
        String problem = CronSupport.problem("0 0 4 1 * MON");
        assertNotNull(problem);
        assertTrue(problem.contains("day-of-month and day-of-week cannot both be constrained"), problem);
    }

    @Test
    void aUnixCronAndAnInvalidTokenAreReported() {
        assertNotNull(CronSupport.problem("0 4 1 * *"), "five fields");
        assertNotNull(CronSupport.problem("0 0 25 * * ?"), "hour 25");
        assertNotNull(CronSupport.problem("every monday"), "not a cron at all");
        assertNotNull(CronSupport.problem(""), "empty");
        assertNotNull(CronSupport.problem(null), "null");
    }
}
