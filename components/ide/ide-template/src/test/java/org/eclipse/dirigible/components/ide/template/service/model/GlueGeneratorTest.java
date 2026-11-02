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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateMetadataSource;

import org.junit.jupiter.api.Test;

/**
 * Covers the .glue backward-compatibility of the posting binding (dirigible #7234): the descriptor
 * keys a generated .glue carries verbatim are read under every spelling a released generator wrote
 * them in, so a project generated between two releases renders the handler its intent asked for,
 * not a silently degraded one, until it is re-generated.
 */
class GlueGeneratorTest {

    @Test
    void aReversalRefusalRendersItsConditionOverTheOriginalAndEscapesItsMessage() {
        // #7886: the neutral terms of a refusal read the loaded original, null-guarded, and the authored
        // message becomes the content of a Java string literal.
        Map<String, Object> term = new LinkedHashMap<>();
        term.put("owner", "source");
        term.put("property", "ReversedBy");
        term.put("equal", Boolean.FALSE);
        term.put("type", "null");
        term.put("numericKey", Boolean.FALSE);
        Map<String, Object> refusal = new LinkedHashMap<>();
        refusal.put("terms", List.of(term));
        refusal.put("message", "Entry \"7\" is already reversed");

        List<Map<String, Object>> rendered = GlueGenerator.refusals(List.of(refusal));

        assertThat(rendered).hasSize(1);
        assertThat(rendered.get(0)
                           .get("condition")).isEqualTo("((source == null ? null : source.ReversedBy) != null)");
        assertThat(rendered.get(0)
                           .get("message")).isEqualTo("Entry \\\"7\\\" is already reversed");
        assertThat(GlueGenerator.refusals(null)).isEmpty();
    }

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
     * A {@code set:} setter's members become the values of its one {@code updateProperties} map, in the
     * authored order, each the value its single-member setter would assign (#8042).
     */
    @Test
    void aSetMapsMembersRenderAsTheValuesOfOneWrite() {
        List<Map<String, Object>> assignments =
                List.of(member("SlotKey", "", Map.of("clear", "true")), member("Status", "3", Map.of("relation", "true")),
                        member("FinishedAt", "{now}", Map.of("now", "true", "nowExpression", "java.time.Instant.now()")),
                        member("ErrorMessage", "{error}", Map.of("errorMessage", "true")), member("Note", "the \"last\" one", Map.of()));

        List<Map<String, Object>> rendered = GlueGenerator.setterAssignments(assignments);

        assertThat(rendered).extracting(member -> member.get("field"))
                            .containsExactly("SlotKey", "Status", "FinishedAt", "ErrorMessage", "Note");
        assertThat(rendered).extracting(member -> member.get("expression"))
                            .containsExactly("null", "3", "java.time.Instant.now()",
                                    "errorMessage == null ? \"\" : errorMessage.toString()", "\"the \\\"last\\\" one\"");
    }

    private static Map<String, Object> member(String field, String value, Map<String, String> flags) {
        Map<String, Object> member = new LinkedHashMap<>();
        member.put("field", field);
        member.put("value", value);
        member.put("relation", "false");
        member.putAll(flags);
        return member;
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
     * A settlement re-sums an invoice's allocation rows to size what it may still take (#7559), and the
     * rows it sums are the ones the paid roll-up counts - its clauses rendered as the same chain the
     * roll-up recompute and the capacity guard append. A descriptor without them, including a .glue
     * generated before the key existed, sums every row.
     */
    @Test
    void aSettlementSumsTheInvoicesRowsThePaidRollupCounts() {
        Map<String, Object> settlement = new LinkedHashMap<>();
        settlement.put("invoiceRowsFilter", List.of(clause("ne", "Status", Map.of("kind", "number", "text", "2"))));
        Map<String, Object> context = new LinkedHashMap<>();

        GlueGenerator.bindSettlement(settlement, context, Map.of("javaGenFolderName", "shop"));
        assertThat(context).containsEntry("invoiceRowsFilter", ".ne(\"Status\", 2)");

        GlueGenerator.bindSettlement(new LinkedHashMap<>(), context, Map.of("javaGenFolderName", "shop"));
        assertThat(context).containsEntry("invoiceRowsFilter", "");
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
     * ...and a GUARDED row's ternaries are not among them (#7649): a classifier the row needs only
     * where it books must not stop a document the row's own {@code when:} excludes. They ride on the
     * row instead, for a null check inside that guard.
     */
    @Test
    void aGuardedRowsRuleCaseCellsGuardThatRowAlone() {
        Map<String, Object> ruleCase = new LinkedHashMap<>();
        ruleCase.put("targetProp", "Account");
        ruleCase.put("reading", Map.of("kind", "ruleCase", "by", "Method", "owner", "source", "cases",
                List.of(Map.of("value", "1", "column", "CashAccount")), "otherwise", "BankAccount"));
        Map<String, Object> guarded = new LinkedHashMap<>();
        guarded.put("guardReading", Map.of("kind", "calcCompare", "owner", "source", "property", "Vat", "equal", false, "text", "0"));
        guarded.put("assigns", List.of(ruleCase));

        List<Map<String, Object>> rows = GlueGenerator.rows(List.of(guarded));

        assertThat(GlueGenerator.conditionalRuleGuards(rows)).isEmpty();
        assertThat(rows.get(0)
                       .get("ruleCaseGuards")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                                              .hasSize(1);
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

    /**
     * A collection imports its child's repository and every element lookup's target repository, so both
     * perspectives are sanitized into package segments, and the Criteria import is wanted even when
     * only an element looks something up.
     */
    @Test
    void anArrivalCollectionIsBoundWithItsPackageSegmentsResolved() throws IOException {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("local", "lookupLinesProduct");
        product.put("targetEntity", "Product");
        product.put("targetPerspective", "Product");
        Map<String, Object> lines = new LinkedHashMap<>();
        lines.put("local", "collectionLines");
        lines.put("childEntity", "OrderLine");
        lines.put("childPerspective", "PurchaseOrder");
        lines.put("lookups", List.of(product));
        Map<String, Object> arrival = arrival();
        arrival.put("hasCollections", Boolean.TRUE);
        arrival.put("collections", List.of(lines));

        for (String collection : List.of("inbound", "inboundMessages", "inboundFiles")) {
            Map<String, Object> context = boundArrival(collection, arrival);

            assertThat(context).as(collection)
                               .containsEntry("hasCollections", Boolean.TRUE)
                               .containsEntry("hasLookups", Boolean.TRUE);
            Map<?, ?> bound = (Map<?, ?>) ((List<?>) context.get("collections")).get(0);
            assertThat(bound.get("javaChildPerspective")).isEqualTo("purchaseorder");
            assertThat(bound.get("childEntity")).isEqualTo("OrderLine");
            Map<?, ?> lookup = (Map<?, ?>) ((List<?>) bound.get("lookups")).get(0);
            assertThat(lookup.get("javaTargetPerspective")).isEqualTo("product");
        }
        assertThat(lines).as("the descriptor itself is left untouched")
                         .doesNotContainKey("javaChildPerspective");
    }

    /**
     * A declared export (#7855) binds its format factory, its field layouts and its role gate as Java.
     */
    @Test
    @SuppressWarnings("unchecked")
    void anExportIsBoundAsTheJavaItsFileAndLayoutsAreBuiltWith() throws IOException {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", "Tax\"Base");
        field.put("width", "12");
        field.put("alignRight", Boolean.TRUE);
        field.put("pad", "'");
        field.put("truncate", Boolean.FALSE);
        field.put("scale", "2");
        field.put("decimalSeparator", ",");
        field.put("attribute", Boolean.FALSE);
        field.put("reading", Map.of("kind", "column", "owner", "row", "alias", "Sum Tax Base"));
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("constant", "LINE_0");
        line.put("element", "line");
        line.put("fields", List.of(field));
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("name", "VatLedger");
        export.put("className", "VatLedger");
        export.put("format", "delimited");
        export.put("encoding", "windows-1251");
        export.put("lineEnding", "\r\n");
        export.put("delimiter", ";");
        export.put("quote", "\"");
        export.put("quoteAll", Boolean.TRUE);
        export.put("ledger", "ExportRun");
        export.put("ledgerPerspective", "ExportRun");
        export.put("sourceKind", "report");
        export.put("report", "PurchasesBySupplier");
        export.put("lineRecords", List.of(line));
        export.put("fileNameReading", Map.of("kind", "string", "text", "vat.csv"));
        export.put("roles", "Accountant,Auditor");

        Map<String, Object> context = boundArrival("exports", export);

        assertThat(context.get("fileFactory")).isEqualTo("ExportFile.delimited(\"windows-1251\", \"\\r\\n\", \";\", \"\\\"\", true)");
        assertThat(context).containsEntry("reportSource", Boolean.TRUE)
                           .containsEntry("reportGenFolder", "purchasesbysupplier")
                           .containsEntry("javaLedgerPerspective", "exportrun")
                           .containsEntry("fileNameExpression", "\"vat.csv\"")
                           .containsEntry("rolesAnnotation", "@Roles({\"Accountant\", \"Auditor\"})");
        Map<String, Object> bound =
                (Map<String, Object>) ((List<Object>) ((Map<String, Object>) ((List<Object>) context.get("lineRecords")).get(0)).get(
                        "fields")).get(0);
        assertThat(bound.get("specExpression")).isEqualTo(
                "ExportField.named(\"Tax\\\"Base\").width(12).alignRight().pad('\\'').scale(2).decimalSeparator(\",\")");
        assertThat(bound.get("valueExpression")).isEqualTo("row.get(\"Sum Tax Base\")");
    }

    /**
     * A field's text normalisation (#7925) is bound as its chain, in the order the field applies it.
     */
    @Test
    @SuppressWarnings("unchecked")
    void anExportFieldsNormalisationIsBoundInItsOrder() throws IOException {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", "Name");
        field.put("replace", List.of(Map.of("target", "\u2116", "replacement", ""), Map.of("target", "/", "replacement", " ")));
        field.put("case", "upper");
        field.put("allowed", "A-Z0-9 \\-");
        field.put("onDisallowed", "strip");
        field.put("collapseSpaces", Boolean.TRUE);
        field.put("failOnLoss", Boolean.TRUE);
        field.put("reading", Map.of("kind", "read", "owner", "row", "property", "Name"));
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("constant", "LINE_0");
        line.put("element", "line");
        line.put("fields", List.of(field));
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("name", "Declaration");
        export.put("className", "Declaration");
        export.put("format", "delimited");
        export.put("ledger", "DeclarationRun");
        export.put("ledgerPerspective", "DeclarationRun");
        export.put("sourceKind", "entity");
        export.put("entity", "Employee");
        export.put("perspective", "Employee");
        export.put("normalises", Boolean.TRUE);
        export.put("lineRecords", List.of(line));
        export.put("fileNameReading", Map.of("kind", "string", "text", "d.csv"));

        Map<String, Object> context = boundArrival("exports", export);

        assertThat(context).containsEntry("normalises", Boolean.TRUE);
        Map<String, Object> bound =
                (Map<String, Object>) ((List<Object>) ((Map<String, Object>) ((List<Object>) context.get("lineRecords")).get(0)).get(
                        "fields")).get(0);
        assertThat(bound.get("specExpression")).isEqualTo("ExportField.named(\"Name\").replace(\"\u2116\", \"\").replace(\"/\", \" \")"
                + ".upperCase().allowed(\"A-Z0-9 \\\\-\").stripDisallowed().collapseSpaces().failOnLoss()");
    }

    /** A descriptor without collections - or one from before them - binds them empty. */
    @Test
    void anArrivalWithoutCollectionsBindsThemEmpty() throws IOException {
        Map<String, Object> context = boundArrival("inboundMessages", arrival());

        assertThat(context).containsEntry("hasCollections", Boolean.FALSE)
                           .containsEntry("hasLookups", Boolean.FALSE);
        assertThat((List<?>) context.get("collections")).isEmpty();
    }

    private static Map<String, Object> arrival() {
        Map<String, Object> arrival = new LinkedHashMap<>();
        arrival.put("name", "orders");
        arrival.put("className", "Orders");
        arrival.put("entity", "PurchaseOrder");
        arrival.put("perspective", "PurchaseOrder");
        arrival.put("hasEnvelope", Boolean.TRUE);
        arrival.put("hasMap", Boolean.TRUE);
        arrival.put("mapFields", List.of());
        arrival.put("lookups", List.of());
        return arrival;
    }

    /**
     * A file import (#7856) appends to its entity's {@code
     *
    <TABLE>
     * _IMPORTS} ledger, which the schema template names with the deployment's table prefix - so the
     * import must carry the same prefix, with the separating underscore a hand-authored one may lack,
     * or the two name different tables.
     */
    @Test
    void anImportsLedgerTableCarriesTheDeploymentsTablePrefix() throws IOException {
        Map<String, Object> declared = new LinkedHashMap<>();
        declared.put("name", "bankCsv");
        declared.put("className", "BankCsv");
        declared.put("entity", "StatementLine");
        declared.put("perspective", "Bank Statements");
        declared.put("table", "BANK_STATEMENT_LINE");

        Map<String, Object> prefixed = bound("imports", declared, Map.of("javaGenFolderName", "bank", "tablePrefix", "ACME"));
        assertThat(prefixed.get("table")).isEqualTo("ACME_BANK_STATEMENT_LINE");
        assertThat(prefixed.get("javaPerspective")).isEqualTo("bank_statements");

        Map<String, Object> unprefixed = bound("imports", declared, Map.of("javaGenFolderName", "bank"));
        assertThat(unprefixed.get("table")).isEqualTo("BANK_STATEMENT_LINE");
    }

    /**
     * Two roll-ups over the same child and relation share one handler per event, but not a filter
     * (#8330): the first filter reads the template's {@code rows}, a different one reads rows of its
     * own, and each aggregate counts the rows ITS filter selects.
     */
    @Test
    void coalescedRollupsWithDifferentFiltersEachReadTheirOwnRows() throws IOException {
        Map<String, Object> credited =
                creditNoteRollup("Credited", "sum", List.of(clause("ne", "Status", Map.of("kind", "number", "text", "1"))));
        credited.put("sumField", "Total");
        Map<String, Object> open =
                creditNoteRollup("OpenCreditNotes", "count", List.of(clause("ne", "Status", Map.of("kind", "number", "text", "3"))));

        Map<String, Object> context = boundRollups(List.of(credited, open));

        assertThat(context).containsEntry("criteriaExpression",
                "Criteria.create().eq(\"SalesInvoice\", entity.SalesInvoice).ne(\"Status\", 1)");
        String block = (String) context.get("aggregateBlock");
        assertThat(block).contains("for (var row : rows) {")
                         .contains("java.util.List<CreditNoteEntity> rows2 = new CreditNoteRepository().findAll("
                                 + "Criteria.create().eq(\"SalesInvoice\", entity.SalesInvoice).ne(\"Status\", 3));")
                         .contains("int count = rows2.size();");
        assertThat(block.indexOf("rows2 =")).isGreaterThan(block.indexOf("parent.Credited = sum;"));
    }

    /**
     * Roll-ups sharing a filter - or declaring none - still read the child rows exactly once.
     */
    @Test
    void coalescedRollupsWithTheSameFilterShareOneQuery() throws IOException {
        List<Map<String, Object>> filter = List.of(clause("ne", "Status", Map.of("kind", "number", "text", "1")));
        Map<String, Object> credited = creditNoteRollup("Credited", "sum", filter);
        credited.put("sumField", "Total");

        String block = (String) boundRollups(List.of(credited, creditNoteRollup("CreditNotes", "count", filter))).get("aggregateBlock");

        assertThat(block).doesNotContain("findAll")
                         .contains("int count = rows.size();");
    }

    private static Map<String, Object> creditNoteRollup(String countField, String op, List<Map<String, Object>> filter) {
        Map<String, Object> rollup = new LinkedHashMap<>();
        rollup.put("className", "CreditNoteSalesInvoiceRollupOnTransition");
        rollup.put("childEntity", "CreditNote");
        rollup.put("childPerspective", "CreditNote");
        rollup.put("parentEntity", "SalesInvoice");
        rollup.put("parentPerspective", "SalesInvoice");
        rollup.put("fkProperty", "SalesInvoice");
        rollup.put("topicSuffix", "-transitioned");
        rollup.put("op", op);
        rollup.put("countField", countField);
        rollup.put("filter", filter);
        return rollup;
    }

    private static Map<String, Object> boundRollups(List<Map<String, Object>> rollups) throws IOException {
        List<Map<String, Object>> contexts = new ArrayList<>();
        ModelTemplateRenderer capturing = new ModelTemplateRenderer(null, null) {
            @Override
            String render(GenerationTemplateMetadataSource source, String content, Map<String, Object> parameters) {
                contexts.add(parameters);
                return "";
            }

            @Override
            String renderPath(String location, String rename, Map<String, Object> parameters) {
                return rename;
            }
        };
        GenerationTemplateMetadataSource source = new GenerationTemplateMetadataSource();
        source.setLocation("/events/Rollup.java.template");
        source.setRename("gen/events/Rollup.java");
        new GlueGenerator(capturing).generate("rollups", source, "", Map.of("rollups", rollups),
                new LinkedHashMap<>(Map.of("javaGenFolderName", "sales", "projectName", "sales")));
        assertThat(contexts).as("roll-ups sharing a child, a relation and an event share one handler")
                            .hasSize(1);
        return contexts.get(0);
    }

    private static Map<String, Object> boundArrival(String collection, Map<String, Object> arrival) throws IOException {
        return bound(collection, arrival, Map.of("javaGenFolderName", "shop"));
    }

    private static Map<String, Object> bound(String collection, Map<String, Object> arrival, Map<String, Object> parameters)
            throws IOException {
        List<Map<String, Object>> contexts = new ArrayList<>();
        ModelTemplateRenderer capturing = new ModelTemplateRenderer(null, null) {
            @Override
            String render(GenerationTemplateMetadataSource source, String content, Map<String, Object> parameters) {
                contexts.add(parameters);
                return "";
            }

            @Override
            String renderPath(String location, String rename, Map<String, Object> parameters) {
                return rename;
            }
        };
        GenerationTemplateMetadataSource source = new GenerationTemplateMetadataSource();
        source.setLocation("/events/" + collection + ".java.template");
        source.setRename("gen/events/" + collection + ".java");
        new GlueGenerator(capturing).generate(collection, source, "", Map.of(collection, List.of(arrival)),
                new LinkedHashMap<>(parameters));
        assertThat(contexts).hasSize(1);
        return contexts.get(0);
    }
}
