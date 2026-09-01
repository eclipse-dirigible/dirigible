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

import org.junit.jupiter.api.Test;

/**
 * A key the intent does not declare is a validation ERROR, never a silent drop - dirigible #6541.
 *
 * <p>
 * Two halves of one failure mode. A key on a typed node (an entity, a field, a relation, a report,
 * ...) is dropped by the Gson mapping, which ignores unknown properties: the artifacts look
 * completely correct and only the promise the author wrote is missing. A key on a seed ROW is
 * dropped by the CSV generator, which emits a column per declared field plus the referenced to-one
 * relations: the column disappears, and when it was a NOT NULL FK the import then skips every row.
 * Both used to pass through the whole pipeline green.
 */
class UnknownKeyIntentTest {

    private static final String YAML = """
            name: contributions
            entities:
              - name: ContributionScheme
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
              - name: Rate
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: percent, type: decimal }
                relations:
                  - { name: ContributionScheme, kind: manyToOne, to: ContributionScheme, required: true }
                  - { name: revisions, kind: oneToMany, to: Rate }
            seeds:
              - name: rates
                entity: Rate
                rows:
                  - { id: 1, percent: 13.78, ContributionScheme: 1 }
                  - { id: 2, percent: 4.80, ContributionScheme: 2 }
            """;

    @Test
    void theShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    /** The case that opened the issue: a seed row key mis-cased against the relation it means. */
    @Test
    void aMisCasedSeedRowKeyIsRejectedAndNamesTheRelationItMeant() {
        String issue = assertIssue(YAML.replace("ContributionScheme: 1", "contributionScheme: 1"), "row references [contributionScheme]");
        assertTrue(issue.contains("[Rate]"), "the message must name the entity: " + issue);
        assertTrue(issue.contains("did you mean [ContributionScheme]?"), "the message must name the nearest declared name: " + issue);
        assertTrue(issue.contains("case-sensitive"), "a pure case slip must say so: " + issue);
    }

    /** A collection relation has no FK column, so a row keyed by it contributes nothing. */
    @Test
    void aSeedRowKeyedByACollectionRelationIsRejected() {
        assertIssue(YAML.replace("percent: 13.78", "revisions: 2"), "row references [revisions]");
    }

    @Test
    void aSeedRowKeyMatchingNothingIsRejected() {
        assertIssue(YAML.replace("percent: 13.78", "percentage: 13.78"), "row references [percentage]");
    }

    /** The lifecycle stage marker is metadata about the row, not a column - it stays accepted. */
    @Test
    void theStageMarkerIsNotAnUnknownSeedRowKey() {
        String yaml = """
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
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                seeds:
                  - name: invoice-statuses
                    entity: InvoiceStatus
                    rows:
                      - { id: 1, name: DRAFT, stage: draft }
                """;
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    /** The other half: a plausible-looking key on a typed node, which used to do nothing at all. */
    @Test
    void anUnknownRelationKeyIsRejectedAndLocated() {
        String issue = assertIssue(
                YAML.replace("to: ContributionScheme, required: true", "to: ContributionScheme, calculatedActionOnCreat: RateAction"),
                "unknown key [calculatedActionOnCreat]");
        assertTrue(issue.contains("entities[Rate].relations[ContributionScheme]"), "the message must locate the key: " + issue);
        assertTrue(issue.contains("did you mean [calculatedActionOnCreate]?"), "the message must suggest the declared name: " + issue);
    }

    @Test
    void anUnknownFieldKeyIsRejected() {
        assertIssue(YAML.replace("name: percent, type: decimal", "name: percent, type: decimal, lenght: 10"), "unknown key [lenght]");
    }

    /** Case matters on a declared key too - the typed mapping is case-sensitive. */
    @Test
    void aMisCasedFieldKeyIsRejected() {
        String issue = assertIssue(YAML.replace("name: name, type: string, required: true", "name: name, type: string, Required: true"),
                "unknown key [Required]");
        assertTrue(issue.contains("case-sensitive"), "a pure case slip must say so: " + issue);
    }

    @Test
    void anUnknownRootKeyIsRejected() {
        String issue = assertIssue(YAML.replace("entities:", "entites:"), "unknown key [entites]");
        assertTrue(issue.contains("at the intent root"), "a root-level key must be located as such: " + issue);
    }

    /** A nested typed block ({@code number:}, {@code dependsOn:}, {@code widget:}) is walked too. */
    @Test
    void anUnknownKeyInsideANestedBlockIsRejected() {
        String yaml = YAML.replace("  - { name: percent, type: decimal }",
                "  - { name: code, type: string, number: { series: Rate, stampOn: create, partition: Company } }");
        String issue = assertIssue(yaml, "unknown key [partition]");
        assertTrue(issue.contains("entities[Rate].fields[code].number"), "the message must locate the key: " + issue);
    }

    /**
     * Author-keyed maps stay opaque: a step's {@code args}, a {@code map:} projection and a relation's
     * {@code where:} carry names from the model being described, not from the intent schema.
     */
    @Test
    void authorKeyedMapsAreNotWalked() {
        String yaml = """
                name: orders
                entities:
                  - name: Country
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: region, type: integer }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: status, type: string }
                    relations:
                      - { name: Country, kind: manyToOne, to: Country, where: { region: 2 } }
                processes:
                  - name: OrderApproval
                    trigger: { onCreate: Order }
                    steps:
                      - { name: review, kind: userTask, args: { assignee: manager } }
                      - { name: activate, kind: serviceTask, args: { setField: status, value: ACTIVE } }
                """;
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    /** Unknown keys join the structural issues, so one parse reports everything to fix. */
    @Test
    void unknownKeysAreReportedTogetherWithTheStructuralIssues() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(YAML.replace("name: contributions", "name: contributions\nversionn: 2")
                                             .replace("entity: Rate", "entity: Rat")));
        assertEquals(2, ex.getIssues()
                          .size(),
                "both problems should be reported in one pass: " + ex.getIssues());
    }

