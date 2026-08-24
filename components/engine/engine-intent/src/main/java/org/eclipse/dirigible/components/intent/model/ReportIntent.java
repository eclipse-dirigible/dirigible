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
import java.util.Locale;

/**
 * Report / aggregation. {@link #source} names the entity to aggregate; {@link #dimensions} are the
 * grouping columns, {@link #measures} the aggregation expressions ({@code count(*)},
 * {@code sum(total)}). {@link #filter} is an optional WHERE-style predicate.
 */
public class ReportIntent {

    private String name;
    private String source;
    /**
     * Optional report kind. {@code balance} is the accounting balance shape: opening / period / closing
     * debit and credit totals per dimension over the runtime {@code fromDate}/{@code toDate} window -
     * {@link #date} drives the window, {@link #debit}/{@link #credit} are the summed amount fields, and
     * the report declares the two date parameters on the generated {@code .report}. Absent (the
     * default) -> a plain aggregation report from {@link #measures}.
     */
    private String kind;
    /**
     * {@code kind: balance}: the {@code date}-typed field driving the period window - a field of the
     * source or a one-hop {@code relation.field} path to it (e.g. {@code journalEntry.entryDate} on a
     * journal-entry item).
     */
    private String date;
    /** {@code kind: balance}: the numeric source field holding the debit amount. */
    private String debit;
    /** {@code kind: balance}: the numeric source field holding the credit amount. */
    private String credit;
    private List<String> dimensions = new ArrayList<>();
    private List<String> measures = new ArrayList<>();
    /**
     * User-set parameters rendered as inputs above the report and bound into the generated query's
     * {@code WHERE} - a from/to window bound, an amount threshold, a name search. Empty (the default)
     * -> the report takes no input beyond the generic per-column filters.
     */
    private List<ReportParameterIntent> parameters = new ArrayList<>();
    private String filter;
    /**
     * Which lifecycle rows of the source this report counts, when the source carries a
     * {@code function: EntityStatus}: a {@link LifecycleStages stage} name ({@code live} - the safe
     * default for an aggregation - or {@code draft} / {@code cancelled} / {@code void}) restricts the
     * query to the statuses classified with it, and {@code all} is the explicit opt-out a report that
     * is ABOUT the lifecycle declares. Absent, an aggregating report over a stage-classified
     * nomenclature defaults to {@code live} - so a draft or voided document cannot silently inflate a
     * total - and one whose dimensions or {@code filter} already reference the status is left alone.
     */
    private String scope;
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

    /** Whether this is a balance report ({@code kind: balance}). */
    public boolean isBalance() {
        return kind != null && "balance".equalsIgnoreCase(kind.trim());
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDebit() {
        return debit;
    }

    public void setDebit(String debit) {
        this.debit = debit;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
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

    public List<ReportParameterIntent> getParameters() {
        return parameters;
    }

    public void setParameters(List<ReportParameterIntent> parameters) {
        this.parameters = parameters == null ? new ArrayList<>() : parameters;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    /** The authored lifecycle scope, trimmed and lower-cased, or {@code null} when none is declared. */
    public String getNormalizedScope() {
        return scope == null || scope.isBlank() ? null
                : scope.trim()
                       .toLowerCase(Locale.ROOT);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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
