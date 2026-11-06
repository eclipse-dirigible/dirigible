/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.generator.report.ReportIntentGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.GeneratesItemsIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.model.ReportParameterIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleConditionIntent;

/**
 * A generation that reads a report's RESULT ROWS (issue #8329), in one of two shapes:
 * <ul>
 * <li>items mode ({@code items: fromReport:}) - one target document whose header is mapped from the
 * source record, plus one line per report row passing {@code where:} and one {@code counter:} line
 * per group. A year close books the result accounts' balances this way, and {@code undo: reverse}
 * books the target's declared reversal;</li>
 * <li>rows mode (a top-level {@code fromReport:}) - one target ROW per report row, each carrying
 * the {@code backReference:} to the source. Next year's opening balances are generated this way,
 * and {@code undo: delete} deletes exactly the rows that back-reference the source.</li>
 * </ul>
 *
 * <p>
 * This class owns the construct on the intent side - the rules the parser enforces
 * ({@link #validate}) and the keys the create-from's glue entry carries ({@link #put}) - so both
 * read one resolution of every reference: a column the parser accepts is exactly one the generator
 * names. In both shapes the back-reference is mandatory: it is how a second run finds what the
 * first one made (and is refused), and how the undo finds what to undo.
 */
public final class ReportItemsSupport {

    /** The undo of items mode: the target's declared reversal. */
    public static final String UNDO_REVERSE = "reverse";

    /** The undo of rows mode: the generated rows are deleted. */
    public static final String UNDO_DELETE = "delete";

    /** The operators a row condition compares with. */
    private static final Set<String> OPERATORS = Set.of("eq", "ne", "gt", "ge", "lt", "le");

    /**
     * The report parameters a balance window is read with - a run that leaves one out reads all time.
     */
    private static final List<String> WINDOW = List.of("fromDate", "toDate");

    /**
     * The closing balance as one number rather than two totals: name -> how it is derived from the
     * closing debit and credit - {@code signed} (debit - credit), or the side it stands on
     * ({@code netDebit} / {@code netCredit}, zero on the other side). The opening row of a paid
     * document nets to nothing, which only a net reading can tell.
     */
    private static final Map<String, String> DERIVED =
            Map.of("closingBalance", "signed", "closingNetDebit", "netDebit", "closingNetCredit", "netCredit");

    private ReportItemsSupport() {}

