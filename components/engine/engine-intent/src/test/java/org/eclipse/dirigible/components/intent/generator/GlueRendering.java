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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.ide.template.service.model.JavaExpressions;
import org.eclipse.dirigible.components.ide.template.service.model.JavaLiterals;

/**
 * Renders the NEUTRAL readings a glue descriptor carries into the Java the template layer emits
 * from them - the same rendering {@code GlueGenerator} performs - so a test of the generated glue
 * can keep asserting the Java a handler ends up with while the glue itself carries only data
 * (issues #7406 and #7425).
 */
final class GlueRendering {

    private GlueRendering() {}

    /**
     * The whole {@code Criteria} a descriptor's clauses render as.
     *
     * @param descriptor the glue descriptor
     * @return the expression
     */
    static String criteria(Map<String, Object> descriptor) {
        return JavaLiterals.criteriaExpression(clauses(descriptor.get("criteria")));
    }

    /**
     * The {@code Criteria} tail a create-from's source-row rule renders as.
     *
     * @param descriptor the glue descriptor
     * @return the chain, empty for no rule
     */
    static String itemWhere(Map<String, Object> descriptor) {
        return JavaLiterals.criteriaChain(clauses(descriptor.get("itemCriteria")));
    }

    /**
     * The Java literal a compared column's default renders as - the empty string where the column
     * carries none, which is the contract the template's own {@code #if} reads.
     *
     * @param entry the assignment or compared-property entry
     * @return the literal, or the empty string
     */
    static String derivedDefault(Map<String, Object> entry) {
        String rendered = JavaLiterals.derivedDefaultExpression(entry.get("derivedDefaultValue"));
        return rendered == null ? "" : rendered;
    }

    /**
     * The Java expression an assignment, a prompted field or a key term writes, rendered from its
     * reading.
     *
     * @param entry the entry carrying a {@code reading}
     * @return the expression
     */
    static String expr(Map<String, Object> entry) {
        return expression(entry.get("reading"));
    }

    /**
     * The entries of an assignment list as the template sees them: every key but the reading, plus the
     * {@code expr} rendered from it - so an assertion can keep naming the Java.
     *
     * @param entries the descriptor's list
     * @return the rendered entries
     */
    static List<Map<String, Object>> rendered(List<Map<String, Object>> entries) {
        List<Map<String, Object>> rendered = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            Map<String, Object> copy = new LinkedHashMap<>(entry);
            copy.remove("reading");
            copy.put("expr", expr(entry));
            rendered.add(copy);
        }
        return rendered;
    }

    /**
     * A derived row's {@code when} guard - the empty string for no guard, as the template reads it.
     *
     * @param row the item row or computed line
     * @return the boolean expression
     */
    static String guard(Map<String, Object> row) {
        return expression(row.get("guardReading"));
    }

    /**
     * A transition's {@code when} guard, with the SDK evaluator fully qualified as the controller
     * template needs it.
     *
     * @param transition the transition descriptor
     * @return the boolean expression, empty for no guard
     */
    static String transitionGuard(Map<String, Object> transition) {
        String rendered = JavaExpressions.qualifiedExpression(transition.get("guardReading"));
        return rendered == null ? "" : rendered;
    }

    /**
     * An event binding's guard, {@code true} for none.
     *
     * @param descriptor the descriptor carrying {@code guardTerms}
     * @return the boolean expression
     */
    static String eventGuard(Map<String, Object> descriptor) {
        return eventGuardOf(descriptor.get("guardTerms"));
    }

    /**
     * The Java boolean a list of neutral guard terms renders as, {@code true} for none.
     *
     * @param terms the terms
     * @return the boolean expression
     */
    static String eventGuardOf(Object terms) {
        String rendered = JavaLiterals.conditionExpression(clauses(terms));
        return rendered == null ? "true" : rendered;
    }

    /**
     * The classifier ternaries a posting handler null-guards, in row and cell order.
     *
     * @param posting the posting descriptor
     * @return the guards
     */
    static List<String> conditionalRuleGuards(Map<String, Object> posting) {
        List<String> guards = new ArrayList<>();
        for (Map<String, Object> row : clauses(posting.get("itemRows"))) {
            for (Map<String, Object> assignment : clauses(row.get("assigns"))) {
                if (assignment.get("reading") instanceof Map<?, ?> reading && "ruleCase".equals(reading.get("kind"))) {
                    guards.add(expr(assignment));
                }
            }
        }
        return guards;
    }

    /**
     * A notify block's print-attachment render language.
     *
     * @param descriptor the descriptor
     * @return the expression, empty for no attachment
     */
    static String attachLanguage(Map<String, Object> descriptor) {
        return expression(descriptor.get("attachLanguage"));
    }

    /**
     * A notify block's attachment file name.
     *
     * @param descriptor the descriptor
     * @return the expression, empty for no attachment
     */
    static String attachFileName(Map<String, Object> descriptor) {
        return expression(descriptor.get("attachFileName"));
    }

    /**
     * A snapshot's render language.
     *
     * @param snapshot the snapshot descriptor
     * @return the expression
     */
    static String language(Map<String, Object> snapshot) {
        return expression(snapshot.get("language"));
    }

    /**
     * A snapshot's stored file name.
     *
     * @param snapshot the snapshot descriptor
     * @return the expression
     */
    static String fileName(Map<String, Object> snapshot) {
        return expression(snapshot.get("fileName"));
    }

    /**
     * A timer loader's due moment.
     *
     * @param loader the timer loader descriptor
     * @return the expression
     */
    static String due(Map<String, Object> loader) {
        return expression(loader.get("due"));
    }

    /**
     * The Java a {@code posts:} {@code set:} value renders to for an unresolvable column.
     *
     * @param raw the authored value
     * @return the expression
     */
    static String postSet(String raw) {
        return postSet(raw, PostSetSupport.TargetType.UNKNOWN);
    }

    /**
     * The Java a {@code posts:} {@code set:} value renders to, typed to the target column.
     *
     * @param raw the authored value
     * @param type the column's type
     * @return the expression
     */
    static String postSet(String raw, PostSetSupport.TargetType type) {
        return expression(PostSetSupport.reading(raw, type));
    }

    /**
     * One reading as Java, the empty string for none - the binder's own contract.
     *
     * @param reading the reading
     * @return the expression
     */
    private static String expression(Object reading) {
        String rendered = JavaExpressions.expression(reading);
        return rendered == null ? "" : rendered;
    }

    /**
     * The clause list, typed.
     *
     * @param raw the descriptor's value
     * @return the clauses
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> clauses(Object raw) {
        List<Map<String, Object>> clauses = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    clauses.add((Map<String, Object>) map);
                }
            }
        }
        return clauses;
    }
}
