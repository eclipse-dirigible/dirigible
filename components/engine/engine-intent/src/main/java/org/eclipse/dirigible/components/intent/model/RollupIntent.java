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
 *
 * <p>
 * Either END may be owned by another model. A cross-model PARENT is named by the {@link #via}
 * relation's own {@code model:} alias (the child is local and owns the event). A cross-model CHILD
 * is named by this roll-up's {@link #model} alias, and then {@link #parent} must name the local
 * entity the total lands on - the child's relations are not in this document, so nothing can derive
 * it from {@link #via}. That is the direction an n:m allocation needs: the link rows are owned by
 * the module that owns one side of the pairing, while the OTHER side's total (a payment's allocated
 * amount) belongs to the module that owns the payment.
 */
public class RollupIntent {

    private String name;
    private String entity;
    /**
     * Optional {@code uses:} alias of the model that owns {@link #entity} - a CROSS-MODEL child. The
     * handler then binds the owner project's topic and reads the rows back through the owner's
     * generated repository; {@link #parent} is required alongside it.
     */
    private String model;
    /**
     * The local entity the total lands on. Required with {@link #model} (and only then): a foreign
     * child's relations are not in this document, so the parent cannot be derived from {@link #via}.
     */
    private String parent;
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
     * Whether the counted child is owned by another model (a {@code model:} alias is declared).
     *
     * @return true when the child is cross-model
     */
    public boolean isCrossModelChild() {
        return model != null && !model.isBlank();
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
