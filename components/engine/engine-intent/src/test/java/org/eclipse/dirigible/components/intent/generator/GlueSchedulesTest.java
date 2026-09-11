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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code schedules} entries the {@link GlueIntentGenerator} emits for the two per-row
 * actions: a {@code generate} schedule (scheduled record generation) carries the create-from target
 * and the pre-rendered field assignments against the loop row ({@code entity.<Prop>}); a
 * {@code notify} schedule keeps the mail plan. The hyphenated schedule name becomes a valid Java
 * class identifier.
 */
class GlueSchedulesTest {

    /** A dunning run: the overdue invoices of a nomenclature seeded in this model. */
    private static final String DUNNING = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: dueOn, type: date }
                  - { name: contactEmail, type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            schedules:
              - name: dunning
                cron: "0 0 8 * * ?"
                entity: SalesInvoice
                where:
                  - { field: Status, op: eq, value: OVERDUE }
                  - { field: dueOn,  op: lt, value: CURRENT_DATE }
                notify:
                  to: contactEmail
                  subject: "Invoice {number} is overdue"
                  body: "Please settle the attached invoice."
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
                  - { id: 3, name: OVERDUE }
                  - { id: 4, name: PAID }
            """;

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

    @SuppressWarnings("unchecked")
    @Test
    void theNaturalKeyIsPreRenderedFromTheSameAssignmentsTheTargetIsWrittenFrom() {
        // Issue #7070: the guard has to look the target up by the values it is about to write, or it
        // drifts from them. The `now` on a month field is the sharp case - it renders
        // YearMonth.now().toString(), which is exactly what makes "the same month" comparable at all;
        // a re-derived LocalDate.now() would never match the row the first tick wrote.
        String yaml = """
                name: timesheets
                entities:
                  - name: Project
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: status, type: string }
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: month }
                    relations:
                      - { name: Project, kind: manyToOne, to: Project }
                schedules:
                  - name: monthly-project-timesheets
                    cron: "0 0 2 1 * ?"
                    entity: Project
                    generate:
                      to: ProjectTimesheet
                      unique: [Project, period]
                      map:
                        Project: id
                      defaults:
                        Period: now
                """;
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(model)
                                                   .get(0);

        assertEquals(true, s.get("hasGenUnique"));
        List<Map<String, Object>> unique = (List<Map<String, Object>>) s.get("genUnique");
        assertEquals(List.of(Map.of("property", "Project", "expr", "entity.Id"),
                Map.of("property", "Period", "expr", "java.time.YearMonth.now().toString()")), unique);
    }

    @SuppressWarnings("unchecked")
    @Test
    void thePeriodOfTheRunRendersAsARangeOverTheDateTheRunWrites() {
        // Issue #7106: a monthly bill generated from a standing template is a plain document with a
        // date and no period column, so #7070's property-only key was not expressible at all. The
        // period is not stored: the guard ranges over the very date this run writes, which is what
        // makes a re-run on the 14th find what the 1st created.
        String yaml = """
                name: purchases
                entities:
                  - name: BillTemplate
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: active, type: boolean }
                    relations:
                      - { name: Supplier, kind: manyToOne, to: Supplier }
                  - name: Supplier
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: PurchaseInvoice
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: date, type: date }
                    relations:
                      - { name: Supplier, kind: manyToOne, to: Supplier }
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: BillTemplate
                    generate:
                      to: PurchaseInvoice
                      unique: [Supplier, { run: month }]
                      map:
                        Supplier: Supplier
                      defaults:
                        date: now
                """;
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                   .get(0);

        assertEquals(true, s.get("hasGenUnique"));
        List<Map<String, Object>> unique = (List<Map<String, Object>>) s.get("genUnique");
        assertEquals(Map.of("property", "Supplier", "expr", "entity.Supplier"), unique.get(0));
        // The bounds are built from the assignment's own LocalDate.now(), so the period the guard
        // queries is by construction the period the row is dated into.
        assertEquals(Map.of("kind", "range", "property", "Date", "lower", "java.time.LocalDate.now().withDayOfMonth(1)", "upper",
                "java.time.LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1)"), unique.get(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void theRunPeriodRangesOverTheDateFieldNotAnotherNowAssignment() {
        // Issue #7229: parser and generator must not each decide "the date this run writes". The parser
        // pins the single `date`-typed default it chose; the generator ranges over exactly that. Here a
        // second default assigns `now` to a `timestamp` field, which renders as the same LocalDate.now()
        // the `date` field does - so a generator that re-derived the run date by string-matching that
        // expression counted two candidates and dropped a schedule the parser had accepted.
        String yaml = """
                name: purchases
                entities:
                  - name: BillTemplate
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Supplier, kind: manyToOne, to: Supplier }
                  - name: Supplier
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: PurchaseInvoice
                    fields:
                      - { name: id,        type: integer,   primaryKey: true, generated: true }
                      - { name: date,      type: date }
                      - { name: createdAt, type: timestamp }
                    relations:
                      - { name: Supplier, kind: manyToOne, to: Supplier }
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: BillTemplate
                    generate:
                      to: PurchaseInvoice
                      unique: [Supplier, { run: month }]
                      map:
                        Supplier: Supplier
                      defaults:
                        date: now
                        createdAt: now
                """;
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                   .get(0);

        assertEquals(true, s.get("hasGenUnique"));
        List<Map<String, Object>> unique = (List<Map<String, Object>>) s.get("genUnique");
        assertEquals(Map.of("property", "Supplier", "expr", "entity.Supplier"), unique.get(0));
        assertEquals(Map.of("kind", "range", "property", "Date", "lower", "java.time.LocalDate.now().withDayOfMonth(1)", "upper",
                "java.time.LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1)"), unique.get(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aQuarterlyRunPeriodRangesOverTheIsoQuarter() {
        String yaml = """
                name: purchases
                entities:
                  - name: BillTemplate
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Supplier, kind: manyToOne, to: Supplier }
                  - name: Supplier
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: PurchaseInvoice
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: date, type: date }
                    relations:
                      - { name: Supplier, kind: manyToOne, to: Supplier }
                schedules:
                  - name: quarterly-recurring-bills
                    cron: "0 0 5 1 1,4,7,10 ?"
                    entity: BillTemplate
                    generate:
                      to: PurchaseInvoice
                      unique: [Supplier, { run: quarter }]
                      map:
                        Supplier: Supplier
                      defaults:
                        date: now
                """;
        List<Map<String, Object>> unique = (List<Map<String, Object>>) GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                                                          .get(0)
                                                                                          .get("genUnique");

        assertEquals("java.time.LocalDate.now().with(java.time.temporal.IsoFields.DAY_OF_QUARTER, 1)", unique.get(1)
                                                                                                             .get("lower"));
        assertEquals("java.time.LocalDate.now().with(java.time.temporal.IsoFields.DAY_OF_QUARTER, 1).plusMonths(3).minusDays(1)",
                unique.get(1)
                      .get("upper"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aScheduleWithNoDeclaredKeyStillGeneratesAndCarriesNoGuard() {
        // Backward compatibility is the point: every intent authored before the key existed keeps
        // generating exactly what it did (the generation reports the advisory separately).
        String yaml = """
                name: hr
                entities:
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
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
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                   .get(0);
        assertEquals("generate", s.get("action"));
        assertEquals(false, s.get("hasGenUnique"));
        assertTrue(((List<Map<String, Object>>) s.get("genUnique")).isEmpty());
    }

    /**
     * Issue #7251: the query of a dunning run names the status the row must stand in, and that name is
     * resolved to its seed id before the typed mapping - so the criteria compares the integer status FK
     * with an integer. Left as the authored name it rendered {@code .eq("Status", "OVERDUE")}, a query
     * that matched nothing for as long as the schedule kept ticking.
     */
    @Test
    void aSeededStatusNameInTheQueryRendersAsItsSeedId() {
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(DUNNING))
                                                   .get(0);

        assertEquals("Criteria.create().eq(\"Status\", 3).lt(\"DueOn\", java.time.LocalDate.now())", s.get("criteriaExpression"));
    }

    /**
     * The backstop that keeps the invariant checkable independently of the resolver's site list: a
     * value the resolver never even reads as a symbol (a blank) is still no status the FK can equal, so
     * it is refused rather than generated into a query that matches nothing.
     */
    @Test
    void aValueThatIsNoStatusAtAllIsRefused() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(DUNNING.replace("value: OVERDUE", "value: \"\"")));

        assertTrue(failure.getMessage()
                          .contains("which is not a status"),
                "the failure must say the value is no status: " + failure.getMessage());
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


    @SuppressWarnings("unchecked")
    @Test
    void keyTermsSerializeInDeclarationOrderOnEveryJvm() {
        // Issue #7130: the terms were built with Map.of, whose iteration order comes from a per-JVM
        // random salt, so the same intent serialized {property, expr} on one container and
        // {expr, property} on the next. The glue is a rewritten-in-place artifact - a regen hunk has
        // to be attributable to a platform change or an authored edit, never to which JVM ran it.
        String yaml = """
                name: purchases
                entities:
                  - name: BillTemplate
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: active, type: boolean }
                    relations:
                      - { name: Supplier, kind: manyToOne, to: Supplier }
                  - name: Supplier
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: PurchaseInvoice
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: date, type: date }
                    relations:
                      - { name: Supplier, kind: manyToOne, to: Supplier }
                schedules:
                  - name: monthly-recurring-bills
                    cron: "0 0 5 1 * ?"
                    entity: BillTemplate
                    generate:
                      to: PurchaseInvoice
                      unique: [Supplier, { run: month }]
                      map:
                        Supplier: Supplier
                      defaults:
                        date: now
                """;
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                   .get(0);

        List<Map<String, Object>> unique = (List<Map<String, Object>>) s.get("genUnique");
        assertEquals(List.of("property", "expr"), List.copyOf(unique.get(0)
                                                                    .keySet()));
        assertEquals(List.of("kind", "property", "lower", "upper"), List.copyOf(unique.get(1)
                                                                                      .keySet()));
    }

    /**
     * A dunning run over another model's invoices - the cross-model SOURCE shape ({@code model:}),
     * whose nomenclature is seeded in the owner model too.
     */
    private static final String CROSS_MODEL_DUNNING = """
            name: dunning
            uses:
              - { model: invoices }
            entities:
              - name: DunningLetter
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: sentOn, type: date }
            schedules:
              - name: dunning
                cron: "0 0 6 * * ?"
                entity: SalesInvoice
                model: invoices
                where:
                  - { field: Status, op: eq, value: OVERDUE }
                generate:
                  to: DunningLetter
                  map:
                    Number: Number
                  defaults:
                    sentOn: now
            """;

    /**
     * The owner model as the invoices project generated it: the status FK is the property the edm
     * generator gave the {@code DOCUMENT_STATUS} widget, which is how a consumer learns WHICH property
     * is the status one.
     */
    private static final String OWNER_MODEL = """
            {
              "model": {
                "entities": [
                  {
                    "name": "SalesInvoice",
                    "perspectiveName": "SalesInvoice",
                    "dataName": "INVOICES_SALESINVOICE",
                    "properties": [
                      { "name": "Id", "dataName": "ID", "dataType": "INTEGER", "dataPrimaryKey": "true" },
                      { "name": "Number", "dataName": "NUMBER", "dataType": "VARCHAR" },
                      { "name": "Status", "dataName": "STATUS_ID", "dataType": "INTEGER",
                        "relationshipEntityName": "SalesInvoiceStatus", "widgetType": "DOCUMENT_STATUS" }
                    ]
                  }
                ]
              }
            }
            """;

    /**
     * A cross-model source's nomenclature is seeded in the owner model, so a status NAME in the row
     * query cannot resolve at parse - and used to be left in place, rendering as
     * {@code .eq("Status", "OVERDUE")} against the integer status FK: a query that matched nothing for
     * as long as the schedule kept ticking, with no diagnostic (dirigible #7288, the #7251 failure one
     * {@code model:} key away). It is refused the way every other cross-model status site is - by seed
     * id only - at the one point the owner {@code .model} tells which condition names the status.
     */
    @Test
    void aStatusNameOnACrossModelScheduleSourceIsRefused() {
        IntentGenerationContext context = contextWithOwnerModel(IntentParser.parse(CROSS_MODEL_DUNNING));

        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> GlueIntentGenerator.buildSchedulesForTest(context.getModel(), context));

        assertTrue(failure.getIssues()
                          .stream()
                          .anyMatch(issue -> issue.contains("[Status]") && issue.contains("[OVERDUE]") && issue.contains("[invoices]")
                                  && issue.contains("numeric seed id")),
                "the refusal must name the relation, the name and the owner model: " + failure.getIssues());
    }

    /** The seed id is the cross-model form, and it renders exactly as a local query does. */
    @Test
    void aStatusSeedIdOnACrossModelScheduleSourceRenders() {
        IntentGenerationContext context =
                contextWithOwnerModel(IntentParser.parse(CROSS_MODEL_DUNNING.replace("value: OVERDUE", "value: 4")));

        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(context.getModel(), context)
                                                   .get(0);

        assertEquals(true, s.get("sourceCrossModel"));
        assertEquals("Criteria.create().eq(\"Status\", 4)", s.get("criteriaExpression"));
        // Read off the owner model, not guessed from the entity name.
        assertEquals("SalesInvoice", s.get("perspective"));
    }

    /**
     * An ordinary column compared with a string stays a string: only the status condition is refused,
     * because only there is a literal a value no row can ever carry.
     */
    @Test
    void aStringOnANonStatusConditionOfACrossModelScheduleSourceRenders() {
        IntentGenerationContext context = contextWithOwnerModel(IntentParser.parse(
                CROSS_MODEL_DUNNING.replace("{ field: Status, op: eq, value: OVERDUE }", "{ field: Number, op: eq, value: SI-1 }")));

        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(context.getModel(), context)
                                                   .get(0);

        assertEquals("Criteria.create().eq(\"Number\", \"SI-1\")", s.get("criteriaExpression"));
    }

    private static IntentGenerationContext contextWithOwnerModel(IntentModel model) {
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(missing.exists()).thenReturn(false);
        IResource owner = mock(IResource.class);
        when(owner.exists()).thenReturn(true);
        when(owner.getContent()).thenReturn(OWNER_MODEL.getBytes(StandardCharsets.UTF_8));
        when(repository.getResource(anyString())).thenReturn(missing);
        when(repository.getResource("/users/admin/workspace/invoices/invoices.model")).thenReturn(owner);
        return TestContexts.context(model, repository, "/users/admin/workspace/dunning", "app");
    }
}
