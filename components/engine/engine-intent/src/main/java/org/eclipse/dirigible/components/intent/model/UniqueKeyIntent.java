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
 * One term of a scheduled generation's natural key ({@code schedules[].generate.unique:}, issues
 * #7070 and #7106). Two shapes, exactly one per entry:
 *
 * <ul>
 * <li>a plain <b>property</b> of the target - {@code unique: [Project, period]} - whose value the
 * guard looks up with the very expression the target is about to be assigned from;
 * <li>the <b>period of the run</b> - {@code unique: [Supplier, { run: month }]} - which keys on
 * WHEN the tick fired rather than on a column holding that period.
 * </ul>
 *
 * <p>
 * Why the second shape exists (#7106): the recurring-template family - a monthly rent bill, a
 * quarterly retainer invoice - generates a plain document with a {@code date} and no period column
 * to name, and the source row has no back-reference on the target either, so #7070's property-only
 * key was simply not expressible there. `base-purchase-invoices`' {@code monthly-recurring-bills}
 * run twice on the same day created three more DRAFT invoices with no key to declare. A
 * {@code run:} term needs no storage at all: the target's own date property - the one this block
 * assigns from {@code now} - already carries the period, so the guard ranges over it
 * ({@code between(first day of the month, last day of the month)}) and a re-run on any day of the
 * same month finds the document the first tick wrote. That is why neither a hidden generated period
 * column nor a run ledger was added: both would be a schema for a value the document already holds,
 * and neither would work when the target is owned by another model.
 */
public class UniqueKeyIntent {

    /**
     * The TARGET property whose value identifies one tick's output - a field or a to-one relation, and
     * always one this same {@code generate} block assigns through {@code map} or {@code defaults}. The
     * shorthand string form of an entry ({@code unique: [Project]}) is normalized to this.
     */
    private String property;

    /**
     * The calendar period of the RUN this key is partitioned by: {@code day}, {@code week},
     * {@code month}, {@code quarter} or {@code year}. Mutually exclusive with {@link #property}.
     */
    private String run;

    /**
     * The target date property a {@link #run} term ranges over. Optional: it defaults to the single
     * {@code date} property this block assigns from {@code now}, and is only needed when the block
     * assigns more than one - naming it then is what keeps the guard's period and the document's own
     * date the same period.
     */
    private String of;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getRun() {
        return run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public String getOf() {
        return of;
    }

    public void setOf(String of) {
        this.of = of;
    }

    /** Whether this entry keys on the period of the run rather than on a property of the target. */
    public boolean isRun() {
        return run != null && !run.isBlank();
    }
}
