/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Covers the .glue backward-compatibility of the posting binding (dirigible #7234): the descriptor
 * keys a generated .glue carries verbatim are read under every spelling a released generator wrote
 * them in, so a project generated between two releases renders the handler its intent asked for,
 * not a silently degraded one, until it is re-generated.
 */
class GlueGeneratorTest {

    @Test
    void theRenamedCompareOnlyWhenDerivedKeyIsReadUnderItsFormerSpelling() {
        // #7188 renamed the #7163 key `expressionDefault`; the template reads only the new one, so a
        // .glue from between the two rendered a CURRENT_DATE-default cell with a plain same() - every
        // redelivery of such a row read as an amendment.
        Map<String, Object> cell = cell("ValueDate");
        cell.put("expressionDefault", Boolean.TRUE);

        List<Map<String, Object>> cells = GlueGenerator.comparedCells(List.of(cell));

        assertThat(cells).hasSize(1);
        assertThat(cells.get(0)).as("the former spelling is honoured as the current one")
                                .containsEntry("compareOnlyWhenDerived", Boolean.TRUE)
                                .containsEntry("name", "ValueDate");
        assertThat(cell).as("the descriptor itself is left untouched")
                        .doesNotContainKey("compareOnlyWhenDerived");
    }

    /**
     * A cell carrying both spellings was written by the current generator, whose key is authoritative.
     */
    @Test
    void theCurrentSpellingWinsOverTheFormerOne() {
        Map<String, Object> cell = cell("Debit");
        cell.put("compareOnlyWhenDerived", Boolean.FALSE);
        cell.put("expressionDefault", Boolean.TRUE);

        List<Map<String, Object>> cells = GlueGenerator.comparedCells(List.of(cell));

        assertThat(cells.get(0)).containsEntry("compareOnlyWhenDerived", Boolean.FALSE);
    }

    /** A plainly compared column, or a .glue written before the amendment half, carries neither key. */
    @Test
    void aCellCarryingNeitherSpellingIsCopiedAsItIs() {
        Map<String, Object> cell = cell("Account");
        cell.put("derivedDefault", "");

        List<Map<String, Object>> cells = GlueGenerator.comparedCells(List.of(cell));

        assertThat(cells.get(0)).containsExactlyEntriesOf(cell);
    }

    /** A .glue written before the amendment half declares no compared cells at all. */
    @Test
    void anAbsentListBindsAsAnEmptyOne() {
        assertThat(GlueGenerator.comparedCells(null)).isEmpty();
        assertThat(GlueGenerator.comparedCells("not a list")).isEmpty();
    }

    /**
     * The header assignments are normalised by their own pass (#7256), which reads the flag through the
     * same rule - a header written between #7163 and #7188 keeps its treatment too.
     */
    @Test
    void aHeaderAssignmentReadsTheFlagUnderItsFormerSpellingToo() {
        Map<String, Object> declared = new LinkedHashMap<>();
        declared.put("targetProp", "ValueDate");
        declared.put("expr", "source.IssueDate");
        declared.put("local", "header1");
        declared.put("expressionDefault", Boolean.TRUE);

        List<Map<String, Object>> assignments = GlueGenerator.headerAssignments(List.of(declared));

        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0)).containsEntry("compareOnlyWhenDerived", Boolean.TRUE)
                                      .containsEntry("value", "header1")
                                      .containsEntry("hoisted", Boolean.TRUE);
    }

    /** A header written by the current generator carries the current key, and that key decides. */
    @Test
    void aHeaderAssignmentCarryingTheCurrentSpellingIgnoresTheFormerOne() {
        Map<String, Object> declared = new LinkedHashMap<>();
        declared.put("targetProp", "Reason");
        declared.put("expr", "source.Reason");
        declared.put("compareOnlyWhenDerived", Boolean.FALSE);
        declared.put("expressionDefault", Boolean.TRUE);

        List<Map<String, Object>> assignments = GlueGenerator.headerAssignments(List.of(declared));

        assertThat(assignments.get(0)).containsEntry("compareOnlyWhenDerived", Boolean.FALSE)
                                      .containsEntry("value", "source.Reason")
                                      .containsEntry("hoisted", Boolean.FALSE);
    }

    /**
     * The authored descriptor values a glue template writes into a Java string literal - a setter's
     * value, a series, a cron, a destination, a webhook path - reach it escaped, so a quote in any of
     * them cannot end the literal and fail the compile of the whole generated module (#7295).
     */
    @Test
    void derivesTheEscapedTwinOfAnAuthoredDescriptorValue() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("value", "the \"issued\" one");
        item.put("series", "C:\\Sales");
        Map<String, Object> context = new LinkedHashMap<>();

        GlueGenerator.copyJavaLiterals(context, item, "value", "series");

        assertThat(context).containsEntry("valueJavaLiteral", "the \\\"issued\\\" one")
                           .containsEntry("seriesJavaLiteral", "C:\\\\Sales");
    }

    /**
     * The glue carries the query's CLAUSES and this layer writes the {@code Criteria} (issue #7406) -
     * the builder package and {@code java.time} leave the process description, and the same facts can
     * reach a template generating something other than Java.
     */
    @Test
    void aSchedulesCriteriaIsBuiltFromTheClausesTheGlueCarries() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("criteria", List.of(clause("lt", "Due", Map.of("kind", "moment", "shape", "date", "offset", "P7D", "forward", false)),
                clause("eq", "Status", Map.of("kind", "number", "text", "3")),
                clause("eq", "Reason", Map.of("kind", "string", "text", "over\"due")),
                clause("eq", "Active", Map.of("kind", "boolean", "text", "true")), clause("eq", "ClosedOn", Map.of("kind", "null"))));
        Map<String, Object> context = new LinkedHashMap<>();

        GlueGenerator.bindCriteria(context, item);

        assertThat(context).containsEntry("criteriaExpression",
                "Criteria.create().lt(\"Due\", java.time.LocalDate.now().minus(java.time.Period.parse(\"P7D\")))"
                        + ".eq(\"Status\", 3).eq(\"Reason\", \"over\\\"due\").eq(\"Active\", true).eq(\"ClosedOn\", null)");
    }

    /**
     * A .glue written before the split carries the rendered expression and no clauses, and renders
     * byte-identically to what it always did - the rule every backward-compatible glue key here
     * follows.
     */
    @Test
    void aDescriptorCarryingTheRenderedCriteriaKeepsIt() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("criteriaExpression", "Criteria.create().eq(\"Status\", 4)");
        Map<String, Object> context = new LinkedHashMap<>();

        GlueGenerator.bindCriteria(context, item);

        assertThat(context).containsEntry("criteriaExpression", "Criteria.create().eq(\"Status\", 4)");
    }

    /**
     * A roll-up and an expansion query the affected rows by the foreign key alone, which the descriptor
     * already carries - so the builder call is written here rather than shipped in the glue.
     */
    @Test
    void aForeignKeyCriteriaIsBuiltFromTheKeyTheDescriptorCarries() {
        Map<String, Object> rollup = new LinkedHashMap<>();
        rollup.put("fkProperty", "Member");
        Map<String, Object> context = new LinkedHashMap<>();

        GlueGenerator.bindForeignKeyCriteria(context, rollup, "entity", "Member");
        assertThat(context).containsEntry("criteriaExpression", "Criteria.create().eq(\"Member\", entity.Member)");

        Map<String, Object> expansion = new LinkedHashMap<>();
        expansion.put("fkProperty", "Contract");
        GlueGenerator.bindForeignKeyCriteria(context, expansion, "master", "Id");
        assertThat(context).containsEntry("criteriaExpression", "Criteria.create().eq(\"Contract\", master.Id)");

        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("criteriaExpression", "Criteria.create().eq(\"Member\", entity.Member)");
        legacy.put("fkProperty", "Ignored");
        GlueGenerator.bindForeignKeyCriteria(context, legacy, "entity", "Ignored");
        assertThat(context).containsEntry("criteriaExpression", "Criteria.create().eq(\"Member\", entity.Member)");
    }

    /**
     * A scheduled generation's {@code run:} key term carries the PERIOD, and the calendar arithmetic
     * the guard queries by is derived from it here - which is also the only place the two bounds stay
     * derived from one another (issue #7406).
     */
    @Test
    void aRunPeriodKeyTermIsRangedHere() {
        List<Map<String, Object>> terms = GlueGenerator.uniqueTerms(List.of(Map.of("property", "Supplier", "expr", "entity.Supplier"),
                Map.of("kind", "range", "property", "Date", "period", "month"), Map.of("property", "Day", "period", "day")));

        assertThat(terms.get(0)).containsEntry("expr", "entity.Supplier")
                                .doesNotContainKey("lower");
        assertThat(terms.get(1)).containsEntry("lower", "java.time.LocalDate.now().withDayOfMonth(1)")
                                .containsEntry("upper", "java.time.LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1)");
        // A single day needs no range: `between(today, today)` is not what `run: day` says.
        assertThat(terms.get(2)).containsEntry("expr", "java.time.LocalDate.now()")
                                .doesNotContainKey("lower");
    }

    /**
     * A term written before the split carries its own bounds and is passed through untouched.
     */
    @Test
    void aKeyTermCarryingItsOwnBoundsKeepsThem() {
        List<Map<String, Object>> terms =
                GlueGenerator.uniqueTerms(List.of(Map.of("kind", "range", "property", "Date", "lower", "LOWER", "upper", "UPPER")));

        assertThat(terms.get(0)).containsEntry("lower", "LOWER")
                                .containsEntry("upper", "UPPER");
    }

    /**
     * A posting's compared default reaches the glue as the reading of the column's own default, and the
     * literal {@code same()} compares against is written here (issue #7406).
     */
    @Test
    void aComparedDefaultIsRenderedFromTheReadingTheGlueCarries() {
        Map<String, Object> number = new LinkedHashMap<>();
        number.put("targetProp", "Debit");
        number.put("derivedDefaultValue", Map.of("kind", "number", "text", "0"));
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("targetProp", "Reason");
        text.put("derivedDefaultValue", Map.of("kind", "string", "text", "auto\"matic"));
        Map<String, Object> flag = new LinkedHashMap<>();
        flag.put("targetProp", "Active");
        flag.put("derivedDefaultValue", Map.of("kind", "boolean", "text", "true"));
        Map<String, Object> none = new LinkedHashMap<>();
        none.put("targetProp", "Account");
        none.put("derivedDefaultValue", Map.of());
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("targetProp", "Credit");
        legacy.put("derivedDefault", "new java.math.BigDecimal(\"7\")");

        List<Map<String, Object>> assignments = GlueGenerator.headerAssignments(List.of(number, text, flag, none, legacy));

        assertThat(assignments.get(0)).containsEntry("derivedDefault", "new java.math.BigDecimal(\"0\")");
        assertThat(assignments.get(1)).containsEntry("derivedDefault", "\"auto\\\"matic\"");
        assertThat(assignments.get(2)).containsEntry("derivedDefault", "Boolean.TRUE");
        // No default: the empty string is what the template's own #if reads as "nothing to compare
        // against", so an absent reading must not become a null that renders as its own text.
        assertThat(assignments.get(3)).containsEntry("derivedDefault", "");
        assertThat(assignments.get(4)).containsEntry("derivedDefault", "new java.math.BigDecimal(\"7\")");
    }

    /**
     * One criteria clause.
     *
     * @param op the operator
     * @param property the property
     * @param value the value reading
     * @return the clause
     */
    private static Map<String, Object> clause(String op, String property, Map<String, Object> value) {
        Map<String, Object> clause = new LinkedHashMap<>();
        clause.put("op", op);
        clause.put("property", property);
        clause.put("value", value);
        return clause;
    }

    /**
     * A key the descriptor does not carry is REMOVED rather than emptied: the context starts as a copy
     * of the generation parameters, and the template reads the key's absence.
     */
    @Test
    void aKeyTheDescriptorDoesNotCarryIsRemoved() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("perDefaultJavaLiteral", "left over from another descriptor");

        GlueGenerator.copyJavaLiterals(context, new LinkedHashMap<>(), "perDefault");

        assertThat(context).doesNotContainKey("perDefaultJavaLiteral");
    }

    private static Map<String, Object> cell(String name) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("name", name);
        return cell;
    }

    /**
     * Issue #7425: an assignment's Java is rendered from the reading the glue carries, and an entry
     * written before the split - carrying the rendered {@code expr} itself - keeps it.
     */
    @Test
    void anAssignmentsExpressionIsRenderedFromItsReading() {
        Map<String, Object> read = new LinkedHashMap<>();
        read.put("targetProp", "Customer");
        read.put("reading", Map.of("kind", "read", "owner", "source", "property", "Customer"));
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("targetProp", "Date");
        legacy.put("expr", "java.time.LocalDate.now()");

        List<Map<String, Object>> rendered = GlueGenerator.assignments(List.of(read, legacy));

        assertThat(rendered.get(0)).containsEntry("expr", "source.Customer")
                                   .containsEntry("targetProp", "Customer");
        assertThat(rendered.get(1)).containsEntry("expr", "java.time.LocalDate.now()");
        assertThat(GlueGenerator.assignments(null)).isEmpty();
    }

    @Test
    void aDerivedRowsGuardAndCellsAreRenderedFromTheirReadings() {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("targetProp", "Amount");
        cell.put("reading", Map.of("kind", "calc", "text", "Net + Vat", "owner", "source", "scale", "2"));
        Map<String, Object> guarded = new LinkedHashMap<>();
        guarded.put("guardReading", Map.of("kind", "calcCompare", "owner", "source", "property", "Vat", "equal", false, "text", "0"));
        guarded.put("assigns", List.of(cell));
        Map<String, Object> unguarded = new LinkedHashMap<>();
        unguarded.put("guardReading", Map.of());
        unguarded.put("assigns", List.of());
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("guard", "Calc.eval(\"Old\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) != 0");
        legacy.put("assigns", List.of());

        List<Map<String, Object>> rows = GlueGenerator.rows(List.of(guarded, unguarded, legacy));

        assertThat(rows.get(0)).containsEntry("guard", "Calc.eval(\"Vat\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) != 0");
        assertThat(ModelValues.asMaps(rows.get(0)
                                          .get("assigns"))
                              .get(0)).containsEntry("expr", "Calc.eval(\"Net + Vat\", source, 2)");
        // No guard renders as the empty string, which the template's own #if reads.
        assertThat(rows.get(1)).containsEntry("guard", "");
        assertThat(rows.get(2)).containsEntry("guard", "Calc.eval(\"Old\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) != 0");
    }

    /**
     * The classifier ternaries the posting handler null-guards are the cells carrying a rule-case
     * reading - derived, in row and cell order, rather than shipped twice.
     */
    @Test
    void theConditionalRuleGuardsAreTheRuleCaseCells() {
        Map<String, Object> plain = new LinkedHashMap<>();
        plain.put("targetProp", "Amount");
        plain.put("reading", Map.of("kind", "read", "owner", "source", "property", "Total"));
        Map<String, Object> ruleCase = new LinkedHashMap<>();
        ruleCase.put("targetProp", "Account");
        ruleCase.put("reading", Map.of("kind", "ruleCase", "by", "Method", "owner", "source", "cases",
                List.of(Map.of("value", "1", "column", "CashAccount")), "otherwise", "BankAccount"));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("assigns", List.of(plain, ruleCase));

        List<String> guards = GlueGenerator.conditionalRuleGuards(GlueGenerator.rows(List.of(row)));

        assertThat(guards).containsExactly(
                "(Calc.eval(\"Method\", source, 6).compareTo(new java.math.BigDecimal(\"1\")) == 0 ? ruleRow.CashAccount : ruleRow.BankAccount)");
    }

    /**
     * An event binding's guard is rendered from its neutral terms - {@code true} for none - and a
     * descriptor written before the split keeps the rendered expression it carries.
     */
    @Test
    void anEventGuardIsRenderedFromItsTerms() {
        Map<String, Object> guarded = new LinkedHashMap<>();
        guarded.put("guardTerms", List.of(term("Internal", false, "boolean", "true")));
        Map<String, Object> unguarded = new LinkedHashMap<>();
        unguarded.put("guardTerms", List.of());
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("guardExpression", "\"APPROVED\".equals(status)");
        Map<String, Object> context = new LinkedHashMap<>();

        GlueGenerator.bindEventGuard(context, guarded);
        assertThat(context).containsEntry("guardExpression", "!java.util.Objects.equals(entity.Internal, true)");
        GlueGenerator.bindEventGuard(context, unguarded);
        assertThat(context).containsEntry("guardExpression", "true");
        GlueGenerator.bindEventGuard(context, legacy);
        assertThat(context).containsEntry("guardExpression", "\"APPROVED\".equals(status)");
        // The untyped guards of a trigger, a wait and a register lookup infer the type from the spelling.
        Map<String, Object> untyped = new LinkedHashMap<>();
        untyped.put("guardTerms", List.of(term("Status", true, "number", "3"), term("Rate", true, "number", "2.5")));
        GlueGenerator.bindEventGuard(context, untyped);
        assertThat(context).containsEntry("guardExpression",
                "java.util.Objects.equals(entity.Status, 3) && java.util.Objects.equals(entity.Rate, 2.5)");
    }

    /**
     * Every other expression key - a transition's guard (the evaluator qualified, the controller
     * importing no Calc), a print attachment's language and file name, a timer's due moment - is bound
     * from its reading, from the rendered key an older descriptor carries, or as the empty string when
     * there is neither.
     */
    @Test
    void anExpressionKeyIsBoundFromItsReadingOrItsRenderedFallback() {
        Map<String, Object> reading = new LinkedHashMap<>();
        reading.put("guardReading", Map.of("kind", "calcCompare", "owner", "source", "property", "Paid", "equal", true, "text", "0"));
        reading.put("attachLanguage", Map.of("kind", "defaultLanguage"));
        reading.put("attachFileName", Map.of());
        reading.put("due", Map.of("kind", "due", "property", "DueAt", "shape", "timestamp"));
        Map<String, Object> context = new LinkedHashMap<>();

        GlueGenerator.bindExpression(context, reading, "guardReading", "guardExpr", true);
        GlueGenerator.bindExpression(context, reading, "attachLanguage", "attachLanguageExpression", false);
        GlueGenerator.bindExpression(context, reading, "attachFileName", "attachFileNameExpression", false);
        GlueGenerator.bindExpression(context, reading, "due", "dueExpression", false);

        assertThat(
                context).containsEntry("guardExpr",
                        "org.eclipse.dirigible.sdk.utils.Calc.eval(\"Paid\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) == 0")
                        .containsEntry("attachLanguageExpression", "org.eclipse.dirigible.sdk.print.Print.defaultLanguage()")
                        .containsEntry("attachFileNameExpression", "")
                        .containsEntry("dueExpression", "entity.DueAt == null"
                                + " ? java.util.Date.from(java.time.Instant.parse(\"9999-12-31T00:00:00Z\")) : java.util.Date.from(entity.DueAt)");

        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("attachLanguageExpression", "\"en\"");
        GlueGenerator.bindExpression(context, legacy, "attachLanguage", "attachLanguageExpression", false);
        GlueGenerator.bindExpression(context, legacy, "attachFileName", "attachFileNameExpression", false);
        assertThat(context).containsEntry("attachLanguageExpression", "\"en\"")
                           .containsEntry("attachFileNameExpression", "");
    }

    @Test
    void aPropertyKeyTermIsRenderedFromTheAssignmentsReading() {
        Map<String, Object> term = new LinkedHashMap<>();
        term.put("property", "Period");
        term.put("reading", Map.of("kind", "now", "shape", "month"));

        List<Map<String, Object>> terms = GlueGenerator.uniqueTerms(List.of(term));

        assertThat(terms.get(0)).containsEntry("expr", "java.time.YearMonth.now().toString()");
    }

    private static Map<String, Object> term(String property, boolean equal, String type, String value) {
        Map<String, Object> term = new LinkedHashMap<>();
        term.put("owner", "entity");
        term.put("property", property);
        term.put("equal", equal);
        term.put("type", type);
        term.put("value", value);
        term.put("numericKey", false);
        return term;
    }
}
