/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.LifecycleStages;
import org.junit.jupiter.api.Test;

/**
 * The lifecycle stage classification of a status nomenclature ({@code stage:} on a seed row) and
 * the report {@code scope:} that resolves through it - dirigible #6645.
 */
class LifecycleStageIntentTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            reports:
              - name: RevenueByMonth
                source: Invoice
                scope: live
                dimensions: ["month(issuedOn)"]
                measures: ["sum(total)"]
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT, stage: draft }
                  - { id: 3, name: ISSUED, stage: live }
                  - { id: 7, name: PAID, stage: live }
                  - { id: 8, name: CANCELLED, stage: cancelled }
                  - { id: 9, name: VOIDED, stage: void }
            """;

    @Test
    void theShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    @Test
    void stagesResolveToTheSeedIdsThatCarryThem() {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, List<Integer>> stages = LifecycleStages.stagesOf(model, "InvoiceStatus");
        assertEquals(List.of(3, 7), stages.get(LifecycleStages.LIVE), "both live statuses should be classified, in seed order");
        assertEquals(List.of(1), stages.get(LifecycleStages.DRAFT));
        assertEquals(List.of(9), stages.get(LifecycleStages.VOID));
    }

    @Test
    void aStageOutsideTheVocabularyIsRejected() {
        assertIssue(YAML.replace("stage: cancelled", "stage: retired"), "declares stage [retired]");
    }

    @Test
    void aStageWithoutAnIdIsRejected() {
        assertIssue(YAML.replace("- { id: 9, name: VOIDED, stage: void }", "- { name: VOIDED, stage: void }"),
                "declares a stage but no [id]");
    }

    /**
     * The marker cannot double as data: a nomenclature that owns a {@code stage} column would make the
     * row key mean two things at once.
     */
    @Test
    void aStagePropertyOnTheNomenclatureCollidesWithTheMarker() {
        assertIssue(
                YAML.replace("      - { name: name, type: string }\n",
                        "      - { name: name, type: string }\n      - { name: stage, type: string }\n"),
                "declares its own `stage` property");
    }

    @Test
    void anUnknownScopeIsRejected() {
        assertIssue(YAML.replace("scope: live", "scope: alive"), "is unknown - expected `all`");
    }

    @Test
    void scopeAllIsTheExplicitOptOut() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("scope: live", "scope: all")));
    }

    @Test
    void aScopeOverASourceWithoutALifecycleIsRejected() {
        String yaml = YAML.replace("      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }\n", "");
        assertIssue(yaml, "to declare a `function: EntityStatus` relation");
    }

    @Test
    void aScopeOverAnUnclassifiedNomenclatureIsRejected() {
        assertIssue(YAML.replace(", stage: live", "")
                        .replace(", stage: draft", "")
                        .replace(", stage: cancelled", "")
                        .replace(", stage: void", ""),
                "to declare `stage:`");
    }

    @Test
    void aScopeNoSeedRowCarriesIsRejected() {
        assertIssue(YAML.replace("scope: live", "scope: cancelled")
                        .replace("- { id: 8, name: CANCELLED, stage: cancelled }", "- { id: 8, name: CANCELLED }"),
                "matches no seed row");
    }

    /**
     * A cross-model nomenclature is seeded in its owner model, which the parser cannot read - so a
     * stage scope must fail loudly rather than emit a query missing its predicate.
     */
    @Test
    void aScopeOverACrossModelNomenclatureIsRejected() {
        String yaml = """
                name: sales
                uses:
                  - { model: nomenclatures }
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, model: nomenclatures, function: EntityStatus }
                reports:
                  - name: Revenue
                    source: Invoice
                    scope: live
                    measures: ["sum(total)"]
                """;
        assertIssue(yaml, "belongs to model [nomenclatures]");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + thrown.getIssues());
    }
}
