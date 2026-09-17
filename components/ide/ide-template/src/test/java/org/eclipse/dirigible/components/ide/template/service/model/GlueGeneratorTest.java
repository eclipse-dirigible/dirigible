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
}
