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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A declarative event-driven row post: on a document event, emit mapped rows into a target entity
 * (per line item), idempotently by a back-reference. Replaces the hand-written ledger-posting glue
 * (goods docs -> StockMovement, document -> journal line). See kf-catalog
 * PROPOSAL_EVENT_POSTING.md.
 *
 * <p>
 * Example (a goods receipt posting one signed movement per line on the POSTED transition):
 *
 * <pre>
 * posts:
 *   - name: goodsReceiptLedger
 *     forEntity: GoodsReceipt
 *     event: POSTED               # a document status value, or `create`
 *     forEach: items           # the composition-child collection (omit = one row per master)
 *     into: StockMovement      # the target entity (local or cross-model)
 *     idempotentBy: GoodsReceipt   # the back-reference FK on the target; skip if rows already exist
 *     guard: negativeStock     # optional precondition; a violation aborts the event
 *     set:
 *       Product:  item.Product     # item.&lt;field&gt;
 *       Store:    Receipt.Store     # &lt;Master&gt;.&lt;field&gt;
 *       Quantity: item.Quantity     # or a Calc expression (e.g. -item.Quantity)
 *       GoodsReceipt: Receipt.Id    # the back-reference (matches idempotentBy)
 * </pre>
 */
public class PostIntent {

    /** Stable name of this post rule (also the generated handler class stem). */
    private String name;

    /** The source document entity this post fires for. */
    private String forEntity;

    /**
     * The trigger EVENT: a document status value (e.g. {@code POSTED}) after that transition, or
     * {@code create}. (Named `event` not `on` - `on` is a YAML 1.1 boolean keyword.)
     */
    private String event;

    /**
     * The composition-child collection to iterate; {@code null} = emit one row per the source record.
     */
    private String forEach;

    /** The target entity rows are emitted into (local or cross-model). */
    private String into;

    /**
     * The back-reference FK on the target: the generated handler writes it and skips when target rows
     * already reference this source.
     */
    private String idempotentBy;

    /**
     * Optional named precondition evaluated before any row is written; a violation aborts the event.
     */
    private String guard;

    /**
     * Field map for each emitted row: target field -> value (a constant, {@code Master.field},
     * {@code item.field}, or a Calc expression).
     */
    private Map<String, String> set = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getForEntity() {
        return forEntity;
    }

    public void setForEntity(String forEntity) {
        this.forEntity = forEntity;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getForEach() {
        return forEach;
    }

    public void setForEach(String forEach) {
        this.forEach = forEach;
    }

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }

    public String getIdempotentBy() {
        return idempotentBy;
    }

    public void setIdempotentBy(String idempotentBy) {
        this.idempotentBy = idempotentBy;
    }

    public String getGuard() {
        return guard;
    }

    public void setGuard(String guard) {
        this.guard = guard;
    }

    public Map<String, String> getSet() {
        return set;
    }

    public void setSet(Map<String, String> set) {
        this.set = set == null ? new LinkedHashMap<>() : set;
    }
}
