/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code schedules} entries the {@link GlueIntentGenerator} emits for the two per-row
 * actions: a {@code generate} schedule (scheduled record generation) carries the create-from target
 * and the pre-rendered field assignments against the loop row ({@code entity.<Prop>}); a
 * {@code notify} schedule keeps the mail plan. The hyphenated schedule name becomes a valid Java
 * class identifier.
 */
class GlueSchedulesTest {

    @SuppressWarnings("unchecked")
    @Test
    void generateScheduleEmitsCreateFromTargetAndRowAssignments() {
        String yaml = """
                name: hr
                entities:
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: status, type: string }
                  - name: EmployeeTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: date }
                    relations:
                      - { name: Employee, kind: manyToOne, to: Employee }
                schedules:
                  - name: monthly-timesheets
                    cron: "0 0 1 1 * ?"
                    entity: Employee
                    where:
                      - { field: status, op: eq, value: ACTIVE }
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                      defaults:
                        Period: now
                """;
        IntentModel model = IntentParser.parse(yaml);
        List<Map<String, Object>> schedules = GlueIntentGenerator.buildSchedulesForTest(model);
        assertEquals(1, schedules.size());
        Map<String, Object> s = schedules.get(0);

        assertEquals("generate", s.get("action"));
        // pascalIdentifier keeps a hyphenated name a legal Java class.
        assertEquals("MonthlyTimesheets", s.get("className"));
        assertEquals("Employee", s.get("entity"));
        assertEquals("EmployeeTimesheet", s.get("genToEntity"));
        assertEquals(false, s.get("genCrossModel"));
        assertTrue(((String) s.get("criteriaExpression")).contains(".eq(\"Status\", \"ACTIVE\")"),
                "criteria: " + s.get("criteriaExpression"));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) s.get("genFieldAssignments");
        // The loop variable in the job template is "entity"; map copies the row, defaults render
        // now/literal.
        assertTrue(fields.contains(Map.of("targetProp", "Employee", "expr", "entity.Id")), "fields: " + fields);
        assertTrue(fields.contains(Map.of("targetProp", "Period", "expr", "java.time.LocalDate.now()")), "fields: " + fields);
    }

    @SuppressWarnings("unchecked")
    @Test
    void generateScheduleRendersNowInTheTargetFieldsOwnShape() {
        // A month/week field is a plain String on the generated entity (VARCHAR at the JDBC
        // level), so the untyped LocalDate.now() would not even compile against it - `now` must
        // render the target field's own value shape.
        String yaml = """
                name: hr
                entities:
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: status, type: string }
                  - name: EmployeeTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: month }
                      - { name: slot, type: week }
                      - { name: bookedOn, type: date }
                    relations:
                      - { name: Employee, kind: manyToOne, to: Employee }
                schedules:
                  - name: monthly-timesheets
                    cron: "0 0 1 1 * ?"
                    entity: Employee
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                      defaults:
                        Period: now
                        Slot: now
                        BookedOn: now
                """;
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(model)
                                                   .get(0);
        List<Map<String, Object>> fields = (List<Map<String, Object>>) s.get("genFieldAssignments");
        assertTrue(fields.contains(Map.of("targetProp", "Period", "expr", "java.time.YearMonth.now().toString()")),
                "a month field's now must be the YYYY-MM string: " + fields);
        assertTrue(fields.stream()
                         .anyMatch(f -> "Slot".equals(f.get("targetProp")) && ((String) f.get("expr")).contains("WEEK_BASED_YEAR")),
                "a week field's now must be the YYYY-Www ISO-week string: " + fields);
        assertTrue(fields.contains(Map.of("targetProp", "BookedOn", "expr", "java.time.LocalDate.now()")),
                "a date field keeps today's LocalDate: " + fields);
    }

    @Test
    void generateScheduleResolvesCrossModelTarget() {
        String yaml = """
                name: hr
                uses:
                  - { model: billing }
                entities:
                  - name: Subscription
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: status, type: string }
                schedules:
                  - name: recurring-invoices
                    cron: "0 0 1 1 * ?"
                    entity: Subscription
                    generate:
                      to: SalesInvoice
                      uses: billing
                      map:
                        Subscription: id
                """;
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(model)
                                                   .get(0);
        assertEquals("generate", s.get("action"));
        assertEquals(true, s.get("genCrossModel"));
        assertEquals("billing", s.get("genToModel"));
        // With no repository, the cross-model perspective falls back to the entity name (convention).
        assertEquals("SalesInvoice", s.get("genToPerspective"));
    }

    @Test
    void localSourceScheduleMarksSourceAsNotCrossModel() {
        // Backward-compatibility: a local-source schedule carries the new source keys with the
        // not-cross-model values, so the template's ${sourceGenFolder} stays this project's folder.
        String yaml = """
                name: hr
                entities:
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: status, type: string }
                  - name: EmployeeTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Employee, kind: manyToOne, to: Employee }
                schedules:
                  - name: monthly-timesheets
                    cron: "0 0 1 1 * ?"
                    entity: Employee
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                """;
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(model)
                                                   .get(0);
        assertEquals(false, s.get("sourceCrossModel"));
        assertEquals("", s.get("sourceModel"));
        assertEquals("Employee", s.get("perspective"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void crossModelSourceScheduleEmitsSourceKeysAndCrossModelForEach() {
        // The source Project lives in the projects model (a declared uses: alias); with no repository
        // (null context) CrossModelSupport falls back to naming-convention defaults, enough to assert
        // the emitted cross-model source + forEach keys and the criteria against the source row.
        String yaml = """
                name: timesheets
                uses:
                  - { model: projects }
                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: date }
                  - name: EmployeeTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet }
                schedules:
                  - name: monthly-project-timesheets
                    cron: "0 0 2 1 * ?"
                    entity: Project
                    model: projects
                    where:
                      - { field: status, op: eq, value: 2 }
                    generate:
                      to: ProjectTimesheet
                      map:
                        Period: now
                      children:
                        - to: EmployeeTimesheet
                          parent: ProjectTimesheet
                          forEach:
                            entity: EmployeeProjectAssignment
                            model: projects
                            match: { Project: id }
                          map: { Employee: Employee }
                """;
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(model)
                                                   .get(0);
        assertEquals("generate", s.get("action"));
        assertEquals(true, s.get("sourceCrossModel"));
        assertEquals("projects", s.get("sourceModel"));
        // Convention fallback (no repository): the owner perspective + key default to the entity name / Id.
        assertEquals("Project", s.get("perspective"));
        assertEquals("Id", s.get("attachKeyProperty"));
        assertTrue(((String) s.get("criteriaExpression")).contains(".eq(\"Status\", 2)"), "criteria: " + s.get("criteriaExpression"));

        List<Map<String, Object>> children = (List<Map<String, Object>>) s.get("genChildren");
        assertEquals(1, children.size());
        Map<String, Object> child = children.get(0);
        assertEquals(true, child.get("forEachCrossModel"));
        assertEquals("projects", child.get("forEachModel"));
        assertEquals("EmployeeProjectAssignment", child.get("forEachEntity"));
        // Convention fallback: the cross-model collection's perspective defaults to the entity name.
        assertEquals("EmployeeProjectAssignment", child.get("forEachPerspective"));
    }

    @Test
    void notifyScheduleStillEmitsMailPlan() {
        String yaml = """
                name: hr
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: dueOn, type: date }
                      - { name: contactEmail, type: string }
                schedules:
                  - name: overdue-reminders
                    cron: "0 0 8 * * ?"
                    entity: Invoice
                    where:
                      - { field: dueOn, op: lt, value: CURRENT_DATE }
                    notify:
                      to: contactEmail
                      subject: "Overdue"
                      body: "Your invoice is overdue"
                """;
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(model)
                                                   .get(0);
        assertEquals("notify", s.get("action"));
        assertEquals("OverdueReminders", s.get("className"));
        assertTrue(s.containsKey("toExpression"));
    }
}