    /**
     * The parse rules of a report-reading generation and of {@code undo:}.
     *
     * @param g the create-from
     * @param source the local source entity, or null when it is unresolved or cross-model
     * @param byName the local entities by name
     * @param model the model
     * @param crossModel whether the target is owned by another model
     * @param issues the collected issues
     */
    public static void validate(GeneratesIntent g, EntityIntent source, Map<String, EntityIntent> byName, IntentModel model,
            boolean crossModel, List<String> issues) {
        String subject = "generates [" + g.getName() + "]";
        if (g.hasUndo() && !g.readsReport()) {
            issues.add(subject + " declares undo, which undoes what a report generated (fromReport) - this create-from reads none");
            return;
        }
        if (!g.readsReport()) {
            return;
        }
        if (g.hasReportItems() && g.hasReportRows()) {
            issues.add(subject + " declares both fromReport and items.fromReport - one document with a line per row, or one row per row");
            return;
        }
        if (crossModel || g.isCrossModelSource() || g.isEventDriven()) {
            issues.add(subject + " reads a report, which needs a local source and target and a button - it is run by a click on the"
                    + " source record");
            return;
        }
        String report = g.hasReportRows() ? g.getFromReport()
                : g.getItems()
                   .getFromReport();
        ReportIntent declared = report(model, report);
        if (declared == null) {
            issues.add(subject + " fromReport [" + report + "] is not a declared report");
            return;
        }
        if (!declared.isBalance() || declared.hasCorrespondence()) {
            issues.add(subject + " fromReport [" + declared.getName() + "] is not a kind: balance report without correspondence - its"
                    + " rows are the balances a generation books");
            return;
        }
        if (source == null || g.getTo() == null) {
            return; // the parser reported the source or the target
        }
        Map<String, String> parameters;
        List<ScheduleConditionIntent> where;
        if (g.hasReportRows()) {
            if (g.getItems() != null || (g.getItemLines() != null && !g.getItemLines()
                                                                       .isEmpty())
                    || g.hasPrompt() || g.hasLink() || g.getSourceStatus() != null || g.hasReopen()) {
                issues.add(subject + " generates one row per report row, which takes no items, prompt, link or sourceStatus");
            }
            EntityIntent written = byName.get(g.getTo());
            if (written == null) {
                return; // reported with the target
            }
            RelationIntent back = toOne(written, g.getBackReference());
            if (back == null || !source.getName()
                                       .equals(back.getTo())) {
                issues.add(subject + " backReference [" + g.getBackReference() + "] is not a to-one relation of [" + written.getName()
                        + "] to [" + source.getName() + "] - every generated row carries it, and a second run and the undo find"
                        + " the rows through it");
            } else if (g.getMap()
                        .keySet()
                        .stream()
                        .anyMatch(key -> key.equalsIgnoreCase(g.getBackReference()))) {
                issues.add(subject + " maps the backReference [" + g.getBackReference() + "] - the run writes it");
            }
            parameters = g.getParameters();
            where = g.getWhere();
            cells(g.getMap(), written, byName, declared, subject + " map", issues);
        } else {
            GeneratesItemsIntent items = g.getItems();
            String at = subject + " items";
            if (items.getFrom() != null && !items.getFrom()
                                                 .isBlank()) {
                issues.add(at + " declares both from and fromReport - its lines are either the source's rows or the report's");
            }
            if (items.hasRefuse()) {
                issues.add(at + " declares refuse - a report row the where leaves out is simply not a line");
            }
            if (backReference(g, source) == null) {
                issues.add(subject + " reads report lines but its map copies no source key onto the target: add the target's to-one back"
                        + " to [" + source.getName() + "] to the map (the source's [" + IntentEntities.keyFieldName(source)
                        + "] as its value) - it is how a second run finds the document the first one made");
            }
            EntityIntent line = items.getTo() == null ? null : byName.get(items.getTo());
            if (line == null) {
                issues.add(at + ".to [" + items.getTo() + "] is not an entity of this model");
                return;
            }
            if (IntentEntities.itemsBackReference(line, g.getTo()) == null) {
                issues.add(at + ".to [" + line.getName() + "] is not a composition child of [" + g.getTo() + "]");
            }
            parameters = items.getParameters();
            where = items.getWhere();
            cells(items.getMap(), line, byName, declared, at + " map", issues);
            cells(items.getCounter(), line, byName, declared, at + " counter", issues);
        }
        validateParameters(parameters, declared, source, subject, issues);
        validateWhere(where, declared, subject, issues);
        if (g.hasUndo()) {
            validateUndo(g, model, subject, issues);
        }
    }

    private static void validateParameters(Map<String, String> bound, ReportIntent report, EntityIntent source, String subject,
            List<String> issues) {
        Set<String> parameters = new java.util.HashSet<>(WINDOW);
        for (ReportParameterIntent parameter : report.getParameters()) {
            parameters.add(parameter.getName());
        }
        for (Map.Entry<String, String> parameter : bound.entrySet()) {
            if (!parameters.contains(parameter.getKey())) {
                issues.add(subject + " parameter [" + parameter.getKey() + "] is not a parameter of report [" + report.getName() + "] "
                        + new java.util.TreeSet<>(parameters));
            }
            if (property(source, parameter.getValue()) == null) {
                issues.add(subject + " parameter [" + parameter.getKey() + "] reads [" + parameter.getValue()
                        + "], which is not a field or to-one relation of [" + source.getName() + "]");
            }
        }
        for (String window : WINDOW) {
            if (!bound.containsKey(window)) {
                issues.add(subject + " binds no [" + window + "] - the report would be read over all time; bind it to a property of ["
                        + source.getName() + "]");
            }
        }
    }

    private static void validateWhere(List<ScheduleConditionIntent> where, ReportIntent report, String subject, List<String> issues) {
        if (where == null) {
            return;
        }
        for (ScheduleConditionIntent condition : where) {
            if (column(report, condition.getField()) == null) {
                issues.add(subject + " where-condition reads [" + condition.getField() + "], which is not a column of report ["
                        + report.getName() + "] " + names(report));
            }
            if (!OPERATORS.contains(condition.getOp())) {
                issues.add(
                        subject + " where-condition uses unsupported operator [" + condition.getOp() + "] (supported: eq/ne/gt/ge/lt/le)");
            } else if (!"eq".equals(condition.getOp()) && !"ne".equals(condition.getOp()) && !isNumber(condition.getValue())) {
                issues.add(subject + " where-condition [" + condition.getField() + " " + condition.getOp() + "] compares with ["
                        + condition.getValue() + "] - an ordering compares a number");
            }
        }
    }

