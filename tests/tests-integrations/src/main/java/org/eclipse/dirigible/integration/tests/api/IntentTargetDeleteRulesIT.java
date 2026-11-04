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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

/**
 * dirigible #7547: what a DELETE of a referenced record does to the records still pointing at it -
 * {@code whenTargetDeleted: restrict | nullify | cascade} on a to-one, {@code restrict} when
 * nothing is declared - same-model and cross-model, end to end on two generated, published
 * projects.
 *
 * <p>
 * The issue's reproductions were both a 200 that left a dangling reference: an expense category
 * used by a claim, and an employee referenced by another module's expense claims. The target's
 * repository cannot know who references it, so the REFERENCING repository contributes an
 * {@code org.eclipse.dirigible.sdk.db.TargetDeleteRule} and every delete applies all of them: first
 * every restriction, before anything is written, then - in one transaction with the delete - every
 * nullify and cascade.
 *
 * <p>
 * dirigible #8100 is the write side of the same references: a create or an update naming a record
 * that does not exist - same-model, cross-model, a self-reference - answered 200 and stored a row
 * pointing at nothing, a numbered one with its number already drawn. It is now a 400 naming the
 * relation, decided before the number is allocated.
 *
 * <p>
 * dirigible #8252 is the two sides at once: both checks read before they write, so a create naming
 * a record and the delete of that record, arriving together, both passed - the delete counted no
 * reference yet, the create still found its target - and the create committed a reference to a
 * record that was gone. Now the delete locks its record before it counts, and the create locks the
 * records it names in the transaction it inserts in, so one of the two waits for the other and is
 * refused as it would be in sequence.
 *
 * <p>
 * dirigible #8194 is the update twin of {@code restrict}: a referenced currency's code was
 * rewritten by a plain PUT, changing what every document referencing it prints. A field declaring
 * {@code fixedWhenReferenced: true} is refused a change with a 409 while a record references the
 * row - the same referencing records, same-model and cross-model, the delete's rules count.
 *
 * <p>
 * dirigible #8114 is a cascade sweeping away a record its own delete refuses: the cascade deleted
 * the referencing record through its repository, which never meets the refusals its REST delete
 * meets, so a payout dated in a closed period - or in a status that freezes it - went with its
 * employee while deleting it directly was refused. The employee's delete is now refused with the
 * payout's own message and status, and nothing is deleted.
 */
class IntentTargetDeleteRulesIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    /** Owns the referenced master data. Its project name differs from the model alias on purpose. */
    private static final String OWNER = "rules-owner";
    /** References the owner's Employee under each rule, and a category of its own. */
    private static final String CONSUMER = "rules-consumer";

    private static final String OWNER_MODEL = "staff";
    private static final String CONSUMER_MODEL = "claims";

    private static final String OWNER_INTENT = """
            name: staff
            description: target-delete fixture - the owner of the referenced master data
            languages: [en, bg]

            entities:
              - name: Employee
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
                  - { name: code, type: string,  length: 20, fixedWhenReferenced: true }
                relations:
                  - { name: Manager, kind: manyToOne, to: Employee }
            """;

    private static final String CONSUMER_INTENT = """
            name: claims
            description: target-delete fixture - one relation per rule, the default among them

            uses:
              - { model: staff, project: rules-owner }

            entities:
              - name: ExpenseCategory
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: name,    type: string,  required: true, length: 100 }
                  - { name: ceiling, type: decimal, fixedWhenReferenced: true }
                relations:
                  - { name: Owner, kind: manyToOne, to: Employee, model: staff, fixedWhenReferenced: true }
              - name: ExpenseClaim
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, model: staff, required: true }
                  - { name: Reviewer, kind: manyToOne, to: Employee, model: staff, whenTargetDeleted: nullify }
                  - { name: Category, kind: manyToOne, to: ExpenseCategory }
              - name: Timesheet
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: hours, type: decimal }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, model: staff, required: true, whenTargetDeleted: cascade }
              - name: Advance
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string,  length: 100, number: { series: Advance, stampOn: create } }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, model: staff }
                  - { name: Approver, kind: manyToOne, to: Employee, model: staff, whenTargetDeleted: keep }
              - name: PayStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
              - name: PayPeriod
                period: { start: startDate, end: endDate, closedWhen: "Status == CLOSED" }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate,   type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: PayStatus, function: EntityStatus, init: 1 }
              - name: Payout
                immutableWhen: "Status == CLOSED"
                immutableInPeriod: { period: PayPeriod, date: paidOn }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: paidOn, type: date }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, model: staff, required: true, whenTargetDeleted: cascade }
                  - { name: Status,   kind: manyToOne, to: PayStatus, function: EntityStatus, init: 1 }

            seeds:
              - name: pay-statuses
                entity: PayStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }
            """;

    /** How many times the create/delete race is run; the unfixed platform lost it in most attempts. */
    private static final int RACE_ATTEMPTS = 20;
    private static final long RACE_TIMEOUT_SECONDS = 60;

    /** The default text of an employee code's refusal (#8194), and a translator's overlay of it. */
    private static final String CODE_REFUSAL = "'Code' cannot change while other records reference this Employee";
    private static final String CODE_REFUSAL_BG = "Кодът не може да се смени, докато други записи сочат служителя";
    private static final String OWNER_BG_OVERLAY = """
            {
              "staff-model": {
                "checks": {
                  "Employee_fixedWhenReferenced_Code": "Кодът не може да се смени, докато други записи сочат служителя"
                }
              }
            }
            """;

    /** The series the advances are numbered from: AD and a 7-digit sequence. */
    private static final String CONSUMER_NUMBERS = "{\"series\": [{\"name\": \"Advance\", \"prefix\": \"AD\", \"size\": 9}]}";

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    private String employees;
    private String categories;
    private String claims;
    private String timesheets;
    private String advances;
    private String payPeriods;
    private String payouts;

    @Test
    void a_targets_delete_applies_the_rules_of_the_records_referencing_it() {
        writeFile(OWNER, "app.intent", OWNER_INTENT);
        generateProject(OWNER);
        writeFile(OWNER, "i18n/bg-BG/" + OWNER_MODEL + ".model.json", OWNER_BG_OVERLAY);
        writeFile(CONSUMER, "app.intent", CONSUMER_INTENT);
        writeFile(CONSUMER, "claims.numbers", CONSUMER_NUMBERS);
        generateProject(CONSUMER);

        String claimRepository = contentOf(CONSUMER, "gen/" + CONSUMER_MODEL + "/data/expenseclaim/ExpenseClaimRepository.java");
        assertTrue(claimRepository.contains("implements org.eclipse.dirigible.sdk.db.TargetDeleteRule"),
                "the referencing repository must contribute its rules: " + claimRepository);

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        String ownerApi = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api";
        String consumerApi = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api";
        employees = ownerApi + "/employee/EmployeeController";
        categories = consumerApi + "/expensecategory/ExpenseCategoryController";
        claims = consumerApi + "/expenseclaim/ExpenseClaimController";
        timesheets = consumerApi + "/timesheet/TimesheetController";
        advances = consumerApi + "/advance/AdvanceController";
        payPeriods = consumerApi + "/payperiod/PayPeriodController";
        payouts = consumerApi + "/payout/PayoutController";

        assertRestrictIsTheDefaultSameModelAndCrossModel();
        assertNullifyClearsTheReference();
        assertCascadeDeletesTheReferencingRecord();
        assertARestrictionIsCheckedBeforeAnythingIsReleased();
        assertACascadeMeetsTheCascadedRecordsOwnRefusals();

        assertACreateNamingNoRecordIsRefused();
        assertAnUpdateNamingNoRecordIsRefused();
        assertAKeepReferenceOutlivesItsTarget();

        assertAFieldFixedWhenReferencedChangesOnlyWhileNothingReferencesTheRow();
        assertARelationFixedWhenReferencedChangesOnlyWhileNothingReferencesTheRow();

        assertACreateAndADeleteOfItsTargetNeverBothWin();
    }

    /**
     * #8252: a create naming an employee and the delete of that employee, released at once. Exactly one
     * may win - the claim stored and the delete refused, or the employee deleted and the claim refused
     * - and never both, which is the claim pointing at a record that is gone. Repeated, because a race
     * that is lost only sometimes proves nothing when it is not lost once.
     */
    private void assertACreateAndADeleteOfItsTargetNeverBothWin() {
        ExecutorService racers = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < RACE_ATTEMPTS; attempt++) {
                int target = create(employees, "{\"Name\":\"Racer " + attempt + "\"}");
                int[] outcome = restAssuredExecutor.executeWithResult(() -> race(racers, target));
                int posted = outcome[0];
                int deleted = outcome[1];
                String attemptSaid = "attempt " + attempt + ": POST " + posted + ", DELETE " + deleted;
                assertTrue(posted == 200 || posted == 400 || posted == 409, attemptSaid + " - the create must win or be refused");
                assertTrue(isSuccess(deleted) || deleted == 409, attemptSaid + " - the delete must win or be refused");
                assertTrue(posted != 200 || !isSuccess(deleted), attemptSaid + " - both won: the claim references a deleted employee");
            }
        } finally {
            racers.shutdownNow();
        }
    }

    /** One POST and one DELETE released by the same barrier; answers their two status codes. */
    private int[] race(ExecutorService racers, int target) {
        CyclicBarrier start = new CyclicBarrier(2);
        Future<Integer> post = racers.submit(() -> {
            start.await();
            return given().contentType("application/json")
                          .body("{\"Employee\":" + target + ",\"Amount\":1}")
                          .when()
                          .post(claims)
                          .statusCode();
        });
        Future<Integer> delete = racers.submit(() -> {
            start.await();
            return given().when()
                          .delete(employees + "/" + target)
                          .statusCode();
        });
        try {
            return new int[] {post.get(RACE_TIMEOUT_SECONDS, TimeUnit.SECONDS), delete.get(RACE_TIMEOUT_SECONDS, TimeUnit.SECONDS)};
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("interrupted while racing a create against a delete", e);
        } catch (Exception e) {
            throw new IllegalStateException("a racing request did not answer", e);
        }
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    /**
     * #8100: a create naming a record that does not exist - cross-model, same-model - is a 400 naming
     * the relation, and a refused numbered create draws no number: the next one is the series' first.
     */
    private void assertACreateNamingNoRecordIsRefused() {
        int eve = create(employees, "{\"Name\":\"Eve\"}");

        assertWriteRefused(claims, false, "{\"Employee\":999999,\"Amount\":10}", "Employee 999999 does not exist");
        assertWriteRefused(claims, false, "{\"Employee\":" + eve + ",\"Category\":99999,\"Amount\":10}", "Category 99999 does not exist");
        assertWriteRefused(advances, false, "{\"Employee\":999999,\"Amount\":10}", "Employee 999999 does not exist");

        int advance = create(advances, "{\"Employee\":" + eve + ",\"Amount\":10}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(advances + "/" + advance)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Number", equalTo("AD0000001")));
    }

    /** #8100: an update re-pointing a relation at nothing is refused - a self-reference included. */
    private void assertAnUpdateNamingNoRecordIsRefused() {
        int fay = create(employees, "{\"Name\":\"Fay\"}");
        int gus = create(employees, "{\"Name\":\"Gus\",\"Manager\":" + fay + "}");

        assertWriteRefused(employees + "/" + gus, true, "{\"Id\":" + gus + ",\"Name\":\"Gus\",\"Manager\":888888}",
                "Manager 888888 does not exist");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(employees + "/" + gus)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Manager", equalTo(fay)));
    }

    /**
     * A {@code keep} reference outlives its target by declaration (#7634) - a reversal posts the storno
     * of a deleted source naming the source it reverses - so it is not held to the target's existence:
     * the record stays editable once its approver is gone, while its checked relation still is.
     */
    private void assertAKeepReferenceOutlivesItsTarget() {
        int ida = create(employees, "{\"Name\":\"Ida\"}");
        int hal = create(employees, "{\"Name\":\"Hal\"}");
        int advance = create(advances, "{\"Employee\":" + ida + ",\"Approver\":" + hal + ",\"Amount\":5}");
        assertDeleted(employees + "/" + hal);

        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + advance + ",\"Employee\":" + ida + ",\"Approver\":" + hal
                                                         + ",\"Amount\":6}")
                                                 .when()
                                                 .put(advances + "/" + advance)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Approver", equalTo(hal)));
        assertWriteRefused(advances + "/" + advance, true,
                "{\"Id\":" + advance + ",\"Employee\":777777,\"Approver\":" + hal + ",\"Amount\":6}", "Employee 777777 does not exist");
    }

    /**
     * #8194: an employee's code, referenced cross-model by a claim and same-model by a report's
     * manager, and a category's ceiling (a decimal) referenced same-model by a claim. Editable while
     * nothing references the row; from the first reference on a change is a 409 in the reader's
     * language, while the same value - a decimal compared by value - and an omitted one still save;
     * editable again once the reference is gone.
     */
    private void assertAFieldFixedWhenReferencedChangesOnlyWhileNothingReferencesTheRow() {
        int kim = create(employees, "{\"Name\":\"Kim\",\"Code\":\"K-1\"}");
        String kimUrl = employees + "/" + kim;
        assertUpdated(kimUrl, "{\"Id\":" + kim + ",\"Name\":\"Kim\",\"Code\":\"K-2\"}", "Code", "K-2");

        // Cross-model: a claim of the consumer references her.
        int claim = create(claims, "{\"Employee\":" + kim + ",\"Amount\":1}");
        assertUpdateRefused(kimUrl, "{\"Id\":" + kim + ",\"Name\":\"Kim\",\"Code\":\"K-3\"}", null, CODE_REFUSAL);
        assertUpdateRefused(kimUrl, "{\"Id\":" + kim + ",\"Name\":\"Kim\",\"Code\":\"K-3\"}", "bg", CODE_REFUSAL_BG);
        assertUpdated(kimUrl, "{\"Id\":" + kim + ",\"Name\":\"Kim Lee\",\"Code\":\"K-2\"}", "Name", "Kim Lee");
        assertUpdated(kimUrl, "{\"Id\":" + kim + ",\"Name\":\"Kim Lee\"}", "Code", "K-2");

        // The reference gone, the code is editable again.
        assertDeleted(claims + "/" + claim);
        assertUpdated(kimUrl, "{\"Id\":" + kim + ",\"Name\":\"Kim Lee\",\"Code\":\"K-3\"}", "Code", "K-3");

        // Same-model: another employee names her as manager.
        int lou = create(employees, "{\"Name\":\"Lou\",\"Manager\":" + kim + "}");
        assertUpdateRefused(kimUrl, "{\"Id\":" + kim + ",\"Name\":\"Kim Lee\",\"Code\":\"K-4\"}", null, CODE_REFUSAL);

        // Same-model in the consumer, a decimal: 100.00 against a stored 100 is no change, 200 is one.
        int fuel = create(categories, "{\"Name\":\"Fuel\",\"Ceiling\":100}");
        String fuelUrl = categories + "/" + fuel;
        assertUpdated(fuelUrl, "{\"Id\":" + fuel + ",\"Name\":\"Fuel\",\"Ceiling\":150}", "Name", "Fuel");
        create(claims, "{\"Employee\":" + lou + ",\"Category\":" + fuel + ",\"Amount\":1}");
        assertUpdated(fuelUrl, "{\"Id\":" + fuel + ",\"Name\":\"Fuel & oil\",\"Ceiling\":150.00}", "Name", "Fuel & oil");
        assertUpdateRefused(fuelUrl, "{\"Id\":" + fuel + ",\"Name\":\"Fuel\",\"Ceiling\":200}", null,
                "'Ceiling' cannot change while other records reference this ExpenseCategory");
    }

    /**
     * #8194 on a to-one - a bank account's currency, a product's type: a category's owner, a
     * cross-model relation, is re-pickable while no claim uses the category and fixed once one does;
     * the same id still saves.
     */
    private void assertARelationFixedWhenReferencedChangesOnlyWhileNothingReferencesTheRow() {
        int ann = create(employees, "{\"Name\":\"Ann\"}");
        int ben = create(employees, "{\"Name\":\"Ben\"}");
        int tools = create(categories, "{\"Name\":\"Tools\",\"Owner\":" + ann + "}");
        String toolsUrl = categories + "/" + tools;
        assertUpdated(toolsUrl, "{\"Id\":" + tools + ",\"Name\":\"Tools\",\"Owner\":" + ben + "}", "Owner", ben);

        create(claims, "{\"Employee\":" + ann + ",\"Category\":" + tools + ",\"Amount\":1}");
        assertUpdateRefused(toolsUrl, "{\"Id\":" + tools + ",\"Name\":\"Tools\",\"Owner\":" + ann + "}", null,
                "'Owner' cannot change while other records reference this ExpenseCategory");
        assertUpdated(toolsUrl, "{\"Id\":" + tools + ",\"Name\":\"Tools & parts\",\"Owner\":" + ben + "}", "Name", "Tools & parts");
    }

    private void assertUpdated(String url, String body, String property, Object expected) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(url)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(property, equalTo(expected)));
    }

    private void assertUpdateRefused(String url, String body, String language, String message) {
        restAssuredExecutor.execute(() -> {
            var request = given().contentType("application/json")
                                 .body(body);
            if (language != null) {
                request = request.header("Accept-Language", language);
            }
            request.when()
                   .put(url)
                   .then()
                   .statusCode(409)
                   .body("message", equalTo(message));
        });
    }

    private void assertWriteRefused(String url, boolean update, String body, String message) {
        restAssuredExecutor.execute(() -> {
            var request = given().contentType("application/json")
                                 .body(body)
                                 .when();
            String answer = (update ? request.put(url) : request.post(url)).then()
                                                                           .statusCode(400)
                                                                           .extract()
                                                                           .asString();
            assertTrue(answer.contains(message), "the 400 must name the relation and the missing id; got: " + answer);
        });
    }

    /** The issue's two reproductions, with nothing declared: both deletes are now refused. */
    private void assertRestrictIsTheDefaultSameModelAndCrossModel() {
        int ada = create(employees, "{\"Name\":\"Ada\"}");
        int travel = create(categories, "{\"Name\":\"Travel\"}");
        int claim = create(claims, "{\"Employee\":" + ada + ",\"Category\":" + travel + ",\"Amount\":12.5}");

        assertRefused(employees + "/" + ada, "This Employee is referenced by 1 Expense Claim record(s) and cannot be deleted");
        assertRefused(categories + "/" + travel, "This Expense Category is referenced by 1 Expense Claim record(s) and cannot be deleted");

        // With the claim gone, nothing restricts either delete any more.
        assertDeleted(claims + "/" + claim);
        assertDeleted(employees + "/" + ada);
        assertDeleted(categories + "/" + travel);
    }

    /** nullify: the employee goes, the claim stays and no longer names a reviewer. */
    private void assertNullifyClearsTheReference() {
        int owner = create(employees, "{\"Name\":\"Owen\"}");
        int bob = create(employees, "{\"Name\":\"Bob\"}");
        int claim = create(claims, "{\"Employee\":" + owner + ",\"Reviewer\":" + bob + ",\"Amount\":7}");

        assertDeleted(employees + "/" + bob);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(claims + "/" + claim)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Reviewer", nullValue())
                                                 .body("Employee", equalTo(owner)));
    }

    /** cascade: the employee and the timesheet referencing it go together. */
    private void assertCascadeDeletesTheReferencingRecord() {
        int cy = create(employees, "{\"Name\":\"Cy\"}");
        int timesheet = create(timesheets, "{\"Employee\":" + cy + ",\"Hours\":8}");

        assertDeleted(employees + "/" + cy);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(timesheets + "/" + timesheet)
                                                 .then()
                                                 .statusCode(404));
    }

    /**
     * One employee both raising a claim (restrict) and reviewing it (nullify): the refusal comes first,
     * so the reviewer is NOT cleared by a delete that is then refused.
     */
    private void assertARestrictionIsCheckedBeforeAnythingIsReleased() {
        int dee = create(employees, "{\"Name\":\"Dee\"}");
        int claim = create(claims, "{\"Employee\":" + dee + ",\"Reviewer\":" + dee + ",\"Amount\":3}");

        assertRefused(employees + "/" + dee, "This Employee is referenced by 1 Expense Claim record(s) and cannot be deleted");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(claims + "/" + claim)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Reviewer", equalTo(dee)));
    }

    /**
     * #8114, the issue's walk: a payout booked into an open period, the period closed, then its
     * employee deleted. The payout's direct delete is refused 409 by the period lock, and the cascade
     * now refuses the employee's delete with the same message, the payout kept - likewise for a payout
     * frozen by its status ({@code immutableWhen}).
     */
    private void assertACascadeMeetsTheCascadedRecordsOwnRefusals() {
        int period = create(payPeriods, "{\"StartDate\":\"2031-01-01\",\"EndDate\":\"2031-01-31\",\"Status\":1}");
        int kim = create(employees, "{\"Name\":\"Kim\"}");
        int booked = create(payouts, "{\"Employee\":" + kim + ",\"PaidOn\":\"2031-01-17\",\"Amount\":34}");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + period
                                                         + ",\"StartDate\":\"2031-01-01\",\"EndDate\":\"2031-01-31\",\"Status\":2}")
                                                 .when()
                                                 .put(payPeriods + "/" + period)
                                                 .then()
                                                 .statusCode(200));

        String closed = "The PayPeriod covering this date is closed - book the correction in an open period";
        assertRefused(payouts + "/" + booked, closed);
        assertRefused(employees + "/" + kim, closed);
        assertFound(payouts + "/" + booked);

        int lee = create(employees, "{\"Name\":\"Lee\"}");
        int settled = create(payouts, "{\"Employee\":" + lee + ",\"Amount\":5,\"Status\":2}");
        String frozen = "This Payout is immutable in its current status - corrections go through the workflow";
        assertRefused(payouts + "/" + settled, frozen);
        assertRefused(employees + "/" + lee, frozen);
        assertFound(payouts + "/" + settled);
    }

    private void assertFound(String record) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(record)
                                                 .then()
                                                 .statusCode(200));
    }

    private void assertRefused(String record, String message) {
        restAssuredExecutor.execute(() -> {
            String body = given().when()
                                 .delete(record)
                                 .then()
                                 .statusCode(409)
                                 .extract()
                                 .asString();
            assertTrue(body.contains(message), "the 409 must say why the delete is refused; got: " + body);
        });
        assertFound(record);
    }

    private void assertDeleted(String record) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(record)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
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
                60);
        return id.get();
    }

    /**
     * Generate the model files from the intent and drive model-to-code from the generate response's own
     * plan.
     */
    private void generateProject(String project) {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + project + "&path=app.intent")
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

    private void publishProject(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeFile(String project, String fileName, String content) {
        String path = projectPath(project) + "/" + fileName;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
        for (String project : List.of(CONSUMER, OWNER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
    }
}
