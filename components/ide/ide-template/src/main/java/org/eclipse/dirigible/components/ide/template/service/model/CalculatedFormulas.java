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
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.isTrue;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The calculated-field formulas of a model the Java templates generate from.
 *
 * <p>
 * The generated repository evaluates a formula with {@code org.eclipse.dirigible.sdk.utils.Calc} -
 * a closed arithmetic language over the entity's properties - and writes it into a Java string, as
 * the generated UI's mirror does. A model authored for the removed TypeScript templates holds
 * JavaScript instead ({@code entity["Quantity"] * entity["Price"]}), and one regenerated with the
 * removed AngularJS-Java template holds Java ({@code entity.Net.add(entity.VAT)}); either went into
 * that string as it was, so the repository did not compile, or failed on every save (#8051).
 *
 * <p>
 * A formula that only reads the entity's properties through {@code entity["X"]} or {@code entity.X}
 * is rewritten to the bare property names, which is the same arithmetic. Anything else - a string,
 * a method call, a ternary, a property the entity does not have - is not a formula the platform can
 * evaluate, and the generation is refused with every such formula named, rather than generating a
 * repository that cannot work.
 */
final class CalculatedFormulas {

    private static final String PROJECTION = "PROJECTION";

    private static final String EXTENSION = "EXTENSION";

    /** The functions a formula may call. */
    private static final Set<String> FUNCTIONS =
            Set.of("round", "abs", "min", "max", "ceil", "floor", "daysBetween", "businessDaysBetween", "monthsBetween");

    /** A property read as {@code entity["X"]} or {@code entity['X']}. */
    private static final Pattern BRACKET_ACCESS = Pattern.compile("\\bentity\\s*\\[\\s*(['\"])([A-Za-z_$][\\w$]*)\\1\\s*\\]");

    /** A property read as {@code entity.X} - not a method call. */
    private static final Pattern DOT_ACCESS = Pattern.compile("\\bentity\\s*\\.\\s*([A-Za-z_$][\\w$]*)\\b(?!\\s*\\()");

    /** The tokens of a formula. */
    private static final Pattern TOKEN = Pattern.compile("\\s+|\\d+(?:\\.\\d+)?|[A-Za-z_$][\\w$]*|[-+*/(),]");

    private CalculatedFormulas() {}

    /**
     * Rewrites the formulas of every calculated property to the bare property names, and refuses a
     * model holding a formula that cannot be evaluated.
     *
     * @param entities the model's entities, processed
     * @throws IllegalArgumentException naming every formula that is not one
     */
    static void resolve(List<Map<String, Object>> entities) {
        List<String> refused = new ArrayList<>();
        for (Map<String, Object> entity : entities) {
            if (PROJECTION.equals(str(entity, "type"))) {
                // Another project's entity, generated - formulas included - by that project.
                continue;
            }
            Set<String> properties = propertyNames(entity);
            if (EXTENSION.equals(str(entity, "type"))) {
                // Merged into the entity it extends, so its formulas read that entity's properties too.
                entities.stream()
                        .filter(base -> !EXTENSION.equals(str(base, "type")) && str(base, "name") != null
                                && str(base, "name").equals(str(entity, "extensionReferencedEntity")))
                        .forEach(base -> properties.addAll(propertyNames(base)));
            }
            for (Map<String, Object> property : asMaps(entity.get("properties"))) {
                if (!isTrue(property, "isCalculatedProperty")) {
                    continue;
                }
                resolve(entity, property, "calculatedPropertyExpressionCreate", "calculatedActionOnCreate", properties, refused);
                resolve(entity, property, "calculatedPropertyExpressionUpdate", "calculatedActionOnUpdate", properties, refused);
            }
        }
        if (!refused.isEmpty()) {
            throw new IllegalArgumentException("The model holds calculated fields the platform cannot evaluate. A formula is arithmetic"
                    + " (+ - * / and parentheses) over the entity's properties, named as they are, and the functions "
                    + String.join(", ", FUNCTIONS.stream()
                                                 .sorted()
                                                 .toList())
                    + ". Rewrite these in the model, or compute the value in Java with calculatedActionOnCreate /"
                    + " calculatedActionOnUpdate:\n" + String.join("\n", refused));
        }
    }

    private static Set<String> propertyNames(Map<String, Object> entity) {
        Set<String> names = new HashSet<>();
        asMaps(entity.get("properties")).forEach(property -> names.add(str(property, "name")));
        return names;
    }

    private static void resolve(Map<String, Object> entity, Map<String, Object> property, String formulaKey, String actionKey,
            Set<String> properties, List<String> refused) {
        String formula = str(property, formulaKey);
        String action = str(property, actionKey);
        if (formula == null || formula.isBlank() || (action != null && !action.isBlank())) {
            // No formula, or a Java action computes the value instead and the formula is not generated.
            return;
        }
        String normalized = normalize(formula);
        if (isFormula(normalized, properties)) {
            property.put(formulaKey, normalized);
        } else {
            refused.add("- " + str(entity, "name") + "." + str(property, "name") + ": " + formula);
        }
    }

    /**
     * Rewrites the reads of the entity's properties to their bare names.
     *
     * @param formula the formula as the model holds it
     * @return the formula over bare property names
     */
    static String normalize(String formula) {
        String bare = BRACKET_ACCESS.matcher(formula)
                                    .replaceAll("$2");
        return DOT_ACCESS.matcher(bare)
                         .replaceAll("$1")
                         .trim();
    }

    /**
     * Tests whether a formula is one the platform evaluates: numbers, the entity's properties, the
     * arithmetic operators, parentheses and the known functions.
     *
     * @param formula the formula over bare property names
     * @param properties the entity's property names
     * @return true when it can be evaluated
     */
    static boolean isFormula(String formula, Set<String> properties) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(formula);
        int at = 0;
        while (at < formula.length()) {
            if (!matcher.find(at) || matcher.start() != at) {
                return false;
            }
            if (!matcher.group()
                        .isBlank()) {
                tokens.add(matcher.group());
            }
            at = matcher.end();
        }
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (!Character.isLetter(token.charAt(0)) && token.charAt(0) != '_' && token.charAt(0) != '$') {
                continue;
            }
            boolean call = i + 1 < tokens.size() && "(".equals(tokens.get(i + 1));
            if (call ? !FUNCTIONS.contains(token) : !properties.contains(token)) {
                return false;
            }
        }
        return !tokens.isEmpty();
    }
}