    private static void validateUndo(GeneratesIntent g, IntentModel model, String subject, List<String> issues) {
        String expected = g.hasReportRows() ? UNDO_DELETE : UNDO_REVERSE;
        if (!expected.equals(g.getUndo())) {
            issues.add(subject + " undo [" + g.getUndo() + "] is not supported here (supported: " + expected + " - "
                    + (g.hasReportRows() ? "the generated rows are deleted" : "the generated document is reversed") + ")");
            return;
        }
        if (g.hasReportRows()) {
            return;
        }
        GeneratesIntent reversal = reversalOf(model, g.getTo());
        if (reversal == null || reversal.getReversal()
                                        .getBackLink() == null
                || reversal.getReversal()
                           .getBackLink()
                           .isBlank()) {
            issues.add(subject + " undo: reverse needs a reversals: entry on [" + g.getTo() + "] declaring backLink - it books that"
                    + " reversal, and the back-link is what retires the reversed document");
        }
    }

    /**
     * The cells of one row shape: every key a property of the written entity a report cell can be
     * written into, every value a column of the report - a decimal property only from an amount (one of
     * the six totals, or a closing balance derived from two of them), which is what lets the run
     * convert it exactly.
     */
    private static void cells(Map<String, String> cells, EntityIntent written, Map<String, EntityIntent> byName, ReportIntent report,
            String at, List<String> issues) {
        for (Map.Entry<String, String> cell : cells.entrySet()) {
            String kind = kind(written, byName, cell.getKey());
            Column column = column(report, cell.getValue());
            if (kind == null) {
                issues.add(at + " [" + cell.getKey() + "] is not a decimal, integer, long or text field, nor a to-one relation, of ["
                        + written.getName() + "]");
            } else if (column == null) {
                issues.add(at + " [" + cell.getKey() + "] reads [" + cell.getValue() + "], which is not a column of the report "
                        + names(report));
            } else if ("decimal".equals(kind) != column.amount()) {
                issues.add(at + " [" + cell.getKey() + "] reads [" + cell.getValue() + "] - an amount column ("
                        + ReportIntentGenerator.BALANCE_AMOUNT_ALIASES.keySet() + ", " + DERIVED.keySet()
                        + ") is written into a decimal field, and only there");
            }
        }
    }

    /**
     * Puts the report half of a generation onto its glue entry: the report, its parameters, the cells,
     * the row rule, the back-reference and the undo - and, in items mode, the counter cells and the
     * liveness test of a document already made.
     *
     * @param g the create-from (reading a report)
     * @param e the glue entry
     * @param source the source entity
     * @param byName the local entities by name
     * @param model the model
     */
    static void put(GeneratesIntent g, Map<String, Object> e, EntityIntent source, Map<String, EntityIntent> byName, IntentModel model) {
        boolean rows = g.hasReportRows();
        GeneratesItemsIntent items = g.getItems();
        ReportIntent report = report(model, rows ? g.getFromReport() : items.getFromReport());
        EntityIntent written = byName.get(rows ? g.getTo() : items.getTo());
        int amountScale = amountScale(report, byName.get(report.getSource()));
        e.put(rows ? "hasReportRows" : "hasReportItems", true);
        e.put("reportName", report.getName());
        List<Map<String, Object>> parameters = new ArrayList<>();
        (rows ? g.getParameters() : items.getParameters()).forEach(
                (name, property) -> parameters.add(Map.of("name", name, "property", IntentNaming.pascalCase(property))));
        e.put("reportParameters", parameters);
        e.put("reportCells", cells(rows ? g.getMap() : items.getMap(), written, byName, report, amountScale));
        List<Map<String, Object>> where = new ArrayList<>();
        List<ScheduleConditionIntent> conditions = rows ? g.getWhere() : items.getWhere();
        if (conditions != null) {
            for (ScheduleConditionIntent condition : conditions) {
                Map<String, Object> term = reading(column(report, condition.getField()));
                term.put("op", condition.getOp());
                // A missing value is the empty cell: eq / ne then ask whether the row holds one at all.
                term.put("value", condition.getValue() == null ? "" : String.valueOf(condition.getValue()));
                term.put("isNull", condition.getValue() == null);
                term.put("sourceScale", amountScale);
                where.add(term);
            }
        }
        e.put("reportWhere", where);
        if (rows) {
            e.put("backRefProperty", IntentNaming.pascalCase(toOne(written, g.getBackReference()).getName()));
            if (g.hasUndo()) {
                e.put("hasUndo", true);
                e.put("undoDelete", true);
            }
            return;
        }
        e.put("counterCells", cells(items.getCounter(), written, byName, report, amountScale));
        e.put("toItemEntity", items.getTo());
        e.put("toFkProperty", IntentEntities.itemsBackReference(written, g.getTo()));
        e.put("backRefProperty", backReference(g, source));
        String dead = GlueIntentGenerator.deadEntryCondition(byName.get(g.getTo()), model);
        e.put("liveCondition", dead.isEmpty() ? "true" : "!(" + dead + ")");
        GeneratesIntent reversal = g.hasUndo() ? reversalOf(model, g.getTo()) : null;
        if (reversal == null) {
            return;
        }
        e.put("hasUndo", true);
        e.put("undoReversalClass", IntentNaming.pascalIdentifier(reversal.getName()));
        e.put("undoReversalRefuses", !reversal.getReversal()
                                              .getRefuse()
                                              .isEmpty());
        e.put("undoBackLinkProperty", IntentNaming.pascalCase(reversal.getReversal()
                                                                      .getBackLink()));
        RelationIntent status = IntentEntities.entityStatusRelation(byName.get(g.getTo()));
        boolean guarded = status != null && reversal.hasFromStatus();
        e.put("undoStatusProperty", guarded ? IntentNaming.pascalCase(status.getName()) : "");
        e.put("undoStatusExpr", guarded ? String.join(" || ", reversal.getFromStatus()
                                                                      .stream()
                                                                      .map(id -> "undoStatus == " + id)
                                                                      .toList())
                : "");
        e.put("undoStatuses", guarded ? String.valueOf(reversal.getFromStatus()) : "");
    }

