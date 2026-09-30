/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMaps;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The JavaScript a {@code visibleWhen} condition renders as (dirigible #7502).
 *
 * <p>
 * The model carries the condition as DATA - one scalar, {@code Status != 1 && Paid == true}, its
 * terms already resolved to the model's property names and to seed ids by the intent generator.
 * Turning it into the expression a generated view evaluates is this layer's business, the same
 * split a default value has ({@code dataDefaultValue} to {@code dataDefaultValueJsLiteral}), so a
 * hand-modeled {@code .edm} carrying the attribute behaves exactly like an intent-derived one.
 *
 * <p>
 * Values compare as strings, as the {@code forbidWhen} panel guard does: a status FK arrives as a
 * number in one response and a string in a form model, and {@code '1' === '1'} holds for both.
 */
final class VisibleWhenLiterals {

    /**
     * One comparison of the condition - the {@code CheckSupport.TERM} grammar of the intent generator
     * that wrote it (a property, {@code ==} or {@code !=}, a quoted string, a number or a bare word),
     * anchored at the start so the terms are read one after another rather than split on {@code &&},
     * which a quoted string may contain.
     */
    private static final Pattern TERM =
            Pattern.compile("\\s*(\\w+)\\s*(==|!=)\\s*('[^']*'|\"[^\"]*\"|-?\\d+|[A-Za-z_][A-Za-z0-9_\\-]*)\\s*(&&|$)");

    private VisibleWhenLiterals() {}

    /** One parsed comparison, its literal unquoted. */
    record Term(String property, boolean equal, String value) {
    }

    /**
     * The terms of a condition, or an empty list when it does not read as the grammar - which the
     * intent parser refuses, so only a hand-edited model gets here, and it then renders no gate rather
     * than a broken page.
     *
     * @param condition the model's {@code visibleWhen} scalar
     * @return the terms, in order
     */
    static List<Term> terms(String condition) {
        List<Term> terms = new ArrayList<>();
        if (condition == null || condition.isBlank()) {
            return terms;
        }
        Matcher matcher = TERM.matcher(condition);
        int at = 0;
        while (at < condition.length() && matcher.find(at) && matcher.start() == at) {
            terms.add(new Term(matcher.group(1), "==".equals(matcher.group(2)), unquote(matcher.group(3))));
            at = matcher.end();
            if (matcher.group(4)
                       .isEmpty()) {
                break;
            }
        }
        return at == condition.length() ? terms : new ArrayList<>();
    }

    /**
     * The condition as the boolean expression a form or document view folds into the field's
     * {@code x-show}, read against the page's {@code form} record and escaped for an HTML attribute.
     *
     * <p>
     * A value the record does not hold yet (null, or the {@code ''} an empty form model starts every
     * text and dropdown input with) reads as the property's default: a create form has not been given
     * its status, and the record it creates starts in the {@code init:} one - so a field shown from
     * {@code Status != DRAFT} stays hidden on the create page instead of flashing up for a record that
     * is about to be a draft.
     *
     * @param condition the model's {@code visibleWhen} scalar
     * @param entity the entity the property belongs to, for the compared properties' defaults
     * @return the expression, or {@code null} when the condition has no terms
     */
    static String formExpression(String condition, Map<String, Object> entity) {
        List<Term> terms = terms(condition);
        if (terms.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (Term term : terms) {
            String read = "form." + term.property();
            String absent = "'" + JsLiterals.escape(defaultOf(entity, term.property())) + "'";
            parts.add("(" + read + " == null || " + read + " === '' ? " + absent + " : String(" + read + ")) "
                    + (term.equal() ? "===" : "!==") + " '" + JsLiterals.escape(term.value()) + "'");
        }
        return html(String.join(" && ", parts));
    }

    /**
     * The condition as the term list a detail registration hands the shared detail panel, in the shape
     * of its {@code forbidWhen} guards: {@code [{ property: 'Status', equal: false, value: '1' }]}.
     *
     * @param condition the model's {@code visibleWhen} scalar
     * @return the JavaScript array literal, or {@code null} when the condition has no terms
     */
    static String termsLiteral(String condition) {
        List<Term> terms = terms(condition);
        if (terms.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (Term term : terms) {
            parts.add("{ property: '" + JsLiterals.escape(term.property()) + "', equal: " + term.equal() + ", value: '"
                    + JsLiterals.escape(term.value()) + "' }");
        }
        return "[" + String.join(", ", parts) + "]";
    }

    /**
     * What the form holds for a property it has not been given a value for: the model default (a status
     * relation's {@code init:} id), an unchecked checkbox's {@code false}, else nothing.
     */
    private static String defaultOf(Map<String, Object> entity, String name) {
        for (Map<String, Object> property : asMaps(entity.get("properties"))) {
            if (!name.equals(str(property, "name"))) {
                continue;
            }
            String value = str(property, "dataDefaultValue");
            if (value != null && !value.isBlank()) {
                return unquote(value.trim());
            }
            return "BOOLEAN".equalsIgnoreCase(str(property, "dataType")) ? "false" : "";
        }
        return "";
    }

    private static String unquote(String literal) {
        if (literal.length() >= 2
                && (literal.startsWith("'") && literal.endsWith("'") || literal.startsWith("\"") && literal.endsWith("\""))) {
            return literal.substring(1, literal.length() - 1);
        }
        return literal;
    }

    /** Escapes an expression for a double-quoted HTML attribute; the browser decodes it for Alpine. */
    private static String html(String expression) {
        return expression.replace("&", "&amp;")
                         .replace("\"", "&quot;")
                         .replace("<", "&lt;")
                         .replace(">", "&gt;");
    }
}
