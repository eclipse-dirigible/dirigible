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
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.ide.template.service.model.JavaLiterals;

/**
 * What the template layer renders out of a glue descriptor's NEUTRAL halves (issue #7406) - the
 * same call {@code GlueGenerator} makes, so a test can keep asserting the Java the generation
 * actually runs on while the descriptor itself carries the facts.
 *
 * <p>
 * The two halves are asserted separately on purpose: a test pinning only the rendering would pass
 * on a descriptor that had quietly stopped describing anything, and a test pinning only the
 * descriptor would pass on a rendering that no longer compiled.
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