    /**
     * A base with a create-from, for the misplaced-key cases below. The map-shaped features are where a
     * key lands at the wrong level, because their nesting is the only place in the DSL where "beside"
     * and "inside" are both plausible readings of the same requirement.
     */
    private static final String GENERATES_YAML = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: violationAt, type: timestamp }
                relations:
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
              - name: FineLog
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: event, type: string }
                relations:
                  - { name: fine, kind: manyToOne, to: Fine }
            generates:
              - name: log-identified
                from: Fine
                to: FineLog
                event: { onTransition: Fine, when: "Status == IDENTIFIED", mode: append }
                map:
                  fine: id
                defaults:
                  event: "IDENTIFIED"
            seeds:
              - name: fine-statuses
                entity: FineStatus
                rows:
                  - { id: 1, name: NEW, stage: draft }
                  - { id: 2, name: IDENTIFIED, stage: live }
            """;

    @Test
    void theGeneratesShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(GENERATES_YAML));
    }

    /**
     * The slip that cost a real intent its append cardinality: `mode` written beside `event:` rather
     * than inside it. A bare "unknown key" reads as "the platform has no such thing" - and the fix an
     * author (or an assistant) then reaches for is to drop the key and accept the wrong cardinality, so
     * the message has to name the place the key IS legal.
     */
    @Test
    void aKeyBelongingOneLevelDownNamesTheMapItBelongsIn() {
        String issue =
                assertIssue(GENERATES_YAML.replace("    to: FineLog\n", "    to: FineLog\n    mode: append\n"), "unknown key [mode]");
        assertTrue(issue.contains("belongs inside `event:`"), "should name the event map: " + issue);
        assertTrue(issue.contains("generates"), "should name the feature: " + issue);
    }

    /** The same slip in the other direction: a key of the create-from written inside its event. */
    @Test
    void aKeyBelongingOneLevelUpSaysToMoveItOut() {
        String issue = assertIssue(GENERATES_YAML.replace("event: { onTransition: Fine,", "event: { to: FineLog, onTransition: Fine,"),
                "unknown key [to]");
        assertTrue(issue.contains("declared on the generates itself"), "should name the owning feature: " + issue);
        assertTrue(issue.contains("not inside `event:`"), "should name the map it was written in: " + issue);
    }

    /** Two levels down: the step binding's own keys, written flat on the event. */
    @Test
    void aKeyBelongingInANestedBindingRendersTheWholeShape() {
        String issue = assertIssue(GENERATES_YAML.replace("event: { onTransition: Fine, when: \"Status == IDENTIFIED\", mode: append }",
                "event: { onStepCompleted: { process: P, step: s }, process: P, mode: append }"), "unknown key [process]");
        assertTrue(issue.contains("`event: { onStepCompleted: ... }`"), "should render the nested shape: " + issue);
    }

    /** A key legal nowhere on the feature still gets the fuzzy near-miss, not a placement claim. */
    @Test
    void aKeyLegalNowhereKeepsTheDidYouMeanFallback() {
        String issue = assertIssue(GENERATES_YAML.replace("    to: FineLog\n", "    too: FineLog\n"), "unknown key [too]");
        assertTrue(issue.contains("did you mean [to]"), "should still guess the near-miss: " + issue);
        assertTrue(!issue.contains("belongs inside"), "should not claim a placement: " + issue);
    }

    private static String assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        String issue = ex.getIssues()
                         .stream()
                         .filter(i -> i.contains(expected))
                         .findFirst()
                         .orElse(null);
        assertTrue(issue != null, "expected an issue containing [" + expected + "] but got " + ex.getIssues());
        return issue;
    }
}
