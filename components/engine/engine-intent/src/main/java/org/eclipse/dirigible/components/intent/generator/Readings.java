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

/**
 * The NEUTRAL expression readings a {@code .glue} carries where it used to carry Java (issue
 * #7425).
 *
 * <p>
 * Every reading is a map with a {@code kind} and the facts the author declared - which record a
 * value is read off, which arithmetic a cell evaluates, which number a guard compares with - and
 * never the code that does it: the rendering lives in the template layer
 * ({@code org.eclipse.dirigible.components.ide.template.service.model.JavaExpressions}), the one
 * layer that knows what language it is generating. The kinds are closed and mirrored there; a kind
 * added here without its rendering is a glue nothing can render.
 *
 * <p>
 * Two rules the shape obeys because the glue is a serialized artefact. Keys are written in one
 * fixed order per kind, so a regenerated glue differs only where the intent did (issue #7130).
 * Every number travels as TEXT and an optional key is OMITTED rather than written as {@code null} -
 * the glue is written through Gson, which drops a null map entry, and read back through a parser
 * that types every number as a {@code Double}.
 */
final class Readings {

    /** The kind key. */
    private static final String KIND = "kind";

    /**
     * Not instantiable.
     */
    private Readings() {}

    /**
     * A property read off a loaded record, {@code owner.Property}.
     *
     * @param owner the local holding the record
     * @param property the PascalCased property
     * @return the reading
     */
    static Map<String, Object> read(String owner, String property) {
        return of(KIND, "read", "owner", owner, "property", property);
    }

    /**
     * A property read off a related record the generated code may not have resolved - null-guarded.
     *
     * @param owner the local the relation was loaded into
     * @param property the PascalCased property
     * @return the reading
     */
    static Map<String, Object> hop(String owner, String property) {
        return of(KIND, "hop", "owner", owner, "property", property);
    }

    /**
     * A local the generated code declares itself (a snapshot copy's {@code version}).
     *
     * @param name the local's name
     * @return the reading
     */
    static Map<String, Object> local(String name) {
        return of(KIND, "local", "name", name);
    }

    /**
     * A string constant.
     *
     * @param text the text
     * @return the reading
     */
    static Map<String, Object> string(String text) {
        return of(KIND, "string", "text", text);
    }

    /**
     * A numeric constant in its bare spelling.
     *
     * @param text the number, as authored
     * @return the reading
     */
    static Map<String, Object> number(String text) {
        return of(KIND, "number", "text", text);
    }

    /**
     * A numeric constant typed to the column it is written into.
     *
     * @param text the number, as authored
     * @param type {@code integer}, {@code long} or {@code decimal}
     * @return the reading
     */
    static Map<String, Object> number(String text, String type) {
        return of(KIND, "number", "text", text, "type", type);
    }

    /**
     * A boolean constant.
     *
     * @param text {@code true} or {@code false}
     * @return the reading
     */
    static Map<String, Object> bool(String text) {
        return of(KIND, "boolean", "text", text);
    }

    /**
     * The null constant.
     *
     * @return the reading
     */
    static Map<String, Object> nil() {
        return of(KIND, "null");
    }

    /**
     * Today, in the target field's own temporal shape.
     *
     * @param shape {@code date}, {@code timestamp}, {@code month} or {@code week}
     * @return the reading
     */
    static Map<String, Object> now(String shape) {
        return of(KIND, "now", "shape", shape);
    }

    /**
     * An authored arithmetic expression evaluated over a loaded record and rounded to a scale.
     *
     * @param text the arithmetic, as authored
     * @param owner the local holding the record its names read off
     * @param scale the scale the result is rounded to
     * @param narrow the Java type the result is narrowed to ({@code int}, {@code long},
     *        {@code double}), or null for the {@code BigDecimal} itself
     * @return the reading
     */
    static Map<String, Object> calc(String text, String owner, int scale, String narrow) {
        Map<String, Object> reading = of(KIND, "calc", "text", text, "owner", owner, "scale", String.valueOf(scale));
        if (narrow != null) {
            reading.put("narrow", narrow);
        }
        return reading;
    }

    /**
     * A value rendered as text.
     *
     * @param of the value
     * @return the reading
     */
    static Map<String, Object> text(Map<String, Object> of) {
        return of(KIND, "text", "of", of);
    }

    /**
     * A string concatenation.
     *
     * @param parts the parts, in order
     * @param forceString whether a lone non-literal part is still forced to a {@code String} result
     * @return the reading
     */
    static Map<String, Object> concat(List<Map<String, Object>> parts, boolean forceString) {
        Map<String, Object> reading = of(KIND, "concat", "parts", new ArrayList<>(parts));
        if (forceString) {
            reading.put("forceString", Boolean.TRUE);
        }
        return reading;
    }

