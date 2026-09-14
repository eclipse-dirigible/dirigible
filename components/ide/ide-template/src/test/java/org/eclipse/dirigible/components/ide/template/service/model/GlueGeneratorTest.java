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
