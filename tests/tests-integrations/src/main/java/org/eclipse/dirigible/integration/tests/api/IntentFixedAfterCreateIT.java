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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

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

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * A to-one relation picked on create and fixed afterwards (intent {@code fixedAfterCreate: true},
 * dirigible #7903), and a field set on create and fixed afterwards (the same key, #8363).
 *
 * <p>
 * The case: an employee moved their own timesheet to another project-month, so its hours landed on
 * another customer's invoice. No construct could say "the user picks it when creating the record
 * and it never moves afterwards" - an update recompute disables the picker on the New form too, a
 * relation refuses {@code readOnly:}, and {@code immutableWhen} locks the whole record.
 *
 * <p>
 * Each answer the guard gives is asserted on the running, generated code, because each is a way it
 * could be wrong: a changed value is refused with a 409 on the power, the personal and the partner
 * surface, in the request's language; the same value and an omitted one still save, the omitted one
 * keeping the stored relation; and a system write - here a workflow step moving the very relation -
 * still lands, since it never meets a controller.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentFixedAfterCreateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "fixedrel";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TIMESHEETS = API + "/timesheet/TimesheetController";
    private static final String MY_TIMESHEETS = API + "/timesheet/TimesheetMyController";
    private static final String EXPENSES = API + "/expense/ExpenseController";
    private static final String PARTNER_EXPENSES = API + "/expense/ExpensePartnerController";
    private static final String APPROVALS = API + "/approval/ApprovalController";
    private static final String EMPLOYEES = API + "/employee/EmployeeController";
    private static final String EXPENSE_FROM_TIMESHEET =
            "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/ExpenseFromTimesheetGenerate/run";
    private static final String SEEDED_UUID = "5f0c8a52-6b1e-4c4e-9a43-2d1e8b7c0001";
    private static final Pattern UUID_SHAPE = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final String CATALOG_KEY = "Timesheet_fixed_Project";
    private static final String REFUSAL = "'Project' cannot change once the Timesheet is created";
    private static final String REFUSAL_BG = "Проектът не може да се смени след създаването на отчета";
    private static final String FIELD_REFUSAL = "'Reference' cannot change once the Timesheet is created";
    private static final String FIELD_REFUSAL_BG = "Номерът не може да се смени след създаването на отчета";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String INVOICE_ITEMS = API + "/invoice/InvoiceItemController";
    private static final String MASTER_REFUSAL =
            "This InvoiceItem belongs to its Invoice - to move it, delete it and add it to the other Invoice";
    private static final String MASTER_REFUSAL_BG = "Редът принадлежи на своята фактура";
    private static final long TIMEOUT_SECONDS = 90;
    /** The workflow step runs once the create event has started its instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: fixedrel
            description: a timesheet's project and owner are picked on create and fixed afterwards
            languages: [en, bg]

            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: email, type: string, required: true, length: 100 }
                  # Loaded by the seed below with the UUID it carries (#8485).
                  - { name: uuid,  type: uuid, unique: true, major: false }

              - name: Project
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Timesheet
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: note,      type: string, length: 200 }
                  # Fields set on create and fixed afterwards (#8363): compared normalized, and by value.
                  - { name: reference, type: string, length: 20, normalize: upper, fixedAfterCreate: true }
                  - { name: rate,      type: decimal, fixedAfterCreate: true }
                  # Fixed once an approval references the row (#8194).
                  - { name: batch,     type: string, length: 20, fixedWhenReferenced: true }
                  # Issued by the server on create, whatever the payload carries (#8485).
                  - { name: uuid,      type: uuid, unique: true, major: false }
                relations:
                  - { name: Project,  kind: manyToOne, to: Project, required: true, fixedAfterCreate: true }
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true, fixedAfterCreate: true }

              # The partner surface guards it too.
              - name: Expense
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: note,      type: string, length: 200 }
                  - { name: reference, type: string, length: 20, normalize: upper, fixedAfterCreate: true }
                  - { name: batch,     type: string, length: 20, fixedWhenReferenced: true }
                  - { name: uuid,      type: uuid, unique: true, major: false }
                relations:
                  - { name: Project,  kind: manyToOne, to: Project, required: true, fixedAfterCreate: true }
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, partner: true }

              # References a timesheet and an expense, which fixes their batch (#8194).
              - name: Approval
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Timesheet, kind: manyToOne, to: Timesheet }
                  - { name: Expense,   kind: manyToOne, to: Expense }

              # A composition: a line belongs to the invoice it was created on, without declaring it (#8306).
              - name: Invoice
                function: Document
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: note,   type: string, length: 200 }
                  - { name: amount, type: decimal, aggregate: true }
              - name: InvoiceItem
                function: DocumentItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: note,   type: string, length: 200 }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

            processes:
              # A system writer of the fixed relation and field: the guard is the controllers', so these still land.
              - name: Reassign
                trigger: { onCreate: Timesheet }
                steps:
                  - { name: move,   kind: serviceTask, args: { setRelationField: Project, value: 2 } }
                  - { name: rename, kind: serviceTask, args: { setField: reference, value: SYS-1 } }
                  - { name: end,    kind: end }

            # A system writer of a uuid: the create-from mints an expense through the repository (#8485).
            generates:
              - name: expense-from-timesheet
                from: Timesheet
                to: Expense
                label: Expense
                map: { Project: Project, Employee: Employee, note: note }

            seeds:
              - name: employees
                entity: Employee
                rows:
                  - { id: 1, name: Admin, email: admin, uuid: 5f0c8a52-6b1e-4c4e-9a43-2d1e8b7c0001 }
              - name: projects
                entity: Project
                rows:
                  - { id: 1, name: Alpha }
                  - { id: 2, name: Beta }
                  - { id: 3, name: Gamma }
            """;

    /** A translator's overlay of the refusal, under the key the generated en-US catalog writes. */
    private static final String BG_OVERLAY = """
            {
              "fixedrel-model": {
                "checks": {
                  "Timesheet_fixed_Project": "Проектът не може да се смени след създаването на отчета",
                  "Timesheet_fixed_Reference": "Номерът не може да се смени след създаването на отчета",
                  "InvoiceItem_fixed_Invoice": "Редът принадлежи на своята фактура"
                }
              }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_relation_fixed_after_create_is_refused_to_a_user_update_and_still_written_by_the_system() {
        generateProject();
        assertGeneratedCatalog();
        assertGeneratedForms();
        writeResource(PROJECT_PATH + "/i18n/bg-BG/fixedrel.model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Picked on create, as on the New form.
        int timesheet = create(TIMESHEETS, "{\"Note\":\"week 1\",\"Project\":1,\"Employee\":1}");
        // The system write lands: the workflow step moves the fixed relation through the repository.
        awaitProject(timesheet, 2);

        // A user update that moves it is refused, in the reader's language.
        put(TIMESHEETS, timesheet, "{\"Note\":\"week 1\",\"Project\":3,\"Employee\":1}", null).statusCode(409)
                                                                                              .body("message", equalTo(REFUSAL));
        put(TIMESHEETS, timesheet, "{\"Note\":\"week 1\",\"Project\":3,\"Employee\":1}", "bg").statusCode(409)
                                                                                              .body("message", equalTo(REFUSAL_BG));
        // ...and so is one through the personal surface, and through the partner one.
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"week 1\",\"Project\":3}", null).statusCode(409)
                                                                                  .body("message", equalTo(REFUSAL));
        int expense = create(EXPENSES, "{\"Note\":\"taxi\",\"Project\":1,\"Employee\":1}");
        put(PARTNER_EXPENSES, expense, "{\"Note\":\"taxi\",\"Project\":3}", null).statusCode(409)
                                                                                 .body("message", equalTo(
                                                                                         "'Project' cannot change once the Expense is created"));
        put(PARTNER_EXPENSES, expense, "{\"Note\":\"taxi home\"}", null).statusCode(200)
                                                                        .body("Project", equalTo(1));

        // An ordinary edit still saves - it carries the relation back unchanged...
        put(TIMESHEETS, timesheet, "{\"Note\":\"edited\",\"Project\":2,\"Employee\":1}", null).statusCode(200);
        // ...and one that omits it keeps the stored value instead of erasing it.
        put(TIMESHEETS, timesheet, "{\"Note\":\"edited again\",\"Employee\":1}", null).statusCode(200);
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"mine\"}", null).statusCode(200);
        read(timesheet).body("Project", equalTo(2))
                       .body("Note", equalTo("mine"));
    }

    /**
     * A field set on create and fixed afterwards (#8363): the same guard as the relation's, comparing
     * the value as it is stored - normalized, and a decimal by value.
     */
    @Test
    void a_field_fixed_after_create_is_refused_to_a_user_update_and_still_written_by_the_system() {
        generateProject();
        assertGeneratedFieldForms();
        writeResource(PROJECT_PATH + "/i18n/bg-BG/fixedrel.model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Set on create, as on the New form.
        int timesheet = create(TIMESHEETS, "{\"Note\":\"week 1\",\"Project\":1,\"Employee\":1,\"Reference\":\"abc-1\",\"Rate\":100}");
        // The system write lands: the workflow step changes the fixed field through the repository.
        awaitProject(timesheet, 2);
        awaitReference(timesheet, "SYS-1");

        // A user update that changes it is refused, in the reader's language, on the personal surface too.
        String changed = "{\"Note\":\"week 1\",\"Project\":2,\"Employee\":1,\"Reference\":\"OTHER\",\"Rate\":100}";
        put(TIMESHEETS, timesheet, changed, null).statusCode(409)
                                                 .body("message", equalTo(FIELD_REFUSAL));
        put(TIMESHEETS, timesheet, changed, "bg").statusCode(409)
                                                 .body("message", equalTo(FIELD_REFUSAL_BG));
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"week 1\",\"Reference\":\"OTHER\"}", null).statusCode(409)
                                                                                            .body("message", equalTo(FIELD_REFUSAL));
        // A decimal compares by value: a different one is a change...
        put(TIMESHEETS, timesheet, "{\"Note\":\"week 1\",\"Project\":2,\"Employee\":1,\"Rate\":120}", null).statusCode(409)
                                                                                                           .body("message", equalTo(
                                                                                                                   "'Rate' cannot change once the Timesheet is created"));
        // ...and the partner surface refuses it as well.
        int expense = create(EXPENSES, "{\"Note\":\"taxi\",\"Project\":1,\"Employee\":1,\"Reference\":\"exp-1\"}");
        put(PARTNER_EXPENSES, expense, "{\"Note\":\"taxi\",\"Reference\":\"EXP-2\"}", null).statusCode(409)
                                                                                           .body("message", equalTo(
                                                                                                   "'Reference' cannot change once the Expense is created"));
        // The same value as stored once normalized passes - `exp-1` against a stored `EXP-1`.
        put(PARTNER_EXPENSES, expense, "{\"Note\":\"taxi home\",\"Reference\":\"exp-1\"}", null).statusCode(200)
                                                                                                .body("Reference", equalTo("EXP-1"));

        // `sys-1` against a stored `SYS-1` and `100` against a stored `100.00` are no change, and the
        // rest of the record still saves.
        put(TIMESHEETS, timesheet, "{\"Note\":\"edited\",\"Project\":2,\"Employee\":1,\"Reference\":\"sys-1\",\"Rate\":100}",
                null).statusCode(200);
        // An update that omits them keeps the stored values instead of erasing them.
        put(TIMESHEETS, timesheet, "{\"Note\":\"edited again\",\"Employee\":1}", null).statusCode(200);
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"mine\"}", null).statusCode(200);
        ValidatableResponse stored = read(timesheet).body("Reference", equalTo("SYS-1"))
                                                    .body("Note", equalTo("mine"));
        Object rate = stored.extract()
                            .path("Rate");
        assertEquals(0, new BigDecimal(String.valueOf(rate)).compareTo(new BigDecimal("100")), "Rate kept: " + rate);

        // A field fixed while referenced (#8194), on the personal and the partner surface: editable
        // until an approval references the row, a change refused from then on, the same value saved.
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"mine\",\"Batch\":\"B-1\"}", null).statusCode(200);
        create(APPROVALS, "{\"Timesheet\":" + timesheet + ",\"Expense\":" + expense + "}");
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"mine\",\"Batch\":\"B-2\"}", null).statusCode(409)
                                                                                    .body("message", equalTo(
                                                                                            "'Batch' cannot change while other records reference this Timesheet"));
        put(PARTNER_EXPENSES, expense, "{\"Note\":\"taxi\",\"Batch\":\"B-2\"}", null).statusCode(409)
                                                                                     .body("message", equalTo(
                                                                                             "'Batch' cannot change while other records reference this Expense"));
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"mine again\",\"Batch\":\"B-1\"}", null).statusCode(200);
    }

    /**
     * A composition child's master is fixed after create by default (#8306): a PUT of a line naming
     * another invoice used to move the line - and its amount - to that invoice. It is refused now, in
     * the reader's language, and neither invoice's total moves; a PUT repeating or omitting the invoice
     * still saves, and a line is still created on either invoice.
     */
    @Test
    void a_composition_childs_master_is_fixed_after_create() {
        generateProject();
        assertTrue(contentOf("i18n/en-US/fixedrel.model.json").contains("\"InvoiceItem_fixed_Invoice\": \"" + MASTER_REFUSAL + "\""),
                "the refusal's default text must be in the en-US catalog");
        writeResource(PROJECT_PATH + "/i18n/bg-BG/fixedrel.model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int first = create(INVOICES, "{\"Note\":\"supplier A\"}");
        int second = create(INVOICES, "{\"Note\":\"supplier B\"}");
        int line = create(INVOICE_ITEMS, "{\"Invoice\":" + first + ",\"Note\":\"goods\",\"Amount\":250}");
        awaitAmount(first, "250");

        // Naming the other invoice is refused, in the reader's language, and moves no money.
        String moved = "{\"Invoice\":" + second + ",\"Note\":\"goods\",\"Amount\":250}";
        put(INVOICE_ITEMS, line, moved, null).statusCode(409)
                                             .body("message", equalTo(MASTER_REFUSAL));
        put(INVOICE_ITEMS, line, moved, "bg").statusCode(409)
                                             .body("message", equalTo(MASTER_REFUSAL_BG));
        readItem(line).body("Invoice", equalTo(first));
        awaitAmount(first, "250");
        awaitAmount(second, "0");

        // Repeating the stored invoice saves, and the edit reaches its total.
        put(INVOICE_ITEMS, line, "{\"Invoice\":" + first + ",\"Note\":\"goods\",\"Amount\":300}", null).statusCode(200);
        awaitAmount(first, "300");
        // Omitting it keeps the stored invoice.
        put(INVOICE_ITEMS, line, "{\"Note\":\"more goods\",\"Amount\":300}", null).statusCode(200)
                                                                                  .body("Invoice", equalTo(first));
        readItem(line).body("Invoice", equalTo(first))
                      .body("Note", equalTo("more goods"));

        // A line is still created on either invoice.
        create(INVOICE_ITEMS, "{\"Invoice\":" + second + ",\"Note\":\"services\",\"Amount\":40}");
        awaitAmount(second, "40");
        awaitAmount(first, "300");
    }

    /**
     * A uuid field is issued by the server on every client create (#8485): the value a client posts -
     * any string, or another record's uuid - is discarded on the power, the personal and the partner
     * surface, and a PUT cannot change the one issued. A system writer keeps the repository's own fill,
     * so a create-from draws one for the record it mints, and data loaded below the application - a
     * seed, a demo CSV - keeps the UUID it was loaded with.
     */
    @Test
    void a_uuid_is_issued_by_the_server_on_create_and_never_taken_from_the_payload() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int timesheet = create(TIMESHEETS, "{\"Note\":\"week 1\",\"Project\":1,\"Employee\":1,\"Uuid\":\"not-a-uuid\"}");
        String issued = issuedUuid(TIMESHEETS, timesheet);
        int mine = create(MY_TIMESHEETS, "{\"Note\":\"week 2\",\"Project\":1,\"Uuid\":\"not-a-uuid\"}");
        issuedUuid(TIMESHEETS, mine);
        int expense = create(PARTNER_EXPENSES, "{\"Note\":\"taxi\",\"Project\":1,\"Uuid\":\"not-a-uuid\"}");
        issuedUuid(EXPENSES, expense);
        // Another record's uuid is not taken either - it was answered 409 by the unique key.
        int copy = create(TIMESHEETS, "{\"Note\":\"week 3\",\"Project\":1,\"Employee\":1,\"Uuid\":\"" + issued + "\"}");
        assertNotEquals(issued, issuedUuid(TIMESHEETS, copy), "a create must not take another record's uuid");

        // An update keeps the issued value, as it did.
        put(TIMESHEETS, timesheet, "{\"Note\":\"edited\",\"Employee\":1,\"Uuid\":\"not-a-uuid\"}", null).statusCode(200);
        assertEquals(issued, issuedUuid(TIMESHEETS, timesheet), "an update must not change the issued uuid");

        // A create-from draws one for the expense it mints. It answers its JSON as text, so the body
        // is parsed rather than path-extracted.
        AtomicReference<String> minted = new AtomicReference<>();
        restAssuredExecutor.execute(() -> minted.set(JsonPath.from(given().contentType("application/json")
                                                                          .body("{\"id\":" + timesheet + "}")
                                                                          .when()
                                                                          .post(EXPENSE_FROM_TIMESHEET)
                                                                          .then()
                                                                          .statusCode(200)
                                                                          .extract()
                                                                          .asString())
                                                             .getString("Uuid")),
                TIMEOUT_SECONDS);
        assertWellFormedUuid(minted.get());
        assertNotEquals(issued, minted.get(), "the minted expense must get its own uuid");

        // The seed's explicit UUID is kept.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(EMPLOYEES + "/1")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Uuid", equalTo(SEEDED_UUID)),
                TIMEOUT_SECONDS);
    }

    /**
     * The record's uuid, read back through the power controller, asserted to be a server-drawn UUID.
     */
    private String issuedUuid(String controller, int id) {
        AtomicReference<String> uuid = new AtomicReference<>();
        restAssuredExecutor.execute(() -> uuid.set(given().when()
                                                          .get(controller + "/" + id)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("Uuid")),
                TIMEOUT_SECONDS);
        assertWellFormedUuid(uuid.get());
        return uuid.get();
    }

    private static void assertWellFormedUuid(String value) {
        assertTrue(value != null && UUID_SHAPE.matcher(value)
                                              .matches(),
                "the uuid must be a server-drawn UUID, not the payload's value: " + value);
    }

    private ValidatableResponse readItem(int item) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(INVOICE_ITEMS + "/" + item)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
    }

    /** An invoice's aggregated amount, compared by value; an invoice with no line reads as zero. */
    private void awaitAmount(int invoice, String expected) {
        restAssuredExecutor.execute(() -> {
            Object amount = given().when()
                                   .get(INVOICES + "/" + invoice)
                                   .then()
                                   .statusCode(200)
                                   .extract()
                                   .path("Amount");
            BigDecimal actual = amount == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(amount));
            assertEquals(0, actual.compareTo(new BigDecimal(expected)), "invoice " + invoice + " amount: " + amount);
        }, TIMEOUT_SECONDS);
    }

    /**
     * The New form takes the fixed field and the Edit form renders it read-only, on every surface; a
     * field the record does not fix stays editable.
     */
    private void assertGeneratedFieldForms() {
        String timesheetForm = contentOf("gen/" + PROJECT + "/views/Timesheet/Timesheet-form.html");
        assertTrue(elementAt(timesheetForm, "id=\"f_Reference\"").contains(":readonly=\"isEdit\""), timesheetForm);
        assertTrue(elementAt(timesheetForm, "id=\"f_Rate\"").contains(":readonly=\"isEdit\""), timesheetForm);
        assertFalse(elementAt(timesheetForm, "id=\"f_Note\"").contains("isEdit"), timesheetForm);
        String myForm = contentOf("gen/" + PROJECT + "/views/my/Timesheet-form.html");
        assertTrue(elementAt(myForm, "x-model=\"form.Reference\"").contains(":readonly=\"mode === 'edit'\""), myForm);
        assertFalse(elementAt(myForm, "x-model=\"form.Note\"").contains("mode === 'edit'"), myForm);
        String partnerForm = contentOf("gen/" + PROJECT + "/views/partner/Expense-form.html");
        assertTrue(elementAt(partnerForm, "x-model=\"form.Reference\"").contains(":readonly=\"mode === 'edit'\""), partnerForm);
    }

    private void awaitReference(int timesheet, String reference) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TIMESHEETS + "/" + timesheet)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Reference", equalTo(reference)),
                PROCESS_TIMEOUT_SECONDS);
    }

    /**
     * The New form offers the picker and the Edit form renders it read-only, on every surface; the
     * relation the record does not fix stays editable.
     */
    private void assertGeneratedForms() {
        String expenseForm = contentOf("gen/" + PROJECT + "/views/Expense/Expense-form.html");
        assertTrue(elementAt(expenseForm, "x-h-select-input id=\"f_Project\"").contains(":disabled=\"isPreview || isEdit\""), expenseForm);
        assertTrue(elementAt(expenseForm, "x-h-select-input id=\"f_Employee\"").contains(":disabled=\"isPreview\""), expenseForm);
        String myForm = contentOf("gen/" + PROJECT + "/views/my/Timesheet-form.html");
        assertTrue(elementAt(myForm, "x-model=\"form.Project\"").contains(":disabled=\"mode === 'edit'\""), myForm);
        String partnerForm = contentOf("gen/" + PROJECT + "/views/partner/Expense-form.html");
        assertTrue(elementAt(partnerForm, "x-model=\"form.Project\"").contains(":disabled=\"mode === 'edit'\""), partnerForm);
    }

    /** The element a marker opens, up to its self-closing end - the picker's attributes may wrap. */
    private static String elementAt(String content, String marker) {
        int start = content.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("no element with " + marker + " in: " + content);
        }
        return content.substring(start, content.indexOf("/>", start));
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    /** The refusal's default text is in the en-US catalog under its stable key, for a translator. */
    private void assertGeneratedCatalog() {
        IResource catalog = repository.getResource(PROJECT_PATH + "/i18n/en-US/fixedrel.model.json");
        assertTrue(catalog.exists(), "the generated en-US catalog is missing");
        String content = new String(catalog.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("\"" + CATALOG_KEY + "\": \"" + REFUSAL + "\""), content);
    }

    private ValidatableResponse put(String controller, int id, String body, String language) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(language).body(body)
                                                                        .when()
                                                                        .put(controller + "/" + id)
                                                                        .then()),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private ValidatableResponse read(int timesheet) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(TIMESHEETS + "/" + timesheet)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
    }

    private void awaitProject(int timesheet, int project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TIMESHEETS + "/" + timesheet)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Project", equalTo(project)),
                PROCESS_TIMEOUT_SECONDS);
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private static RequestSpecification request(String language) {
        RequestSpecification request = given().contentType("application/json");
        return language == null ? request : request.header("Accept-Language", language);
    }

    private void generateProject() {
        writeResource(PROJECT_PATH + "/app.intent", INTENT_YAML);
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeResource(String path, String content) {
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
