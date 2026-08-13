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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMap;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMaps;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;

/**
 * Compiles a mapping model into the Java expressions its mapper renders.
 *
 * <p>
 * This is the one template whose preparation is a compiler rather than marshalling: every target
 * column carries a declared source - a direct source column, a constant, a neutral arithmetic
 * formula, or a call-out to a mapper bean - and an optional criteria that drops the record. Each of
 * those becomes a Java expression here, at generation time, so an unparseable criteria fails the
 * generation loudly instead of emitting code that misbehaves once it runs.
 */
final class MappingCompiler {

    /** A criteria is a comparison suffix appended to the mapped value ("&gt; 100", "== null"). */
    private static final Pattern CRITERIA = Pattern.compile("^\\s*(===|!==|==|!=|>=|<=|>|<)\\s*(.+?)\\s*$");

    /** A quoted criteria operand, in either quote style. */
    private static final Pattern QUOTED = Pattern.compile("^'(.*)'$|^\"(.*)\"$");

    /** A numeric criteria operand. */
    private static final Pattern NUMERIC = Pattern.compile("^[+-]?[0-9]+(\\.[0-9]+)?$");

    /** Everything that does not belong in a Java identifier. */
    private static final Pattern NON_IDENTIFIER = Pattern.compile("[^A-Za-z0-9]+");

    /** The scale a formula result is rounded to when the column does not declare one. */
    private static final int DEFAULT_SCALE = 2;

    /** The class name of a mapping whose file name yields no identifier at all. */
    private static final String DEFAULT_CLASS_NAME = "Mapper";

    /**
     * Not instantiable.
     */
    private MappingCompiler() {}

    /**
     * Compiles a mapping model into the template model its mapper renders.
     *
     * @param modelText the raw mapping file
     * @return the target structure's name, its compiled columns, and whether a numeric criteria needs
     *         the numeric reader
     * @throws IllegalArgumentException when the mapping declares no target, or a criteria is not a
     *         comparison
     */
    static Map<String, Object> compile(String modelText) {
        Map<String, Object> mapping = asMap(ModelJson.parseObject(modelText)
                                                     .get("mapping"));
        List<Map<String, Object>> structures = mapping == null ? List.of() : asMaps(mapping.get("structures"));
        List<Map<String, Object>> targets = new ArrayList<>();
        for (Map<String, Object> structure : structures) {
            if ("TARGET".equals(str(structure, "type"))) {
                targets.add(structure);
            }
        }
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("The mapping model declares no TARGET structure - nothing to map to.");
        }
        // The last TARGET wins, as it always has - so an existing model keeps generating the same mapper.
        Map<String, Object> target = targets.get(targets.size() - 1);

        List<Object> columns = new ArrayList<>();
        boolean usesNumber = false;
        for (Map<String, Object> column : asMaps(target.get("columns"))) {
            String name = str(column, "name");
            Map<String, Object> compiled = new LinkedHashMap<>();
            compiled.put("name", name);
            compiled.put("nameLiteral", javaString(name));
            compiled.put("expression", valueExpression(column));
            String criteria = str(column, "criteria");
            if (criteria != null && !criteria.isEmpty()) {
                String comparison = criteriaExpression(name, criteria, "target.get(" + javaString(name) + ")");
                compiled.put("criteria", comparison);
                usesNumber = usesNumber || comparison.startsWith("number(");
            }
            columns.add(compiled);
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("mappingTarget", target.get("name"));
        model.put("mappingColumns", columns);
        model.put("usesNumber", usesNumber);
        return model;
    }

    /**
     * The Java class name of a mapper generated from a file name.
     *
     * @param fileName the mapping file's name without its extension
     * @return a valid Java class name
     */
    static String className(String fileName) {
        StringBuilder className = new StringBuilder();
        for (String word : NON_IDENTIFIER.split(fileName == null ? "" : fileName)) {
            if (word.isEmpty()) {
                continue;
            }
            className.append(Character.toUpperCase(word.charAt(0)))
                     .append(word, 1, word.length());
        }
        if (className.isEmpty()) {
            return DEFAULT_CLASS_NAME;
        }
        // A Java identifier cannot start with a digit, and a mapping may well be called "2024-prices".
        return Character.isDigit(className.charAt(0)) ? "_" + className : className.toString();
    }

