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
 * One line of a {@code kind: statement} report - a fixed row of a balance sheet or an income
 * statement.
 *
 * <p>
 * A line is either a <b>leaf</b>, which reads the ledger ({@link #accounts} selects the accounts,
 * {@link #measure} says which of their balances to take), or <b>computed</b>, which is arithmetic
 * over other lines referenced by their {@link #code} ({@link #sum} adds them, {@link #less}
 * subtracts). The two shapes are exclusive: a line that both reads accounts and adds other lines
 * would double-count silently.
 */
public class StatementLineIntent {

    /**
     * The line's reference, unique within the statement - the statutory line code ({@code A.I}) other
     * lines reference and the first output column.
     */
    private String code;
    /** The line's caption, rendered verbatim as the statement's second output column. */
    private String label;
    /**
     * A leaf line's account selector: comma-separated terms over the account code - a prefix
     * ({@code 20*}), an inclusive range of equally long prefixes ({@code 60-69}), or an exact code
     * ({@code 4110}). A row matching any term contributes.
     */
    private String accounts;
    /**
     * A leaf line's balance: {@code opening}/{@code period}/{@code closing} x
     * {@code Debit}/{@code Credit}, plus the {@code Net} variants that net an account's two sides
     * before taking it ({@code closingNetDebit} - so a both-type account lands on the statement side
     * its actual balance puts it on).
     */
    private String measure;
    /** A computed line's addends: the codes of other lines of this statement. */
    private List<String> sum = new ArrayList<>();
    /** A computed line's subtrahends: the codes of other lines of this statement. */
    private List<String> less = new ArrayList<>();

    /** Whether this line reads the ledger rather than other lines. */
    public boolean isLeaf() {
        return accounts != null && !accounts.isBlank();
    }

    /** Whether this line is arithmetic over other lines. */
    public boolean isComputed() {
        return !sum.isEmpty() || !less.isEmpty();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAccounts() {
        return accounts;
    }

    public void setAccounts(String accounts) {
        this.accounts = accounts;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public List<String> getSum() {
        return sum;
    }

    public void setSum(List<String> sum) {
        this.sum = sum == null ? new ArrayList<>() : sum;
    }

    public List<String> getLess() {
        return less;
    }

    public void setLess(List<String> less) {
        this.less = less == null ? new ArrayList<>() : less;
    }
}
