/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.print;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.sdk.db.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client SDK for assembling the payload of a print: follows the to-one references of a record whose
 * entity the caller does not own.
 *
 * <p>
 * A generated {@code PrintFeeder} names the fields of the records its own model owns, and copies a
 * record of another model field by field at runtime, so the other model's schema is never compiled
 * into it. That copy leaves the record's own references as bare keys - {@code Customer.City} is an
 * integer - and the feeder cannot resolve them itself without naming the other model's relations.
 * {@link #expandReferences(Object, Map, Map)} resolves them from what the record's class declares
 * ({@link Reference}), so <code>&#123;&#123;document.Customer.City.Name&#125;&#125;</code> prints
 * and the owner of the record remains the only place its relations are written down.
 */
public final class PrintRecords {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintRecords.class);

    /** The key a relation node carries its display label under - what a bare relation path prints. */
    private static final String LABEL_KEY = "__label";

    private PrintRecords() {}

    /**
     * Replaces, on the payload node of a record, the key of every to-one reference the record's class
     * declares with the referenced record's own fields - exactly one hop: the references of the
     * referenced record stay keys.
     *
     * <p>
     * A reference that is not set, whose repository does not exist (its model is not installed, or was
     * generated before the declaration existed) or whose record is gone keeps the key it had, so a
     * payload never loses a value to this call.
     *
     * @param record the loaded record
     * @param target the payload node the record's fields were copied onto
     * @param loaded the records already loaded for this payload, by repository and key - pass the same
     *        map for every call of one payload, so a record referenced from many rows is read once
     */
    public static void expandReferences(Object record, Map<String, Object> target, Map<String, Map<String, Object>> loaded) {
        if (record == null) {
            return;
        }
        for (Field field : record.getClass()
                                 .getFields()) {
            Reference reference = field.getAnnotation(Reference.class);
            if (reference == null || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object key = keyOf(field, record);
            if (key == null) {
                continue;
            }
            String cacheKey = reference.repository() + "#" + key;
            if (!loaded.containsKey(cacheKey)) {
                loaded.put(cacheKey, load(reference, key, record.getClass()
                                                                .getClassLoader()));
            }
            Map<String, Object> referenced = loaded.get(cacheKey);
            if (referenced != null) {
                // A copy per node: the label is the REFERENCE's, and two references may name one record.
                Map<String, Object> node = new LinkedHashMap<>(referenced);
                if (!reference.label()
                              .isEmpty()) {
                    node.put(LABEL_KEY, referenced.get(reference.label()));
                }
                target.put(field.getName(), node);
            }
        }
    }

    private static Object keyOf(Field field, Object record) {
        try {
            return field.get(record);
        } catch (IllegalAccessException | RuntimeException ex) {
            LOGGER.warn(
                    "Could not read the reference [{}] of [{}] for the print payload - its key is kept", field.getName(), record.getClass()
                                                                                                                                .getName(),
                    ex);
            return null;
        }
    }

    /**
     * Loads a referenced record through its repository, as the generated code of its own model would,
     * so the repository's reads apply to it (the caller's language overlay among them).
     *
     * @return the record's fields, or {@code null} when it cannot be loaded
     */
    private static Map<String, Object> load(Reference reference, Object key, ClassLoader loader) {
        try {
            Class<?> repositoryClass = Class.forName(reference.repository(), true, loader);
            Object repository = repositoryClass.getDeclaredConstructor()
                                               .newInstance();
            Object referenced = repositoryClass.getMethod("findById", Object.class)
                                               .invoke(repository, key);
            if (referenced == null) {
                return null;
            }
            Map<String, Object> fields = new LinkedHashMap<>();
            copyFields(referenced, fields);
            return fields;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            LOGGER.warn("Could not load [{}] through [{}] for the print payload - the reference keeps its key", key, reference.repository(),
                    ex);
            return null;
        }
    }

    /**
     * Dates become their ISO string, everything else stays raw - the shape a feeder gives its own
     * fields.
     */
    private static void copyFields(Object record, Map<String, Object> target) throws IllegalAccessException {
        for (Field field : record.getClass()
                                 .getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value = field.get(record);
            boolean temporal = value instanceof Temporal || value instanceof Date;
            target.put(field.getName(), temporal ? value.toString() : value);
        }
    }
}
