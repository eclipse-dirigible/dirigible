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
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits one {@code <report>.report} per {@link ReportIntent}, in the JSON shape the report editor and
 * the report runtime consume (the codbex convention - see {@code codbex-invoices/*.report}): an outer
 * record with {@code name} / {@code alias} (base-table alias) / {@code table} (physical base table) /
 * {@code columns} / a fully materialised SQL {@code query} / {@code conditions} / {@code security}.
 *
 * <p>
 * The report is rooted at {@link ReportIntent#getSource()}. Each dimension and measure resolves to a
 * physical column:
 * <ul>
 * <li>a plain field ({@code dueOn}) -&gt; a column on the source table;</li>
 * <li>a {@code relation.field} path ({@code member.name}) -&gt; an {@code INNER JOIN} to the related
 * entity plus a column on it - this is how a report shows columns from a parent/related entity;</li>
 * <li>a bare to-one relation name ({@code book}) -&gt; the foreign-key column on the source;</li>
 * <li>a measure {@code count(*)} / {@code sum(total)} / {@code avg(price)} / {@code min}/{@code max}
 * -&gt; an aggregate column (and the dimensions become the {@code GROUP BY}).</li>
 * </ul>
 * {@link ReportIntent#getFilter()} becomes the {@code WHERE} predicate, with the intent's field names
 * rewritten to their qualified physical columns (so {@code dueOn <= CURRENT_DATE} ->
 * {@code Loan.LOAN_DUE_ON <= CURRENT_DATE}); non-field tokens (operators, {@code CURRENT_DATE},
 * literals) pass through untouched.
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
     * {@code >} / {@code <} operators (the platform's {@code JsonHelper} escapes them to {@code \\u003d}
     * etc.; valid JSON, but unreadable and unlike the codbex {@code .report} files). Maps only - no
     * {@code @Expose} concern.
     */
    private static final Gson REPORT_JSON = new GsonBuilder().setPrettyPrinting()
                                                             .disableHtmlEscaping()
                                                             .create();

    /** {@code aggregate(field)} pattern - aggregate in group 1, field in group 2. */
    private static final Pattern AGGREGATE_EXPRESSION = Pattern.compile("\\s*(\\w+)\\s*\\(\\s*([^)]*)\\s*\\)\\s*");
    private static final Set<String> KNOWN_AGGREGATES = Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");
    private static final Pattern DOTTED_REF = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Pattern SIMPLE_CONDITION = Pattern.compile("^\\s*(\\S+)\\s*(<=|>=|<>|!=|=|<|>)\\s*(.+?)\\s*$");

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

    private static Map<String, Object> build(IntentGenerationContext context, ReportIntent report) {
        IntentModel model = context.getModel();
        EntityIntent source = entityByName(model, report.getSource());
        String baseAlias = report.getSource() == null ? report.getName() : report.getSource();
        String baseTable = report.getSource() == null ? "" : IntentNaming.tableName(context, report.getSource());

        boolean aggregated = report.getMeasures()
                                   .stream()
                                   .anyMatch(m -> m != null && !m.isBlank());

        Map<String, Join> joins = new LinkedHashMap<>();
        List<Map<String, Object>> columns = new ArrayList<>();
        List<String> selectParts = new ArrayList<>();
        List<String> groupParts = new ArrayList<>();

        for (String dimension : report.getDimensions()) {
            if (dimension == null || dimension.isBlank()) {
                continue;
            }
            ColumnRef ref = resolve(context, model, source, baseAlias, dimension.trim());
            registerJoin(joins, ref);
            columns.add(column(ref.tableAlias, ref.displayAlias, ref.physicalColumn, ref.reportType, "NONE", aggregated));
            selectParts.add(ref.qualified() + " as \"" + ref.displayAlias + "\"");
            if (aggregated) {
                groupParts.add(ref.qualified());
            }
        }
        for (String measure : report.getMeasures()) {
            if (measure == null || measure.isBlank()) {
                continue;
            }
            addMeasure(context, model, source, baseAlias, measure.trim(), joins, columns, selectParts);
        }

        String where = buildWhere(context, model, source, baseAlias, joins, report.getFilter());
        String query = buildQuery(baseTable, baseAlias, joins, selectParts, where, aggregated ? groupParts : List.of());

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("name", report.getName());
        document.put("alias", baseAlias);
        document.put("table", baseTable);
        document.put("tId", translationId(report.getName()));
        document.put("label", humanize(report.getName()));
        if (report.getDescription() != null && !report.getDescription()
                                                      .isBlank()) {
            document.put("description", report.getDescription());
        }
        document.put("columns", columns);
        document.put("query", query);
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
                String type = "COUNT".equals(aggregate) ? "INTEGER" : ("MIN".equals(aggregate) || "MAX".equals(aggregate) ? ref.reportType : "DECIMAL");
                columns.add(column(ref.tableAlias, alias, ref.physicalColumn, type, aggregate, false));
                selectParts.add(aggregate + "(" + ref.qualified() + ") as \"" + alias + "\"");
                return;
            }
        }
        LOGGER.warn("Measure [{}] did not match the aggregate(field) convention - skipping", measure);
    }

    /** Resolve a dimension/measure field reference to its physical column, joining when it crosses a relation. */
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
                ref.reportType = reportType(targetField == null ? null : targetField.getType());
                ref.displayAlias = humanize(reference.replace('.', ' '));
                ref.join = join(context, source, relation, target, targetAlias, baseAlias);
                return ref;
            }
        }
        // Bare reference: a field, else a to-one relation's FK column, else a best-effort raw column.
        FieldIntent field = source == null ? null : fieldByName(source, reference);
        ref.tableAlias = baseAlias;
        ref.physicalColumn = column(source == null ? baseAlias : source.getName(), reference);
        ref.displayAlias = humanize(reference);
        if (field != null) {
            ref.reportType = reportType(field.getType());
        } else {
            RelationIntent relation = source == null ? null : relationByName(source, reference);
            ref.reportType = relation != null ? "INTEGER" : "CHARACTER VARYING";
        }
        return ref;
    }

    private static Join join(IntentGenerationContext context, EntityIntent source, RelationIntent relation, EntityIntent target,
            String targetAlias, String baseAlias) {
        FieldIntent targetPk = target == null ? null : primaryKeyOf(target);
        String fkColumn = column(source.getName(), relation.getName());
        String pkColumn = column(targetAlias, targetPk == null ? "id" : targetPk.getName());
        return new Join(IntentNaming.tableName(context, targetAlias), targetAlias,
                baseAlias + "." + fkColumn + " = " + targetAlias + "." + pkColumn);
    }

    private static void registerJoin(Map<String, Join> joins, ColumnRef ref) {
        if (ref.join != null) {
            joins.putIfAbsent(ref.join.alias, ref.join);
        }
    }

    private static String buildQuery(String baseTable, String baseAlias, Map<String, Join> joins, List<String> selectParts,
            String where, List<String> groupParts) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append(selectParts.isEmpty() ? "*" : String.join(", ", selectParts));
        sql.append("\nFROM ")
           .append(baseTable)
           .append(" as ")
           .append(baseAlias);
        for (Join join : joins.values()) {
            sql.append("\nINNER JOIN ")
               .append(join.table)
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

    /** Rewrite the intent filter's field names to qualified physical columns; pass other tokens through. */
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
                joins.putIfAbsent(targetAlias, join(context, source, relation, target, targetAlias, baseAlias));
                matcher.appendReplacement(dotted, Matcher.quoteReplacement(targetAlias + "." + column(targetAlias, matcher.group(2))));
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
                        Matcher.quoteReplacement(baseAlias + "." + column(source.getName(), field.getName())));
            }
        }
        return where.trim();
    }

    /** Best-effort structured condition for a single binary predicate (matches what the editor shows). */
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

    /** Qualify a single filter token to a physical column when it names a field/relation.field; else leave it. */
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

    private static Map<String, Object> column(String tableAlias, String alias, String physicalColumn, String reportType,
            String aggregate, boolean grouping) {
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
        return column;
    }

    private static String column(String entityName, String fieldName) {
        return IntentNaming.upperSnake(entityName) + "_" + IntentNaming.upperSnake(fieldName);
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

    /** A resolved column reference: where it lives, its physical name + type, display alias, optional join. */
    private static final class ColumnRef {
        private String tableAlias;
        private String physicalColumn;
        private String reportType;
        private String displayAlias;
        private Join join;

        private String qualified() {
            return tableAlias + "." + physicalColumn;
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
