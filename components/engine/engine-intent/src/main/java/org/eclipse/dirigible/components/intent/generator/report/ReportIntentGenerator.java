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

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits one {@code <report>.report} per {@link ReportIntent} declared in the intent. The output is
 * the JSON shape the report editor in the IDE consumes: an outer record with {@code alias} /
 * {@code tId} / {@code label} / {@code query} plus a {@code columns} array whose entries carry
 * {@code name} / {@code alias} / {@code type} / {@code aggregate}.
 *
 * <p>
 * Dimensions become columns with {@code aggregate: NONE}; measures are parsed by the
 * {@code count(*)} / {@code sum(field)} / {@code avg(field)} / {@code min(field)} /
 * {@code max(field)} convention into columns with the matching aggregate. Any measure that doesn't
 * match the pattern is logged and emitted as a {@code NONE}-aggregate column carrying the raw text
 * as its name.
 *
 * <p>
 * {@code query} is left empty; the report editor builds the SQL from {@code baseTable} +
 * {@code columns} + {@code joins} + {@code filters} on open, and intent doesn't yet model joins or
 * filters. {@code baseTable} is the upper-snake of the report's {@code source} entity name -
 * matching what the EDM generator emits as {@code dataName}, so the two artefacts line up.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(500)
public class ReportIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportIntentGenerator.class);

    /**
     * {@code aggregate(field)} pattern. Captures the aggregate name in group 1 and the field in group
     * 2.
     */
    private static final Pattern AGGREGATE_EXPRESSION = Pattern.compile("\\s*(\\w+)\\s*\\(\\s*([^)]*)\\s*\\)\\s*");

    private static final Set<String> KNOWN_AGGREGATES = Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");

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
                LOGGER.warn("Skipping unnamed report in intent [{}]", context.getIntent()
                                                                             .getName());
                continue;
            }
            String fileName = report.getName() + ".report";
            if (!seenFiles.add(fileName)) {
                LOGGER.warn("Duplicate report [{}] in intent [{}] - keeping the first occurrence", report.getName(), context.getIntent()
                                                                                                                            .getName());
                continue;
            }
            Map<String, Object> document = build(context, report);
            context.writeModelFile(fileName, JsonHelper.toJson(document));
        }
    }

    private static Map<String, Object> build(IntentGenerationContext context, ReportIntent report) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("alias", report.getName());
        document.put("tId", translationId(report.getName()));
        document.put("label", humanize(report.getName()));
        if (report.getDescription() != null && !report.getDescription()
                                                      .isBlank()) {
            document.put("description", report.getDescription());
        }
        if (report.getSource() != null && !report.getSource()
                                                 .isBlank()) {
            document.put("baseTable", IntentNaming.tableName(context, report.getSource()));
        }
        document.put("query", "");
        document.put("columns", buildColumns(report));
        document.put("joins", new ArrayList<>());
        document.put("filters", new ArrayList<>());
        document.put("orders", new ArrayList<>());
        return document;
    }

    private static List<Map<String, Object>> buildColumns(ReportIntent report) {
        List<Map<String, Object>> columns = new ArrayList<>();
        for (String dimension : report.getDimensions()) {
            if (dimension == null || dimension.isBlank()) {
                continue;
            }
            columns.add(dimensionColumn(dimension));
        }
        for (String measure : report.getMeasures()) {
            if (measure == null || measure.isBlank()) {
                continue;
            }
            columns.add(measureColumn(measure));
        }
        return columns;
    }

    /**
     * Dimension columns carry the field path verbatim and default to a VARCHAR with no aggregate.
     * Dotted paths (e.g. {@code customer.country}) are kept as-is - the report editor resolves the
     * cross-table reference on open.
     */
    private static Map<String, Object> dimensionColumn(String dimension) {
        String alias = aliasOfDimension(dimension);
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("name", dimension);
        column.put("alias", alias);
        column.put("tId", translationId(alias));
        column.put("label", humanize(alias));
        column.put("type", "VARCHAR");
        column.put("aggregate", "NONE");
        return column;
    }

    /**
     * Parse a measure expression like {@code count(*)} or {@code sum(total)} into a column with the
     * matching aggregate. Unknown shapes fall back to NONE-aggregate columns carrying the raw text so
     * the report editor at least loads the file.
     */
    private static Map<String, Object> measureColumn(String measure) {
        Matcher matcher = AGGREGATE_EXPRESSION.matcher(measure);
        if (matcher.matches()) {
            String aggregate = matcher.group(1)
                                      .toUpperCase(Locale.ROOT);
            String field = matcher.group(2)
                                  .trim();
            if (KNOWN_AGGREGATES.contains(aggregate)) {
                String columnName = field.isEmpty() ? "*" : field;
                String alias = aliasOfMeasure(aggregate, field);
                Map<String, Object> column = new LinkedHashMap<>();
                column.put("name", columnName);
                column.put("alias", alias);
                column.put("tId", translationId(alias));
                column.put("label", humanize(alias));
                column.put("type", numericTypeFor(aggregate));
                column.put("aggregate", aggregate);
                return column;
            }
        }
        LOGGER.warn("Measure [{}] did not match the aggregate(field) convention - emitting as a NONE-aggregate raw column", measure);
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("name", measure);
        column.put("alias", aliasOfDimension(measure));
        column.put("tId", translationId(measure));
        column.put("label", humanize(measure));
        column.put("type", "VARCHAR");
        column.put("aggregate", "NONE");
        return column;
    }

    private static String aliasOfDimension(String dimension) {
        return dimension.replace('.', '_')
                        .replace(' ', '_');
    }

    private static String aliasOfMeasure(String aggregate, String field) {
        if (field.isEmpty() || "*".equals(field)) {
            return aggregate.toLowerCase(Locale.ROOT);
        }
        return aggregate.toLowerCase(Locale.ROOT) + "_" + field.replace('.', '_');
    }

    /**
     * COUNT yields an integer; the rest of the supported aggregates can carry decimals.
     */
    private static String numericTypeFor(String aggregate) {
        return "COUNT".equals(aggregate) ? "INTEGER" : "DECIMAL";
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

    /**
     * camelCase / snake_case / dotted-path identifier to a human label.
     */
    private static String humanize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 4);
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '_' || c == '-' || c == '.') {
                out.append(' ');
                capitalizeNext = true;
                continue;
            }
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(raw.charAt(i - 1))) {
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
}
