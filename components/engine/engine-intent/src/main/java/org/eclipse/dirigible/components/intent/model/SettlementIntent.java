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
 * An auto-settlement (payment allocation) between two entities linked by a junction carrying an
 * amount - e.g. allocate a {@code CustomerPayment} across a customer's open {@code SalesInvoice}s
 * via the {@code SalesInvoiceCustomerPayment} junction, oldest first.
 *
 * <p>
 * Generates two client-Java glue classes: an {@code onPayment} {@code MessageHandler} (on the
 * payment's create event, spreads the payment across open invoices) and an {@code onInvoice}
 * {@code JavaDelegate} (wired as a {@code delegate:} service task after the invoice becomes
 * payable, pulls the customer's unallocated payment balance onto it). Allocation for both
 * directions is bounded by the invoice open amount ({@link #total} - {@link #paid}) and the payment
 * pot ({@link #pot} minus what it has already allocated), and restricted to rows agreeing on every
 * {@link #match} relation.
 */
public class SettlementIntent {

    private String name;
    /**
     * The junction entity (carries a FK to the invoice, a FK to the payment, and the amount payload).
     */
    private String junction;
    /** The "open receivable" entity (e.g. SalesInvoice). */
    private String invoice;
    /** The "pot" entity, typically cross-model (e.g. CustomerPayment). */
    private String payment;
    /** The junction's amount payload field (the allocated slice). */
    private String amount;
    /** The invoice capacity field (e.g. total); open = total - paid. */
    private String total;
    /** The invoice consumed field (e.g. paid); open = total - paid. */
    private String paid;
    /** The payment pot field (e.g. amount); unallocated = pot - already-allocated. */
    private String pot;
    /** The field both sides are ordered by, oldest first (e.g. date). */
    private String order;
    /**
     * Shared to-one relations both the invoice and the payment must agree on (e.g. Customer, Currency).
     */
    private List<String> match = new ArrayList<>();
    /** The invoice's status relation (e.g. Status) checked against {@link #payableStatuses}. */
    private String status;
    /**
     * Invoice status seed ids that make it payable (e.g. ISSUED / SENT / PARTIAL); others are skipped.
     */
    private List<Integer> payableStatuses = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJunction() {
        return junction;
    }

    public void setJunction(String junction) {
        this.junction = junction;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }

    public String getPayment() {
        return payment;
    }

    public void setPayment(String payment) {
        this.payment = payment;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getPaid() {
        return paid;
    }

    public void setPaid(String paid) {
        this.paid = paid;
    }

    public String getPot() {
        return pot;
    }

    public void setPot(String pot) {
        this.pot = pot;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public List<String> getMatch() {
        return match;
    }

    public void setMatch(List<String> match) {
        this.match = match;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Integer> getPayableStatuses() {
        return payableStatuses;
    }

    public void setPayableStatuses(List<Integer> payableStatuses) {
        this.payableStatuses = payableStatuses == null ? new ArrayList<>() : payableStatuses;
    }
}
