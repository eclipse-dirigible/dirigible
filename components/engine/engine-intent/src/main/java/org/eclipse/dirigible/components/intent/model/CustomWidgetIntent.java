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

/**
 * A custom dashboard widget (top-level {@code widgets:} block) whose content the developer supplies
 * — the escape hatch beside the report-attached KPIs ({@code reports[].widget}):
 * <ul>
 * <li>{@code kind: kpi} — a compact number tile; {@link #url} is a REST endpoint (typically a
 * client-Java {@code @Controller} under the project's {@code custom/} folder) returning
 * {@code {value, description?}};</li>
 * <li>{@code kind: page} — a large tile embedding the HTML page at {@link #url} (like a report
 * preview tile).</li>
 * </ul>
 * The kind implies how the URL is consumed (JSON fetch vs iframe) — there is no separate source
 * type.
 */
public class CustomWidgetIntent {

    private String name;
    /** {@code kpi} (a number tile fed by a REST endpoint) or {@code page} (an embedded HTML page). */
    private String kind;
    /** Same-origin URL of the REST endpoint ({@code kpi}) or the HTML page ({@code page}). */
    private String url;
    /** Tile label; defaults to the humanized name. */
    private String label;
    /** Lucide icon name; defaults to {@code gauge}. */
    private String icon;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
