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

import java.text.ParseException;

import org.quartz.CronExpression;

/**
 * The cron an intent schedule (or a polled inbound folder) declares, in the form the platform's
 * Quartz scheduler can actually run (issue #8235).
 *
 * <p>
 * The generated {@code JobHandler.cron()} is handed to Quartz unchanged, and Quartz differs from
 * the Spring-style six-field cron an author naturally writes in ONE respect: it cannot constrain
 * day-of-month and day-of-week at once, so exactly one of the two must be {@code ?} - and
 * {@code "0 0 4 1 * *"}, the first of the month at 04:00, is rejected as "both set". Generate
 * reported success, Problems stayed empty, {@code /services/jobs} listed the job as enabled, and it
 * never fired: only a WARN in the log on every reconciliation pass said why.
 *
 * <p>
 * Two halves close that. {@link #toQuartz(String)} translates the common authoring form: a day
 * field left at {@code *} while the other day field carries the schedule means "no constraint
 * here", which is exactly what Quartz spells {@code ?}; both at {@code *} is every day, spelled
 * {@code * * ?} in Quartz. The guide's own examples ({@code "0 0 8 * * *"}, "Spring cron") keep
 * working, and the generated job carries the Quartz form. {@link #problem(String)} then validates
 * the translated expression with Quartz's own parser, so an expression it still cannot schedule -
 * both day fields genuinely constrained, a five-field Unix cron, a mistyped token - is refused at
 * parse, naming the schedule, instead of becoming a job that is listed and never runs.
 */
public final class CronSupport {

    private static final int DAY_OF_MONTH = 3;
    private static final int DAY_OF_WEEK = 5;

    private CronSupport() {}

    /**
     * The declared cron in Quartz form: a day-of-month / day-of-week pair where one side is {@code *}
     * and the other carries the constraint has the {@code *} side rewritten to {@code ?}; both at
     * {@code *} has day-of-week rewritten to {@code ?}. Everything else - the other fields, an
     * expression that is not six or seven fields, a day pair that already uses {@code ?} - is kept as
     * written (a six- or seven-field expression is re-joined with single spaces), so the validation
     * that follows reports the author's own tokens.
     *
     * @param cron the declared cron expression
     * @return the expression Quartz schedules, or the input when there is nothing to translate
     */
    public static String toQuartz(String cron) {
        if (cron == null) {
            return null;
        }
        String[] fields = cron.trim()
                              .split("\\s+");
        if (fields.length != 6 && fields.length != 7) {
            return cron;
        }
        String dayOfMonth = fields[DAY_OF_MONTH];
        String dayOfWeek = fields[DAY_OF_WEEK];
        boolean monthAny = "*".equals(dayOfMonth);
        boolean weekAny = "*".equals(dayOfWeek);
        boolean monthUnset = "?".equals(dayOfMonth);
        boolean weekUnset = "?".equals(dayOfWeek);
        if (monthAny && weekAny) {
            fields[DAY_OF_WEEK] = "?";
        } else if (monthAny && !weekUnset) {
            fields[DAY_OF_MONTH] = "?";
        } else if (weekAny && !monthUnset) {
            fields[DAY_OF_WEEK] = "?";
        }
        return String.join(" ", fields);
    }

    /**
     * Why Quartz cannot schedule the declared cron, or {@code null} when it can. Checked on the
     * {@link #toQuartz(String) translated} form, so the Spring-style pair is not reported; what is
     * reported is what the translation cannot repair - both day fields constrained, a wrong field
     * count, an invalid token - in Quartz's own words.
     *
     * @param cron the declared cron expression
     * @return the reason it cannot be scheduled, or {@code null} when it can
     */
    public static String problem(String cron) {
        if (cron == null || cron.isBlank()) {
            return "it is empty";
        }
        String quartz = toQuartz(cron);
        try {
            new CronExpression(quartz);
            return null;
        } catch (ParseException e) {
            String reason = e.getMessage();
            if (reason != null && reason.contains("day-of-week AND a day-of-month")) {
                return "day-of-month and day-of-week cannot both be constrained; leave one of them at * (which is emitted as ?)";
            }
            return reason == null || reason.isBlank() ? "it is not a valid Quartz cron expression" : reason;
        }
    }
}
