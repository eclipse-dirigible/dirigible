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

import org.eclipse.dirigible.components.intent.model.PickableIntent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The rule of a to-one relation's {@code pickable:} declaration (issue #7496) - the grammar the
 * parser refuses on and the reading the EDM generator emits, in one place so the two cannot drift.
 *
 * <p>
 * The condition is the closed comparison grammar every other {@code when} guard takes
 * ({@link CheckSupport#TERM}), read over the TARGET's properties, plus the one test a completeness
 * rule cannot do without: {@code <property> != null} (present - neither null nor blank) and
 * {@code <property> == null} (absent). A list is the AND of its terms.
 *
 * <p>
 * What the model carries is data, not code (issue #7405): {@link #rule} renders the terms as JSON,
 * which the generated picker hands, as an object literal, to the shared runtime that evaluates it
 * over each fetched target row.
 */
public final class PickableSupport {

    /** The default {@code else:} - a failing row is listed, disabled, with the message under it. */
    public static final String MARK = "mark";

    /** The {@code else:} that leaves a failing row out of the picker. */
    public static final String HIDE = "hide";

    /** The unquoted literal that turns a comparison into a presence test. */
    private static final String NULL = "null";

    /**
     * The JSON is embedded as an object literal in generated JavaScript, never inside an HTML or a
     * string context, so the HTML escaping Gson applies by default would only make the model file
     * unreadable (every {@code =} written as a unicode escape).
     */
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
                                                      .create();

    private PickableSupport() {}

    /**
     * One test of a pickable rule.
     *
     * @param property the target property, as authored
     * @param op {@code eq}, {@code ne}, {@code present} or {@code absent}
     * @param literal the authored literal for {@code eq} / {@code ne} (quotes included when it carried
     *        them), {@code null} for a presence test
     */
    public record Term(String property, String op, String literal) {

        /**
         * @return whether this is a presence test rather than a comparison with a value
         */
        public boolean presence() {
            return literal == null;
        }
    }

    /**
     * Parses one authored term.
     *
     * @param authored the authored comparison
     * @return the term, or {@code null} when it does not have the shape
     */
    public static Term parse(String authored) {
        CheckSupport.Comparison comparison = CheckSupport.parse(authored);
        if (comparison == null) {
            return null;
        }
        if (NULL.equals(comparison.literal())) {
            return new Term(comparison.property(), comparison.equal() ? "absent" : "present", null);
        }
        return new Term(comparison.property(), comparison.equal() ? "eq" : "ne", comparison.literal());
    }

    /**
     * Whether the authored {@code else:} means hide.
     *
     * @param pickable the declaration
     * @return {@code true} for {@code hide}, {@code false} for {@code mark} or absent
     */
    public static boolean hides(PickableIntent pickable) {
        return HIDE.equals(pickable.getOtherwise());
    }

    /**
     * The rule as the JSON object the picker evaluates: {@code {"when": [{"property", "op", "value"?}],
     * "hide": <boolean>, "message": <text>}}, properties PascalCased to the REST JSON the target
     * controller returns. The message defaults to the authored condition, so a marked row always says
     * why it is not offered.
     *
     * @param pickable a declaration the parser accepted
     * @return the JSON text
     */
    public static String rule(PickableIntent pickable) {
        List<String> authored = CheckSupport.terms(pickable.getWhen());
        List<Map<String, Object>> terms = new ArrayList<>();
        for (String text : authored) {
            Term term = parse(text);
            Map<String, Object> reading = new LinkedHashMap<>();
            reading.put("property", IntentNaming.pascalCase(term.property()));
            reading.put("op", term.op());
            if (!term.presence()) {
                reading.put("value", CheckSupport.unquote(term.literal()));
            }
            terms.add(reading);
        }
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("when", terms);
        rule.put("hide", hides(pickable));
        rule.put("message", message(pickable, authored));
        return GSON.toJson(rule);
    }

    /** The authored message, or - absent - the authored condition itself. */
    private static String message(PickableIntent pickable, List<String> authored) {
        if (pickable.getMessage() != null && !pickable.getMessage()
                                                      .isBlank()) {
            return pickable.getMessage();
        }
        return String.join(", ", authored.stream()
                                         .map(String::trim)
                                         .toList());
    }

    /**
     * The target properties a rule reads, PascalCased - what a cross-model target must declare.
     *
     * @param pickable a declaration the parser accepted
     * @return the properties, in authored order
     */
    public static List<String> properties(PickableIntent pickable) {
        List<String> properties = new ArrayList<>();
        for (String text : CheckSupport.terms(pickable.getWhen())) {
            properties.add(IntentNaming.pascalCase(parse(text).property()));
        }
        return properties;
    }
}
