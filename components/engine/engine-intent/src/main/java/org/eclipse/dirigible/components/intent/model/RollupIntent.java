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
    /** The aggregation: {@code count} (default), {@code sum}, or {@code latest}. */
    private String op;
    /**
     * The child field aggregated onto {@link #field}: summed when {@link #op} is {@code sum}, or copied
     * from the most-recent child row when {@link #op} is {@code latest}.
     */
    private String of;
    /**
     * Required for {@code op: latest}: the child date/timestamp field that orders the rows; the
     * {@link #of} value of the row with the greatest {@code by} is copied onto the parent
     * {@link #field} (the "keep the parent's rate equal to the latest child rate" shape).
     */
    private String by;
    /**
     * Optional (sum roll-ups only): a numeric "capacity" field on the parent the sum is measured
     * against - e.g. an invoice's {@code total} against which the paid sum is compared. Enables
     * {@link #balance} and {@link #status} derivation.
     */
    private String capacity;
    /**
     * Optional (sum roll-ups only, requires {@link #capacity}): a parent field kept equal to
     * {@code capacity - sum} (e.g. an invoice's outstanding {@code balance}).
     */
    private String balance;
    /**
     * Optional (sum roll-ups only, requires {@link #capacity}): a parent to-one relation set to
     * {@link #statusWhenFull} when {@code sum >= capacity}, or {@link #statusWhenPartial} when
     * {@code 0 < sum < capacity} (left unchanged at zero). E.g. an invoice's {@code Status} → PAID /
     * PARTIAL as payments accumulate.
     */
    private String status;
    /** Seed id set on {@link #status} when the sum reaches the capacity (fully consumed). */
    private Integer statusWhenFull;
    /** Seed id set on {@link #status} when the sum is positive but below the capacity. */
    private Integer statusWhenPartial;

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

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
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

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusWhenFull() {
        return statusWhenFull;
    }

    public void setStatusWhenFull(Integer statusWhenFull) {
        this.statusWhenFull = statusWhenFull;
    }

    public Integer getStatusWhenPartial() {
        return statusWhenPartial;
    }

    public void setStatusWhenPartial(Integer statusWhenPartial) {
        this.statusWhenPartial = statusWhenPartial;
    }
}
