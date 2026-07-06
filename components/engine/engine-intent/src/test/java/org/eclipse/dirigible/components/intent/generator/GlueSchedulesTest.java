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
