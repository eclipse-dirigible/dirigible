/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A small, typed, fluent query criteria for {@link JavaRepository#findAll(Criteria)}. It is the
 * type-safe alternative to hand-writing HQL: each condition binds its value as a named parameter,
 * so values can never be injected, and property names are validated to be plain identifiers.
 *
 * <p>
 * Example (client / generated glue code):
 * {@code repository.findAll(Criteria.create().lt("dueOn", today).eq("status", "ACTIVE").orderByDesc("dueOn"));}
 *
 * <p>
 * Conditions are combined with {@code AND} in declaration order. The criteria renders to an HQL
 * fragment ({@link #append(String)}) plus a parameter map ({@link #parameters()}); it holds no
 * database state and is reusable.
 */
public final class Criteria {

    /**
     * Property names are plain identifiers, optionally dotted for a nested path (e.g.
     * {@code member.name}).
     */
    private static final Pattern PROPERTY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    private final List<String> conditions = new ArrayList<>();
    private final List<String> orderings = new ArrayList<>();
    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private int parameterIndex;

    private Criteria() {}

    /**
     * @return a new, empty criteria
     */
    public static Criteria create() {
        return new Criteria();
    }

    /**
     * Equals ({@code property = value}).
     *
     * @param property the entity property name
     * @param value the value to match
     * @return this criteria
     */
    public Criteria eq(String property, Object value) {
        return binary(property, "=", value);
    }

    /**
     * Not equals ({@code property <> value}).
     *
     * @param property the entity property name
     * @param value the value to exclude
     * @return this criteria
     */
    public Criteria ne(String property, Object value) {
        return binary(property, "<>", value);
    }

    /**
     * Greater than ({@code property > value}).
     *
     * @param property the entity property name
     * @param value the lower bound (exclusive)
     * @return this criteria
     */
    public Criteria gt(String property, Object value) {
        return binary(property, ">", value);
    }

    /**
     * Greater than or equal ({@code property >= value}).
     *
     * @param property the entity property name
     * @param value the lower bound (inclusive)
     * @return this criteria
     */
    public Criteria ge(String property, Object value) {
        return binary(property, ">=", value);
    }

    /**
     * Less than ({@code property < value}).
     *
     * @param property the entity property name
     * @param value the upper bound (exclusive)
     * @return this criteria
     */
    public Criteria lt(String property, Object value) {
        return binary(property, "<", value);
    }

    /**
     * Less than or equal ({@code property <= value}).
     *
     * @param property the entity property name
     * @param value the upper bound (inclusive)
     * @return this criteria
     */
    public Criteria le(String property, Object value) {
        return binary(property, "<=", value);
    }

    /**
     * SQL {@code LIKE} ({@code property like pattern}); the pattern supplies its own wildcards.
     *
     * @param property the entity property name
     * @param pattern the LIKE pattern (e.g. {@code "%foo%"})
     * @return this criteria
     */
    public Criteria like(String property, String pattern) {
        return binary(property, "like", pattern);
    }

    /**
     * SQL {@code BETWEEN} ({@code property between lower and upper}), bounds inclusive.
     *
     * @param property the entity property name
     * @param lower the lower bound
     * @param upper the upper bound
     * @return this criteria
     */
    public Criteria between(String property, Object lower, Object upper) {
        validate(property);
        String low = bind(lower);
        String high = bind(upper);
        conditions.add(property + " between :" + low + " and :" + high);
        return this;
    }

    /**
     * SQL {@code IN} ({@code property in (values)}).
     *
     * @param property the entity property name
     * @param values the candidate values; an empty collection matches nothing
     * @return this criteria
     */
    public Criteria in(String property, Collection<?> values) {
        validate(property);
        String name = bind(values);
        conditions.add(property + " in (:" + name + ")");
        return this;
    }

    /**
     * {@code property is null}.
     *
     * @param property the entity property name
     * @return this criteria
     */
    public Criteria isNull(String property) {
        validate(property);
        conditions.add(property + " is null");
        return this;
    }

    /**
     * {@code property is not null}.
     *
     * @param property the entity property name
     * @return this criteria
     */
    public Criteria isNotNull(String property) {
        validate(property);
        conditions.add(property + " is not null");
        return this;
    }

    /**
     * Add an ascending {@code ORDER BY} on the property (applied in call order).
     *
     * @param property the entity property name
     * @return this criteria
     */
    public Criteria orderByAsc(String property) {
        validate(property);
        orderings.add(property + " asc");
        return this;
    }

    /**
     * Add a descending {@code ORDER BY} on the property (applied in call order).
     *
     * @param property the entity property name
     * @return this criteria
     */
    public Criteria orderByDesc(String property) {
        validate(property);
        orderings.add(property + " desc");
        return this;
    }

    /**
     * Append this criteria's {@code WHERE} and {@code ORDER BY} clauses to a {@code from ...} prefix.
     *
     * @param fromClause the HQL {@code from <entity>} prefix
     * @return the full HQL query string
     */
    public String append(String fromClause) {
        StringBuilder hql = new StringBuilder(fromClause);
        if (!conditions.isEmpty()) {
            hql.append(" where ")
               .append(String.join(" and ", conditions));
        }
        if (!orderings.isEmpty()) {
            hql.append(" order by ")
               .append(String.join(", ", orderings));
        }
        return hql.toString();
    }

    /**
     * @return the named-parameter bindings collected by the conditions, in insertion order
     */
    public Map<String, Object> parameters() {
        return parameters;
    }

    private Criteria binary(String property, String operator, Object value) {
        validate(property);
        String name = bind(value);
        conditions.add(property + " " + operator + " :" + name);
        return this;
    }

    private String bind(Object value) {
        String name = "p" + parameterIndex++;
        parameters.put(name, value);
        return name;
    }

    private static void validate(String property) {
        if (property == null || !PROPERTY.matcher(property)
                                         .matches()) {
            throw new IllegalArgumentException("Invalid property name: [" + property + "]");
        }
    }
}
