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
import java.util.List;

/**
 * A keyed cross-entity aggregate: a running sum/count of a source entity's field, grouped by one or
 * more of its to-one relations, materialised into a SEPARATE target entity keyed by that group (one
 * target row per key-tuple). The two-key balance-rollup the ledger on-hand needs (CLAUDE.md §13) -
 * a generalisation of {@link RollupIntent} (which is single-key, child -> composition parent). See
 * the driving suite's PROPOSAL_AGGREGATE_CHECKS.md.
 *
 * <p>
 * Example - live on-hand per product+store from the signed stock ledger:
 *
 * <pre>
 * aggregates:
 *   - name: onHand
 *     of: StockMovement        # the source rows
 *     op: sum                  # sum (default) | count
 *     sum: quantity            # the source field summed (signed)
 *     by: [Product, Store]     # the grouping keys (the source's to-one relations)
 *     into: ProductAvailability # the materialised target, keyed by the same relations
 *     field: onHand            # the target field holding the sum
 * </pre>
 */
public class AggregateIntent {

    /** Stable name of this aggregate (also the generated handler class stem). */
    private String name;

    /** The source entity whose rows are aggregated. */
    private String of;

    /** The aggregation: {@code sum} (default) or {@code count}. */
    private String op;

    /** The source field summed (required for {@code op: sum}; ignored for {@code count}). */
    private String sum;

    /** The grouping keys - the source's to-one relation names; one target row per distinct tuple. */
    private List<String> by = new ArrayList<>();

    /** The target entity the aggregate is materialised into (keyed by {@link #by}). */
    private String into;

    /** The target field that holds the running aggregate value. */
    private String field;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOf() {
        return of;
    }

    public void setOf(String of) {
        this.of = of;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public List<String> getBy() {
        return by;
    }

    public void setBy(List<String> by) {
        this.by = by == null ? new ArrayList<>() : by;
    }

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
