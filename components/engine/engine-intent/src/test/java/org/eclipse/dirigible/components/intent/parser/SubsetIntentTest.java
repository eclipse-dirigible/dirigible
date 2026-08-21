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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@code kind: subset} - a set-valued reference to a small lookup entity, held as ONE
 * value (the selected keys, comma-separated), never as rows. Everything that describes a to-one FK
 * or a row set is rejected rather than carried nowhere.
 */
class SubsetIntentTest {

    private static final String SCHEDULES = """
            name: schedules
            entities:
              - name: PayerType
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
              - name: Schedule
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string, required: true }
                relations:
                  - { name: payerTypes, kind: subset, to: PayerType, required: true }
            seeds:
              - name: payerTypes
                entity: PayerType
                rows:
                  - { id: 1, name: Health fund }
                  - { id: 2, name: Paid visit }
                  - { id: 3, name: Corporate client }
            """;

    @Test
    void aMinimalSubsetParsesAndSurvivesUnexpanded() {
        IntentModel model = IntentParser.parse(SCHEDULES);

        RelationIntent relation = entity(model, "Schedule").getRelations()
                                                           .get(0);
        assertEquals("subset", relation.getKind(), "no expansion consumes a subset relation");
        assertEquals("PayerType", relation.getTo());
        assertTrue(relation.isRequired());
        assertEquals(2, model.getEntities()
                             .size(),
                "no link entity is materialized - the value lives on the declaring entity");
    }

    @Test
    void theFullAttributeSetIsAccepted() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: active, type: boolean }
                  - name: Schedule
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - name: payerTypes
                        kind: subset
                        to: PayerType
                        required: true
                        where: { active: true }
                        major: false
                        size: 6
                        description: Payment methods this schedule's hours support
                """;
        IntentModel model = IntentParser.parse(yaml);

        RelationIntent relation = entity(model, "Schedule").getRelations()
                                                           .get(0);
        assertEquals(Integer.valueOf(6), relation.getSize());
        assertEquals(Boolean.FALSE, relation.isMajor());
        assertNotNull(relation.getWhere());
    }

    @Test
    void anUnknownTargetIsRejected() {
        String yaml = """
                name: schedules
                entities:
                  - name: Schedule
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerKind }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("entity [Schedule] relation [payerTypes] points to unknown entity [PayerKind]"),
                ex.getMessage());
    }

    @Test
    void aCrossModelTargetIsRejectedNamingTheLimit() {
        String yaml = """
                name: schedules
                uses:
                  - { model: payers }
                entities:
                  - name: Schedule
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerType, model: payers }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("entity [Schedule] relation [payerTypes] is a subset relation so it cannot be cross-model (model: payers)"),
                ex.getMessage());
        assertTrue(ex.getMessage()
                     .contains("manyToMany supports a cross-model target"),
                ex.getMessage());
    }

    @Test
    void theToOneAndRowSetAttributesAreRejectedInOneGroupedMessage() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Schedule
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - name: payerTypes
                        kind: subset
                        to: PayerType
                        composition: true
                        init: "1"
                        through: SchedulePayerType
                        show: [name]
                        leafOnly: true
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        String message = ex.getMessage();
        assertTrue(message.contains("entity [Schedule] relation [payerTypes] is a subset relation so it cannot declare"), message);
        assertTrue(message.contains("composition"), message);
        assertTrue(message.contains("init"), message);
        assertTrue(message.contains("through"), message);
        assertTrue(message.contains("show"), message);
        assertTrue(message.contains("leafOnly"), message);
        assertTrue(message.contains("use manyToMany"), message);
    }

    @Test
    void functionDependsOnPersonalAndCalculatedAreRejectedToo() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Schedule
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - name: owner
                        kind: manyToOne
                        to: PayerType
                      - name: payerTypes
                        kind: subset
                        to: PayerType
                        function: EntityStatus
                        dependsOn: { relation: owner }
                        personal: true
                        partner: true
                        calculatedActionOnCreate: SomeAction
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        String message = ex.getMessage();
        assertTrue(message.contains("function"), message);
        assertTrue(message.contains("dependsOn"), message);
        assertTrue(message.contains("personal"), message);
        assertTrue(message.contains("partner"), message);
        assertTrue(message.contains("calculatedAction"), message);
    }

    @Test
    void aWherePropertyMustExistOnTheTarget() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Schedule
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerType, where: { kind: 1 } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("where [kind] is not a field or to-one relation of [PayerType]"),
                ex.getMessage());
    }

    @Test
    void aSeedRowSetsTheValueByTheRelationsAuthoredName() {
        String yaml = SCHEDULES + """
                - name: schedules
                  entity: Schedule
                  rows:
                    - { id: 1, title: Morning, payerTypes: "1,3" }
                """.indent(2);
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("1,3", model.getSeeds()
                                 .get(1)
                                 .getRows()
                                 .get(0)
                                 .get("payerTypes"));
    }

    @Test
    void aSeedValueOffTheNormativeShapeIsRejected() {
        String yaml = SCHEDULES + """
                - name: schedules
                  entity: Schedule
                  rows:
                    - { id: 1, title: Morning, payerTypes: "Health fund, Corporate client" }
                """.indent(2);
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("row sets the subset relation [payerTypes]"),
                ex.getMessage());
        assertTrue(ex.getMessage()
                     .contains("Selecting by seeded name is not supported yet"),
                ex.getMessage());
    }

    @Test
    void theControlOrderMayListIt() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Schedule
                    order: [id, payerTypes, title]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: title, type: string }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerType }
                """;
        assertNotNull(IntentParser.parse(yaml), "order: may interleave a subset relation like any other property");
    }

    @Test
    void aReportDimensionOverItIsRejected() {
        String yaml = SCHEDULES + """
                reports:
                  - name: SchedulesByPayerType
                    source: Schedule
                    dimensions: [payerTypes]
                    measures: ["count(*)"]
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("report [SchedulesByPayerType] dimension [payerTypes] is a subset relation"),
                ex.getMessage());
        assertTrue(ex.getMessage()
                     .contains("manyToMany"),
                ex.getMessage());
    }

    @Test
    void aReportFilterOverItIsRejected() {
        String yaml = SCHEDULES + """
                reports:
                  - name: CorporateSchedules
                    source: Schedule
                    dimensions: [title]
                    measures: ["count(*)"]
                    filter: "payerTypes == 3"
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("report [CorporateSchedules] filter references the subset relation [payerTypes]"),
                ex.getMessage());
    }

    @Test
    void aUniqueKeyOverItIsRejected() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Schedule
                    unique:
                      - fields: [title, payerTypes]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: title, type: string }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerType }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("names the subset relation [payerTypes]"),
                ex.getMessage());
        assertTrue(ex.getMessage()
                     .contains("not an identity"),
                ex.getMessage());
    }

    @Test
    void aLabelTokenOverItIsRejected() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Schedule
                    label: "{title} - {payerTypes}"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: title, type: string }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerType }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("payerTypes"),
                ex.getMessage());
    }

    private static EntityIntent entity(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .filter(entity -> name.equals(entity.getName()))
                    .findFirst()
                    .orElseThrow();
    }
}
