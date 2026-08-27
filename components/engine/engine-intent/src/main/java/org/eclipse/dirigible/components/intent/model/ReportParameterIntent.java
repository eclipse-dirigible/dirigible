/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.Locale;

/**
 * A user-set report parameter: an input rendered above the report whose value the generated query
 * binds into its {@code WHERE} - a from/to window bound, an amount threshold, a name search.
 *
 * <p>
 * {@link #target} is the field the parameter filters ({@code dueOn}, or a one-hop
 * {@code relation.field} path like {@code member.name}) and {@link #op} how it compares
 * ({@code ge}/{@code le}/{@code eq}/{@code like}). {@link #initial} is the value bound when the
 * user leaves the input empty, so it is what the report shows unparameterized: a window bound and a
 * {@code like} search have a neutral default and need none, while an {@code eq} selector and a
 * numeric bound have no neutral value and must declare one.
 *
 * <p>
 * {@link #type} is optional - the target field types the parameter - and is validated against it
 * when declared.
 */
public class ReportParameterIntent {

    /** The parameter name: the input's label source and the named marker the query binds. */
    private String name;
    /**
     * Optional declared type ({@code date}, {@code timestamp}, {@code number}, {@code string}),
     * validated against {@link #target}'s own type. Absent -> derived from the target.
     */
    private String type;
    /** The filtered field: a field of the report's source, or a one-hop {@code relation.field} path. */
    private String target;
    /**
     * How the value compares against {@link #target}: {@code ge}, {@code le}, {@code eq}, {@code like}.
     */
    private String op;
    /** The value bound when the input is left empty. */
    private String initial;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    /** The authored comparison, trimmed and lower-cased, or {@code null} when none is declared. */
    public String getNormalizedOp() {
        return normalized(op);
    }

    /** The authored type, trimmed and lower-cased, or {@code null} when none is declared. */
    public String getNormalizedType() {
        return normalized(type);
    }

    /** The authored target, trimmed, or {@code null} when none is declared. */
    public String getNormalizedTarget() {
        return target == null || target.isBlank() ? null : target.trim();
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null
                : value.trim()
                       .toLowerCase(Locale.ROOT);
    }
}
