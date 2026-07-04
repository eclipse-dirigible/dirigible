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

import java.util.ArrayList;
import java.util.List;

/**
 * Report / aggregation. {@link #source} names the entity to aggregate; {@link #dimensions} are the
 * grouping columns, {@link #measures} the aggregation expressions ({@code count(*)},
 * {@code sum(total)}). {@link #filter} is an optional WHERE-style predicate.
 */
public class ReportIntent {

    private String name;
    private String source;
    private List<String> dimensions = new ArrayList<>();
    private List<String> measures = new ArrayList<>();
    private String filter;
    private String description;
    /**
     * Whether this report gets a tile on the home dashboard. Absent (the default) → shown;
     * {@code dashboard: false} excludes it (it still appears in the sidebar Reports section).
     */
    private Boolean dashboard;
    /**
     * Optional dashboard KPI derived from this report; when present, the dashboard shows the KPI tile
     * instead of the report's preview tile.
     */
    private WidgetIntent widget;
    /**
     * Optional chart rendering for the report page. Absent (the default) → a data table;
     * {@code chart: bar} (or {@code line}/{@code pie}/{@code doughnut}/{@code polarArea}/{@code radar})
     * renders the aggregated rows as that chart type, labelled by the grouping dimension with one
     * dataset per measure. Carried on the generated {@code .report} and read by the report page.
     */
    private String chart;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions == null ? new ArrayList<>() : dimensions;
    }

    public List<String> getMeasures() {
        return measures;
    }

    public void setMeasures(List<String> measures) {
        this.measures = measures == null ? new ArrayList<>() : measures;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** Whether this report is excluded from the home dashboard ({@code dashboard: false}). */
    public boolean isDashboardExcluded() {
        return Boolean.FALSE.equals(dashboard);
    }

    public Boolean getDashboard() {
        return dashboard;
    }

    public void setDashboard(Boolean dashboard) {
        this.dashboard = dashboard;
    }

    public WidgetIntent getWidget() {
        return widget;
    }

    public void setWidget(WidgetIntent widget) {
        this.widget = widget;
    }

    public String getChart() {
        return chart;
    }

    public void setChart(String chart) {
        this.chart = chart;
    }
}
