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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.CheckIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.junit.jupiter.api.Test;

/**
 * An optional {@code uses:} entry (dirigible #7979): the model parses as written, must also stand
 * without the optional model, and {@link IntentParser#parseWithout} is that model.
 */
class OptionalUsesIntentTest {

    private static final String YAML = """
            name: expenses
            uses:
              - { model: projects, optional: true }
              - { model: tax-rates }
            entities:
              - name: Claim
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Project, kind: manyToOne, to: Project, model: projects }
              - name: ClaimItem
                order: [Id, Claim, Project, billable, amount]
                checks:
                  - { kind: compare, field: amount, op: gt, value: 0, message: "positive" }
                  - { kind: requiredWhen, field: Project, when: "billable == true", message: "needs a project" }
                  - { kind: forbidWhen, when: ["billable == true", "Project.billable == false"], message: "not billable" }
                  - { kind: forbidWhen, when: "TaxRate.rate == 0", message: "Project is a word in a message, not a path" }
                  - { kind: compare, field: amount, op: le, value: 1000, message: "at most 1000" }
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                  - { name: billable, type: boolean }
                relations:
                  - { name: Claim, kind: manyToOne, to: Claim, composition: true, required: true }
                  - { name: Project, kind: manyToOne, to: Project, model: projects }
                  - { name: TaxRate, kind: manyToOne, to: TaxRate, model: tax-rates }
            reports:
              - { name: ByProject, source: ClaimItem, dimensions: [Project, billable], measures: ["sum(amount)"] }
              - { name: ByClaimProject, source: ClaimItem, dimensions: [Claim.Project], measures: ["sum(amount)"] }
              - { name: ByBillable, source: ClaimItem, dimensions: [billable], measures: ["sum(amount)"] }
            rollups:
              - { name: projectSpend, entity: ClaimItem, via: Project, field: spent, op: sum, of: amount }
            """;

    @Test
    void anOptionalUsesParsesAsWritten() {
        IntentModel model = IntentParser.parse(YAML);
        assertTrue(model.getUses()
                        .get(0)
                        .isOptional());
        assertFalse(model.getUses()
                         .get(1)
                         .isOptional());
        assertEquals(List.of("Claim", "Project", "TaxRate"), relationNames(entity(model, "ClaimItem")));
        assertEquals(3, model.getReports()
                             .size());
    }

    @Test
    void withoutTheOptionalModelWhatDependsOnItIsLeftOut() {
        List<String> dropped = new ArrayList<>();
        IntentModel model = IntentParser.parseWithout(YAML, Set.of("projects"), dropped);

        assertEquals(List.of("tax-rates"), model.getUses()
                                                .stream()
                                                .map(u -> u.getModel())
                                                .toList());
        EntityIntent item = entity(model, "ClaimItem");
        assertEquals(List.of("Claim", "TaxRate"), relationNames(item));
        assertTrue(entity(model, "Claim").getRelations()
                                         .isEmpty());
        assertEquals(List.of("Id", "Claim", "billable", "amount"), item.getOrder());
        // The checks naming the relation are gone; a message merely mentioning the word is not a path.
        // The survivors keep the catalog key of their place.
        assertEquals(List.of("compare_0", "forbidWhen_3", "compare_4"), item.getChecks()
                                                                            .stream()
                                                                            .map(CheckIntent::getId)
                                                                            .toList());
        assertEquals(List.of("ByBillable"), model.getReports()
                                                 .stream()
                                                 .map(ReportIntent::getName)
                                                 .toList());
        assertEquals(
                List.of("relation [Claim.Project]", "relation [ClaimItem.Project]", "check [ClaimItem_requiredWhen_1]",
                        "check [ClaimItem_forbidWhen_2]", "report [ByProject]", "report [ByClaimProject]", "rollups [projectSpend]"),
                dropped);
        assertTrue(model.getRollups()
                        .isEmpty());
    }

    @Test
    void aRelationToAnOptionalModelCannotBeRequiredOrPartOfTheRecordsIdentity() {
        assertIssue(
                YAML.replace("to: Project, model: projects }\n  - name: ClaimItem",
                        "to: Project, model: projects, required: true }\n  - name: ClaimItem"),
                "entity [Claim] relation [Project] targets the optional uses [projects] and is required");
        assertIssue(YAML.replace("  - name: Claim\n    fields:", "  - name: Claim\n    unique: [{ fields: [Project] }]\n    fields:"),
                "entity [Claim] relation [Project] targets the optional uses [projects] and is part of the unique key [Project]");
        assertIssue(
                YAML.replace(
                        "      - { name: id, type: integer, primaryKey: true, generated: true }\n    relations:\n      - { name: Project",
                        "      - { name: id, type: integer, primaryKey: true, generated: true }\n"
                                + "      - { name: number, type: string, number: { series: Claim, per: Project } }\n"
                                + "    relations:\n      - { name: Project"),
                "entity [Claim] relation [Project] targets the optional uses [projects] and partitions the number series of [number]");
    }

    @Test
    void whatCannotBeLeftOutIsRefusedAtParse() {
        // A field filled from the optional relation's target is not one of the constructs left out: the
        // model would not stand without the projects model, so it is refused before any Generate.
        String yaml = YAML.replace("      - { name: billable, type: boolean }",
                "      - { name: billable, type: boolean }\n      - { name: budget, type: decimal, dependsOn: { relation: Project, valueFrom: budget } }");
        IntentValidationException e = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(e.getIssues()
                    .stream()
                    .anyMatch(issue -> issue.startsWith("without the optional uses [projects]: ") && issue.contains("budget")),
                e.getIssues()
                 .toString());
        // ... and the same field on the mandatory model's relation parses.
        IntentParser.parse(yaml.replace("relation: Project, valueFrom: budget", "relation: TaxRate, valueFrom: rate"));
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException e = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(e.getIssues()
                    .stream()
                    .anyMatch(issue -> issue.startsWith(expected)),
                "expected [" + expected + "] in " + e.getIssues());
    }

    private static EntityIntent entity(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .filter(e -> name.equals(e.getName()))
                    .findFirst()
                    .orElseThrow();
    }

    private static List<String> relationNames(EntityIntent entity) {
        return entity.getRelations()
                     .stream()
                     .map(RelationIntent::getName)
                     .toList();
    }
}
