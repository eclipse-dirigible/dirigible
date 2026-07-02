/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The immutable, insertion-ordered attribute collection of an AST node. Attribute values are kept
 * as plain strings exactly as authored — Mustache placeholders like <code>{{invoice.number}}</code>
 * are not evaluated; validation and data binding happen in later layers.
 */
public final class Attributes {

    /** The shared empty collection. */
    public static final Attributes EMPTY = new Attributes(List.of());

    private final List<Attribute> attributes;
    private final Map<String, Attribute> byName;

    /**
     * A single attribute as authored in the template.
     *
     * @param name the attribute name
     * @param value the raw attribute value (placeholders untouched)
     * @param position the position of the attribute name in the source
     */
    public record Attribute(String name, String value, SourcePosition position) {
    }

    private Attributes(List<Attribute> attributes) {
        this.attributes = attributes;
        Map<String, Attribute> map = new LinkedHashMap<>();
        for (Attribute attribute : attributes) {
            if (map.putIfAbsent(attribute.name(), attribute) != null) {
                throw new IllegalArgumentException("Duplicate attribute '" + attribute.name() + "'");
            }
        }
        this.byName = map;
    }

    /**
     * Creates an attribute collection preserving the given order.
     *
     * @param attributes the attributes in document order
     * @return the collection
     */
    public static Attributes of(List<Attribute> attributes) {
        return attributes.isEmpty() ? EMPTY : new Attributes(List.copyOf(attributes));
    }

    /**
     * The raw value of the named attribute.
     *
     * @param name the attribute name
     * @return the value, or {@code null} when the attribute is absent
     */
    public String get(String name) {
        Attribute attribute = byName.get(name);
        return attribute == null ? null : attribute.value();
    }

    /**
     * The raw value of the named attribute with a fallback.
     *
     * @param name the attribute name
     * @param defaultValue the value to return when the attribute is absent
     * @return the value or the fallback
     */
    public String getOrDefault(String name, String defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * Whether the named attribute is present.
     *
     * @param name the attribute name
     * @return {@code true} when present
     */
    public boolean has(String name) {
        return byName.containsKey(name);
    }

    /**
     * The source position of the named attribute.
     *
     * @param name the attribute name
     * @return the position, or {@code null} when the attribute is absent
     */
    public SourcePosition positionOf(String name) {
        Attribute attribute = byName.get(name);
        return attribute == null ? null : attribute.position();
    }

    /**
     * The attribute names in document order.
     *
     * @return an unmodifiable, insertion-ordered set
     */
    public Set<String> names() {
        return byName.keySet();
    }

    /**
     * The attributes in document order.
     *
     * @return an unmodifiable list
     */
    public List<Attribute> asList() {
        return attributes;
    }

    /**
     * The number of attributes.
     *
     * @return the count
     */
    public int size() {
        return attributes.size();
    }

    /**
     * Whether the collection is empty.
     *
     * @return {@code true} when no attributes are present
     */
    public boolean isEmpty() {
        return attributes.isEmpty();
    }
}
