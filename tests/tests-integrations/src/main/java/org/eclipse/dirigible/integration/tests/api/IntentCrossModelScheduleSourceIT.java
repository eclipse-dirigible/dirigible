/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Cross-model schedule SOURCE coverage: a schedule owned by the CONSUMER model iterates a source
 * entity that lives in another (owner) model via {@code model: <uses alias>}, generating a local
 * target record - and a cross-model forEach child - per matching source row.
 *
 * <p>
 * Two mutually-referencing intent modules: {@code xmsource} owns the source entities
 * ({@code Project} + its {@code ProjectAssignment} collection), {@code xmconsumer}
 * {@code uses: xmsource} and owns the generated {@code ProjectTimesheet} (+
 * {@code EmployeeTimesheet} lines). The schedule's source is cross-model; so is the forEach
 * collection.
 *
 * <p>
 * Per the emission + runtime testing contract the assertions target the OUTERMOST observable layer:
 * first the generated {@code Job} TOKENS (the owner-package source import, the Criteria expression,
 * the owner-package forEach class FQNs), then the RUNNING instance - the cron-fired job creates the
 * target + child rows, asserted over REST. Two loud-failure paths are covered too: the owner
 * {@code .model} absent, and a mistyped {@code where} field - both drop the schedule with a warning
 * in the generate response (never a job that cannot compile).
 *
 * <p>
 * It also runs a generation's {@code children} row rule and {@code onRefused: skip} (#8491),
 * triggered directly: which collection rows become children - in one model, and through a hop from
 * a cross-model collection into a third model - and what one child the target refuses costs:
 * itself, or the whole source row.
 */
