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
     * the report declares the two date parameters on the generated {@code .report}. {@code statement}
     * is the statutory statement shape over the same signed ledger: the same window and amount fields,
     * but the output is the fixed {@link #lines} of a balance sheet or an income statement rather than
     * one row per dimension value. {@code ledger} is the account analysis (account card) over the same
     * ledger: the posted lines of ONE {@link #account} in date order with a running balance, framed by
     * the opening balance at the window start and the closing balance at its end. Absent (the default)
     * -> a plain aggregation report from {@link #measures}.
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
    /**
     * {@code kind: balance}: an extra grouping dimension that buckets each ledger line by the accounts
     * on the OPPOSITE side of the same document - the general ledger's "in correspondence with" axis
     * (e.g. account 411's debit turnover split by the credit accounts it corresponded with). The
     * correspondent line is a sibling row of the source, so this resolves like a dimension but against
     * that sibling: a field of the source, a {@code relation.field} path or a bare to-one relation.
     *
     * <p>
     * The document the lines share is the first hop of {@link #date} - the source's to-one relation to
     * its journal entry / voucher - so a balance report declaring this must take its date over that
     * relation. A compound entry (M debit lines against N credit lines) is allocated
     * <b>proportionally</b> by the counter-side amounts, and a line with no counter side at all lands
     * in one empty bucket, so each account's totals across the correspondence buckets still add up to
     * the same figures the plain balance report shows for the same window.
     */
    private String correspondence;
    /**
     * {@code kind: ledger}: the source's to-one relation to the account whose lines the report lists -
     * bound at runtime as the {@code account} report parameter (the account's key). When the account
     * entity declares a {@code hierarchy}, a group account lists the lines of every account below it.
     *
     * <p>
     * {@code kind: statement}: the account-code field the statement groups the ledger by - a
     * {@code string} field of the source or a one-hop {@code relation.field} path to it (e.g.
     * {@code account.code}). It is the code the {@link StatementLineIntent#getAccounts() line
     * selectors} match against, so it is the chart-of-accounts code and never the display name.
     */
    private String account;
    /**
     * {@code kind: statement}: switches the statement to the CASH BASIS - the direct-method cash-flow
     * statement. The value is an account selector over the {@link #account} code, in the line
     * selectors' own grammar ({@code 50*}, {@code 501-503}, {@code 5010}), naming the cash accounts.
     * The lines then read not an account's own balance but the cash its postings carried: for every
     * entry touching a cash account, each NON-cash line of the entry is attributed its proportional
     * share of the entry's cash movement - a credit to it is cash received from it (its
     * {@code periodCredit}), a debit is cash paid to it ({@code periodDebit}). Over a balanced entry
     * the shares add up exactly to the entry's net cash movement, so the lines of a complete statement
     * reconcile with the change in the cash accounts' balance over the window. Only the {@code period}
     * measures apply - a cash flow is a movement within the window - and a transfer between two cash
     * accounts is no cash flow at all.
     */
    private String cashAccounts;
    /**
     * {@code kind: statement}: the statement's fixed lines, in the order they are rendered - each one
     * either reading the ledger through an account selector or computed from other lines.
     */
    private List<StatementLineIntent> lines = new ArrayList<>();
    private List<String> dimensions = new ArrayList<>();
    /**
     * {@code kind: ledger}: the per-line columns shown between the date and the amounts - fields of the
     * source, one-hop {@code relation.field} paths, or bare to-one relations (shown by their label).
     */
    private List<String> columns = new ArrayList<>();
    private List<String> measures = new ArrayList<>();
    /**
     * User-set parameters rendered as inputs above the report and bound into the generated query's
     * {@code WHERE} - a from/to window bound, an amount threshold, a name search. Empty (the default)
     * -> the report takes no input beyond the generic per-column filters.
     */
    private List<ReportParameterIntent> parameters = new ArrayList<>();
    /**
     * Optional aging shape - open documents bucketed by days past due as of a report date, one column
     * per bucket beside the dimensions (aged receivables / payables). See {@link ReportAgingIntent}.
     */
    private ReportAgingIntent aging;
    /**
     * The predicate the report's rows satisfy. Besides field comparisons it accepts the relative
     * moments of {@link org.eclipse.dirigible.components.intent.generator.report.ReportMoments} -
     * {@code CURRENT_DATE-P30D}, {@code START_OF_YEAR} - so a report can cover "this year" or "the last
     * 30 days" without a parameter (dirigible #8435).
     */
    private String filter;
    /**
     * The order of the report's rows: each term names one of the report's {@link #dimensions} or
     * {@link #measures} exactly as authored ({@code customer.name}, {@code sum(quantity)}), optionally
     * with {@code dir: desc}. Absent, a report with a {@code month(x)} / {@code year(x)} dimension is
     * ordered by it ascending, and any other report keeps the database's order (dirigible #8435).
     */
    private List<OrderByIntent> orderBy = new ArrayList<>();
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

    /** Whether this is a financial statement report ({@code kind: statement}). */
    public boolean isStatement() {
        return kind != null && "statement".equalsIgnoreCase(kind.trim());
    }

    /** Whether this is a line-level account analysis ({@code kind: ledger}). */
    public boolean isLedger() {
        return kind != null && "ledger".equalsIgnoreCase(kind.trim());
    }

    /**
     * Whether this report reads the signed ledger - the three kinds sharing {@link #date} /
     * {@link #debit} / {@link #credit} and the {@code fromDate}/{@code toDate} window.
     */
    public boolean isLedgerKind() {
        return isBalance() || isStatement() || isLedger();
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

    public String getCorrespondence() {
        return correspondence;
    }

    public void setCorrespondence(String correspondence) {
        this.correspondence = correspondence;
    }

    /**
     * Whether this report reads the correspondent accounts of the same document - a balance report's
     * grouping axis, or a ledger's per-line counter-account column.
     */
    public boolean hasCorrespondence() {
        return (isBalance() || isLedger()) && correspondence != null && !correspondence.isBlank();
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCashAccounts() {
        return cashAccounts;
    }

    public void setCashAccounts(String cashAccounts) {
        this.cashAccounts = cashAccounts;
    }

    /** Whether this statement is on the cash basis - a direct-method cash-flow statement. */
    public boolean isCashBasis() {
        return isStatement() && cashAccounts != null && !cashAccounts.isBlank();
    }

    public List<StatementLineIntent> getLines() {
        return lines;
    }

    public void setLines(List<StatementLineIntent> lines) {
        this.lines = lines == null ? new ArrayList<>() : lines;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions == null ? new ArrayList<>() : dimensions;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns == null ? new ArrayList<>() : columns;
    }

    public List<String> getMeasures() {
        return measures;
    }

    public List<OrderByIntent> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<OrderByIntent> orderBy) {
        this.orderBy = orderBy == null ? new ArrayList<>() : orderBy;
    }

    /**
     * Whether the report AGGREGATES - a ledger kind, an aging report, or any declared measure. Its rows
     * are then groups, not records, which is what a {@code kind: count} dashboard widget must not
     * confuse (dirigible #7102).
     */
    public boolean isAggregated() {
        return isLedgerKind() || isAging() || measures.stream()
                                                      .anyMatch(measure -> measure != null && !measure.isBlank());
    }

    /**
     * The declared {@code count(*)} measure, or {@code null}. It is the one measure whose per-group
     * values SUM to the report's record count, so it - and not the number of result rows - is what a
     * {@code kind: count} widget over an aggregating report shows.
     */
    public String getCountMeasure() {
        return measures.stream()
                       .filter(ReportIntent::isCountAll)
                       .findFirst()
                       .orElse(null);
    }

    /**
     * Whether the measure is {@code count(*)} - or {@code count()}, the spelling the generator treats
     * alike - however it is spaced. Compared as a whitespace-free key rather than matched by a pattern:
     * the expression is authored text, and a regex of adjacent {@code \s*} runs over it is a
     * polynomial-backtracking surface for no gain.
     */
    private static boolean isCountAll(String measure) {
        if (measure == null) {
            return false;
        }
        StringBuilder compact = new StringBuilder(measure.length());
        for (int i = 0; i < measure.length(); i++) {
            char character = measure.charAt(i);
            if (!Character.isWhitespace(character)) {
                compact.append(character);
            }
        }
        String key = compact.toString()
                            .toLowerCase(Locale.ROOT);
        return "count(*)".equals(key) || "count()".equals(key);
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

    public ReportAgingIntent getAging() {
        return aging;
    }

    public void setAging(ReportAgingIntent aging) {
        this.aging = aging;
    }

    /** Whether this report declares the {@code aging:} shape. */
    public boolean isAging() {
        return aging != null;
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
