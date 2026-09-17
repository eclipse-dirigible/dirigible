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

/**
 * Marks an entity as a PERIOD REGISTER: its rows are the dated windows other entities are locked
 * by, and each row's status says whether its window is still open.
 *
 * <p>
 * A fiscal period is an ordinary entity - a row with a start date, an end date and a lifecycle - so
 * nothing here creates one. What the DSL cannot derive is which of the entity's fields are the two
 * bounds and which statuses mean CLOSED, and this declares exactly that, once, where the period
 * lives. Every document guarded by it then names it in a single line
 * ({@link EntityIntent#getImmutableInPeriod()}).
 *
 * <pre>
 * entities:
 *   - name: AccountingPeriod
 *     period:
 *       start: startDate
 *       end: endDate                     # inclusive - a date-only bound covers its whole day
 *       closedWhen: "Status == CLOSED"   # same grammar as immutableWhen; seeded names or ids
 * </pre>
 *
 * <p>
 * Closing a period is a plain status transition, so it is authored with the machinery that already
 * exists - a {@code transitions:} button, a {@code lifecycle:} edge, a workflow step. The register
 * is only ever READ here.
 */
public class PeriodIntent {

    /** The period entity's own {@code date} field holding the first day of the window. */
    private String start;

    /** The period entity's own {@code date} field holding the last day of the window (inclusive). */
    private String end;

    /**
     * When a period row is CLOSED: a boolean expression over the period entity's
     * {@code function: EntityStatus} relation - terms {@code <Status> == <seed id>} joined with
     * {@code ||}, seeded names accepted - exactly the {@code immutableWhen} grammar.
     */
    private String closedWhen;

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

    public String getClosedWhen() {
        return closedWhen;
    }

    public void setClosedWhen(String closedWhen) {
        this.closedWhen = closedWhen;
    }
}