    /**
     * A null-safe negation of a numeric property.
     *
     * @param owner the local holding the record
     * @param property the PascalCased property
     * @return the reading
     */
    static Map<String, Object> negate(String owner, String property) {
        return of(KIND, "negate", "owner", owner, "property", property);
    }

    /**
     * The conversion of a posted value to a prompted field's type.
     *
     * @param type the field's intent type, or {@code relation} for a to-one's key
     * @return the reading
     */
    static Map<String, Object> convert(String type) {
        return of(KIND, "convert", "type", type);
    }

    /**
     * A null-safe numeric guard: a field read through the arithmetic evaluator compared with a number.
     *
     * @param owner the local holding the record
     * @param property the PascalCased property
     * @param equal whether the comparison is an equality
     * @param text the number compared with, as authored
     * @return the reading
     */
    static Map<String, Object> calcCompare(String owner, String property, boolean equal, String text) {
        return of(KIND, "calcCompare", "owner", owner, "property", property, "equal", equal, "text", text);
    }

    /**
     * A conditional rule column: the rule row's column chosen by the source's classifier value.
     *
     * @param by the PascalCased classifier property of the source
     * @param owner the local holding the source
     * @param cases the cases, each a {@code value} / {@code column} pair, in authored order
     * @param otherwise the PascalCased column an unmatched value falls to, or null for none
     * @return the reading
     */
    static Map<String, Object> ruleCase(String by, String owner, List<Map<String, Object>> cases, String otherwise) {
        Map<String, Object> reading = of(KIND, "ruleCase", "by", by, "owner", owner, "cases", new ArrayList<>(cases));
        if (otherwise != null) {
            reading.put("otherwise", otherwise);
        }
        return reading;
    }

    /**
     * One case of a conditional rule column.
     *
     * @param value the classifier value, as authored
     * @param column the PascalCased rule column chosen for it
     * @return the case
     */
    static Map<String, Object> ruleCaseOf(String value, String column) {
        return of("value", value, "column", column);
    }

    /**
     * The platform's application language.
     *
     * @return the reading
     */
    static Map<String, Object> defaultLanguage() {
        return of(KIND, "defaultLanguage");
    }

    /**
     * A render language read off a loaded record, the platform default when blank.
     *
     * @param owner the local the record was loaded into
     * @param property the PascalCased language property
     * @return the reading
     */
    static Map<String, Object> languageFrom(String owner, String property) {
        return of(KIND, "languageFrom", "owner", owner, "property", property);
    }

    /**
     * One sanitized file-name value, optionally formatted.
     *
     * @param of the value
     * @param format the date pattern, or null for none
     * @return the reading
     */
    static Map<String, Object> part(Map<String, Object> of, String format) {
        Map<String, Object> reading = of(KIND, "part", "of", of);
        if (format != null && !format.isEmpty()) {
            reading.put("format", format);
        }
        return reading;
    }

    /**
     * The first non-blank of several file-name values.
     *
     * @param parts the alternatives, in order
     * @return the reading
     */
    static Map<String, Object> first(List<Map<String, Object>> parts) {
        return of(KIND, "first", "parts", new ArrayList<>(parts));
    }

    /**
     * A rendered document's default name: its number when it declares one, else its entity name plus
     * the record id.
     *
     * @param owner the local holding the record
     * @param entity the entity's name
     * @param key the PascalCased key property
     * @param number the PascalCased {@code number:} property, or null when the entity declares none
     * @return the reading
     */
    static Map<String, Object> numberOrId(String owner, String entity, String key, String number) {
        Map<String, Object> reading = of(KIND, "numberOrId", "owner", owner, "entity", entity, "key", key);
        if (number != null) {
            reading.put("number", number);
        }
        return reading;
    }

    /**
     * The moment a task's boundary timer fires: the record's date field, read at task entry.
     *
     * @param property the PascalCased date field
     * @param shape {@code date} or {@code timestamp}
     * @return the reading
     */
    static Map<String, Object> due(String property, String shape) {
        return of(KIND, "due", "property", property, "shape", shape);
    }

    /**
     * A map iterating in the order its keys are written here.
     *
     * @param keysAndValues the keys and values, alternating
     * @return the map
     */
    private static Map<String, Object> of(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("A reading is spelled as key/value pairs, got " + keysAndValues.length + " arguments");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keysAndValues.length; index += 2) {
            map.put(String.valueOf(keysAndValues[index]), keysAndValues[index + 1]);
        }
        return map;
    }
}
