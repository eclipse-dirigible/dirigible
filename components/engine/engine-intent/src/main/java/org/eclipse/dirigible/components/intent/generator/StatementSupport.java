/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The two vocabularies a {@code kind: statement} report is authored in - the per-line
 * {@code measure} and the {@code accounts} selector - and the SQL each lowers to.
 *
 * <p>
 * Shared by the parser and the generator on purpose: what the parser accepts and what the generator
 * emits are the same grammar, and the one failure this must never have is a selector that validates
 * and then silently selects nothing. Everything here reads the per-account balance columns of the
 * statement query's own subquery (see {@code ReportIntentGenerator}); it never touches the
 * application's tables.
 */
public final class StatementSupport {

    /** The three windows a statement reads, in the order the subquery exposes them. */
    private static final List<String> WINDOWS = List.of("opening", "period", "closing");

    /**
     * One per-account balance the statement subquery exposes: the column it lands in, whether it sums
     * the debit or the credit side, and the window it covers.
     *
     * @param column the quoted subquery column
     * @param debit whether this is the debit side ({@code false} = the credit side)
     * @param window the window it covers - {@code opening}, {@code period} or {@code closing}
     */
    public record Balance(String column, boolean debit, String window) {
    }

    /**
     * The six per-account balances of the statement subquery, in a fixed order. The generator emits
     * exactly these columns and every measure reads them, so the subquery's shape is stated once.
     */
    private static final List<Balance> BALANCES = balances();

    /** The six balances: each window in its debit and its credit form. */
    private static List<Balance> balances() {
        List<Balance> balances = new ArrayList<>();
        for (String window : WINDOWS) {
            balances.add(new Balance(column(window, "DEBIT"), true, window));
            balances.add(new Balance(column(window, "CREDIT"), false, window));
        }
        return List.copyOf(balances);
    }

    /** The six per-account balances the statement subquery exposes. */
    public static List<Balance> balanceColumns() {
        return BALANCES;
    }

    /** The quoted subquery column of one window and side, e.g. {@code "OPENING_DEBIT"}. */
    private static String column(String window, String side) {
        return "\"" + window.toUpperCase(Locale.ROOT) + "_" + side + "\"";
    }

    /**
     * A line's balance: which of the per-account sums it takes, and how.
     *
     * <p>
     * The four plain measures take a side raw - a turnover. The four {@code Net} ones net an account's
     * two sides <b>before</b> the line sums it and keep only what is left on the named side. That is
     * what puts a both-type account on the statement side its actual balance puts it on - a settlement
     * account in debit is a receivable, the same account in credit is a payable - and it is why the
     * netting cannot happen after the line's sum: a line summing raw debits and raw credits reports
     * gross turnover, not a balance.
     *
     * @param authored the name this measure is authored under
     * @param sql what one account contributes to a line taking it
     */
    public record Measure(String authored, String sql) {
    }

    /**
     * The twelve measures by their lower-cased authored name - the full cross product of the three
     * windows, the two sides and the netted/raw choice, generated rather than listed so a name and the
     * balance it reads cannot disagree.
     */
    private static final Map<String, Measure> MEASURES = measures();

    private static Map<String, Measure> measures() {
        Map<String, Measure> measures = new LinkedHashMap<>();
        for (String window : WINDOWS) {
            String debit = column(window, "DEBIT");
            String credit = column(window, "CREDIT");
            add(measures, window + "Debit", debit);
            add(measures, window + "Credit", credit);
            add(measures, window + "NetDebit", net(debit, credit));
            add(measures, window + "NetCredit", net(credit, debit));
        }
        return Collections.unmodifiableMap(measures);
    }

    private static void add(Map<String, Measure> measures, String authored, String sql) {
        measures.put(authored.toLowerCase(Locale.ROOT), new Measure(authored, sql));
    }

    /** {@code kept - other}, floored at zero: what is left on the kept side once the two are netted. */
    private static String net(String kept, String other) {
        String difference = kept + " - " + other;
        return "CASE WHEN " + difference + " > 0 THEN " + difference + " ELSE 0 END";
    }

    private StatementSupport() {}

    /**
     * The measure authored under the given name.
     *
     * @param authored the authored name (case-insensitive, may be null or blank)
     * @return the measure, or {@code null} when the name is not one
     */
    public static Measure measure(String authored) {
        return authored == null ? null
                : MEASURES.get(authored.trim()
                                       .toLowerCase(Locale.ROOT));
    }

    /** The authored measure names, for an author-facing error message. */
    public static List<String> measureNames() {
        return MEASURES.values()
                       .stream()
                       .map(Measure::authored)
                       .toList();
    }

