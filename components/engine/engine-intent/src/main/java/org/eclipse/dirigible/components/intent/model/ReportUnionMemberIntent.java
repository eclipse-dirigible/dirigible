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
 * One member of a report's {@code union:} (dirigible #7946) - an entity whose rows join the
 * report's rows, each mapped onto the same logical columns:
 *
 * <pre>
 * union:
 *   - { source: SalesInvoice, counterparty: customer, date: date, due: due, amount: "payable - paid" }
 *   - { source: CreditNote,   counterparty: customer, date: date, due: date, amount: "allocated - total" }
 *   - { source: OpeningBalance, counterparty: customer, date: documentDate, due: dueDate,
 *       amount: "debit - credit", filter: "customer IS NOT NULL" }
 * </pre>
 *
 * <p>
 * The report then reads {@code source} (the member's entity name), {@code counterparty},
 * {@code date}, {@code due} and {@code amount} as if they were the fields of one entity - as
 * dimensions, measures, parameters and as the {@code aging:} block's inputs.
 */
public class ReportUnionMemberIntent {

    /** The member entity. */
    private String source;
    /** The {@code uses:} alias of the model owning {@link #source}, when it is not this model's. */
    private String model;
    /**
     * The member's to-one relation the report reads as {@code counterparty}. Every member that names
     * one must point at the same entity.
     */
    private String counterparty;
    /** The member's document date, read as {@code date}. */
    private String date;
    /** The member's due date, read as {@code due}. */
    private String due;
    /**
     * The member's signed amount, read as {@code amount}: one numeric field, or numeric fields joined
     * by {@code +} and {@code -}, optionally with a leading sign. A credit note subtracts by writing
     * its fields the other way round ({@code allocated - total}) or negated ({@code -total}).
     */
    private String amount;
    /** An optional predicate over the member's own fields, narrowing the rows it contributes. */
    private String filter;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /** Whether the member entity is owned by another model. */
    public boolean isCrossModel() {
        return model != null && !model.isBlank();
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDue() {
        return due;
    }

    public void setDue(String due) {
        this.due = due;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
