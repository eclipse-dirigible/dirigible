/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.StatementSupport;
import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.LifecycleStages;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.model.ReportParameterIntent;
import org.eclipse.dirigible.components.intent.model.StatementLineIntent;
import org.eclipse.dirigible.components.intent.model.WidgetIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits one {@code <report>.report} per {@link ReportIntent}, in the JSON shape the report editor
 * and the report runtime consume (the Dirigible convention): an outer record with {@code name} /
 * {@code alias} (base-table alias) / {@code table} (physical base table) / {@code columns} / a
 * fully materialised SQL {@code query} / {@code conditions} / {@code security}.
 *
 * <p>
 * The report is rooted at {@link ReportIntent#getSource()}. Each dimension and measure resolves to
 * a physical column:
 * <ul>
 * <li>a plain field ({@code dueOn}) -&gt; a column on the source table;</li>
 * <li>a {@code relation.field} path ({@code member.name}) -&gt; an {@code INNER JOIN} to the
 * related entity plus a column on it - this is how a report shows columns from a parent/related
 * entity;</li>
 * <li>a bare to-one relation name ({@code book}) -&gt; the foreign-key column on the source;</li>
 * <li>a measure {@code count(*)} / {@code sum(total)} / {@code avg(price)} /
 * {@code min}/{@code max} -&gt; an aggregate column (and the dimensions become the
 * {@code GROUP BY}).</li>
 * </ul>
 * {@link ReportIntent#getFilter()} becomes the {@code WHERE} predicate, with the intent's field
 * names rewritten to their qualified physical columns (so {@code dueOn <= CURRENT_DATE} ->
 * {@code Loan."LOAN_DUE_ON" <= CURRENT_DATE}); non-field tokens (operators, {@code CURRENT_DATE},
 * literals) pass through untouched.
 *
 * <p>
 * {@link ReportIntent#getParameters()} are the report's <b>user-set</b> inputs: each one is
 * rendered above the report and bound into the same {@code WHERE} as a named marker, so a from/to
 * window bound, an amount threshold or a name search is a report definition rather than a
 * hand-written query - see {@link #parameterConditions}.
 *
 * <p>
 * {@link ReportIntent#getScope()} adds the <b>lifecycle</b> predicate on top of that - see
 * {@link #scopeCondition}: an aggregation over an entity carrying a {@code function: EntityStatus}
 * counts only the statuses classified {@code stage: live} unless it says otherwise, so a draft or a
 * voided document cannot silently inflate a total.
 *
 * <p>
 * Physical table and column identifiers in the generated {@code query} are double-quoted (table
 * aliases are not) so the SQL runs on PostgreSQL, which folds unquoted identifiers to lower case
 * and would otherwise never match the quoted UPPER_SNAKE objects the platform creates; H2 accepts
 * the quoted form too.
 *
 * <p>
 * The document carries the query <b>twice</b> - as this structured model and as the materialised
 * {@code query} - and the report editor rebuilds the query from the model on open to decide whether
 * its visual builder may own it. So the two must agree: {@link #buildQuery} emits the query
 * <i>from</i> the {@code columns} / {@code joins} / {@code conditions} it also writes out, and
 * anything the builder cannot represent is deliberately left out of the model rather than
 * half-emitted, which parks the report in the editor's free-style mode instead of corrupting it on
 * save (dirigible #6675).
 *
 * <p>
 * A dimension bound to a translatable property of a {@code multilingual} entity is read through its
 * sibling <code>&lt;TABLE&gt;_LANG</code> table for the caller's language - see {@link #translate}.
 * Only the SELECT list is overlaid; {@code filter} and the lifecycle scope compile against the base
 * table.
 *
 * <p>
 * Column physical names and the base table mirror what {@code EdmIntentGenerator} emits
 * ({@code <ENTITY>_<FIELD>} columns, {@code <INTENT>_<ENTITY>} table) so the report can never drift
 * from the model. Generation is idempotent - identical input yields byte-identical output.
 */
@Component
@Order(500)
public class ReportIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportIntentGenerator.class);

    /**
     * Pretty-printed JSON with HTML-escaping OFF so the SQL {@code query} keeps literal {@code =} /
     * {@code >} / {@code <} operators (the platform's {@code JsonHelper} escapes them to
     * {@code \\u003d} etc.; valid JSON, but unreadable and unlike the standard {@code .report} files).
     * Maps only - no {@code @Expose} concern.
     */
    private static final Gson REPORT_JSON = new GsonBuilder().setPrettyPrinting()
                                                             .disableHtmlEscaping()
                                                             .create();

    /** {@code aggregate(field)} pattern - aggregate in group 1, field in group 2. */
    private static final Pattern AGGREGATE_EXPRESSION = Pattern.compile("\\s*(\\w+)\\s*\\(\\s*([^)]*)\\s*\\)\\s*");
    private static final Set<String> KNOWN_AGGREGATES = Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");
    private static final Pattern DOTTED_REF = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Pattern SIMPLE_CONDITION = Pattern.compile("^\\s*(\\S+)\\s*(<=|>=|<>|!=|=|<|>)\\s*(.+?)\\s*$");
    /**
     * {@code month(field)} / {@code year(field)} dimension - the bucket function in group 1, field in
     * group 2.
     */
    private static final Pattern DATE_BUCKET = Pattern.compile("\\s*(month|year)\\s*\\(\\s*([^)]+?)\\s*\\)\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * {@code ageing(field, [30, 60, 90])} dimension - the date field in group 1, the comma-separated
     * day thresholds in group 2.
     */
    private static final Pattern AGEING_BUCKET =
            Pattern.compile("\\s*ageing\\s*\\(\\s*([^,\\[]+?)\\s*,\\s*\\[\\s*([^\\]]+?)\\s*\\]\\s*\\)\\s*", Pattern.CASE_INSENSITIVE);

    /** The sibling translation table of a multilingual entity, and its bookkeeping columns. */
    private static final String LANGUAGE_TABLE_SUFFIX = "_LANG";
    private static final String LANGUAGE_ID_COLUMN = "Id";
    private static final String LANGUAGE_CODE_COLUMN = "Language";

    /**
     * The named parameter the generated report repository binds from the caller's
     * {@code Accept-Language}. It is the only runtime input the query has that is not a declared report
     * parameter, so its name is fixed on both sides.
     */
    private static final String LANGUAGE_PARAMETER = ":language";

    /** Disqualifies a filter term from the structured decomposition - see {@link #conditions}. */
    private static final Pattern OR_OPERATOR = Pattern.compile("\\bOR\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Every entity join a report resolves is an inner one; the editor's builder offers the other kinds.
     */
    private static final String JOIN_TYPE = "INNER";

    /** A translation table is joined leniently - a row without a translation keeps its base value. */
    private static final String LANGUAGE_JOIN_TYPE = "LEFT";

    /** The SQL type of a date-valued report parameter - a date picker on the report page. */
    private static final String DATE_TYPE = "DATE";

    /** The all-time window bounds a date parameter falls back to when the request carries no value. */
    private static final String NEUTRAL_FROM_DATE = "1900-01-01";
    private static final String NEUTRAL_TO_DATE = "9999-12-31";

    private static final String LIKE_OPERATION = "LIKE";

    /** The report types a parameter binds as a number. */
    private static final Set<String> NUMERIC_TYPES = Set.of("INTEGER", "BIGINT", "DECIMAL");

    /** An authored parameter's {@code op} as its SQL operator. */
    private static final Map<String, String> SQL_OPERATIONS = Map.of("ge", ">=", "le", "<=", "eq", "=", "like", LIKE_OPERATION);

    @Override
    public String name() {
        return "report";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getReports()
                 .isEmpty()) {
            return;
        }
        Set<String> seenFiles = new HashSet<>();
        for (ReportIntent report : model.getReports()) {
            if (report.getName() == null || report.getName()
                                                  .isBlank()) {
                LOGGER.warn("Skipping unnamed report in intent [{}]", IntentNaming.baseName(context));
                continue;
            }
            String fileName = report.getName() + ".report";
            if (!seenFiles.add(fileName)) {
                LOGGER.warn("Duplicate report [{}] in intent [{}] - keeping the first occurrence", report.getName(),
                        IntentNaming.baseName(context));
                continue;
            }
            context.writeModelFile(fileName, REPORT_JSON.toJson(build(context, report)));
        }
    }

    /** Test hook: the assembled {@code .report} document for one report. */
    static Map<String, Object> buildForTest(IntentGenerationContext context, ReportIntent report) {
        return build(context, report);
    }

    private static Map<String, Object> build(IntentGenerationContext context, ReportIntent report) {
        IntentModel model = context.getModel();
        EntityIntent source = entityByName(model, report.getSource());
        String baseAlias = report.getSource() == null ? report.getName() : report.getSource();
        String baseTable = report.getSource() == null ? "" : IntentNaming.tableName(context, report.getSource());

        boolean balance = report.isBalance();
        boolean statement = report.isStatement();
        boolean aggregated = balance || statement || report.getMeasures()
                                                           .stream()
                                                           .anyMatch(m -> m != null && !m.isBlank());

        Map<String, Join> joins = new LinkedHashMap<>();
        List<Map<String, Object>> columns = new ArrayList<>();
        // Widget resolution inputs: the column each authored dimension/measure expression produced
        // (keyed by the whitespace/case-insensitive expression), plus the date-bucket function of a
        // month(x)/year(x) dimension so the KPI runtime can resolve the `now` token type-aware.
        Map<String, WidgetDimension> dimensionColumns = new LinkedHashMap<>();
        Map<String, Map<String, Object>> measureColumns = new LinkedHashMap<>();

        for (String dimension : report.getDimensions()) {
            if (dimension == null || dimension.isBlank()) {
                continue;
            }
            // A month(field)/year(field) dimension buckets a date for aggregation: month emits the
            // sortable YYYYMM integer (EXTRACT(YEAR) * 100 + EXTRACT(MONTH) - e.g. 202607), year the
            // plain year. EXTRACT is standard SQL (H2, PostgreSQL); SQL Server does not support it -
            // date-bucketed reports are an H2/PostgreSQL feature for now.
            Matcher bucket = DATE_BUCKET.matcher(dimension.trim());
            if (bucket.matches()) {
                String function = bucket.group(1)
                                        .toLowerCase(Locale.ROOT);
                String fieldReference = bucket.group(2)
                                              .trim();
                ColumnRef ref = resolve(context, model, source, baseAlias, fieldReference);
                registerJoin(joins, ref);
                String expression = "month".equals(function)
                        ? "(EXTRACT(YEAR FROM " + ref.qualified() + ") * 100 + EXTRACT(MONTH FROM " + ref.qualified() + "))"
                        : "EXTRACT(YEAR FROM " + ref.qualified() + ")";
                String alias = humanize(function + " " + fieldReference.replace('.', ' '));
                Map<String, Object> bucketColumn =
                        column(ref.tableAlias, alias, ref.physicalColumn, "INTEGER", "NONE", aggregated, expression);
                columns.add(bucketColumn);
                dimensionColumns.put(expressionKey(dimension), new WidgetDimension(bucketColumn, function));
                continue;
            }
            // An ageing(field, [30, 60, 90]) dimension buckets a date by how long ago it fell, so the
            // receivables ageing family (0-30 / 31-60 / 61-90 / 90+) is a report definition instead of
            // hand SQL. Emitted as a CASE over DATE BOUNDARIES (`field > CURRENT_DATE - INTERVAL 'n'
            // DAY`), deliberately NOT as day-count arithmetic: `CURRENT_DATE - field` yields an integer
            // on PostgreSQL but an INTERVAL on H2, so comparing it to a number is not portable - and the
            // .report query is a static string with no dialect to switch on. The interval form is
            // standard SQL and verified on both CI databases.
            Matcher ageing = AGEING_BUCKET.matcher(dimension.trim());
            if (ageing.matches()) {
                String fieldReference = ageing.group(1)
                                              .trim();
                List<Integer> thresholds = ageingThresholds(ageing.group(2));
                ColumnRef ageingRef = resolve(context, model, source, baseAlias, fieldReference);
                registerJoin(joins, ageingRef);
                String expression = ageingExpression(ageingRef.qualified(), thresholds);
                String alias = humanize("ageing " + fieldReference.replace('.', ' '));
                Map<String, Object> ageingColumn =
                        column(ageingRef.tableAlias, alias, ageingRef.physicalColumn, "VARCHAR", "NONE", aggregated, expression);
                columns.add(ageingColumn);
                dimensionColumns.put(expressionKey(dimension), new WidgetDimension(ageingColumn, "ageing"));
                continue;
            }
            ColumnRef ref = resolve(context, model, source, baseAlias, dimension.trim());
            registerJoin(joins, ref);
            Map<String, Object> dimensionColumn = column(ref.tableAlias, ref.displayAlias, ref.physicalColumn, ref.reportType, "NONE",
                    aggregated, ref.translationExpression());
            columns.add(dimensionColumn);
            dimensionColumns.put(expressionKey(dimension), new WidgetDimension(dimensionColumn, null));
        }
        StatementQuery statementQuery = null;
        if (balance) {
            addBalanceMeasures(context, model, source, baseAlias, report, joins, columns);
        } else if (statement) {
            // The ledger references are resolved (and their joins registered) BEFORE the filter's, so
            // the emitted FROM introduces the statement's own tables first - the order the balance
            // report already establishes.
            statementQuery = prepareStatement(context, model, source, baseAlias, report, joins, columns);
        } else {
            for (String measure : report.getMeasures()) {
                if (measure == null || measure.isBlank()) {
                    continue;
                }
                int before = columns.size();
                addMeasure(context, model, source, baseAlias, measure.trim(), joins, columns);
                if (columns.size() > before) {
                    measureColumns.put(expressionKey(measure), columns.get(before));
                }
            }
        }

        warnOnRestrictedColumns(context, model, source, report);
        String filter = buildWhere(context, model, source, baseAlias, joins, report.getFilter());
        Map<String, Object> scope = scopeCondition(context, model, source, baseAlias, report, aggregated);
        // A statement takes the balance window too: its ledger reduction is the balance report's,
        // and the authored `parameters:` are appended to the same list below.
        List<Map<String, Object>> parameters = report.isLedgerKind() ? balanceParameters() : new ArrayList<>();
        List<Map<String, Object>> parameterConditions = parameterConditions(context, model, source, baseAlias, report, joins, parameters);
        List<Map<String, Object>> conditions = conditions(filter, scope);
        String where;
        if (conditions == null) {
            where = rawWhere(filter, scope, parameterConditions.isEmpty() ? null : predicate(parameterConditions));
        } else {
            conditions.addAll(parameterConditions);
            where = predicate(conditions);
        }
        List<Map<String, Object>> joinRows = joinRows(joins);
        String query = statementQuery == null ? buildQuery(baseTable, baseAlias, joinRows, columns, where)
                : statementQuery.sql(baseTable, baseAlias, joinRows, where);

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("name", report.getName());
        document.put("alias", baseAlias);
        document.put("table", baseTable);
        String tId = translationId(report.getName());
        document.put("tId", tId);
        document.put("label", humanize(report.getName()));
        if (report.getDescription() != null && !report.getDescription()
                                                      .isBlank()) {
            document.put("description", report.getDescription());
            // Externalize the description as its own catalog key so it localizes alongside the label.
            document.put("descriptionTId", tId + "Description");
        }
        // dashboard: false excludes the report's tile from the home dashboard (it still shows in the
        // sidebar). Carried on the .report; the Harmonia reports store reads it.
        document.put("dashboard", report.isDashboardExcluded() ? Boolean.FALSE : Boolean.TRUE);
        // chart: bar (or line/pie/...) makes the report page render the aggregated rows as that chart
        // type instead of a table (the grouping dimension labels the axis, one dataset per measure).
        if (report.getChart() != null && !report.getChart()
                                                .isBlank()) {
            document.put("chart", report.getChart()
                                        .trim());
        }
        if (report.getWidget() != null) {
            document.put("widget", widget(report, dimensionColumns, measureColumns));
        }
        if (balance) {
            // The report kind rides on the .report so the generated page knows to render the
            // balance affordances (window pickers, totals row).
            document.put("kind", "balance");
        } else if (statement) {
            document.put("kind", "statement");
        }
        document.put("columns", columns);
        // A statement's joins and filter live inside its own subquery, not in a SELECT the report
        // editor's visual builder could rebuild - so they are deliberately NOT emitted as the
        // builder-owned model. The builder's round-trip check then fails to reproduce the query and
        // the report opens free-style, where the query string is the source of truth: the honest
        // outcome, and the one that keeps the editor from rewriting the statement into a flat SELECT
        // on save (dirigible #6675).
        if (!statement && !joinRows.isEmpty()) {
            document.put("joins", joinRows);
        }
        document.put("query", query);
        if (!parameters.isEmpty()) {
            document.put("parameters", parameters);
        }
        // Only when the predicate round-trips: an empty `conditions` used to make the editor emit a
        // bare `WHERE`, and a partial one would have silently dropped the rest of the filter.
        if (!statement && conditions != null && !conditions.isEmpty()) {
            document.put("conditions", conditions);
        }
        document.put("security", security(context, report.getName()));
        return document;
    }

    private static void addMeasure(IntentGenerationContext context, IntentModel model, EntityIntent source, String baseAlias,
            String measure, Map<String, Join> joins, List<Map<String, Object>> columns) {
        Matcher matcher = AGGREGATE_EXPRESSION.matcher(measure);
        if (matcher.matches()) {
            String aggregate = matcher.group(1)
                                      .toUpperCase(Locale.ROOT);
            String field = matcher.group(2)
                                  .trim();
            if (KNOWN_AGGREGATES.contains(aggregate)) {
                if (field.isEmpty() || "*".equals(field)) {
                    String alias = aggregate.charAt(0) + aggregate.substring(1)
                                                                  .toLowerCase(Locale.ROOT);
                    // The bare star is the column NAME, so the builder emits COUNT(*) rather than
                    // qualifying it as COUNT(<alias>.*), which H2 rejects as an aggregate argument.
                    columns.add(column(baseAlias, alias, "*", "INTEGER", aggregate, false));
                    return;
                }
                ColumnRef ref = resolve(context, model, source, baseAlias, field);
                registerJoin(joins, ref);
                String alias = humanize(aggregate.toLowerCase(Locale.ROOT) + " " + leaf(field));
                String type = "COUNT".equals(aggregate) ? "INTEGER"
                        : ("MIN".equals(aggregate) || "MAX".equals(aggregate) ? ref.reportType : "DECIMAL");
                columns.add(column(ref.tableAlias, alias, ref.physicalColumn, type, aggregate, false));
                return;
            }
        }
        LOGGER.warn("Measure [{}] did not match the aggregate(field) convention - skipping", measure);
    }

    /**
     * The six balance totals - opening / period / closing debit and credit sums around the runtime
     * {@code :fromDate}/{@code :toDate} window. Opening is strictly before {@code fromDate}, the period
     * is inclusive of both bounds, closing is everything up to and including {@code toDate} - so
     * opening + period = closing for every row. The date and the two amounts resolve like any dimension
     * ({@code relation.field} joins), the window bounds are the named parameters the generated
     * repository binds from the request (or the all-time defaults).
     */
    /**
     * The day thresholds of an {@code ageing(field, [30, 60, 90])} dimension, in the authored order.
     * The parser has already rejected anything but ascending positive integers, so this stays lenient:
     * a value that still does not parse is skipped rather than failing the whole generation.
     *
     * @param raw the comma-separated threshold list
     * @return the thresholds
     */
    private static List<Integer> ageingThresholds(String raw) {
        List<Integer> thresholds = new ArrayList<>();
        for (String token : raw.split(",")) {
            try {
                thresholds.add(Integer.valueOf(token.trim()));
            } catch (NumberFormatException ex) {
                LOGGER.warn("Skipping non-numeric ageing threshold [{}]", token.trim(), ex);
            }
        }
        return thresholds;
    }

    /**
     * The ageing bucket as a portable {@code CASE}: a row falls in the first bucket whose boundary it
     * is still newer than, so {@code [30, 60, 90]} yields {@code 0-30} / {@code 31-60} / {@code 61-90}
     * / {@code 90+}. A null date is bucketed as {@code n/a} rather than falling into the oldest bucket,
     * which would misreport it as maximally overdue.
     *
     * @param qualified the qualified date column
     * @param thresholds the ascending day thresholds
     * @return the CASE expression
     */
    private static String ageingExpression(String qualified, List<Integer> thresholds) {
        StringBuilder expression = new StringBuilder("CASE WHEN ").append(qualified)
                                                                  .append(" IS NULL THEN 'n/a'");
        int previous = 0;
        for (Integer threshold : thresholds) {
            expression.append(" WHEN ")
                      .append(qualified)
                      .append(" > CURRENT_DATE - INTERVAL '")
                      .append(threshold)
                      .append("' DAY THEN '")
                      .append(previous == 0 ? "0" : String.valueOf(previous + 1))
                      .append('-')
                      .append(threshold)
                      .append('\'');
            previous = threshold;
        }
        return expression.append(" ELSE '")
                         .append(previous)
                         .append("+' END")
                         .toString();
    }

    private static void addBalanceMeasures(IntentGenerationContext context, IntentModel model, EntityIntent source, String baseAlias,
            ReportIntent report, Map<String, Join> joins, List<Map<String, Object>> columns) {
        ColumnRef date = resolve(context, model, source, baseAlias, report.getDate()
                                                                          .trim());
        registerJoin(joins, date);
        ColumnRef debit = resolve(context, model, source, baseAlias, report.getDebit()
                                                                           .trim());
        registerJoin(joins, debit);
        ColumnRef credit = resolve(context, model, source, baseAlias, report.getCredit()
                                                                            .trim());
        registerJoin(joins, credit);
        String opening = date.qualified() + " < :fromDate";
        String period = date.qualified() + " >= :fromDate AND " + date.qualified() + " <= :toDate";
        String closing = date.qualified() + " <= :toDate";
        addBalanceColumn(columns, debit, opening, "Opening Debit");
        addBalanceColumn(columns, credit, opening, "Opening Credit");
        addBalanceColumn(columns, debit, period, "Debit");
        addBalanceColumn(columns, credit, period, "Credit");
        addBalanceColumn(columns, debit, closing, "Closing Debit");
        addBalanceColumn(columns, credit, closing, "Closing Credit");
    }

    private static void addBalanceColumn(List<Map<String, Object>> columns, ColumnRef amount, String window, String alias) {
        // COALESCE the amount: a one-sided ledger line (the exactlyOne debit/credit shape) holds
        // NULL on the other side, and SUM over all-NULL yields NULL instead of the 0 a balance shows.
        String expression = "CASE WHEN " + window + " THEN COALESCE(" + amount.qualified() + ", 0) ELSE 0 END";
        columns.add(column(amount.tableAlias, alias, amount.physicalColumn, "DECIMAL", "SUM", false, expression));
    }

    /**
     * The balance window as declared {@code .report} parameters, in the report editor's {@code {name,
     * type, initial}} shape the generated repository already binds. The defaults make an
     * unparameterized call return the all-time balance (empty opening, everything in the period).
     */
    private static List<Map<String, Object>> balanceParameters() {
        List<Map<String, Object>> parameters = new ArrayList<>();
        parameters.add(reportParameter("fromDate", DATE_TYPE, NEUTRAL_FROM_DATE));
        parameters.add(reportParameter("toDate", DATE_TYPE, NEUTRAL_TO_DATE));
        return parameters;
    }

    /**
     * The report's authored {@code parameters:} as the {@code WHERE} terms that bind them, appending
     * each one's declaration to {@code parameters} on the way.
     *
     * <p>
     * A parameter is bound on EVERY call - the generated repository falls back to the declared
     * {@code initial} when the request carries no value - so a term is a plain binary comparison
     * against a named marker and the neutral case is expressed by the default, not by a nullable
     * predicate. That keeps each term representable as a structured {@code conditions} row, so
     * declaring a parameter cannot park the report in the editor's free-style mode.
     *
     * <p>
     * A {@code timestamp} target is compared as a DATE ({@code CAST(col AS DATE)}): the input is a date
     * picker, and comparing the raw instant against midnight of the chosen day would silently drop that
     * day's rows from a {@code le} bound. A {@code like} parameter matches anywhere in the value
     * ({@code '%' || :p || '%'}), which is also what makes the empty default match every row. A
     * NULLABLE target is read through {@link #emptyLiteral} - without it a row holding no value in the
     * target column would drop out of the report the moment a parameter is declared, before the user
     * sets anything, which is the opposite of what the neutral default promises.
     *
     * @param context the generation context
     * @param model the intent model
     * @param source the report's source entity
     * @param baseAlias the base table alias
     * @param report the report
     * @param joins the collected joins - a parameter over a {@code relation.field} path joins like a
     *        dimension
     * @param parameters the collecting parameter declarations
     * @return the parameter conditions, in the authored order
     */
    private static List<Map<String, Object>> parameterConditions(IntentGenerationContext context, IntentModel model, EntityIntent source,
            String baseAlias, ReportIntent report, Map<String, Join> joins, List<Map<String, Object>> parameters) {
        List<Map<String, Object>> conditions = new ArrayList<>();
        for (ReportParameterIntent parameter : report.getParameters()) {
            String name = parameter.getName();
            String target = parameter.getNormalizedTarget();
            String operation = SQL_OPERATIONS.get(parameter.getNormalizedOp());
            if (name == null || name.isBlank() || target == null || operation == null) {
                LOGGER.warn("Skipping incomplete parameter [{}] of report [{}]", name, report.getName());
                continue;
            }
            ColumnRef ref = resolve(context, model, source, baseAlias, target);
            registerJoin(joins, ref);
            boolean timestamp = "TIMESTAMP".equals(ref.reportType);
            String type = timestamp ? DATE_TYPE : ref.reportType;
            String left = timestamp ? "CAST(" + ref.qualified() + " AS DATE)" : ref.qualified();
            if (ref.nullable) {
                left = "COALESCE(" + left + ", " + emptyLiteral(type, operation) + ")";
            }
            String marker = ":" + name.trim();
            String right = LIKE_OPERATION.equals(operation) ? "'%' || " + marker + " || '%'" : marker;
            conditions.add(condition(left, operation, right));
            parameters.add(reportParameter(name.trim(), type, initialValue(parameter, type, operation)));
        }
        return conditions;
    }

    /**
     * The value a parameter binds when the request carries none - the authored {@code initial}, or the
     * neutral default of the comparisons that have one: a date window bound widens to all time and a
     * {@code like} search to the empty pattern, which matches every value. An {@code eq} selector and a
     * numeric bound have no neutral value; the parser requires their {@code initial}, so an empty one
     * here is a report authored before that check and binds as-is.
     *
     * @param parameter the parameter
     * @param type the parameter's SQL type
     * @param operation the SQL operator it compares with
     * @return the initial value
     */
    private static String initialValue(ReportParameterIntent parameter, String type, String operation) {
        if (parameter.getInitial() != null && !parameter.getInitial()
                                                        .isBlank()) {
            return parameter.getInitial()
                            .trim();
        }
        if (DATE_TYPE.equals(type) && !LIKE_OPERATION.equals(operation)) {
            return "<=".equals(operation) ? NEUTRAL_TO_DATE : NEUTRAL_FROM_DATE;
        }
        return "";
    }

    /**
     * What a missing value in the target column reads as, so an untouched parameter cannot hide a row:
     * a number counts as zero and a string as empty, both of which the neutral default still matches,
     * and a date as the far end of the window in the direction of the bound. Only a NULLABLE target is
     * coalesced - a required column keeps the plain, index-friendly comparison.
     *
     * @param type the parameter's SQL type
     * @param operation the SQL operator it compares with
     * @return the SQL literal
     */
    private static String emptyLiteral(String type, String operation) {
        if (DATE_TYPE.equals(type)) {
            return "DATE '" + ("<=".equals(operation) ? NEUTRAL_TO_DATE : NEUTRAL_FROM_DATE) + "'";
        }
        return NUMERIC_TYPES.contains(type) ? "0" : "''";
    }

    private static Map<String, Object> reportParameter(String name, String type, String initial) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("name", name);
        parameter.put("type", type);
        parameter.put("initial", initial);
        return parameter;
    }

    /** The alias of the per-account balance subquery a statement's lines read. */
    private static final String ACCOUNT_BALANCES = "\"ACCOUNT_BALANCES\"";

    /** The alias of the derived table holding the statement's lines. */
    private static final String STATEMENT_LINES_ALIAS = "STATEMENT_LINES";

    /** The same alias, quoted, as the query refers to it. */
    private static final String STATEMENT_LINES = "\"" + STATEMENT_LINES_ALIAS + "\"";

    /** The account-code column the per-account balance subquery exposes to the line selectors. */
    private static final String ACCOUNT_CODE = "\"ACCOUNT_CODE\"";

    /**
     * Resolve a statement's ledger references, register their joins and emit its three output columns.
     *
     * @param context the generation context
     * @param model the intent model
     * @param source the report's source entity
     * @param baseAlias the base-table alias
     * @param report the statement report
     * @param joins the joins collected so far, added to
     * @param columns the emitted columns, appended to
     * @return everything the query assembly needs afterwards
     */
    private static StatementQuery prepareStatement(IntentGenerationContext context, IntentModel model, EntityIntent source,
            String baseAlias, ReportIntent report, Map<String, Join> joins, List<Map<String, Object>> columns) {
        ColumnRef date = resolve(context, model, source, baseAlias, report.getDate()
                                                                          .trim());
        registerJoin(joins, date);
        ColumnRef debit = resolve(context, model, source, baseAlias, report.getDebit()
                                                                           .trim());
        registerJoin(joins, debit);
        ColumnRef credit = resolve(context, model, source, baseAlias, report.getCredit()
                                                                            .trim());
        registerJoin(joins, credit);
        ColumnRef account = resolve(context, model, source, baseAlias, report.getAccount()
                                                                             .trim());
        // Only the entity join, never the language overlay: the account CODE is an identifier the line
        // selectors match on, and matching a translated value would make a statement's lines depend on
        // the reader's language.
        if (account.join != null) {
            joins.putIfAbsent(account.join.alias, account.join);
        }
        columns.add(column(STATEMENT_LINES_ALIAS, "Code", "Code", "CHARACTER VARYING", "NONE", false));
        columns.add(column(STATEMENT_LINES_ALIAS, "Label", "Label", "CHARACTER VARYING", "NONE", false));
        columns.add(column(STATEMENT_LINES_ALIAS, "Amount", "Amount", "DECIMAL", "NONE", false));
        return new StatementQuery(account.qualified(), balanceSums(date, debit, credit), statementLines(report));
    }

    /**
     * The six per-account windowed sums of the statement subquery, in the {@code <sql> as "<column>"}
     * form. The windows are the balance report's, to the token: opening strictly before
     * {@code :fromDate}, the period inclusive of both bounds, closing everything up to {@code :toDate}
     * - so the two kinds cannot disagree about what a period is.
     *
     * @param date the window-driving date column
     * @param debit the debit amount column
     * @param credit the credit amount column
     * @return the select terms
     */
    private static List<String> balanceSums(ColumnRef date, ColumnRef debit, ColumnRef credit) {
        Map<String, String> windows = Map.of("opening", date.qualified() + " < :fromDate", "period",
                date.qualified() + " >= :fromDate AND " + date.qualified() + " <= :toDate", "closing", date.qualified() + " <= :toDate");
        List<String> sums = new ArrayList<>();
        for (StatementSupport.Balance balance : StatementSupport.balanceColumns()) {
            ColumnRef amount = balance.debit() ? debit : credit;
            sums.add("SUM(CASE WHEN " + windows.get(balance.window()) + " THEN COALESCE(" + amount.qualified() + ", 0) ELSE 0 END) as "
                    + balance.column());
        }
        return sums;
    }

    /**
     * The statement's lines, each resolved to the amount expression it selects from the per-account
     * balances. Computed lines are FLATTENED here - a line summing other lines is replaced by their own
     * account terms, recursively - so every emitted line is one aggregate over the same subquery and no
     * line has to be evaluated before another. The parser has already rejected a cycle and an unknown
     * reference; the walk still carries its own path guard, because a cycle reaching the generator
     * would recurse until the stack ran out rather than report anything.
     *
     * @param report the statement report
     * @return the lines, in the authored order
     */
    private static List<StatementLine> statementLines(ReportIntent report) {
        Map<String, StatementLineIntent> byCode = new LinkedHashMap<>();
        for (StatementLineIntent line : report.getLines()) {
            if (line.getCode() != null && !line.getCode()
                                               .isBlank()) {
                byCode.putIfAbsent(line.getCode()
                                       .trim(),
                        line);
            }
        }
        List<StatementLine> lines = new ArrayList<>();
        for (StatementLineIntent line : report.getLines()) {
            List<String> terms = new ArrayList<>();
            collectStatementTerms(line, 1, byCode, terms, new LinkedHashSet<>());
            String amount = terms.isEmpty() ? "0" : String.join(" ", terms);
            lines.add(new StatementLine(line.getCode()
                                            .trim(),
                    line.getLabel()
                        .trim(),
                    amount));
        }
        return lines;
    }

    /**
     * Append the signed account terms a line contributes, following its {@code sum}/{@code less}
     * references down to the leaves.
     *
     * @param line the line to flatten
     * @param sign {@code 1} when the line is added, {@code -1} when it is subtracted
     * @param byCode the statement's lines by code
     * @param terms the emitted terms, appended to - each carrying its own leading sign
     * @param path the codes on the current walk, guarding against a cycle
     */
    private static void collectStatementTerms(StatementLineIntent line, int sign, Map<String, StatementLineIntent> byCode,
            List<String> terms, Set<String> path) {
        if (line == null) {
            return;
        }
        String code = line.getCode() == null ? null
                : line.getCode()
                      .trim();
        if (code != null && !path.add(code)) {
            LOGGER.warn("Statement line [{}] takes part in a cycle - dropping the reference that closes it", code);
            return;
        }
        if (line.isLeaf()) {
            // The parser has already reported anything wrong with the selector, so the issues it
            // collects here are a duplicate of what the author has been told and are discarded.
            List<String> reported = new ArrayList<>();
            StatementSupport.Selector selector = StatementSupport.selector(line.getAccounts(), reported, "");
            StatementSupport.Measure measure = StatementSupport.measure(line.getMeasure());
            if (selector != null && measure != null) {
                terms.add((terms.isEmpty() && sign > 0 ? "" : (sign > 0 ? "+ " : "- ")) + "COALESCE(SUM(CASE WHEN "
                        + selector.sql(ACCOUNT_CODE) + " THEN " + measure.sql() + " ELSE 0 END), 0)");
            }
        } else {
            for (String reference : line.getSum()) {
                collectStatementTerms(byCode.get(trimmed(reference)), sign, byCode, terms, path);
            }
            for (String reference : line.getLess()) {
                collectStatementTerms(byCode.get(trimmed(reference)), -sign, byCode, terms, path);
            }
        }
        if (code != null) {
            path.remove(code);
        }
    }

    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    /** One emitted statement line: its code, its caption and the amount it selects. */
    private record StatementLine(String code, String label, String amount) {
    }

    /**
     * A statement's query: the per-account balances it aggregates and the fixed lines it renders from
     * them.
     *
     * @param accountColumn the qualified account-code column of the source
     * @param balanceSums the six windowed per-account sums, as select terms
     * @param lines the statement's lines, already flattened
     */
    private record StatementQuery(String accountColumn, List<String> balanceSums, List<StatementLine> lines) {

        /**
         * The statement SQL: one subquery reducing the ledger to a balance per account, then one row per
         * declared line reading it.
         *
         * <p>
         * The per-account level is not an optimisation, it is the semantics: a {@code Net} measure nets an
         * account's two sides before the line sums it, so the reduction has to happen per account and
         * exactly once. It is a common table expression for that reason - repeating the subquery per line
         * would re-scan the ledger once per statement line.
         *
         * @param baseTable the physical source table
         * @param baseAlias the source alias
         * @param joins the resolved joins
         * @param where the WHERE predicate restricting which ledger rows count, or null
         * @return the query
         */
        String sql(String baseTable, String baseAlias, List<Map<String, Object>> joins, String where) {
            StringBuilder sql = new StringBuilder("WITH ").append(ACCOUNT_BALANCES)
                                                          .append(" as (\nSELECT ")
                                                          .append(accountColumn)
                                                          .append(" as ")
                                                          .append(ACCOUNT_CODE);
            for (String balance : balanceSums) {
                sql.append(", ")
                   .append(balance);
            }
            sql.append("\nFROM ")
               .append(quote(baseTable))
               .append(" as ")
               .append(baseAlias);
            for (Map<String, Object> join : joins) {
                sql.append('\n')
                   .append(join.get("type"))
                   .append(" JOIN ")
                   .append(quote((String) join.get("name")))
                   .append(" as ")
                   .append(join.get("alias"))
                   .append(" ON ")
                   .append(join.get("condition"));
            }
            if (where != null && !where.isBlank()) {
                sql.append("\nWHERE ")
                   .append(where);
            }
            sql.append("\nGROUP BY ")
               .append(accountColumn)
               .append("\n)\nSELECT ")
               .append(STATEMENT_LINES)
               .append(".\"Code\" as \"Code\", ")
               .append(STATEMENT_LINES)
               .append(".\"Label\" as \"Label\", ")
               .append(STATEMENT_LINES)
               .append(".\"Amount\" as \"Amount\"\nFROM (");
            for (int ordinal = 0; ordinal < lines.size(); ordinal++) {
                StatementLine line = lines.get(ordinal);
                if (ordinal > 0) {
                    sql.append("\nUNION ALL");
                }
                // The line's ordinal is what ORDERs the statement: a statement's rows are a structure,
                // and its codes sort lexicographically wrong (A.II before A.X). It is selected but not
                // projected - the reader gets Code / Label / Amount.
                // Every literal is CAST, so the union's own column types are the declared ones rather
                // than whatever the first branch's literal happened to be long enough for.
                sql.append("\nSELECT ")
                   .append(ordinal + 1)
                   .append(" as \"Ordinal\", CAST('")
                   .append(line.code())
                   .append("' AS VARCHAR(255)) as \"Code\", CAST('")
                   .append(line.label())
                   .append("' AS VARCHAR(4000)) as \"Label\", ")
                   .append(line.amount())
                   .append(" as \"Amount\"\nFROM ")
                   .append(ACCOUNT_BALANCES);
            }
            return sql.append("\n) as ")
                      .append(STATEMENT_LINES)
                      .append("\nORDER BY ")
                      .append(STATEMENT_LINES)
                      .append(".\"Ordinal\"")
                      .toString();
        }
    }

    /**
     * A dimension's emitted column plus its date-bucket function ({@code month}/{@code year}), if any.
     */
    record WidgetDimension(Map<String, Object> column, String bucket) {
    }

    /**
     * The report's dashboard KPI, resolved from authored expressions to the report's own column aliases
     * so the runtime can query the generated report controller directly: {@code kind: count} uses the
     * count endpoint, {@code kind: value} reads {@code valueColumn} off the row matching the {@code at}
     * pins (typed EQ conditions), {@code kind: list} shows the first {@code limit} rows. The
     * {@code now} token stays symbolic - the dashboard resolves it client-side, type-aware per the
     * pinned column's {@code bucket}/{@code type}. No SQL and no URLs live in this block.
     */
    static Map<String, Object> widget(ReportIntent report, Map<String, WidgetDimension> dimensionColumns,
            Map<String, Map<String, Object>> measureColumns) {
        WidgetIntent intent = report.getWidget();
        String kind = intent.getKind() != null && !intent.getKind()
                                                         .isBlank() ? intent.getKind()
                                                                            .trim()
                                                                 : (intent.getValue() != null ? "value" : "count");
        Map<String, Object> widget = new LinkedHashMap<>();
        widget.put("kind", kind);
        widget.put("label", intent.getLabel() != null && !intent.getLabel()
                                                                .isBlank() ? intent.getLabel() : humanize(report.getName()));
        widget.put("tId", translationId("widget" + report.getName()));
        widget.put("icon", intent.getIcon() != null && !intent.getIcon()
                                                              .isBlank() ? intent.getIcon() : "gauge");
        if ("value".equals(kind)) {
            Map<String, Object> measureColumn = measureColumns.get(expressionKey(intent.getValue()));
            if (measureColumn == null) {
                LOGGER.warn("Widget of report [{}] references measure [{}] which produced no column - the KPI will not resolve",
                        report.getName(), intent.getValue());
            } else {
                widget.put("valueColumn", measureColumn.get("alias"));
                widget.put("valueType", measureColumn.get("type"));
                if (measureColumn.containsKey("pattern")) {
                    widget.put("pattern", measureColumn.get("pattern"));
                }
            }
        }
        if ("list".equals(kind)) {
            widget.put("limit", intent.getLimit() == null ? 5 : intent.getLimit());
        }
        List<Map<String, Object>> pins = new ArrayList<>();
        for (Map.Entry<String, Object> at : intent.getAt()
                                                  .entrySet()) {
            WidgetDimension dimension = dimensionColumns.get(expressionKey(at.getKey()));
            if (dimension == null) {
                LOGGER.warn("Widget of report [{}] pins unknown dimension [{}] - skipping the pin", report.getName(), at.getKey());
                continue;
            }
            Map<String, Object> pin = new LinkedHashMap<>();
            pin.put("column", dimension.column()
                                       .get("alias"));
            pin.put("type", dimension.column()
                                     .get("type"));
            if (dimension.bucket() != null) {
                pin.put("bucket", dimension.bucket());
            }
            if ("now".equals(at.getValue())) {
                pin.put("token", "now");
            } else {
                pin.put("value", at.getValue());
            }
            pins.add(pin);
        }
        if (!pins.isEmpty()) {
            widget.put("at", pins);
        }
        return widget;
    }

    /** Whitespace/case-insensitive compare key for authored measure and dimension expressions. */
    private static String expressionKey(String expression) {
        return expression == null ? ""
                : expression.replaceAll("\\s+", "")
                            .toLowerCase(Locale.ROOT);
    }

    /**
     * Resolve a dimension/measure field reference to its physical column, joining when it crosses a
     * relation.
     */
    private static ColumnRef resolve(IntentGenerationContext context, IntentModel model, EntityIntent source, String baseAlias,
            String reference) {
        ColumnRef ref = new ColumnRef();
        int dot = reference.indexOf('.');
        if (dot > 0 && source != null) {
            String relationName = reference.substring(0, dot);
            String fieldName = reference.substring(dot + 1);
            RelationIntent relation = relationByName(source, relationName);
            if (relation != null && relation.getTo() != null) {
                EntityIntent target = entityByName(model, relation.getTo());
                String targetAlias = relation.getTo();
                ref.tableAlias = targetAlias;
                ref.physicalColumn = column(targetAlias, fieldName);
                FieldIntent targetField = fieldByName(target, fieldName);
                // A cross-model target's fields are not in this model; string is the safe display type.
                ref.reportType = targetField == null ? "CHARACTER VARYING" : reportType(targetField.getType());
                ref.nullable = targetField == null || !targetField.isRequired();
                ref.displayAlias = humanize(reference.replace('.', ' '));
                ref.join = join(context, model, source, relation, target, targetAlias, baseAlias);
                translate(context, ref, target, crossModelInfo(context, model, relation), fieldName);
                return ref;
            }
        }
        // A plain field on the source table.
        FieldIntent field = source == null ? null : fieldByName(source, reference);
        if (field != null) {
            ref.tableAlias = baseAlias;
            ref.physicalColumn = column(source.getName(), reference);
            ref.reportType = reportType(field.getType());
            ref.nullable = !field.isRequired();
            ref.displayAlias = humanize(reference);
            translate(context, ref, source, null, reference);
            return ref;
        }
        // A bare to-one relation (e.g. `member`): JOIN the related table and show its label (name)
        // field rather than the raw FK id - "group by member" should display the member, not its id.
        // Use `relation.field` to pick a specific column instead.
        RelationIntent relation = source == null ? null : relationByName(source, reference);
        if (relation != null && relation.getTo() != null
                && ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
            EntityIntent target = entityByName(model, relation.getTo());
            String targetAlias = relation.getTo();
            // A cross-model target's label comes from the resolved owner model (its Name-like field).
            CrossModelSupport.TargetInfo info = crossModelInfo(context, model, relation);
            String labelField = info != null ? info.labelField() : labelFieldName(target);
            FieldIntent labeled = fieldByName(target, labelField);
            ref.tableAlias = targetAlias;
            ref.physicalColumn = column(targetAlias, labelField);
            ref.reportType = info != null ? "CHARACTER VARYING" : reportType(labeled == null ? null : labeled.getType());
            ref.displayAlias = humanize(reference);
            ref.join = join(context, model, source, relation, target, targetAlias, baseAlias);
            // The label of a multilingual nomenclature is exactly the value a list page shows
            // translated - this is the column the issue was raised about (dirigible #6544).
            translate(context, ref, target, info, labelField);
            return ref;
        }
        // Best-effort: treat the reference as a raw column on the source.
        ref.tableAlias = baseAlias;
        ref.physicalColumn = column(source == null ? baseAlias : source.getName(), reference);
        ref.reportType = "CHARACTER VARYING";
        ref.displayAlias = humanize(reference);
        return ref;
    }

    /**
     * Give a resolved column the multilingual overlay when the entity it lives on keeps per-language
     * values and the selected property is one of the translated ones.
     *
     * <p>
     * A multilingual entity's own repository translates every read from its sibling
     * <code>&lt;TABLE&gt;_LANG</code> table for the caller's {@code Accept-Language} - but a report is
     * raw SQL over the base tables, so without this a report column showed the base-language value
     * right next to a list page showing the translated one (dirigible #6544). The column becomes
     * {@code COALESCE(<lang>."<Property>", <base>."<COLUMN>")} over a LEFT JOIN keyed on the base row
     * and the {@code :language} named parameter the generated repository binds; a row with no
     * translation, or a caller with no language, reads its base value.
     *
     * <p>
     * The overlay is deliberately confined to the SELECT list: {@link ReportIntent#getFilter()} and the
     * lifecycle scope compile against the BASE table, which is why translating a nomenclature can never
     * change what a report filter matches.
     *
     * @param context the generation context (for the same-model physical table name)
     * @param ref the resolved column, mutated in place when it is translatable
     * @param owner the entity the column lives on, for a same-model target
     * @param info the resolved owner-model facts, for a cross-model target (null otherwise)
     * @param name the selected property - the authored field name same-model, the target's property
     *        name cross-model
     */
    private static void translate(IntentGenerationContext context, ColumnRef ref, EntityIntent owner, CrossModelSupport.TargetInfo info,
            String name) {
        String property = IntentNaming.pascalCase(name);
        String table;
        String keyColumn;
        if (info != null) {
            if (!info.translatedProperties()
                     .contains(property)) {
                return;
            }
            table = info.tableDataName();
            keyColumn = info.keyColumn();
        } else {
            if (owner == null || !owner.isMultilingual() || !isTranslatable(fieldByName(owner, name))) {
                return;
            }
            FieldIntent primaryKey = primaryKeyOf(owner);
            table = IntentNaming.tableName(context, owner.getName());
            keyColumn = column(owner.getName(), primaryKey == null ? "id" : primaryKey.getName());
        }
        String alias = ref.tableAlias + LANGUAGE_TABLE_SUFFIX;
        ref.languageColumn = property;
        ref.languageJoin = new Join(table + LANGUAGE_TABLE_SUFFIX, alias, alias + "." + quote(LANGUAGE_ID_COLUMN) + " = " + ref.tableAlias
                + "." + quote(keyColumn) + " AND " + alias + "." + quote(LANGUAGE_CODE_COLUMN) + " = " + LANGUAGE_PARAMETER, true);
    }

    /**
     * Whether a field has a column in its entity's language table - mirroring what the schema template
     * emits there: a character-typed field that is neither the primary key nor calculated. (A relation
     * is not a field, and the audit columns are not authored ones, so neither can reach this.)
     *
     * @param field the field, or null when the reference names no declared field
     * @return true when the language table carries a column for it
     */
    private static boolean isTranslatable(FieldIntent field) {
        if (field == null || field.isPrimaryKey() || field.isCalculated()) {
            return false;
        }
        String type = field.getType() == null ? "string"
                : field.getType()
                       .toLowerCase(Locale.ROOT);
        return "string".equals(type) || "text".equals(type);
    }

    /**
     * The related entity's label field (its {@code name}-like field; else first text field; else PK).
     */
    private static String labelFieldName(EntityIntent target) {
        if (target == null) {
            return "id";
        }
        for (FieldIntent field : target.getFields()) {
            if (field.getName() != null && "name".equalsIgnoreCase(field.getName())) {
                return field.getName();
            }
        }
        for (FieldIntent field : target.getFields()) {
            if (field.getName() != null && !field.isPrimaryKey() && isTextType(field.getType())) {
                return field.getName();
            }
        }
        FieldIntent pk = primaryKeyOf(target);
        return pk == null ? "id" : pk.getName();
    }

    private static boolean isTextType(String type) {
        if (type == null) {
            return false;
        }
        String t = type.toLowerCase(Locale.ROOT);
        return "string".equals(t) || "text".equals(t) || "uuid".equals(t);
    }

    private static Join join(IntentGenerationContext context, IntentModel model, EntityIntent source, RelationIntent relation,
            EntityIntent target, String targetAlias, String baseAlias) {
        String fkColumn = quote(column(source.getName(), relation.getName()));
        // A cross-model target's table and primary-key column come from the resolved owner model -
        // this model's intent-prefixed naming would point at a non-existent local table.
        CrossModelSupport.TargetInfo info = crossModelInfo(context, model, relation);
        if (info != null) {
            return new Join(info.tableDataName(), targetAlias,
                    baseAlias + "." + fkColumn + " = " + targetAlias + "." + quote(info.keyColumn()));
        }
        FieldIntent targetPk = target == null ? null : primaryKeyOf(target);
        String pkColumn = quote(column(targetAlias, targetPk == null ? "id" : targetPk.getName()));
        return new Join(IntentNaming.tableName(context, targetAlias), targetAlias,
                baseAlias + "." + fkColumn + " = " + targetAlias + "." + pkColumn);
    }

    /**
     * The resolved owner-model facts for a cross-model relation, or null for a same-model one.
     * Resolution mirrors the EDM generator (workspace, then registry; convention fallback only with a
     * null context) and fails loudly for an unresolvable dependency - generate leaf-first.
     */
    private static CrossModelSupport.TargetInfo crossModelInfo(IntentGenerationContext context, IntentModel model,
            RelationIntent relation) {
        if (!relation.isCrossModel()) {
            return null;
        }
        for (UsesIntent uses : model.getUses()) {
            if (relation.getModel()
                        .equals(uses.getModel())) {
                return CrossModelSupport.resolve(context, uses, relation.getTo());
            }
        }
        return null;
    }

    private static void registerJoin(Map<String, Join> joins, ColumnRef ref) {
        if (ref.join != null) {
            joins.putIfAbsent(ref.join.alias, ref.join);
        }
        // The language join is registered right after the entity join it hangs off, so the emitted FROM
        // clause always introduces an alias before the join that references it. Two translated columns
        // of the same entity share one join - same alias, same ON.
        if (ref.languageJoin != null) {
            joins.putIfAbsent(ref.languageJoin.alias, ref.languageJoin);
        }
    }

    /**
     * The SQL, built from the very {@code columns} / {@code joins} / {@code conditions} the document
     * carries - so the structured model and the materialised {@code query} cannot disagree.
     *
     * <p>
     * That is the contract the report editor's round-trip guard checks: it rebuilds the query from the
     * structured model on open and, when the two match, lets the visual builder own the query. A
     * generated report whose model said something else than its query opened dirty and was rewritten
     * destructively on save (dirigible #6675) - so the emission here is aligned token for token with
     * {@code buildQuery()} in {@code editor-report/js/editor.js}. Keep the two in step.
     *
     * @param baseTable the physical base table (unquoted; blank for a source-less report)
     * @param baseAlias the base-table alias
     * @param joins the emitted join rows
     * @param columns the emitted column rows
     * @param where the WHERE predicate, or null/blank for none
     * @return the query
     */
    private static String buildQuery(String baseTable, String baseAlias, List<Map<String, Object>> joins, List<Map<String, Object>> columns,
            String where) {
        List<String> selectParts = new ArrayList<>();
        List<String> groupParts = new ArrayList<>();
        for (Map<String, Object> column : columns) {
            String term = columnTerm(column);
            String aggregate = (String) column.get("aggregate");
            selectParts.add(("NONE".equals(aggregate) ? term : aggregate + "(" + term + ")") + " as \"" + column.get("alias") + "\"");
            if (Boolean.TRUE.equals(column.get("grouping"))) {
                groupParts.add(term);
            }
        }
        StringBuilder sql = new StringBuilder();
        // A report with neither dimensions nor measures still has to run, so it selects everything -
        // the builder has no columns to own there anyway, and the editor opens it free-style.
        sql.append("SELECT ")
           .append(selectParts.isEmpty() ? "*" : String.join(", ", selectParts));
        if (!baseTable.isBlank()) {
            sql.append("\nFROM ")
               .append(quote(baseTable))
               .append(" as ")
               .append(baseAlias);
        }
        for (Map<String, Object> join : joins) {
            sql.append('\n')
               .append(join.get("type"))
               .append(" JOIN ")
               .append(quote((String) join.get("name")))
               .append(" as ")
               .append(join.get("alias"))
               .append(" ON ")
               .append(join.get("condition"));
        }
        if (where != null && !where.isBlank()) {
            sql.append("\nWHERE ")
               .append(where);
        }
        if (!groupParts.isEmpty()) {
            sql.append("\nGROUP BY ")
               .append(String.join(", ", groupParts));
        }
        return sql.toString();
    }

    /**
     * What a column contributes to SELECT and GROUP BY: its {@code expression} verbatim when it has one
     * (a date bucket, an ageing CASE, a balance window), the bare star of a {@code count(*)} measure,
     * else its quoted qualified physical column.
     *
     * @param column the emitted column row
     * @return the SQL term
     */
    private static String columnTerm(Map<String, Object> column) {
        String expression = (String) column.get("expression");
        if (expression != null) {
            return expression;
        }
        String name = (String) column.get("name");
        return "*".equals(name) ? name : column.get("table") + "." + quote(name);
    }

    /** The joins as the {@code .report} rows the editor's builder reads and re-emits. */
    private static List<Map<String, Object>> joinRows(Map<String, Join> joins) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Join join : joins.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("alias", join.alias);
            row.put("name", join.table);
            // A language join is LEFT: an untranslated row - or a caller with no language at all - must
            // still appear, carrying the base value the SELECT's COALESCE then falls back to.
            row.put("type", join.language ? LANGUAGE_JOIN_TYPE : JOIN_TYPE);
            row.put("condition", join.on);
            rows.add(row);
        }
        return rows;
    }

    /**
     * The lifecycle predicate a report is restricted by:
     * {@code <alias>."<STATUS FK>" IN (<stage ids>)}.
     *
     * <p>
     * An aggregate over an entity that carries a {@code function: EntityStatus} is wrong by default -
     * drafts nobody has issued, cancelled and voided rows all land in the sum unless the author
     * remembers a status predicate. So a report whose source is stage-classified (see
     * {@link LifecycleStages}) and which aggregates defaults to {@code live}: {@code scope: all} is the
     * explicit opt-out, an explicit stage name selects that stage, and a report whose dimensions or
     * {@code filter} already speak about the status is left exactly as authored (its own predicate is
     * authoritative, and a breakdown BY status must not lose its draft rows).
     *
     * <p>
     * When the nomenclature carries no stage classification at all there is nothing to default to - the
     * omission is then reported as a generation warning rather than silently producing an inflated
     * total, which is the whole point of this feature (dirigible #6645).
     *
     * @return the condition, or {@code null} when the report counts every row
     */
    private static Map<String, Object> scopeCondition(IntentGenerationContext context, IntentModel model, EntityIntent source,
            String baseAlias, ReportIntent report, boolean aggregated) {
        RelationIntent status = LifecycleStages.statusRelation(source);
        if (status == null || status.getTo() == null) {
            return null;
        }
        String declared = report.getNormalizedScope();
        if (LifecycleStages.SCOPE_ALL.equals(declared)) {
            return null;
        }
        // A cross-model nomenclature is seeded in its owner model, so no stage is resolvable here; the
        // parser rejects an explicit scope in that case and the warning below still covers the omission.
        Map<String, List<Integer>> stages = status.isCrossModel() ? Map.of() : LifecycleStages.stagesOf(model, status.getTo());
        String stage = declared;
        if (stage == null) {
            if (!aggregated || referencesStatus(report, status.getName())) {
                return null;
            }
            if (stages.isEmpty()) {
                context.addIssue("report [" + report.getName() + "] aggregates over [" + source.getName()
                        + "], which carries a lifecycle status [" + status.getName()
                        + "], but neither declares `scope:` nor filters on that status - draft, cancelled and voided rows are"
                        + " counted in every total. Classify the seed rows of [" + status.getTo()
                        + "] with `stage:` (draft/live/cancelled/void), or declare `scope: all` to count them deliberately.");
                return null;
            }
            stage = LifecycleStages.LIVE;
        }
        List<Integer> ids = stages.get(stage);
        if (ids == null || ids.isEmpty()) {
            context.addIssue("report [" + report.getName() + "] scope [" + stage + "] matches no seed row of [" + status.getTo()
                    + "] - the report is generated over every status");
            return null;
        }
        return condition(baseAlias + "." + quote(column(source.getName(), status.getName())), "IN", "(" + ids.stream()
                                                                                                             .map(String::valueOf)
                                                                                                             .collect(Collectors.joining(
                                                                                                                     ", "))
                + ")");
    }

    /**
     * Whether the report already speaks about its source's status - as a dimension (a breakdown BY
     * status) or inside its {@code filter} (a hand-written predicate). Either way the authored intent
     * wins over the implicit {@code live} default.
     */
    private static boolean referencesStatus(ReportIntent report, String relationName) {
        if (relationName == null || relationName.isBlank()) {
            return false;
        }
        Pattern token = Pattern.compile("\\b" + Pattern.quote(relationName) + "\\b");
        for (String dimension : report.getDimensions()) {
            if (dimension != null && token.matcher(dimension)
                                          .find()) {
                return true;
            }
        }
        return report.getFilter() != null && token.matcher(report.getFilter())
                                                  .find();
    }

    /**
     * A report has no field-level scoping: its query runs as written and every row it returns reaches
     * everyone the report itself is readable by. So a report that groups by, sums or filters on a
     * {@code visibleTo:} field re-serves the restricted figure through a second door - the same leak
     * the entity closed, one artefact further out. It is a legitimate thing to author (a payroll report
     * over payroll data is exactly the point), so this is a generation warning rather than a refusal:
     * the author is told which report re-exposes which field, and scopes the report's own roles
     * accordingly.
     *
     * <p>
     * Own fields and one-hop {@code relation.field} paths are both scanned, over the dimensions, the
     * measures, the filter and the ledger kinds' amount / account fields.
     */
    private static void warnOnRestrictedColumns(IntentGenerationContext context, IntentModel model, EntityIntent source,
            ReportIntent report) {
        if (source == null) {
            return;
        }
        List<String> expressions = new ArrayList<>(report.getDimensions());
        expressions.addAll(report.getMeasures());
        expressions.add(report.getFilter());
        expressions.add(report.getDebit());
        expressions.add(report.getCredit());
        expressions.add(report.getAccount());
        for (FieldIntent field : source.getFields()) {
            if (!field.getVisibleTo()
                      .isEmpty()
                    && mentions(expressions, field.getName())) {
                warnRestrictedColumn(context, report, source.getName(), field);
            }
        }
        for (RelationIntent relation : source.getRelations()) {
            EntityIntent target = relation.getTo() == null ? null : entityByName(model, relation.getTo());
            if (target == null) {
                continue;
            }
            for (FieldIntent field : target.getFields()) {
                if (!field.getVisibleTo()
                          .isEmpty()
                        && mentions(expressions, relation.getName() + "." + field.getName())) {
                    warnRestrictedColumn(context, report, target.getName(), field);
                }
            }
        }
    }

    /** The report re-exposes one restricted field - say which, and to whom the entity restricts it. */
    private static void warnRestrictedColumn(IntentGenerationContext context, ReportIntent report, String entityName, FieldIntent field) {
        context.addIssue("report [" + report.getName() + "] reads [" + entityName + "." + field.getName() + "], which [" + entityName
                + "] restricts to " + field.getVisibleTo()
                + " with `visibleTo` - a report carries no field-level scoping, so everyone who may open it sees the value");
    }

    /** Whether any of the expressions mentions the token as a whole word (dotted paths included). */
    private static boolean mentions(List<String> expressions, String token) {
        Pattern pattern = Pattern.compile("(?<![\\w.])" + Pattern.quote(token) + "\\b", Pattern.CASE_INSENSITIVE);
        for (String expression : expressions) {
            if (expression != null && pattern.matcher(expression)
                                             .find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rewrite the intent filter's field names to qualified physical columns; pass other tokens through.
     */
    private static String buildWhere(IntentGenerationContext context, IntentModel model, EntityIntent source, String baseAlias,
            Map<String, Join> joins, String filter) {
        if (filter == null || filter.isBlank() || source == null) {
            return filter == null ? null : filter.trim();
        }
        Matcher matcher = DOTTED_REF.matcher(filter);
        StringBuilder dotted = new StringBuilder();
        while (matcher.find()) {
            RelationIntent relation = relationByName(source, matcher.group(1));
            if (relation != null && relation.getTo() != null) {
                EntityIntent target = entityByName(model, relation.getTo());
                String targetAlias = relation.getTo();
                joins.putIfAbsent(targetAlias, join(context, model, source, relation, target, targetAlias, baseAlias));
                matcher.appendReplacement(dotted,
                        Matcher.quoteReplacement(targetAlias + "." + quote(column(targetAlias, matcher.group(2)))));
            } else {
                matcher.appendReplacement(dotted, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(dotted);
        String where = dotted.toString();
        for (FieldIntent field : source.getFields()) {
            if (field.getName() != null && !field.getName()
                                                 .isBlank()) {
                where = where.replaceAll("\\b" + Pattern.quote(field.getName()) + "\\b",
                        Matcher.quoteReplacement(baseAlias + "." + quote(column(source.getName(), field.getName()))));
            }
        }
        // A bare to-one RELATION name filters by its FK column (`Status != 8` -> the status FK
        // id column) - previously it passed through untranslated and broke the generated SQL.
        // The negative lookahead skips join-alias usages (`Customer."CUSTOMER_NAME"` from the
        // dotted-ref pass above); the lookbehind skips already-qualified column tokens.
        if (source.getRelations() != null) {
            for (RelationIntent relation : source.getRelations()) {
                if (relation.getName() != null && !relation.getName()
                                                           .isBlank()) {
                    where = where.replaceAll("(?<![.\"\\w])" + Pattern.quote(relation.getName()) + "\\b(?!\\s*[.\"])",
                            Matcher.quoteReplacement(baseAlias + "." + quote(column(source.getName(), relation.getName()))));
                }
            }
        }
        // Authors used to the intent's guard syntax write `Status == 2`; SQL equality is a single
        // `=` (H2 tolerates `==`, PostgreSQL rejects it), so normalize. `<=`/`>=`/`!=` are untouched.
        // Normalize only OUTSIDE single-quoted string literals so a value literal that itself contains
        // `==` (e.g. Code == 'A==B') is left intact.
        where = normalizeEqualityOperator(where);
        return where.trim();
    }

    /**
     * Collapse the guard-style {@code ==} equality operator to SQL {@code =}, but only outside
     * single-quoted string literals so a literal value containing {@code ==} is preserved verbatim.
     *
     * @param where the WHERE fragment
     * @return the fragment with operator-level {@code ==} collapsed to {@code =}
     */
    private static String normalizeEqualityOperator(String where) {
        StringBuilder result = new StringBuilder(where.length());
        boolean inStringLiteral = false;
        for (int i = 0; i < where.length(); i++) {
            char current = where.charAt(i);
            if (current == '\'') {
                inStringLiteral = !inStringLiteral;
                result.append(current);
            } else if (!inStringLiteral && current == '=' && i + 1 < where.length() && where.charAt(i + 1) == '=') {
                result.append('=');
                i++;
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    /**
     * The WHERE predicate as the structured {@code conditions} rows the editor's builder owns - or
     * {@code null} when it cannot be represented, in which case the query string stays the only source
     * of truth and the editor opens the report free-style.
     *
     * <p>
     * The rows must reproduce the predicate exactly ({@link #predicate}), so the filter is only
     * decomposed when every {@code AND}-separated term is a plain binary comparison. A term carrying an
     * unbalanced quote or an {@code OR} is refused: the first means the split cut through a string
     * literal, and the second means dropping the filter's parentheses around the whole predicate would
     * change how it binds against the appended scope condition.
     *
     * @param filter the rewritten filter, or null/blank for none
     * @param scope the lifecycle scope condition, or null for none
     * @return the conditions (possibly empty), or null when the predicate does not round-trip
     */
    private static List<Map<String, Object>> conditions(String filter, Map<String, Object> scope) {
        List<Map<String, Object>> conditions = new ArrayList<>();
        if (filter != null && !filter.isBlank()) {
            for (String term : filter.trim()
                                     .split(" AND ")) {
                Matcher matcher = SIMPLE_CONDITION.matcher(term);
                if (!matcher.matches() || countOf(term, '\'') % 2 != 0 || OR_OPERATOR.matcher(term)
                                                                                     .find()) {
                    return null;
                }
                conditions.add(condition(matcher.group(1), matcher.group(2), matcher.group(3)));
            }
        }
        if (scope != null) {
            conditions.add(scope);
        }
        return conditions;
    }

    private static Map<String, Object> condition(String left, String operation, String right) {
        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("left", left);
        condition.put("operation", operation);
        condition.put("right", right);
        return condition;
    }

    /** The conditions as the WHERE predicate - the inverse of {@link #conditions}. */
    private static String predicate(List<Map<String, Object>> conditions) {
        return conditions.stream()
                         .map(condition -> condition.get("left") + " " + condition.get("operation") + " " + condition.get("right"))
                         .collect(Collectors.joining(" AND "));
    }

    /**
     * The WHERE predicate for a filter the builder cannot represent: the filter is parenthesised
     * whenever anything is appended to it, so neither the lifecycle scope nor a parameter predicate can
     * rebind it - an {@code OR}-carrying filter is exactly the one that does not round-trip, and
     * {@code AND} binds tighter than {@code OR}.
     *
     * @param filter the rewritten filter, or null/blank for none
     * @param scope the lifecycle scope condition, or null for none
     * @param parameters the parameter predicate, or null for none
     * @return the predicate, or null when there is nothing to filter by
     */
    private static String rawWhere(String filter, Map<String, Object> scope, String parameters) {
        String appended = and(scope == null ? null : predicate(List.of(scope)), parameters);
        if (filter == null || filter.isBlank()) {
            return appended;
        }
        return appended == null ? filter : "(" + filter + ") AND " + appended;
    }

    /**
     * The two predicates ANDed, tolerating either being absent. The left one is already parenthesised
     * where it needs to be ({@link #rawWhere}); a parameter predicate is a conjunction of plain
     * comparisons, so it needs none.
     *
     * @param left the left predicate, or null
     * @param right the right predicate, or null
     * @return the combined predicate, or null when both are absent
     */
    private static String and(String left, String right) {
        if (left == null || left.isBlank()) {
            return right;
        }
        if (right == null || right.isBlank()) {
            return left;
        }
        return left + " AND " + right;
    }

    private static int countOf(String value, char character) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == character) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, Object> security(IntentGenerationContext context, String reportName) {
        Map<String, Object> security = new LinkedHashMap<>();
        security.put("generateDefaultRoles", "true");
        String project = context.getProjectName();
        String prefix = project == null || project.isEmpty() ? IntentNaming.baseName(context) : project;
        security.put("roleRead", prefix + ".Report." + reportName + "ReadOnly");
        return security;
    }

    private static Map<String, Object> column(String tableAlias, String alias, String physicalColumn, String reportType, String aggregate,
            boolean grouping) {
        return column(tableAlias, alias, physicalColumn, reportType, aggregate, grouping, null);
    }

    /**
     * One {@code columns} row.
     *
     * @param tableAlias the alias of the table the column lives on
     * @param alias the display alias
     * @param physicalColumn the physical column name (unquoted; {@code *} for a bare aggregate)
     * @param reportType the SQL type the report editor records
     * @param aggregate the aggregate function, or {@code NONE}
     * @param grouping whether the column is part of the GROUP BY
     * @param expression the verbatim SQL the column emits instead of its qualified physical column (a
     *        date bucket, an ageing CASE, a balance window), or null. The report editor's builder reads
     *        it back, which is what keeps a computed dimension from degrading to its raw column on
     *        save.
     * @return the row
     */
    private static Map<String, Object> column(String tableAlias, String alias, String physicalColumn, String reportType, String aggregate,
            boolean grouping, String expression) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("table", tableAlias);
        column.put("alias", alias);
        column.put("name", physicalColumn);
        if (expression != null) {
            column.put("expression", expression);
        }
        column.put("type", reportType);
        column.put("aggregate", aggregate);
        column.put("select", Boolean.TRUE);
        column.put("grouping", grouping && "NONE".equals(aggregate));
        column.put("tId", translationId(alias));
        column.put("label", alias);
        // Rendering metadata carried on the model so every report UI aligns and formats consistently:
        // numeric columns right-align; decimals carry the platform money pattern.
        boolean numeric = "INTEGER".equals(reportType) || "BIGINT".equals(reportType) || "DECIMAL".equals(reportType);
        column.put("align", numeric ? "right" : "left");
        if ("DECIMAL".equals(reportType)) {
            column.put("pattern", "### ### ### ##0.00");
        }
        return column;
    }

    private static String column(String entityName, String fieldName) {
        return IntentNaming.upperSnake(entityName) + "_" + IntentNaming.upperSnake(fieldName);
    }

    /**
     * Double-quote a physical identifier (table or column) so the SQL is portable. Dirigible creates
     * tables/columns as quoted UPPER_SNAKE, and PostgreSQL folds <i>unquoted</i> identifiers to lower
     * case - so an unquoted {@code LIBRARY_LOAN} / {@code LOAN_DUE_ON} would never match the actual
     * object on Postgres. Table aliases are intentionally left unquoted (they fold consistently on both
     * sides). H2 accepts the quoted form too, so the output runs on both.
     */
    private static String quote(String identifier) {
        return "\"" + identifier + "\"";
    }

    private static EntityIntent entityByName(IntentModel model, String name) {
        if (name == null) {
            return null;
        }
        for (EntityIntent entity : model.getEntities()) {
            if (name.equals(entity.getName())) {
                return entity;
            }
        }
        return null;
    }

    private static FieldIntent fieldByName(EntityIntent entity, String name) {
        if (entity == null || name == null) {
            return null;
        }
        for (FieldIntent field : entity.getFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private static RelationIntent relationByName(EntityIntent entity, String name) {
        if (entity == null || name == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    private static FieldIntent primaryKeyOf(EntityIntent entity) {
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() && field.getName() != null) {
                return field;
            }
        }
        return null;
    }

    /** Logical intent field type to the SQL type the report editor records. */
    private static String reportType(String type) {
        if (type == null) {
            return "CHARACTER VARYING";
        }
        switch (type.toLowerCase(Locale.ROOT)) {
            case "integer":
            case "int":
                return "INTEGER";
            case "long":
                return "BIGINT";
            case "decimal":
            case "double":
                return "DECIMAL";
            case "boolean":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "timestamp":
                return "TIMESTAMP";
            case "text":
                return "CHARACTER LARGE OBJECT";
            case "uuid":
            case "string":
            default:
                return "CHARACTER VARYING";
        }
    }

    private static String leaf(String reference) {
        int dot = reference.lastIndexOf('.');
        return dot < 0 ? reference : reference.substring(dot + 1);
    }

    private static String translationId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(" ", "")
                  .replace("_", "")
                  .replace(".", "")
                  .replace(":", "")
                  .replace("*", "all");
    }

    /** camelCase / snake_case / dotted-path / spaced identifier to a human label. */
    private static String humanize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 4);
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '_' || c == '-' || c == '.' || c == ' ') {
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') {
                    out.append(' ');
                }
                capitalizeNext = true;
                continue;
            }
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(raw.charAt(i - 1)) && out.length() > 0
                    && out.charAt(out.length() - 1) != ' ') {
                out.append(' ');
            }
            if (capitalizeNext) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * A resolved column reference: where it lives, its physical name + type, display alias, optional
     * join, and - when its entity is multilingual - the language-table join that overlays it.
     */
    private static final class ColumnRef {
        private String tableAlias;
        private String physicalColumn;
        private String reportType;
        private String displayAlias;
        /** Whether the column may hold no value - what makes a parameter predicate coalesce it. */
        private boolean nullable = true;
        private Join join;
        private Join languageJoin;
        private String languageColumn;

        private String qualified() {
            return tableAlias + "." + quote(physicalColumn);
        }

        /**
         * The verbatim SQL a translated column SELECTs (and groups by): the caller's translation with the
         * base value as its fallback. Null when the column needs no overlay, so it stays a plain qualified
         * physical column.
         */
        private String translationExpression() {
            return languageJoin == null ? null : "COALESCE(" + languageJoin.alias + "." + quote(languageColumn) + ", " + qualified() + ")";
        }
    }

    /** A join to a related entity's table - INNER, or LEFT for a language table. */
    private static final class Join {
        private final String table;
        private final String alias;
        private final String on;
        private final boolean language;

        private Join(String table, String alias, String on) {
            this(table, alias, on, false);
        }

        private Join(String table, String alias, String on, boolean language) {
            this.table = table;
            this.alias = alias;
            this.on = on;
            this.language = language;
        }
    }
}