    /**
     * The undo action's name - its descriptor id, translation key and settings entry.
     *
     * @param g the create-from
     * @return the name
     */
    public static String undoName(GeneratesIntent g) {
        return g.getName() + "-undo";
    }

    /**
     * The undo action's label: "Undo " and the create-from's own.
     *
     * @param g the create-from
     * @return the label
     */
    public static String undoLabel(GeneratesIntent g) {
        return "Undo " + IntentNaming.customActionLabel(g.getName(), g.getLabel());
    }

    /** One row shape's cells, each with the conversion its target property takes. */
    private static List<Map<String, Object>> cells(Map<String, String> cells, EntityIntent written, Map<String, EntityIntent> byName,
            ReportIntent report, int amountScale) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, String> cell : cells.entrySet()) {
            String kind = kind(written, byName, cell.getKey());
            FieldIntent field = field(written, cell.getKey());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("targetProp", IntentNaming.pascalCase(cell.getKey()));
            entry.putAll(reading(column(report, cell.getValue())));
            entry.put("kind", kind);
            entry.put("scale", "decimal".equals(kind) && field.getScale() != null ? field.getScale() : 2);
            entry.put("sourceScale", amountScale);
            out.add(entry);
        }
        return out;
    }

    /** What a cell or condition reads off a row: one column, or a closing balance derived from two. */
    private static Map<String, Object> reading(Column column) {
        Map<String, Object> reading = new LinkedHashMap<>();
        reading.put("alias", column.alias());
        if (column.derive() != null) {
            reading.put("minusAlias", column.minusAlias());
            reading.put("derive", column.derive());
        }
        return reading;
    }

    /**
     * A column of a balance report: a dimension or one of the six totals by its alias, or a closing
     * balance derived from the closing debit and credit.
     */
    private record Column(String alias, String minusAlias, String derive, boolean amount) {
    }

    /** The column an authored name reads, or null. */
    private static Column column(ReportIntent report, String name) {
        if (name == null) {
            return null;
        }
        for (Map.Entry<String, String> derived : DERIVED.entrySet()) {
            if (ReportIntentGenerator.columnKey(derived.getKey())
                                     .equals(ReportIntentGenerator.columnKey(name))) {
                return new Column(ReportIntentGenerator.BALANCE_AMOUNT_ALIASES.get("closingDebit"),
                        ReportIntentGenerator.BALANCE_AMOUNT_ALIASES.get("closingCredit"), derived.getValue(), true);
            }
        }
        String alias = ReportIntentGenerator.balanceAliases(report)
                                            .get(ReportIntentGenerator.columnKey(name));
        return alias == null ? null : new Column(alias, null, null, ReportIntentGenerator.BALANCE_AMOUNT_ALIASES.containsValue(alias));
    }

    /** The column names an author may use, for a refusal. */
    private static String names(ReportIntent report) {
        List<String> names = new ArrayList<>(ReportIntentGenerator.balanceAliases(report)
                                                                  .keySet());
        DERIVED.keySet()
               .stream()
               .sorted()
               .forEach(name -> names.add(ReportIntentGenerator.columnKey(name)));
        return names.toString();
    }

    /**
     * The conversion a report cell takes into a property of the written entity: {@code decimal},
     * {@code integer}, {@code long} or {@code text} - a to-one relation by its target's key - or null
     * for a property no report cell can be written into.
     */
    private static String kind(EntityIntent written, Map<String, EntityIntent> byName, String property) {
        FieldIntent field = field(written, property);
        String type;
        if (field != null) {
            type = field.getType();
        } else {
            RelationIntent relation = toOne(written, property);
            EntityIntent target = relation == null ? null : byName.get(relation.getTo());
            if (target == null) {
                return null;
            }
            FieldIntent key = IntentEntities.primaryKeyOf(target);
            type = key == null ? "integer" : key.getType();
        }
        return switch (IntentEntities.sqlType(type)) {
            case "DECIMAL" -> "decimal";
            case "INTEGER" -> "integer";
            case "BIGINT" -> "long";
            case "VARCHAR" -> type == null || Set.of("string", "text", "uuid")
                                                 .contains(type.trim()
                                                               .toLowerCase(Locale.ROOT)) ? "text" : null;
            default -> null;
        };
    }

    /**
     * The scale of the report's amounts: the larger of its debit and credit fields' (a decimal without
     * one is stored at 2, a whole-number amount at 0). A row read before report decimals are exact
     * (#8487) is trusted only below the magnitude this scale leaves 15 significant digits for.
     */
    private static int amountScale(ReportIntent report, EntityIntent ledger) {
        int scale = 0;
        for (String amount : List.of(report.getDebit(), report.getCredit())) {
            FieldIntent field = ledger == null || amount == null ? null : field(ledger, amount.trim());
            if (field != null && "DECIMAL".equals(IntentEntities.sqlType(field.getType()))) {
                scale = Math.max(scale, field.getScale() == null ? 2 : field.getScale());
            }
        }
        return scale;
    }

    /** The target property the header map copies the source's key onto, or null. */
    private static String backReference(GeneratesIntent g, EntityIntent source) {
        String key = IntentEntities.keyFieldName(source);
        for (Map.Entry<String, String> mapping : g.getMap()
                                                  .entrySet()) {
            if (mapping.getValue() != null && key.equals(IntentNaming.pascalCase(mapping.getValue()))) {
                return IntentNaming.pascalCase(mapping.getKey());
            }
        }
        return null;
    }

    /**
     * The declared reversal of the entity (the create-from a {@code reversals:} entry expands into).
     */
    private static GeneratesIntent reversalOf(IntentModel model, String entity) {
        for (GeneratesIntent candidate : model.getGenerates()) {
            if (candidate.isReversal() && entity != null && entity.equals(candidate.getTo())) {
                return candidate;
            }
        }
        return null;
    }

    private static ReportIntent report(IntentModel model, String name) {
        for (ReportIntent report : model.getReports()) {
            if (report.getName() != null && report.getName()
                                                  .equals(name)) {
                return report;
            }
        }
        return null;
    }

    private static boolean isNumber(Object value) {
        if (value instanceof Number) {
            return true;
        }
        try {
            new java.math.BigDecimal(String.valueOf(value));
            return true;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    private static Object property(EntityIntent entity, String name) {
        FieldIntent field = field(entity, name);
        return field != null ? field : toOne(entity, name);
    }

    private static FieldIntent field(EntityIntent entity, String name) {
        if (entity == null || name == null) {
            return null;
        }
        for (FieldIntent field : entity.getFields()) {
            if (field.getName() != null && field.getName()
                                                .equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    private static RelationIntent toOne(EntityIntent entity, String name) {
        if (entity == null || name == null || entity.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.getName() != null && relation.getName()
                                                      .equalsIgnoreCase(name)
                    && ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                return relation;
            }
        }
        return null;
    }
}
