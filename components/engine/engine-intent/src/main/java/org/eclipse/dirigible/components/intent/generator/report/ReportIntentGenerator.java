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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
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
 * Physical table and column identifiers in the generated {@code query} are double-quoted (table
 * aliases are not) so the SQL runs on PostgreSQL, which folds unquoted identifiers to lower case
 * and would otherwise never match the quoted UPPER_SNAKE objects the platform creates; H2 accepts
 * the quoted form too.
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
        boolean aggregated = balance || report.getMeasures()
                                              .stream()
                                              .anyMatch(m -> m != null && !m.isBlank());

        Map<String, Join> joins = new LinkedHashMap<>();
        List<Map<String, Object>> columns = new ArrayList<>();
        List<String> selectParts = new ArrayList<>();
        List<String> groupParts = new ArrayList<>();
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
                Map<String, Object> bucketColumn = column(ref.tableAlias, alias, ref.physicalColumn, "INTEGER", "NONE", aggregated);
                columns.add(bucketColumn);
                dimensionColumns.put(expressionKey(dimension), new WidgetDimension(bucketColumn, function));
                selectParts.add(expression + " as \"" + alias + "\"");
                if (aggregated) {
                    groupParts.add(expression);
                }
                continue;
            }
            ColumnRef ref = resolve(context, model, source, baseAlias, dimension.trim());
            registerJoin(joins, ref);
            Map<String, Object> dimensionColumn =
                    column(ref.tableAlias, ref.displayAlias, ref.physicalColumn, ref.reportType, "NONE", aggregated);
            columns.add(dimensionColumn);
            dimensionColumns.put(expressionKey(dimension), new WidgetDimension(dimensionColumn, null));
            selectParts.add(ref.qualified() + " as \"" + ref.displayAlias + "\"");
            if (aggregated) {
                groupParts.add(ref.qualified());
            }
        }
        if (balance) {
            addBalanceMeasures(context, model, source, baseAlias, report, joins, columns, selectParts);
        } else {
            for (String measure : report.getMeasures()) {
                if (measure == null || measure.isBlank()) {
                    continue;
                }
                int before = columns.size();
                addMeasure(context, model, source, baseAlias, measure.trim(), joins, columns, selectParts);
                if (columns.size() > before) {
                    measureColumns.put(expressionKey(measure), columns.get(before));
                }
            }
        }

        String where = buildWhere(context, model, source, baseAlias, joins, report.getFilter());
        String query = buildQuery(baseTable, baseAlias, joins, selectParts, where, aggregated ? groupParts : List.of());

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
        }
        document.put("columns", columns);
        document.put("query", query);
        if (balance) {
            document.put("parameters", balanceParameters());
        }
        document.put("conditions", conditions(context, model, source, baseAlias, report.getFilter()));
        document.put("security", security(context, report.getName()));
        return document;
    }

    private static void addMeasure(IntentGenerationContext context, IntentModel model, EntityIntent source, String baseAlias,
            String measure, Map<String, Join> joins, List<Map<String, Object>> columns, List<String> selectParts) {
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
                    columns.add(column(baseAlias, alias, "*", "INTEGER", aggregate, false));
                    selectParts.add(aggregate + "(*) as \"" + alias + "\"");
                    return;
                }
                ColumnRef ref = resolve(context, model, source, baseAlias, field);
                registerJoin(joins, ref);
                String alias = humanize(aggregate.toLowerCase(Locale.ROOT) + " " + leaf(field));
                String type = "COUNT".equals(aggregate) ? "INTEGER"
                        : ("MIN".equals(aggregate) || "MAX".equals(aggregate) ? ref.reportType : "DECIMAL");
                columns.add(column(ref.tableAlias, alias, ref.physicalColumn, type, aggregate, false));
                selectParts.add(aggregate + "(" + ref.qualified() + ") as \"" + alias + "\"");
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
    private static void addBalanceMeasures(IntentGenerationContext context, IntentModel model, EntityIntent source, String baseAlias,
            ReportIntent report, Map<String, Join> joins, List<Map<String, Object>> columns, List<String> selectParts) {
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
        addBalanceColumn(columns, selectParts, debit, opening, "Opening Debit");
        addBalanceColumn(columns, selectParts, credit, opening, "Opening Credit");
        addBalanceColumn(columns, selectParts, debit, period, "Debit");
        addBalanceColumn(columns, selectParts, credit, period, "Credit");
        addBalanceColumn(columns, selectParts, debit, closing, "Closing Debit");
        addBalanceColumn(columns, selectParts, credit, closing, "Closing Credit");
    }

    private static void addBalanceColumn(List<Map<String, Object>> columns, List<String> selectParts, ColumnRef amount, String window,
            String alias) {
        columns.add(column(amount.tableAlias, alias, amount.physicalColumn, "DECIMAL", "SUM", false));
        // COALESCE the amount: a one-sided ledger line (the exactlyOne debit/credit shape) holds
        // NULL on the other side, and SUM over all-NULL yields NULL instead of the 0 a balance shows.
        selectParts.add("SUM(CASE WHEN " + window + " THEN COALESCE(" + amount.qualified() + ", 0) ELSE 0 END) as \"" + alias + "\"");
    }

    /**
     * The balance window as declared {@code .report} parameters, in the report editor's {@code {name,
     * type, initial}} shape the generated repository already binds. The defaults make an
     * unparameterized call return the all-time balance (empty opening, everything in the period).
     */
    private static List<Map<String, Object>> balanceParameters() {
        List<Map<String, Object>> parameters = new ArrayList<>();
        parameters.add(reportParameter("fromDate", "1900-01-01"));
        parameters.add(reportParameter("toDate", "9999-12-31"));
        return parameters;
    }

    private static Map<String, Object> reportParameter(String name, String initial) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("name", name);
        parameter.put("type", "DATE");
        parameter.put("initial", initial);
        return parameter;
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
                ref.displayAlias = humanize(reference.replace('.', ' '));
                ref.join = join(context, model, source, relation, target, targetAlias, baseAlias);
                return ref;
            }
        }
        // A plain field on the source table.
        FieldIntent field = source == null ? null : fieldByName(source, reference);
        if (field != null) {
            ref.tableAlias = baseAlias;
            ref.physicalColumn = column(source.getName(), reference);
            ref.reportType = reportType(field.getType());
            ref.displayAlias = humanize(reference);
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
    }

    private static String buildQuery(String baseTable, String baseAlias, Map<String, Join> joins, List<String> selectParts, String where,
            List<String> groupParts) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append(selectParts.isEmpty() ? "*" : String.join(", ", selectParts));
        sql.append("\nFROM ")
           .append(baseTable.isBlank() ? baseTable : quote(baseTable))
           .append(" as ")
           .append(baseAlias);
        for (Join join : joins.values()) {
            sql.append("\nINNER JOIN ")
               .append(quote(join.table))
               .append(" as ")
               .append(join.alias)
               .append(" ON ")
               .append(join.on);
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
     * Best-effort structured condition for a single binary predicate (matches what the editor shows).
     */
    private static List<Map<String, Object>> conditions(IntentGenerationContext context, IntentModel model, EntityIntent source,
            String baseAlias, String filter) {
        List<Map<String, Object>> conditions = new ArrayList<>();
        if (filter == null || filter.isBlank() || source == null) {
            return conditions;
        }
        Matcher matcher = SIMPLE_CONDITION.matcher(filter.trim());
        if (matcher.matches()) {
            Map<String, Object> condition = new LinkedHashMap<>();
            condition.put("left", qualifyToken(model, source, baseAlias, matcher.group(1)));
            condition.put("operation", matcher.group(2));
            condition.put("right", qualifyToken(model, source, baseAlias, matcher.group(3)));
            conditions.add(condition);
        }
        return conditions;
    }

    /**
     * Qualify a single filter token to a physical column when it names a field/relation.field; else
     * leave it.
     */
    private static String qualifyToken(IntentModel model, EntityIntent source, String baseAlias, String token) {
        int dot = token.indexOf('.');
        if (dot > 0) {
            RelationIntent relation = relationByName(source, token.substring(0, dot));
            if (relation != null && relation.getTo() != null) {
                return relation.getTo() + "." + column(relation.getTo(), token.substring(dot + 1));
            }
            return token;
        }
        return fieldByName(source, token) != null ? baseAlias + "." + column(source.getName(), token) : token;
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
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("table", tableAlias);
        column.put("alias", alias);
        column.put("name", physicalColumn);
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
     * join.
     */
    private static final class ColumnRef {
        private String tableAlias;
        private String physicalColumn;
        private String reportType;
        private String displayAlias;
        private Join join;

        private String qualified() {
            return tableAlias + "." + quote(physicalColumn);
        }
    }

    /** An INNER JOIN to a related entity's table. */
    private static final class Join {
        private final String table;
        private final String alias;
        private final String on;

        private Join(String table, String alias, String on) {
            this.table = table;
            this.alias = alias;
            this.on = on;
        }
    }
}
