/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A period expansion: a master entity's date span ({@link #between}) is expanded into generated
 * child rows - one per {@link #unit} (day / week / month) - e.g. a vacation request's day items or
 * a loan's monthly installments.
 *
 * <pre>
 * expansions:
 *   - name: installments
 *     from: Loan                                    # the span master
 *     into: LoanInstallment                         # the generated child (to-one back to Loan)
 *     unit: month                                   # day (default) | week | month
 *     between: { start: startDate, end: endDate }   # date fields of the master
 *     skipDays: [0, 6]                              # unit day only: weekdays skipped (0=Sun..6=Sat)
 *     map: { dueDate: period }                      # child date field <- the iterated period date
 *     defaults: { note: generated }                 # child field <- literal
 *     spread: { total: principal, into: amount, round: 2 }   # divide a master total across the rows
 *     count: periods                                # write the generated row count to a master field
 * </pre>
 *
 * The generator emits client-Java {@code MessageHandler}s on the master's create and update events
 * that (re)generate the child set through the child's repository - so the per-row create/delete
 * events fire and any roll-ups or capacity guards on the child run as for hand-entered rows. The
 * expansion OWNS the child set: a regeneration replaces every row pointing at the master. It is
 * idempotent (an unchanged span is detected and skipped), which also bounds the event cascade.
 */
public class ExpansionIntent {

    private String name;
    /** The span master entity. */
    private String from;
    /** The generated child entity - must have a to-one relation back to {@link #from}. */
    private String into;
    /** The period unit: {@code day} (default), {@code week} or {@code month}. */
    private String unit;
    /** The master's span: {@code start} / {@code end} date field names. */
    private Between between;
    /**
     * Weekdays skipped when {@link #unit} is {@code day} ({@code 0} = Sunday .. {@code 6} = Saturday).
     */
    private List<Integer> skipDays = new ArrayList<>();
    /**
     * Child field assignments per generated row. The only supported value is the token {@code period} -
     * the iterated period date - on a date field of the child.
     */
    private Map<String, String> map = new LinkedHashMap<>();
    /** Literal child field defaults per generated row. */
    private Map<String, Object> defaults = new LinkedHashMap<>();
    /** Optional: divide a master total across the generated rows (last row takes the remainder). */
    private Spread spread;
    /** Optional: a numeric master field the generated row count is written back to. */
    private String count;

    /** The master's span field names. */
    public static class Between {

        private String start;
        private String end;

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }

    /** A master total divided across the generated rows. */
    public static class Spread {

        /** The master's numeric field holding the total to divide. */
        private String total;
        /** The child's numeric field each share is written to. */
        private String into;
        /** Decimal places of each share (default 2); the last row absorbs the rounding remainder. */
        private Integer round;

        public String getTotal() {
            return total;
        }

        public void setTotal(String total) {
            this.total = total;
        }

        public String getInto() {
            return into;
        }

        public void setInto(String into) {
            this.into = into;
        }

        public Integer getRound() {
            return round;
        }

        public void setRound(Integer round) {
            this.round = round;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Between getBetween() {
        return between;
    }

    public void setBetween(Between between) {
        this.between = between;
    }

    public List<Integer> getSkipDays() {
        return skipDays;
    }

    public void setSkipDays(List<Integer> skipDays) {
        this.skipDays = skipDays == null ? new ArrayList<>() : skipDays;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map == null ? new LinkedHashMap<>() : map;
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults == null ? new LinkedHashMap<>() : defaults;
    }

    public Spread getSpread() {
        return spread;
    }

    public void setSpread(Spread spread) {
        this.spread = spread;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }
}
