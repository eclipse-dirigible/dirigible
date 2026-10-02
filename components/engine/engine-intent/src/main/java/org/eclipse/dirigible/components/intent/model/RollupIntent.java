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
     * {@code 0 < sum < capacity}. E.g. an invoice's {@code Status} → PAID / PARTIAL as payments
     * accumulate. The status the first such move displaces is remembered in a hidden parent column
     * ({@code Displaced<Status>}) and put back when the sum returns to zero, so an invoice whose only
     * allocation is deleted is CONFIRMED (or ISSUED) again rather than PAID with nothing paid; a status
     * the roll-up did not set itself (a manual void) is never touched.
     */
    private String status;
    /** Seed id set on {@link #status} when the sum reaches the capacity (fully consumed). */
    private Integer statusWhenFull;
    /** Seed id set on {@link #status} when the sum is positive but below the capacity. */
    private Integer statusWhenPartial;

    /**
     * Optional row filter: which of the child's rows this roll-up counts at all (issue #7542). Without
     * one a roll-up counts EVERY child, so a cancelled or voided document keeps consuming the parent's
     * capacity forever and its replacement can never be issued.
     *
     * <p>
     * The same {@code {field, op, value}} triples a {@code schedules[].where} and a create-from's
     * {@code items: where:} carry, over the CHILD's own fields and to-one relations, with a status
     * named by its seed name like every other status site. It narrows both the async recompute the
     * handlers run and the synchronous re-sum the {@link #capacity} guard runs - one authored
     * definition, so the stored balance and the enforced ceiling cannot disagree.
     */
    private List<ScheduleConditionIntent> where = new ArrayList<>();

    /**
     * Optional (requires {@link #capacity}): the status the capacity guard is enforced AT, as a seed
     * name or id (issue #7542).
     *
     * <p>
     * Without it the guard runs on every write of the child, which is right for a row that carries its
     * own typed amount (an allocation's) and useless for one whose amount is a DOCUMENT TOTAL: that is
     * recomputed from the lines after the header is written, so the guard only ever sees the 0 the
     * header was created with. Naming a status moves the check to the moment the document is persisted
     * carrying it - by which time its lines, and so its total, are in - and leaves a DRAFT free to
     * exceed the ceiling while it is still being edited. A gated write is on the synchronous path
     * (#7014 / #7063), so the refusal reaches whoever pressed the button.
     */
    private String guardAt;

    /**
     * Optional refusal message for the {@link #capacity} guard, with {@code {capacity}}, {@code {sum}},
     * {@code {requested}} and {@code {remaining}} placeholders (issue #7542). Without one the guard
     * reports the same figures in its own words.
     */
    private String message;

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

    public List<ScheduleConditionIntent> getWhere() {
        return where;
    }

    public void setWhere(List<ScheduleConditionIntent> where) {
        this.where = where == null ? new ArrayList<>() : where;
    }

    public String getGuardAt() {
        return guardAt;
    }

    public void setGuardAt(String guardAt) {
        this.guardAt = guardAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