    /**
     * The expression assigning one target column, from whichever source it declares.
     *
     * <p>
     * The precedence - module, formula, constant, direct - is the order the sequential assignments of
     * the original generator left the last-declared source winning in.
     *
     * @param column the column
     * @return the Java expression
     */
    private static String valueExpression(Map<String, Object> column) {
        String module = str(column, "module");
        if (module != null && !module.isEmpty()) {
            return "org.eclipse.dirigible.sdk.component.Beans.get(" + module + ".class).map(source, target, "
                    + javaString(str(column, "name")) + ")";
        }
        String formula = str(column, "formula");
        if (formula != null && !formula.isEmpty()) {
            return "org.eclipse.dirigible.sdk.utils.Calc.eval(" + javaString(formula) + ", source, " + scaleOf(column) + ")";
        }
        String constant = str(column, "constant");
        if (constant != null && !constant.isEmpty()) {
            return javaString(constant);
        }
        String direct = str(column, "direct");
        if (direct != null && !direct.isEmpty()) {
            return "source.get(" + javaString(direct) + ")";
        }
        return "null";
    }

    /**
     * Compiles a column's criteria into a typed Java comparison over the mapped value.
     *
     * @param name the column's name, for the error message
     * @param criteria the authored criteria
     * @param valueExpression the expression reading the mapped value
     * @return the Java comparison
     * @throws IllegalArgumentException when the criteria is not a comparison against a number, a quoted
     *         string or null
     */
    private static String criteriaExpression(String name, String criteria, String valueExpression) {
        Matcher match = CRITERIA.matcher(criteria);
        if (!match.matches()) {
            throw new IllegalArgumentException("Column '" + name + "': criteria '" + criteria + "' is not a comparison."
                    + " Expected an operator (==, !=, >, <, >=, <=) followed by a number, a quoted string or null.");
        }
        String operator = match.group(1)
                               .replace("===", "==")
                               .replace("!==", "!=");
        String operand = match.group(2);

        if ("null".equals(operand)) {
            return valueExpression + ("==".equals(operator) ? " == " : " != ") + "null";
        }
        Matcher quoted = QUOTED.matcher(operand);
        if (quoted.matches()) {
            String literal = javaString(quoted.group(1) != null ? quoted.group(1) : quoted.group(2));
            String equals = "java.util.Objects.equals(" + valueExpression + ", " + literal + ")";
            return "==".equals(operator) ? equals : "!" + equals;
        }
        if (!NUMERIC.matcher(operand)
                    .matches()) {
            throw new IllegalArgumentException(
                    "Column '" + name + "': criteria operand '" + operand + "' is not a number, a quoted string or null.");
        }
        return "number(" + valueExpression + ") " + operator + " " + operand + "d";
    }

    /**
     * The scale a formula result is rounded to. The mapping editor only writes it for decimal columns,
     * so everything else falls back to two decimal places.
     *
     * @param column the column
     * @return the scale
     */
    private static int scaleOf(Map<String, Object> column) {
        Object scale = column.get("scale");
        if (scale instanceof Number number) {
            double value = number.doubleValue();
            if (value >= 0 && value == Math.floor(value) && !Double.isInfinite(value)) {
                return (int) value;
            }
            return DEFAULT_SCALE;
        }
        String text = str(column, "scale");
        if (text != null && NUMERIC.matcher(text)
                                   .matches()) {
            double value = Double.parseDouble(text);
            if (value >= 0 && value == Math.floor(value)) {
                return (int) value;
            }
        }
        return DEFAULT_SCALE;
    }

    /**
     * A value as a Java string literal.
     *
     * @param value the value
     * @return the literal, quotes included
     */
    private static String javaString(Object value) {
        String text = String.valueOf(value);
        return "\"" + text.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                          .replace("\r\n", "\\n")
                          .replace("\n", "\\n")
                + "\"";
    }

}
