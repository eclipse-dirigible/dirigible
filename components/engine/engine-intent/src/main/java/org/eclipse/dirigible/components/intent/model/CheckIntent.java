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

import java.util.List;

/**
 * A declarative validation on an {@link EntityIntent} - the cross-field / cross-line rules a plain
 * {@code required}/{@code unique} cannot express. Five kinds:
 * <ul>
 * <li>{@code exactlyOne} (row-level): exactly one of {@link #fields} is non-null on the record (a
 * journal line is either debit or credit) - enforced on every user write;</li>
 * <li>{@code compare} (row-level): {@link #field} compared to {@link #than} with {@link #op} - two
 * values of the SAME row that must stand in a relation to each other (a due date not before the
 * document date, a validity end not before its start) - enforced on every user write;</li>
 * <li>{@code requiredWhen}: {@link #field} - the record's own field, or a one-hop
 * {@code Relation.field} - must carry a value while {@link #when} holds (an e-mailed invoice needs
 * the customer's address). Enforced on every user write, or, with a {@link #status} gate, when the
 * document is persisted carrying that status - the moment the value is finally needed;</li>
 * <li>{@code forbidWhen}: the reject-twin of {@code requiredWhen} - reject the write while
 * {@link #when} holds (a payment allocation cannot be added to an already PAID invoice). It carries
 * no {@link #field}/value, only the condition; its one added reach is that a {@link #when} term may
 * name a one-hop {@code Relation.field}, so a child can test its parent (the status literal there
 * resolving against the relation target's nomenclature). Same gate rule as {@code requiredWhen}: no
 * gate = every user write (a 400 the generated controller raises), a gate = the repository when the
 * record is persisted carrying that status;</li>
 * <li>{@code itemsSumEqual} (document-level): the sums of the two {@link #over} fields across the
 * document's composition items are equal (the double-entry invariant) - enforced when the document
 * is persisted carrying the {@link #status} gate seed id, i.e. at the workflow transition;</li>
 * <li>{@code itemsMin} (document-level): the document has at least {@link #count} items - same
 * gate.</li>
 * </ul>
 */
public class CheckIntent {

    private String kind;
    /** {@code exactlyOne}: the record's own fields, exactly one of which must be non-null. */
    private List<String> fields;
    /**
     * The value the check is ABOUT - the two row-level kinds that name one share the key.
     * {@code compare}: the record's own field on the left of the comparison. {@code requiredWhen}: the
     * value that must be present - the record's own field, or a one-hop {@code Relation.field} over a
     * to-one (whose target may be owned by another model, as everywhere else a path is walked).
     */
    private String field;
    /**
     * {@code compare}: the comparison - {@code ge}, {@code gt}, {@code le}, {@code lt}, {@code eq} or
     * {@code ne}. Required: an omitted operator has no defensible default (a due date not BEFORE the
     * document date and one strictly AFTER it are different rules).
     */
    private String op;
    /** {@code compare}: the record's own field on the right of the comparison. */
    private String than;
    /** {@code itemsSumEqual}: the two numeric item fields whose sums must be equal. */
    private List<String> over;
    /** {@code itemsMin}: the minimum number of items. */
    private Integer count;
    /**
     * {@code requiredWhen} / {@code forbidWhen}: the condition - a {@code <Property> == <literal>} /
     * {@code != } comparison, or a list of them (an implicit AND). {@code requiredWhen} reads the
     * record's own properties; {@code forbidWhen} additionally accepts a one-hop {@code Relation.field}
     * (a child testing its parent). A status name resolves to its seed id, as in every other guard.
     */
    private Object when;
    /**
     * The {@code status} gate. On the document-level checks it is required; on {@code requiredWhen} /
     * {@code forbidWhen} it is optional and it is the routing: without one the check holds on every
     * user write (the generated controller), with one it runs in the repository when the record is
     * persisted carrying this status (the workflow transition into e.g. POSTED), so drafting
     * item-by-item stays unconstrained.
     */
    private Integer status;
    /** The user-facing message when the check fails. */
    private String message;
    /**
     * {@code guard}: the name of an {@code aggregates:} entry whose {@code of} is THIS entity. The
     * guard recomputes that keyed sum from the store for the incoming record's key-tuple (race-free,
     * unlike the async-maintained target) and blocks the write when the post-state would break
     * {@link #minimum} - the negative-stock / credit-limit precondition. v1: the guarded entity is the
     * aggregate source.
     */
    private String aggregate;
    /**
     * {@code guard}: the recomputed sum (prior rows + this row) must stay {@code >= minimum} (default
     * 0).
     */
    private java.math.BigDecimal minimum;
    /**
     * {@code guard} (optional): a configuration key gating the guard - it is enforced only when
     * {@code Configurations.get(enabledBy)} equals {@code "true"} (case-insensitive). Omitted = always
     * on.
     */
    private String enabledBy;
    /**
     * {@code guard}: what a violation does. {@code block} (the default) throws, so the write fails with
     * 4xx. {@code task} does NOT fail the write: it stamps {@link #marker} so the entity's process can
     * branch to a hold/review step (the credit-limit shape - the order is accepted but parked). {@code
     * reject} does not fail the write either: it forces the record's {@code function: EntityStatus} FK
     * to {@link #setStatus} (the leave-request shape - the request is filed, already rejected).
     */
    private String outcome;
    /**
     * {@code guard} with {@code outcome: task}: a boolean field of the entity set to {@code false} when
     * the guard is violated and {@code true} when it holds. It is the BRANCH INPUT a process
     * {@code decision} reads to route to the hold step - this keyword stamps the flag, the process
     * decides what to do with it.
     */
    private String marker;
    /**
     * {@code guard} with {@code outcome: reject}: the EntityStatus seed id forced onto the record when
     * the guard is violated.
     */
    private Integer setStatus;

    public String getKind() {
        return kind;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getWhen() {
        return when;
    }

    public void setWhen(Object when) {
        this.when = when;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public Integer getSetStatus() {
        return setStatus;
    }

    public void setSetStatus(Integer setStatus) {
        this.setStatus = setStatus;
    }

    public String getAggregate() {
        return aggregate;
    }

    public void setAggregate(String aggregate) {
        this.aggregate = aggregate;
    }

    public java.math.BigDecimal getMinimum() {
        return minimum;
    }

    public void setMinimum(java.math.BigDecimal minimum) {
        this.minimum = minimum;
    }

    public String getEnabledBy() {
        return enabledBy;
    }

    public void setEnabledBy(String enabledBy) {
        this.enabledBy = enabledBy;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getThan() {
        return than;
    }

    public void setThan(String than) {
        this.than = than;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getOver() {
        return over;
    }

    public void setOver(List<String> over) {
        this.over = over;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