    /**
     * An {@code accounts} selector: the comma-separated terms over the account code, and the SQL they
     * match with.
     *
     * @param terms the parsed terms, in the authored order
     */
    public record Selector(List<Term> terms) {

        /**
         * The predicate selecting an account, over the given account-code column.
         *
         * @param accountColumn the quoted account-code column of the statement subquery
         * @return the predicate - parenthesised, so it composes inside a CASE
         */
        public String sql(String accountColumn) {
            List<String> predicates = new ArrayList<>();
            for (Term term : terms) {
                predicates.add(term.sql(accountColumn));
            }
            return predicates.size() == 1 ? predicates.get(0) : "(" + String.join(" OR ", predicates) + ")";
        }
    }

    /** One term of an {@code accounts} selector. */
    public sealed interface Term {

        /**
         * The predicate this term matches an account with.
         *
         * @param accountColumn the quoted account-code column
         * @return the predicate
         */
        String sql(String accountColumn);
    }

    /** {@code 20*} - every account whose code starts with the prefix. */
    public record Prefix(String value) implements Term {

        @Override
        public String sql(String accountColumn) {
            return accountColumn + " LIKE '" + value + "%'";
        }
    }

    /** {@code 4110} - exactly this account. */
    public record Exact(String value) implements Term {

        @Override
        public String sql(String accountColumn) {
            return accountColumn + " = '" + value + "'";
        }
    }

    /**
     * {@code 60-69} - every account whose code starts inside the range. The bounds are equally long
     * prefixes and the comparison is over exactly that many leading characters, so {@code 60-69}
     * selects {@code 601} and {@code 6999} as an accountant expects - a plain
     * {@code BETWEEN '60' AND '69'} would drop both, since {@code '601' > '69'} lexicographically.
     */
    public record Range(String from, String to) implements Term {

        @Override
        public String sql(String accountColumn) {
            String prefix = "SUBSTRING(" + accountColumn + " FROM 1 FOR " + from.length() + ")";
            return "(" + prefix + " >= '" + from + "' AND " + prefix + " <= '" + to + "')";
        }
    }

    /**
     * Parse an {@code accounts} selector.
     *
     * @param authored the authored selector (may be null or blank)
     * @param issues the issues found, appended to - each one names what is wrong with which term
     * @param prefix the message prefix identifying the line
     * @return the parsed selector, or {@code null} when it did not parse
     */
    public static Selector selector(String authored, List<String> issues, String prefix) {
        if (authored == null || authored.isBlank()) {
            return null;
        }
        List<Term> terms = new ArrayList<>();
        for (String raw : authored.split(",")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                issues.add(prefix + " accounts [" + authored.trim() + "] has an empty term");
                continue;
            }
            Term term = term(token, issues, prefix);
            if (term != null) {
                terms.add(term);
            }
        }
        return terms.isEmpty() ? null : new Selector(terms);
    }

    /** One selector term: a range when it carries the separator, else a prefix or an exact code. */
    private static Term term(String token, List<String> issues, String prefix) {
        int separator = token.indexOf('-');
        if (separator >= 0) {
            String from = token.substring(0, separator);
            String to = token.substring(separator + 1);
            if (!code(from, prefix, token, issues) || !code(to, prefix, token, issues)) {
                return null;
            }
            if (from.length() != to.length()) {
                issues.add(prefix + " accounts term [" + token + "] is a range whose bounds are of different length"
                        + " - a range compares equally long code prefixes");
                return null;
            }
            if (from.compareTo(to) > 0) {
                issues.add(prefix + " accounts term [" + token + "] is a range that ends before it starts");
                return null;
            }
            return new Range(from, to);
        }
        if (token.endsWith("*")) {
            String value = token.substring(0, token.length() - 1);
            return code(value, prefix, token, issues) ? new Prefix(value) : null;
        }
        return code(token, prefix, token, issues) ? new Exact(token) : null;
    }

    /**
     * An account code an author may write into a selector: letters, digits, dot and underscore. The
     * charset is closed deliberately - the code goes into the query as a literal, so a quote or a
     * {@code LIKE} wildcard must never reach it, and the hyphen is the range separator.
     */
    private static boolean code(String value, String prefix, String token, List<String> issues) {
        if (value.isEmpty()) {
            issues.add(prefix + " accounts term [" + token + "] has an empty account code");
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!Character.isLetterOrDigit(character) && character != '.' && character != '_') {
                issues.add(prefix + " accounts term [" + token + "] contains [" + character
                        + "] - an account code may hold letters, digits, dot and underscore;"
                        + " a hyphen separates the bounds of a range and a trailing asterisk makes a prefix");
                return false;
            }
        }
        return true;
    }
}
