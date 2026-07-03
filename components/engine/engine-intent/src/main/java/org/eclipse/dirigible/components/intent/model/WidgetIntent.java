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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard KPI attached to a report ({@code reports[].widget}). The report supplies the data
 * (source, dimensions, measures, filter); the widget only says which single number the dashboard
 * tile shows:
 * <ul>
 * <li>{@code kind: count} (the default) — the number of records the report yields;</li>
 * <li>{@code kind: value} — one aggregate cell: {@link #value} names a declared measure and
 * {@link #at} pins dimension columns to a predefined token ({@code now}, resolved client-side and
 * type-aware) or a literal;</li>
 * <li>{@code kind: list} — the report's first {@link #limit} rows as a compact table tile.</li>
 * </ul>
 */
public class WidgetIntent {

    /** {@code count} (default), {@code value} or {@code list}. */
    private String kind;
    /** For {@code kind: value}: the declared measure supplying the number, e.g. {@code sum(total)}. */
    private String value;
    /** Dimension pins: authored dimension → token ({@code now}) or literal. */
    private Map<String, Object> at = new LinkedHashMap<>();
    /** Tile label; defaults to the report label. */
    private String label;
    /** Lucide icon name; defaults to {@code gauge}. */
    private String icon;
    /** For {@code kind: list}: how many rows the tile shows (default 5). */
    private Integer limit;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, Object> getAt() {
        return at;
    }

    public void setAt(Map<String, Object> at) {
        this.at = at == null ? new LinkedHashMap<>() : at;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
