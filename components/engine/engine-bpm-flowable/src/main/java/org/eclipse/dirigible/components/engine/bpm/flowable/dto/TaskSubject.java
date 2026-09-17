/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Where a task's row can read the business identity of the record it is about - the document's
 * number, its counterparty and its total, instead of the bare {@code Ref <id>} the BPM business key
 * left behind (issue #7077).
 * <p>
 * It carries LOCATORS, never values: the record's own REST URL and id, plus the properties to show
 * and how to read each one. The client resolves them live, exactly as the task form resolves the
 * record it edits - a subject snapshotted when the process started would state the total of a
 * document whose lines are added afterwards.
 * <p>
 * Everything here comes from process variables the generated trigger seeded: {@code __entityUrl} /
 * {@code __entityId} (the record), {@code __subjectFields} (the {@code <Property>:<kind>} list the
 * intent derived from the entity) and, per relation property, the {@code __<Property>EntityUrl} /
 * {@code __<Property>EntityLabel} pair the task form already uses to render a foreign key as a
 * name. A process that seeds none - a hand-authored BPMN, a deployment generated before #7077 - has
 * no subject and its rows keep reading as they did.
 *
 * @param url the record's REST controller URL
 * @param id the record's primary key
 * @param fields the properties to show, in reading order
 */
public record TaskSubject(String url, String id, List<Field> fields) {

    /** The process variable naming the properties a task's subject line is built from. */
    private static final String SUBJECT_FIELDS = "__subjectFields";

    /** The process variable holding the record's REST controller URL. */
    private static final String ENTITY_URL = "__entityUrl";

    /** The process variable holding the record's primary key. */
    private static final String ENTITY_ID = "__entityId";

    /** The kind of a property that is a to-one relation, resolved to the target's label. */
    private static final String RELATION = "relation";

    /**
     * One property of a subject line.
     *
     * @param property the property to read off the record
     * @param kind how to render it - {@code number}, {@code date}, {@code relation} or {@code text}
     * @param url for a {@code relation}, the target's REST controller URL; {@code null} otherwise
     * @param label for a {@code relation}, the target property to show; {@code null} otherwise
     */
    public record Field(String property, String kind, String url, String label) {
    }

    /**
     * Reads the subject a generated trigger seeded into the process variables.
     *
     * @param variables the task's process variables
     * @return the subject, or {@code null} when the process declares none
     */
    public static TaskSubject from(Map<String, Object> variables) {
        String declaration = text(variables.get(SUBJECT_FIELDS));
        String url = text(variables.get(ENTITY_URL));
        String id = key(variables.get(ENTITY_ID));
        if (declaration == null || url == null || id == null) {
            return null;
        }
        List<Field> fields = new ArrayList<>();
        for (String entry : declaration.split(",")) {
            int separator = entry.lastIndexOf(':');
            if (separator <= 0) {
                continue;
            }
            String property = entry.substring(0, separator)
                                   .trim();
            String kind = entry.substring(separator + 1)
                               .trim();
            if (property.isEmpty()) {
                continue;
            }
            if (RELATION.equals(kind)) {
                // The same locator pair the task form resolves a foreign key through; without it the
                // relation would render as a raw id, so the property is dropped instead.
                String relationUrl = text(variables.get("__" + property + "EntityUrl"));
                String relationLabel = text(variables.get("__" + property + "EntityLabel"));
                if (relationUrl == null || relationLabel == null) {
                    continue;
                }
                fields.add(new Field(property, RELATION, relationUrl, relationLabel));
            } else {
                fields.add(new Field(property, kind, null, null));
            }
        }
        return fields.isEmpty() ? null : new TaskSubject(url, id, fields);
    }

    /**
     * The record's key, as the whole number it is (issue #7373). An instance started before the
     * variable store stopped widening an untyped JSON number to a {@code Double} carries the key as
     * {@code 33.0}, and the generated controller's integer path parameter refuses that - so the card
     * could not load the record it exists to show. Anything that is not an integral number is read as
     * it stands.
     *
     * @param value the raw {@code __entityId} variable
     * @return the key as text, or null when there is none
     */
    private static String key(Object value) {
        if (value instanceof Double || value instanceof Float) {
            double raw = ((Number) value).doubleValue();
            if (Double.isFinite(raw) && raw == Math.rint(raw)) {
                return String.valueOf((long) raw);
            }
        }
        return text(value);
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value)
                            .trim();
        return text.isEmpty() ? null : text;
    }
}
