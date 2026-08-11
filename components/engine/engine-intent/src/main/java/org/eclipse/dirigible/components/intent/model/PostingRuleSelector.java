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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The conditional form of a posting item's {@code rule(...)} account reference: the rule-row column
 * is chosen at runtime by a classifier field of the source, e.g.
 *
 * <pre>
 * account: "rule(by: PaymentMethod, cases: { 1: BankAccount, 2: CashAccount }, default: SuspenseAccount)"
 * </pre>
 *
 * reads the resolved rule row's {@code BankAccount} column when the source's {@code PaymentMethod}
 * classifier is {@code 1}, {@code CashAccount} when {@code 2}, else {@code SuspenseAccount} (or,
 * with no {@code default}, nothing - which skips the posting to the unposted worklist). It mirrors
 * the classifier-with-cases shape the conditional {@code dependsOn} {@code valueFrom} already uses
 * ({@code by} / {@code cases} / {@code default}) and extends the plain {@code rule(<column>)} idiom
 * without changing the string-valued item-cell shape - so a single item row replaces the
 * {@code when:}-gated row pair that used to be the only way to vary the account column by a source
 * value.
 *
 * @param by the classifier: a field or to-one relation name of the source (read null-safe as a
 *        number)
 * @param cases the classifier value (a seed id / number, as authored) to rule-row column mapping,
 *        in authoring order
 * @param defaultColumn the rule-row column used when no case matches, or {@code null} for none
 */
public record PostingRuleSelector(String by, Map<String, String> cases, String defaultColumn) {

    /**
     * Matches {@code rule(by: <field>, cases: { <k>: <col>, ... }, default: <col> )} with an optional
     * {@code default}. The cases body is captured raw (it never contains a nested brace) and split +
     * trimmed downstream. The whole value must be this form - a plain {@code rule(<column>)} does not
     * match.
     * <p>
     * The cases body is captured with a single greedy {@code [^}]*} - NOT {@code \s*([^}]*?)\s*} - on
     * purpose: {@code \s} is a subset of {@code [^}]}, so wrapping the body in
     * {@code \s*}...{@code \s*} makes the space runs ambiguous between three quantifiers and backtracks
     * catastrophically on a crafted {@code cases:{ } with many spaces and no closing brace (a ReDoS -
     * CodeQL flagged it). Edge whitespace is harmless here because every split fragment is trimmed in
     * {@link #parse}.
     */
    private static final Pattern PATTERN =
            Pattern.compile("\\s*rule\\(\\s*by:\\s*(\\w+)\\s*,\\s*cases:\\s*\\{([^}]*)\\}\\s*(?:,\\s*default:\\s*(\\w+)\\s*)?\\)\\s*");

    /**
     * Parse the conditional {@code rule(by: ..., cases: ..., default: ...)} form.
     *
     * @param value the authored item-cell value (typically a quoted scalar, since it carries colons and
     *        braces)
     * @return the parsed selector, or {@link Optional#empty()} when {@code value} is not the
     *         conditional form (a plain {@code rule(<column>)}, an expression, or a source-relation
     *         copy)
     */
    public static Optional<PostingRuleSelector> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Map<String, String> cases = new LinkedHashMap<>();
        for (String part : matcher.group(2)
                                  .split(",")) {
            if (part.isBlank()) {
                continue;
            }
            int colon = part.indexOf(':');
            if (colon < 0) {
                // A malformed case (no `key: column`) - keep the raw fragment as a key with an empty
                // column so the parser can report it precisely rather than silently dropping it.
                cases.put(part.trim(), "");
                continue;
            }
            cases.put(part.substring(0, colon)
                          .trim(),
                    part.substring(colon + 1)
                        .trim());
        }
        return Optional.of(new PostingRuleSelector(matcher.group(1), cases, matcher.group(3)));
    }
}
