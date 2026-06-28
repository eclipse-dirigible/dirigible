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
 * A denormalized roll-up: maintain a {@link #field} on a parent entity derived from the
 * {@link #entity} (child) rows pointing at it through the {@link #via} to-one relation.
 *
 * <p>
 * {@link #op} selects the aggregation: {@code count} (the default) keeps an integer count of child
 * rows, while {@code sum} keeps a decimal sum of the child rows' {@link #of} field on the parent's
 * {@link #field}. Sum roll-ups are how a document header's totals (e.g. {@code Net} / {@code Vat} /
 * {@code Total}) stay equal to the sum of their line items by name convention.
 *
 * <p>
 * The generator emits client-Java {@code MessageHandler}s on the child's lifecycle events (create +
 * delete for a count; create + update + delete for a sum, since editing a row changes the sum) that
 * recompute the value for the affected parent and write it back. It is recomputed from the store on
 * each event (not blindly incremented), so it self-heals; under high write concurrency it is
 * eventually consistent rather than transactionally exact.
 */
public class RollupIntent {

    private String name;
    private String entity;
    private String via;
    private String field;
    /** The aggregation: {@code count} (default) or {@code sum}. */
    private String op;
    /** The child field summed onto {@link #field} when {@link #op} is {@code sum}. */
    private String of;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getOf() {
        return of;
    }

    public void setOf(String of) {
        this.of = of;
    }
}
