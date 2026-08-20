/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.WriterSupport;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The two emitted halves of an {@code editable} to-one relation on a task form: the picker control
 * and the write-back field.
 *
 * <p>
 * The control names the PROCESS VARIABLES that locate its option list rather than a controller URL
 * - the intent layer must stay ignorant of the paths a template publishes, and the trigger listener
 * already seeds {@code __<Fk>EntityUrl} / {@code __<Fk>EntityLabel} for every to-one relation of
 * its trigger entity. The write-back needs no new coercion: the FK column holds the target's
 * integer key, which is the branch the Writer template has always had.
 */
class TaskFormRelationPickerTest {

    private static final String YAML = """
            name: fines
            entities:
              - name: Driver
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: vehicle, type: string }
                  - { name: note, type: string }
                relations:
                  - { name: driver, kind: manyToOne, to: Driver, required: true }
            processes:
              - name: Identify
                trigger: { onCreate: Fine }
                steps:
                  - { name: identify, kind: userTask, args: { assignee: officer, form: IdentifyDriver } }
                  - { name: done, kind: end }
            forms:
              - name: IdentifyDriver
                forEntity: Fine
                fields: [vehicle, driver, note]
                editable: [driver]
                actions: [identify]
            """;

    private static Map<String, Object> control(String yaml, String id) {
        Map<String, Object> form = FormIntentGenerator.buildFormsForTest(IntentParser.parse(yaml))
                                                      .get("IdentifyDriver");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> controls = (List<Map<String, Object>>) form.get("form");
        return controls.stream()
                       .filter(c -> id.equals(c.get("id")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no control [" + id + "] in " + controls));
    }

    @Test
    void anEditableRelationRendersAsAPickerOverTheRelatedRecords() {
        Map<String, Object> picker = control(YAML, "DriverId");
        assertEquals("input-select", picker.get("controlId"));
        assertEquals("Driver", picker.get("model"), "the FK property - what the Writer persists");
        assertEquals(Boolean.FALSE, picker.get("readonly"));
        assertEquals(Boolean.FALSE, picker.get("staticData"));
        assertEquals(Boolean.TRUE, picker.get("required"), "a required relation must still be answered");
        assertEquals("Driver", picker.get("label"));
    }

    /**
     * The generator emits variable NAMES, never a path: the URL is composed by the events template,
     * which is the layer that knows the route, and reaches the form as a process variable.
     */
    @Test
    void thePickerNamesTheProcessVariablesThatLocateItsOptions() {
        Map<String, Object> picker = control(YAML, "DriverId");
        assertEquals("__DriverEntityUrl", picker.get("options"));
        assertEquals("__DriverEntityLabel", picker.get("optionLabel"));
        assertEquals("Id", picker.get("optionValue"), "the target's own key property");
        assertTrue(picker.values()
                         .stream()
                         .noneMatch(v -> String.valueOf(v)
                                               .contains("/services/")),
                "no template-engine path may appear in the emitted control: " + picker);
    }

    /** A relation that is displayed but NOT editable keeps rendering as the read-only Label: Value. */
    @Test
    void aDisplayedButNotEditableRelationIsNotAPicker() {
        String yaml = YAML.replace("editable: [driver]", "editable: [note]");
        Map<String, Object> control = control(yaml, "DriverId");
        assertEquals("input-textfield", control.get("controlId"));
        assertEquals(Boolean.TRUE, control.get("readonly"));
    }

    /**
     * The picker is a TASK-FORM control: it locates its options through the process variables the
     * trigger seeds, which exist only there. A non-task form listing a bare to-one relation keeps the
     * plain text control - the picker branch would render an editable select that stays permanently
     * empty, where a text control at least shows the raw key.
     */
    @Test
    void aRelationOnANonTaskFormNeverBecomesAPicker() {
        String yaml = YAML.replace("""
                processes:
                  - name: Identify
                    trigger: { onCreate: Fine }
                    steps:
                      - { name: identify, kind: userTask, args: { assignee: officer, form: IdentifyDriver } }
                      - { name: done, kind: end }
                """, "")
                          .replace("editable: [driver]\n    actions: [identify]", "");
        assertTrue(!yaml.contains("processes:") && !yaml.contains("editable:"),
                "the fixture surgery must actually detach the form from any process, or this test passes vacuously");
        Map<String, Object> form = FormIntentGenerator.buildFormsForTest(IntentParser.parse(yaml))
                                                      .get("IdentifyDriver");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> controls = (List<Map<String, Object>>) form.get("form");
        assertTrue(controls.stream()
                           .noneMatch(c -> "input-select".equals(c.get("controlId"))),
                "a non-task form has no process context to feed a picker: " + controls);
    }

    /**
     * The FK is the target's integer key, so it rides the Writer's existing integer branch - a relation
     * adds no new coercion category.
     */
    @Test
    void theWriterPersistsTheChosenIdIntoTheForeignKey() {
        IntentModel model = IntentParser.parse(YAML);
        List<WriterSupport.Writer> writers = WriterSupport.writers(model);
        assertEquals(1, writers.size());
        assertEquals("Fine", writers.get(0)
                                    .entity());
        assertEquals(List.of(new WriterSupport.WriteField("Driver", "integer")), writers.get(0)
                                                                                        .fields());
    }

    /** A long-keyed target is written through the long branch, for the same reason. */
    @Test
    void aLongKeyedTargetUsesTheLongCoercion() {
        // Driver is the first entity, so the first key declaration is its own.
        String yaml = YAML.replaceFirst("integer, primaryKey", "long, primaryKey");
        assertEquals(List.of(new WriterSupport.WriteField("Driver", "long")), WriterSupport.writers(IntentParser.parse(yaml))
                                                                                           .get(0)
                                                                                           .fields());
    }
}
