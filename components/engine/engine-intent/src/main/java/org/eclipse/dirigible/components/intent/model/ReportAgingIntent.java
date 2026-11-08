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
 * A report's {@code aging:} block - the aged receivables / payables shape (dirigible #7888). Every
 * open document's {@link #amount open amount} is classified by how many days past its {@link #due}
 * date it is on the report's as-of date and summed into one column per bucket, beside the report's
 * dimensions:
 *
 * <pre>
 * aging:
 *   due: due
 *   amount: "total - paid"
 *   buckets: [30, 60, 90]     # -> Not Yet Due, Overdue 0-30, Overdue 31-60, Overdue 61-90, Overdue 90+
 * </pre>
 *
 * <p>
 * The buckets add up to the emitted {@code Open Amount} column for every row, so a counterparty's
 * buckets always reconcile with its open balance. A document not yet due on the as-of date has its
 * OWN bucket - it is never counted as {@code 0-30} - and so does a document with no due date, which
 * cannot be past one.
 */
public class ReportAgingIntent {

    /** The as-of parameter's name when the block names none. */
    public static final String DEFAULT_AS_OF = "asOf";

    /** The {@code date}/{@code timestamp} field the document falls due on - own or one relation hop. */
    private String due;
    /**
     * The document's open amount: one numeric field, or numeric fields joined by {@code +} and
     * {@code -} ({@code total - paid}), optionally with a leading sign ({@code -total}). A missing
     * value in any of them reads as zero.
     */
    private String amount;
    /**
     * The ascending day thresholds of the overdue buckets. A leading {@code 0} is accepted and ignored
     * - the not-yet-due bucket always exists on its own.
     */
    private List<Integer> buckets = new ArrayList<>();
    /**
     * The name of the date parameter the report is aged as of. The report declares it on its own behalf
     * and it defaults to the day the report is run; absent -> {@value #DEFAULT_AS_OF}.
     */
    private String asOf;
    /**
     * Optional document date: when declared, a document dated after the as-of date is not yet open on
     * it and is left out of the report.
     */
    private String date;

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

    public List<Integer> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<Integer> buckets) {
        this.buckets = buckets == null ? new ArrayList<>() : buckets;
    }

    public String getAsOf() {
        return asOf;
    }

    public void setAsOf(String asOf) {
        this.asOf = asOf;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    /** The as-of parameter's name, trimmed, defaulted to {@value #DEFAULT_AS_OF}. */
    public String getAsOfName() {
        return asOf == null || asOf.isBlank() ? DEFAULT_AS_OF : asOf.trim();
    }

    /**
     * The overdue thresholds without a leading {@code 0}: {@code [0, 30, 60, 90]} and
     * {@code [30, 60, 90]} both describe the same five buckets.
     */
    public List<Integer> getThresholds() {
        List<Integer> thresholds = new ArrayList<>();
        for (Integer bucket : buckets) {
            if (bucket != null && !(thresholds.isEmpty() && bucket == 0)) {
                thresholds.add(bucket);
            }
        }
        return thresholds;
    }

    /** One signed term of {@link #amount}. */
    public record AmountTerm(int sign, String path) {
    }

    /**
     * {@link #amount} split into its signed field paths, or {@code null} when it is not a sum and
     * difference of field paths - the only shape the open amount may take, which keeps it a closed
     * grammar rather than raw SQL.
     */
    public List<AmountTerm> getAmountTerms() {
        return amountTerms(amount);
    }

    /**
     * An amount expression split into its signed field paths, or {@code null} when it is blank or not a
     * sum and difference of field paths - the grammar shared by {@link #amount} and a union member's
     * {@link ReportUnionMemberIntent#getAmount() amount}.
     *
     * @param amount the authored expression
     * @return the signed terms, or null
     */
    public static List<AmountTerm> amountTerms(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }
        List<AmountTerm> terms = new ArrayList<>();
        int sign = 1;
        StringBuilder path = new StringBuilder();
        String text = amount.trim();
        for (int i = 0; i <= text.length(); i++) {
            char character = i < text.length() ? text.charAt(i) : '+';
            if (character == '+' || character == '-') {
                String term = path.toString()
                                  .trim();
                if (term.isEmpty() && terms.isEmpty() && i < text.length() && text.substring(0, i)
                                                                                  .isBlank()) {
                    // A leading sign - "-total" is the negated total (a credit note's revenue).
                    sign = character == '-' ? -1 : 1;
                    path.setLength(0);
                    continue;
                }
                if (!isPath(term)) {
                    return null;
                }
                terms.add(new AmountTerm(sign, term));
                sign = character == '-' ? -1 : 1;
                path.setLength(0);
            } else {
                path.append(character);
            }
        }
        return terms;
    }

    private static boolean isPath(String term) {
        if (term.isEmpty()) {
            return false;
        }
        for (String segment : term.split("\\.", -1)) {
            if (segment.isEmpty() || !Character.isJavaIdentifierStart(segment.charAt(0))) {
                return false;
            }
            for (int i = 1; i < segment.length(); i++) {
                if (!Character.isJavaIdentifierPart(segment.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }
}
