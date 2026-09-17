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
 * Date-based immutability: while the period covering one of this record's dates is closed, the
 * record is read-only for USER writes.
 *
 * <p>
 * {@code immutableWhen} guards a record by its OWN status - a posted journal entry stops being
 * editable because of what it is. Period locking guards it by WHEN it falls: once the accountant
 * closes March, nothing dated in March may be created, edited or deleted any more, whatever status
 * it carries. The two compose; an entity may declare either, both, or neither.
 *
 * <pre>
 * entities:
 *   - name: JournalEntry
 *     immutableInPeriod: { period: AccountingPeriod, date: entryDate }
 * </pre>
 *
 * <p>
 * As for {@code immutableWhen}, this guards the USER surface only: the generated controllers answer
 * 409, and workflow / system writes through the repository stay possible, because a correction to a
 * closed period is a reversal booked in an open one, never an edit of the original.
 */
public class PeriodLockIntent {

    /**
     * The period register this record is locked by: an entity of THIS model declaring
     * {@link PeriodIntent}.
     */
    private String period;

    /**
     * The {@code date} field of this record whose value decides which period it falls in - the tax
     * event date, the entry date, the date the document takes effect. A record whose value is unset
     * falls in no period and stays writable.
     */
    private String date;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
