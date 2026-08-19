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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A to-one relation may be listed in a task form's {@code editable} - the shape that lets a person
 * PICK A RELATED RECORD inside a process (pick the driver, the account, the approver).
 *
 * <p>
 * Before this, {@code editable} took plain fields only and {@code setRelationField} took a literal
 * seed id baked in at authoring time, so a flow whose fallback was "a person chooses the record"
 * had to be built outside the process as an ordinary entity form plus a guarded {@code transitions}
 * button - two user actions where the model describes one.
 *
 * <p>
 * The picker's option list rides the {@code __<Fk>EntityUrl} process variables the trigger seeds
 * for its own trigger entity's relations, and the write-back targets that same entity, so the
 * remaining rules are about the shapes whose picker could only render empty or write a value the
 * model forbids. Each is asserted here by the message it produces.
 */
class EditableRelationIntentTest {

    private static final String YAML = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Driver
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: vehicle, type: string }
                relations:
                  - { name: driver, kind: manyToOne, to: Driver }
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
            processes:
              - name: Identify
                trigger: { onCreate: Fine }
                steps:
                  - { name: identify, kind: userTask, args: { assignee: officer, form: IdentifyDriver } }
                  - { name: done, kind: end }
            forms:
              - name: IdentifyDriver
                forEntity: Fine
                fields: [vehicle, driver]
                editable: [driver]
                actions: [identify]
            """;

    /** The reported case verbatim - the officer picks the driver on the task form. */
    @Test
    void aToOneRelationCanBeEditableOnATaskForm() {
        IntentParser.parse(YAML);
    }

    /**
     * The status is moved by the lifecycle graph - a setter step or a transitions button - and offering
     * it as a list would put every seeded status in front of the user, edges and all.
     */
    @Test
    void theEntityStatusRelationCannotBePicked() {
        String yaml = YAML.replace("fields: [vehicle, driver]", "fields: [vehicle, Status]")
                          .replace("editable: [driver]", "editable: [Status]");
        assertMessage(yaml, "is the function: EntityStatus relation");
    }

    /** Re-pointing the composition parent would file the record under another master mid-flow. */
    @Test
    void theCompositionParentCannotBePicked() {
        String yaml = YAML.replace("- { name: driver, kind: manyToOne, to: Driver }",
                "- { name: driver, kind: manyToOne, to: Driver, composition: true }");
        assertMessage(yaml, "is the composition parent");
    }

    /** A relation.field was never editable and still is not - editing it would not write back. */
    @Test
    void aRelationFieldIsStillRejected() {
        String yaml = YAML.replace("fields: [vehicle, driver]", "fields: [vehicle, driver.name]")
                          .replace("editable: [driver]", "editable: [driver.name]");
        assertMessage(yaml, "is a relation.field, which cannot be edited");
    }

    /** Neither a field nor a to-one relation - the old message, widened to name both. */
    @Test
    void anUnknownNameNamesBothShapes() {
        String yaml = YAML.replace("fields: [vehicle, driver]", "fields: [vehicle, drivr]")
                          .replace("editable: [driver]", "editable: [drivr]");
        assertMessage(yaml, "is not a field or to-one relation of [Fine]");
    }

    /**
     * Without a process there are no {@code __<Fk>EntityUrl} variables to load the options from - and
     * no writer to persist the choice either. Both come from the trigger, so one check covers both.
     */
    @Test
    void aRelationOnANonTaskFormIsRejected() {
        String yaml = YAML.replace("args: { assignee: officer, form: IdentifyDriver }", "args: { assignee: officer }");
        assertMessage(yaml, "can only be picked on a TASK form");
    }

    /**
     * The locators and the write-back both belong to the TRIGGER entity, so a task form bound to
     * anything else has neither. The form below binds Driver while the process triggers on Fine.
     */
    @Test
    void aTaskFormBoundToSomethingOtherThanTheTriggerEntityIsRejected() {
        assertMessage("""
                name: fines
                entities:
                  - name: Depot
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Driver
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: depot, kind: manyToOne, to: Depot }
                  - name: Fine
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                processes:
                  - name: Identify
                    trigger: { onCreate: Fine }
                    steps:
                      - { name: identify, kind: userTask, args: { assignee: officer, form: PickDepot } }
                      - { name: done, kind: end }
                forms:
                  - { name: PickDepot, forEntity: Driver, fields: [depot], editable: [depot], actions: [identify] }
                """, "is not the trigger entity of every process that uses it as a task form");
    }

    /**
     * A cross-model target's key belongs to its owner model, so this layer cannot say what value the
     * picker should submit.
     */
    @Test
    void aCrossModelRelationCannotBePicked() {
        assertMessage("""
                name: fines
                uses:
                  - { model: people, project: people }
                entities:
                  - name: Fine
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: driver, kind: manyToOne, to: Person, model: people }
                processes:
                  - name: Identify
                    trigger: { onCreate: Fine }
                    steps:
                      - { name: identify, kind: userTask, args: { assignee: officer, form: IdentifyDriver } }
                      - { name: done, kind: end }
                forms:
                  - { name: IdentifyDriver, forEntity: Fine, fields: [driver], editable: [driver], actions: [identify] }
                """, "keep it read-only and set it with a delegate");
    }

    private static void assertMessage(String yaml, String expected) {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(failure.getMessage()
                          .contains(expected),
                failure.getMessage());
    }
}