// One Dirigible boot for the whole class: each method cleans up after itself (or is read-only), so
// the per-method context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCrossModelScheduleSourceIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String OWNER = "xmsource";
    private static final String CONSUMER = "xmconsumer";

    /** The owner module: the schedule's cross-model source entities. */
    private static final String OWNER_INTENT = """
            name: xmsource
            description: cross-model schedule source fixture - owns the source entities

            entities:
              - name: Project
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string,  required: true, length: 100 }
                  - { name: status, type: integer, required: true }

              - name: ProjectAssignment
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: project,  type: integer, required: true }
                  - { name: employee, type: string,  required: true, length: 100 }
            """;

    /**
     * The consumer module: owns the created rows and schedules off the owner's Project (+ its
     * ProjectAssignment collection), both cross-model. A fast cron so the job fires within the test.
     */
    private static String consumerIntent(String whereField) {
        return """
                name: xmconsumer
                description: cross-model schedule consumer fixture - owns created rows, schedules off xmsource

                uses:
                  - { model: xmsource }

                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id,      type: integer, primaryKey: true, generated: true }
                      - { name: project, type: integer }
                      - { name: period,  type: date }

                  - name: EmployeeTimesheet
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: employee, type: string, length: 100 }
                    relations:
                      - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet }

                schedules:
                  - name: monthlyProjectTimesheets
                    cron: "0/5 * * * * ?"
                    entity: Project
                    model: xmsource
                    where:
                      - { field: %s, op: eq, value: 1 }
                    generate:
                      to: ProjectTimesheet
                      map:
                        Project: id
                      defaults:
                        Period: now
                      children:
                        - to: EmployeeTimesheet
                          parent: ProjectTimesheet
                          forEach:
                            entity: ProjectAssignment
                            model: xmsource
                            match: { Project: id }
                          map: { Employee: Employee }
                """.formatted(whereField);
    }

    /**
     * The owner module of the customer-statement case: the mailed rows are its customers.
     */
    private static final String STATEMENT_OWNER_INTENT = """
            name: xmsource
            description: cross-model schedule source fixture - owns the mailed customers

            entities:
              - name: Customer
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: name,        type: string,  required: true, length: 100 }
                  - { name: email,       type: string,  length: 100 }
                  - { name: openBalance, type: decimal, precision: 15, scale: 2 }
            """;

    /**
     * The consumer module of the customer-statement case (dirigible #7030): it owns the invoices and
     * therefore the statement REPORT, so the monthly mail can only be declared here - and its source,
     * the customer, is cross-model. {@code recipientField} is the notify recipient, so a mistyped one
     * exercises the loud drop.
     */
    private static String statementConsumerIntent(String recipientField) {
        return """
                name: xmconsumer
                description: cross-model schedule notify fixture - owns the statement report

                uses:
                  - { model: xmsource }

                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: issuedOn, type: date }
                      - { name: total,    type: decimal, precision: 15, scale: 2 }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, model: xmsource }

                reports:
                  - name: CustomerStatement
                    source: SalesInvoice
                    dimensions: [issuedOn]
                    measures: ["sum(total)"]
                    parameters:
                      - { name: customer, target: Customer.name, op: like }

                schedules:
                  - name: monthlyCustomerStatements
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    model: xmsource
                    where:
                      - { field: openBalance, op: gt, value: 0 }
                    notify:
                      to: %s
                      subject: "Your account statement"
                      body: "Dear {name}, your statement is attached."
                      attach: { report: CustomerStatement, bind: { customer: name } }
                """.formatted(recipientField);
    }

    /** The module of the children row-rule case (#8491) - every entity local. */
    private static final String KIDS = "schedkids";

    /**
     * One timesheet per assignment of a project, one day line per working day under it. The row rule
     * leaves out an inactive employee's assignment through one relation hop; a timesheet's capacity
     * ({@code maxHours}) refuses the day line that would exceed it, AFTER the timesheet and its first
     * line were written - so leaving the timesheet out must take those writes back too. The three
     * schedules select one project each and differ in where the skip is: on the timesheet, nowhere, and
     * on the day (with a month-day child and a peer grandchild, so every place a skip is written in the
     * job is compiled and run). Their cron (the 1st of January at 03:00) never fires inside a test run;
     * they are triggered.
     */
    private static final String KIDS_INTENT =
            """
                    name: schedkids
                    description: scheduled generate children - a row rule, and a refused child left out (#8491)

                    entities:
                      - name: Employee
                        fields:
                          - { name: id,     type: integer, primaryKey: true, generated: true }
                          - { name: name,   type: string,  length: 100 }
                          - { name: active, type: boolean }

                      - name: Project
                        fields:
                          - { name: id,   type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string,  length: 100 }

                      - name: Assignment
                        fields:
                          - { name: id,       type: integer, primaryKey: true, generated: true }
                          - { name: maxHours, type: decimal, precision: 10, scale: 2 }
                        relations:
                          - { name: Project,  kind: manyToOne, to: Project }
                          - { name: Employee, kind: manyToOne, to: Employee }

                      - name: ProjectMonth
                        fields:
                          - { name: id,         type: integer, primaryKey: true, generated: true }
                          - { name: timesheets, type: integer }
                        relations:
                          - { name: Project, kind: manyToOne, to: Project }

                      - name: MonthDay
                        fields:
                          - { name: id,  type: integer, primaryKey: true, generated: true }
                          - { name: day, type: date }
                        relations:
                          - { name: ProjectMonth, kind: manyToOne, to: ProjectMonth }

                      - name: Timesheet
                        fields:
                          - { name: id,       type: integer, primaryKey: true, generated: true }
                          - { name: maxHours, type: decimal, precision: 10, scale: 2 }
                          - { name: hours,    type: decimal, precision: 10, scale: 2 }
                        relations:
                          - { name: ProjectMonth, kind: manyToOne, to: ProjectMonth }
                          - { name: Employee,     kind: manyToOne, to: Employee }

                      - name: DayLine
                        fields:
                          - { name: id,    type: integer, primaryKey: true, generated: true }
                          - { name: day,   type: date }
                          - { name: hours, type: decimal, precision: 10, scale: 2 }
                        relations:
                          - { name: Timesheet, kind: manyToOne, to: Timesheet }

                      - name: Peer
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                        relations:
                          - { name: Timesheet, kind: manyToOne, to: Timesheet }
                          - { name: Employee,  kind: manyToOne, to: Employee }

                    rollups:
                      - { name: monthTimesheets, entity: Timesheet, via: ProjectMonth, field: timesheets }
                      - { name: timesheetHours, entity: DayLine, via: Timesheet, field: hours, op: sum, of: hours, capacity: maxHours }

                    schedules:
                      - name: monthly-skip
                        cron: "0 0 3 1 1 ?"
                        entity: Project
                        where: [ { field: name, op: eq, value: Alpha } ]
                        generate:
                          to: ProjectMonth
                          unique: [Project]
                          map: { Project: id }
                          children:
                            - to: Timesheet
                              parent: ProjectMonth
                              forEach: { entity: Assignment, match: { Project: id } }
                              where: [ { field: Employee.active, op: eq, value: true } ]
                              map: { Employee: Employee, maxHours: maxHours }
                              onRefused: skip
                              children:
                                - { to: DayLine, parent: Timesheet, forEach: { days: workingDays }, dayField: day, defaults: { hours: 8 } }

                      - name: monthly-strict
                        cron: "0 0 3 1 1 ?"
                        entity: Project
                        where: [ { field: name, op: eq, value: Beta } ]
                        generate:
                          to: ProjectMonth
                          unique: [Project]
                          map: { Project: id }
                          children:
                            - to: Timesheet
                              parent: ProjectMonth
                              forEach: { entity: Assignment, match: { Project: id } }
                              where: [ { field: Employee.active, op: eq, value: true } ]
                              map: { Employee: Employee, maxHours: maxHours }
                              children:
                                - { to: DayLine, parent: Timesheet, forEach: { days: workingDays }, dayField: day, defaults: { hours: 8 } }

                      - name: monthly-days
                        cron: "0 0 3 1 1 ?"
                        entity: Project
                        where: [ { field: name, op: eq, value: Gamma } ]
                        generate:
                          to: ProjectMonth
                          unique: [Project]
                          map: { Project: id }
                          children:
                            - { to: MonthDay, parent: ProjectMonth, forEach: { days: workingDays }, dayField: day, onRefused: skip }
                            - to: Timesheet
                              parent: ProjectMonth
                              forEach: { entity: Assignment, match: { Project: id } }
                              where: [ { field: Employee.active, op: eq, value: true } ]
                              map: { Employee: Employee, maxHours: maxHours }
                              children:
                                - { to: DayLine, parent: Timesheet, forEach: { days: workingDays }, dayField: day, defaults: { hours: 8 }, onRefused: skip }
                                - to: Peer
                                  parent: Timesheet
                                  forEach: { entity: Assignment, match: { Project: id } }
                                  where: [ { field: Employee.active, op: eq, value: true } ]
                                  map: { Employee: Employee }
                                  onRefused: skip
                    """;

    /**
     * The three modules of the cross-model row-rule case (#8491): timesheets -> projects -> employees.
     */
    private static final String STAFF = "xmemployees";
    private static final String PROJECTS = "xmprojects";
    private static final String SHEETS = "xmtimesheets";

    private static final String STAFF_INTENT = """
            name: xmemployees
            description: cross-model row-rule fixture - owns the employees

            entities:
              - name: Employee
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string,  length: 100 }
                  - { name: active, type: boolean }
            """;

    /**
     * Owns the projects and their assignments; an assignment points at an employee of a third model.
     */
    private static final String PROJECTS_INTENT = """
            name: xmprojects
            description: cross-model row-rule fixture - owns the projects and their staffing

            uses:
              - { model: xmemployees }

            entities:
              - name: Project
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  length: 100 }

              - name: EmployeeProjectAssignment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Project,  kind: manyToOne, to: Project }
                  - { name: Employee, kind: manyToOne, to: Employee, model: xmemployees }
            """;

    /**
     * Owns the generated rows. Both the schedule's source and its forEach collection are the projects
     * model's, and the row rule hops from the collection into the employees model - the shape of the
     * monthly project-timesheets job the issue was filed for. It does not even name the employees
     * model: the projects model's own {@code .model} says who owns the employee.
     */
    private static final String SHEETS_INTENT = """
            name: xmtimesheets
            description: cross-model row-rule fixture - a monthly timesheet per active employee's assignment

            uses:
              - { model: xmprojects }

            entities:
              - name: ProjectTimesheet
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: project, type: integer }

              - name: EmployeeTimesheet
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: employee, type: integer }
                relations:
                  - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet }

            schedules:
              - name: monthly-project-timesheets
                cron: "0 0 3 1 1 ?"
                entity: Project
                model: xmprojects
                generate:
                  to: ProjectTimesheet
                  unique: [Project]
                  map: { Project: id }
                  children:
                    - to: EmployeeTimesheet
                      parent: ProjectTimesheet
                      forEach: { entity: EmployeeProjectAssignment, model: xmprojects, match: { Project: id } }
                      where:
                        - { field: Employee.active, op: eq, value: true }
                      map: { Employee: Employee }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void cross_model_source_schedule_generates_target_and_child_rows() {
        generateProject(OWNER, OWNER_INTENT);
        generateProject(CONSUMER, consumerIntent("status"));

        assertJobTokens();

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        // Seed the source rows over the OWNER's REST controllers: one active Project + two assignments.
        int projectId = createProject();
        createAssignment(projectId, "emp-1");
        createAssignment(projectId, "emp-2");

        assertGeneratedRowsAppear();
    }

    /**
     * Layer 1: the generated Job imports the OWNER's gen classes and queries the cross-model source.
     */
    private void assertJobTokens() {
        String job = contentOf(CONSUMER, "gen/events/xmconsumer/MonthlyProjectTimesheetsJob.java");
        assertTrue(job.contains("package gen.events.xmconsumer;"), "the job lives in the consumer's events package");
        assertTrue(job.contains("import gen.xmsource.data.project.ProjectEntity;"),
                "the source entity import must resolve against the OWNER's gen package");
        assertTrue(job.contains("import gen.xmsource.data.project.ProjectRepository;"),
                "the source repository import must resolve against the OWNER's gen package");
        assertTrue(job.contains(".eq(\"Status\", 1)"), "the Criteria must filter the source rows: " + job);
        assertTrue(job.contains("gen.xmsource.data.projectassignment.ProjectAssignmentRepository"),
                "the cross-model forEach collection class must resolve against the OWNER's gen package");
        assertTrue(job.contains("gen.xmconsumer.data.projecttimesheet.ProjectTimesheetEntity"),
                "the generation target stays local to the consumer");
        assertTrue(job.contains("gen.xmconsumer.data.employeetimesheet.EmployeeTimesheetEntity"),
                "the child target stays local to the consumer");
    }

    /**
     * Layer 2: the running instance - the cron-fired job reads the cross-model source and creates the
     * local target + child rows, so both consumer controllers report a non-empty count.
     */
    private void assertGeneratedRowsAppear() {
        String api = "/services/java/" + CONSUMER + "/gen/xmconsumer/api";
        // The job fires every five seconds; allow a generous window for sync + the first tick.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(api + "/projecttimesheet/ProjectTimesheetController/count")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("count", greaterThanOrEqualTo(1)),
                90);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(api + "/employeetimesheet/EmployeeTimesheetController/count")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("count", greaterThanOrEqualTo(1)),
                90);
    }

    @Test
    void missing_owner_model_drops_the_schedule_with_a_warning() {
        // The consumer is generated WITHOUT the owner model present in the workspace or registry, so the
        // cross-model source cannot be resolved: the schedule is dropped with a warning, not a 422.
        writeIntent(CONSUMER, consumerIntent("status"));
        List<String> warnings = generateWarnings(CONSUMER);
        assertTrue(warnings.stream()
                           .anyMatch(w -> w.contains("monthlyProjectTimesheets") && w.contains("cannot be resolved")
                                   && w.contains("xmsource")),
                "expected an unresolvable-owner warning naming the schedule and owner model, got: " + warnings);
    }

    @Test
    void mistyped_where_field_drops_the_schedule_naming_the_owner_model() {
        // With the owner model present, a where field that does not exist on the owner's Project is
        // caught at generation and the schedule is dropped with a warning naming the owner model.
        generateProject(OWNER, OWNER_INTENT);
        writeIntent(CONSUMER, consumerIntent("nonexistentField"));
        List<String> warnings = generateWarnings(CONSUMER);
        assertTrue(warnings.stream()
                           .anyMatch(w -> w.contains("monthlyProjectTimesheets") && w.contains("where field [nonexistentField]")
                                   && w.contains("xmsource")),
                "expected a where-field warning naming the schedule, field and owner model, got: " + warnings);
    }

    @Test
    void cross_model_source_schedule_mails_the_statement_it_owns_the_report_for() {
        // The suite layout #6931's mechanism was filed for: the statement report lives with the
        // invoices, the customer lives in the owner module, and the schedule has no other legal home -
        // so its source is cross-model and its action is notify.
        generateProject(OWNER, STATEMENT_OWNER_INTENT);
        generateProject(CONSUMER, statementConsumerIntent("email"));

        String job = contentOf(CONSUMER, "gen/events/xmconsumer/MonthlyCustomerStatementsJob.java");
        assertTrue(job.contains("import gen.xmsource.data.customer.CustomerEntity;"), "the mailed row is the OWNER's entity: " + job);
        assertTrue(job.contains("new CustomerRepository().findAll("), "the rows come from the owner's repository: " + job);
        // The recipient local is declared ahead of the row's fail-soft try (#7233) and assigned inside it.
        assertTrue(job.contains("to = entity.Email;"), "the recipient is a field of the cross-model row: " + job);
        assertTrue(job.contains("reportFilter.put(\"customer\", reportValue(entity.Name));"),
                "the report parameter is bound from the row, so the PDF is this customer's: " + job);
        assertTrue(job.contains("CustomerStatementRepository"), "the attached report is the CONSUMER's own: " + job);

        // Layer 2: it compiles. The whole point of resolving the owner's facts is a job that builds.
        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();
        String ours = "findAll { it.category == 'Compilation' && it.location.startsWith('/" + CONSUMER + "/') }";
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/ide/problems")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(ours + ".size()", equalTo(0)),
                60);
    }

    @Test
    void mistyped_notify_recipient_drops_the_schedule_naming_the_owner_model() {
        // A direct path is emitted as a plain field read, so a name the owner does not carry would
        // reach javac: it is caught against the owner's .model and the schedule is dropped instead.
        generateProject(OWNER, STATEMENT_OWNER_INTENT);
        writeIntent(CONSUMER, statementConsumerIntent("mailbox"));
        List<String> warnings = generateWarnings(CONSUMER);
        assertTrue(warnings.stream()
                           .anyMatch(w -> w.contains("monthlyCustomerStatements") && w.contains("notify recipient [mailbox]")
                                   && w.contains("xmsource")),
                "expected a notify-recipient warning naming the schedule, field and owner model, got: " + warnings);
    }

    @Test
    void children_row_rule_and_a_refused_child_left_out_or_rolling_the_row_back() {
        generateProject(KIDS, KIDS_INTENT);
        publishProject(KIDS);
        synchronizationProcessor.forceProcessSynchronizers();

        int ann = createIn(KIDS, "employee/EmployeeController", "{\"Name\":\"Ann\",\"Active\":true}");
        int bob = createIn(KIDS, "employee/EmployeeController", "{\"Name\":\"Bob\",\"Active\":true}");
        int cid = createIn(KIDS, "employee/EmployeeController", "{\"Name\":\"Cid\",\"Active\":false}");
        int alpha = createIn(KIDS, "project/ProjectController", "{\"Name\":\"Alpha\"}");
        int beta = createIn(KIDS, "project/ProjectController", "{\"Name\":\"Beta\"}");
        int gamma = createIn(KIDS, "project/ProjectController", "{\"Name\":\"Gamma\"}");
        for (int project : List.of(alpha, beta, gamma)) {
            // Bob's 10 hours hold one 8-hour day: his second day line is refused by the capacity guard.
            createIn(KIDS, "assignment/AssignmentController", assignment(project, ann, 1000));
            createIn(KIDS, "assignment/AssignmentController", assignment(project, bob, 10));
            createIn(KIDS, "assignment/AssignmentController", assignment(project, cid, 1000));
        }

        triggerJob(KIDS, "MonthlySkipJob");
        triggerJob(KIDS, "MonthlyStrictJob");
        triggerJob(KIDS, "MonthlyDaysJob");

        java.time.LocalDate first = java.time.LocalDate.now()
                                                       .withDayOfMonth(1);
        int workingDays = (int) first.datesUntil(first.plusMonths(1))
                                     .filter(day -> day.getDayOfWeek()
                                                       .getValue() < 6)
                                     .count();
        String months = "projectmonth/ProjectMonthController";
        String timesheets = "timesheet/TimesheetController";
        String days = "dayline/DayLineController";

        // No skip: Bob's refusal rolls Beta's whole month back, as before.
        assertCountIn(KIDS, months, "it.Project == " + beta, 0);
        // Skip on the timesheet: Alpha's month keeps Ann's timesheet with all its days. Bob's was refused
        // at his second day and left out with the day it had already written; Cid's assignment never
        // qualified.
        int alphaMonth = idOfIn(KIDS, months, "it.Project == " + alpha);
        assertCountIn(KIDS, timesheets, "it.ProjectMonth == " + alphaMonth, 1);
        int annAlpha = idOfIn(KIDS, timesheets, "it.ProjectMonth == " + alphaMonth + " && it.Employee == " + ann);
        assertCountIn(KIDS, days, "it.Timesheet == " + annAlpha, workingDays);
        // Skip on the day: Gamma's month keeps Bob's timesheet with the one day that fit.
        int gammaMonth = idOfIn(KIDS, months, "it.Project == " + gamma);
        assertCountIn(KIDS, timesheets, "it.ProjectMonth == " + gammaMonth, 2);
        int bobGamma = idOfIn(KIDS, timesheets, "it.ProjectMonth == " + gammaMonth + " && it.Employee == " + bob);
        assertCountIn(KIDS, days, "it.Timesheet == " + bobGamma, 1);
        assertCountIn(KIDS, "monthday/MonthDayController", "it.ProjectMonth == " + gammaMonth, workingDays);
        // ...and each of its two timesheets got a peer per qualifying assignment: Ann and Bob, not Cid.
        assertCountIn(KIDS, "peer/PeerController", "true", 4);
        // Nothing a left-out child wrote survives: Ann's two months of days and Bob's one.
        assertCountIn(KIDS, days, "true", 2 * workingDays + 1);
        // The parent's roll-up counts the children that were created, not the one left out.
        assertCountIn(KIDS, months, "it.Id == " + alphaMonth + " && it.Timesheets == 1", 1);
    }

    /**
     * The row rule across three models (#8491): the collection is the projects model's, the hop lands
     * in the employees model, and an inactive employee's assignment gets no timesheet. Its repository
     * is the owner's generated one, found through the projection the projects model carries.
     */
    @Test
    void children_row_rule_hops_from_a_cross_model_collection_into_a_third_model() {
        generateProject(STAFF, STAFF_INTENT);
        generateProject(PROJECTS, PROJECTS_INTENT);
        generateProject(SHEETS, SHEETS_INTENT);
        assertTrue(contentOf(SHEETS, "gen/events/xmtimesheets/MonthlyProjectTimesheetsJob.java").contains(
                ".in(\"Employee\", new gen.xmemployees.data.employee.EmployeeRepository().findAll(Criteria.create().eq(\"Active\", true))"),
                "the hop must query the employees model's own repository");
        publishProject(STAFF);
        publishProject(PROJECTS);
        publishProject(SHEETS);
        synchronizationProcessor.forceProcessSynchronizers();

        int ann = createIn(STAFF, "employee/EmployeeController", "{\"Name\":\"Ann\",\"Active\":true}");
        int cid = createIn(STAFF, "employee/EmployeeController", "{\"Name\":\"Cid\",\"Active\":false}");
        int alpha = createIn(PROJECTS, "project/ProjectController", "{\"Name\":\"Alpha\"}");
        createIn(PROJECTS, "employeeprojectassignment/EmployeeProjectAssignmentController",
                "{\"Project\":" + alpha + ",\"Employee\":" + ann + "}");
        createIn(PROJECTS, "employeeprojectassignment/EmployeeProjectAssignmentController",
                "{\"Project\":" + alpha + ",\"Employee\":" + cid + "}");

        triggerJob(SHEETS, "MonthlyProjectTimesheetsJob");

        assertCountIn(SHEETS, "projecttimesheet/ProjectTimesheetController", "it.Project == " + alpha, 1);
        assertCountIn(SHEETS, "employeetimesheet/EmployeeTimesheetController", "it.Employee == " + ann, 1);
        assertCountIn(SHEETS, "employeetimesheet/EmployeeTimesheetController", "it.Employee == " + cid, 0);
    }

    private static String assignment(int project, int employee, int maxHours) {
        return "{\"Project\":" + project + ",\"Employee\":" + employee + ",\"MaxHours\":" + maxHours + "}";
    }

    private int createIn(String project, String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post("/services/java/" + project + "/gen/" + project + "/api/" + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                60);
        return id.get();
    }

    private int idOfIn(String project, String controller, String condition) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().queryParam("$limit", 1000)
                                                        .when()
                                                        .get("/services/java/" + project + "/gen/" + project + "/api/" + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .<Integer>path("find { " + condition + " }.Id")),
                30);
        return id.get();
    }

    /** Runs the job as the Jobs perspective's play button does - synchronously on the Java engine. */
    private void triggerJob(String project, String job) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("[]")
                                                 .when()
                                                 .post("/services/jobs/trigger/gen.events." + project + "." + job)
                                                 .then()
                                                 .statusCode(200),
                60);
    }

    private void assertCountIn(String project, String controller, String condition, int expected) {
        restAssuredExecutor.execute(() -> given().queryParam("$limit", 1000)
                                                 .when()
                                                 .get("/services/java/" + project + "/gen/" + project + "/api/" + controller)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { " + condition + " }.size()", equalTo(expected)),
                30);
    }

    private int createProject() {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Name\":\"Alpha\",\"Status\":1}")
                                                        .when()
                                                        .post("/services/java/" + OWNER + "/gen/xmsource/api/project/ProjectController")
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                60);
        return id.get();
    }

    private void createAssignment(int projectId, String employee) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Project\":" + projectId + ",\"Employee\":\"" + employee + "\"}")
                                                 .when()
                                                 .post("/services/java/" + OWNER
                                                         + "/gen/xmsource/api/projectassignment/ProjectAssignmentController")
                                                 .then()
                                                 .statusCode(200));
    }

    /** Write the intent and drive model-to-code from the generate response's own plan. */
    private void generateProject(String project, String yaml) {
        writeIntent(project, yaml);
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + project + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        // The intent engine runs the model-to-code recipes itself, in the same call: assert each one
        // succeeded instead of replaying them from here.
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    /** Generate the intent (models only) and return the non-fatal warnings from the response. */
    private List<String> generateWarnings(String project) {
        AtomicReference<List<String>> warnings = new AtomicReference<>();
        restAssuredExecutor.execute(() -> warnings.set(given().when()
                                                              .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                      + project + "&path=app.intent")
                                                              .then()
                                                              .statusCode(200)
                                                              .extract()
                                                              .jsonPath()
                                                              .getList("warnings")));
        return warnings.get();
    }

    private void publishProject(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent(String project, String yaml) {
        String path = projectPath(project) + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String contentOf(String project, String fileName) {
        return new String(repository.getResource(projectPath(project) + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(OWNER, CONSUMER, KIDS, SHEETS, PROJECTS, STAFF)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
        // The context (and its Quartz scheduler) now outlives the method: run the synchronizers so the
        // JobSynchronizer's DELETE branch unschedules the published 5-second cron job - otherwise it
        // keeps firing against unloaded gen classes for the rest of the class.
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
