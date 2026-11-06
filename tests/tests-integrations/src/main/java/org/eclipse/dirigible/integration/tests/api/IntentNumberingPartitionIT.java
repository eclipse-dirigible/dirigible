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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
 * Two companies allocate from one {@code per: Company} series and both start at 1 - at runtime,
 * through the published application.
 *
 * <p>
 * The partition fix of #7101 was pinned only by assertions over the emitted DAO and glue descriptor
 * (issue #7162), which is why it could go dead without a red test: the marker the templates used to
 * read moved into the {@code applyDefaults} the repository runs first (#7147), and nothing executed
 * the allocation to notice. The defect it exists for is silent in exactly the same way - the
 * default company's documents number on the series' BASE row while the second company's first
 * document forks from that counter (`VAC0000009` for a company that had never issued anything) - so
 * the assertion has to be the allocated string itself.
 *
 * <p>
 * The two series here differ only in the {@code init:} on the partition relation, and the second
 * one is the control that reproduces the defect: with nothing to resolve an unset FK to, its
 * documents run the tenant-wide base row up, and the partition materialized afterwards for the
 * second company forks from that counter instead of starting at 1 (#6517). That is exactly what the
 * {@code init:} series did before the fix, so the control also keeps the four assertions above from
 * passing on a build that had simply stopped partitioning.
 *
 * <p>
 * The same fixture pins that a create the database refuses after its number was drawn spends none
 * (#8113) - on every generated create path.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentNumberingPartitionIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "numbering";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String REQUESTS = API + "/request/RequestController";
    private static final String TICKETS = API + "/ticket/TicketController";
    private static final String VOUCHERS = API + "/voucher/VoucherController";
    private static final String CLAIMS = API + "/claim/ClaimController";
    private static final String MY_CLAIMS = API + "/claim/ClaimMyController";
    private static final String PARTNER_CLAIMS = API + "/claim/ClaimPartnerController";
    private static final String CLAIM_FROM_TRIP = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/ClaimFromTripGenerate/run";
    /** Past what the claim's DECIMAL(5, 2) amount holds: refused by the insert, after the draw. */
    private static final String OVERFLOW = "123456";
    private static final long TIMEOUT_SECONDS = 90;

    /**
     * Both series are provisioned for the tenant at publish. Prefix + total width 8 → {@code REQ00001}
     * / {@code TCK00001}, so the assertions can read the sequence off the tail.
     */
    private static final String NUMBERS_JSON = """
            {"series": [
              {"name": "Partitioned Request", "prefix": "REQ", "size": 8},
              {"name": "Partitioned Ticket",  "prefix": "TCK", "size": 8},
              {"name": "Refused Voucher",     "prefix": "VCH", "size": 8},
              {"name": "Refused Claim",       "prefix": "CLM", "size": 8}
            ]}
            """;

    private static final String INTENT_YAML = """
            name: numbering
            description: partitioned numbering fixture - every company's own counter starts at 1

            entities:
              - name: Company
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              # The reported shape: the partition relation carries the default company as its
              # init:, so a document filed without one still belongs to a company.
              - name: Request
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Partitioned Request, per: Company, stampOn: create } }
                  - { name: note,   type: string, length: 200 }
                relations:
                  - { name: Company, kind: manyToOne, to: Company, init: 1 }

              # The control: the same partitioned series on a relation with NO init:, where an
              # unset FK is the tenant-wide base row by design.
              - name: Ticket
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Partitioned Ticket, per: Company, stampOn: create } }
                  - { name: note,   type: string, length: 200 }
                relations:
                  - { name: Company, kind: manyToOne, to: Company }

              # A create the database refuses after the draw (#8113): a duplicate ref, an amount past
              # its column. The voucher has no to-one, so no reference check wraps its insert in a unit
              # of work - the claim has, and the owner relations that give it a my and a partner surface.
              - name: Voucher
                label: "{number}"
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Refused Voucher, stampOn: create } }
                  - { name: ref,    type: string, length: 20, unique: true }
                  - { name: amount, type: decimal, precision: 5, scale: 2 }

              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string, required: true, length: 100 }

              - name: Claim
                label: "{number} {ref}"
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Refused Claim, per: Company, stampOn: create } }
                  - { name: ref,    type: string, length: 20, unique: true }
                  - { name: amount, type: decimal, precision: 5, scale: 2 }
                relations:
                  - { name: Company,  kind: manyToOne, to: Company, init: 1 }
                  - { name: Employee, kind: manyToOne, to: Employee, personal: true }
                  - { name: Partner,  kind: manyToOne, to: Employee, partner: true }

              - name: Trip
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 20 }

            generates:
              - name: claim-from-trip
                from: Trip
                to: Claim
                label: Claim
                map: { ref: name }
                prompt:
                  - { field: amount }

            seeds:
              - name: companies
                entity: Company
                rows:
                  - { id: 1, name: Default }
                  - { id: 2, name: Second }
              - name: employees
                entity: Employee
                rows:
                  - { id: 1, email: admin }
              - name: trips
                entity: Trip
                rows:
                  - { id: 1, name: T-1 }
                  - { id: 2, name: A }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void every_company_numbers_in_its_own_partition_and_both_start_at_one() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The default company's first document - filed with no company at all, the way the UI files
        // one when the field is left to its default.
        assertEquals(1, sequenceOf(create(REQUESTS, "{\"Note\":\"default one\"}", 1)),
                "the default company's first document must open ITS partition, not the series' base row");

        // The second company's first document. This is the reported defect: it used to continue the
        // counter the default company had been running up on the base row.
        assertEquals(1, sequenceOf(create(REQUESTS, "{\"Note\":\"second one\",\"Company\":2}", 2)),
                "a company that has issued nothing must start at 1");

        // ...and the two counters run on independently of each other.
        assertEquals(2, sequenceOf(create(REQUESTS, "{\"Note\":\"default two\"}", 1)),
                "the default company's series must continue where ITS own last document left it");
        assertEquals(2, sequenceOf(create(REQUESTS, "{\"Note\":\"second two\",\"Company\":2}", 2)),
                "the second company's series must continue where ITS own last document left it");

        // The control - and the defect itself, reproduced. With no init: on the partition relation there
        // is nothing to resolve an unset FK to, so those documents run the tenant-wide BASE row up...
        assertEquals(1, sequenceOf(create(TICKETS, "{\"Note\":\"base one\"}", null)));
        assertEquals(2, sequenceOf(create(TICKETS, "{\"Note\":\"base two\"}", null)));
        // ...and a partition materialized afterwards forks from wherever that base row stands (#6517):
        // the second company's FIRST ticket is number 3. That is precisely what the default company's
        // documents did before #7101 - and what the four assertions above prove they no longer do, so
        // this control is what keeps them from passing on a build that had simply stopped partitioning.
        assertEquals(3, sequenceOf(create(TICKETS, "{\"Note\":\"second one\",\"Company\":2}", 2)),
                "a partition materialized after the base row advanced forks from it - the #7101 mechanism");
    }

    /**
     * A create the database refuses AFTER its number was drawn - a duplicate key (409), a value its
     * column cannot hold (500) - spends none (#8113). Both refusals come from the insert itself, past
     * every guard, and the counter used to commit before it: each left a permanent hole in a series
     * that must be gap-free. Every generated create path is driven - the power, my and partner
     * controllers and a create-from - and each must hand the next good create the very next number.
     */
    @Test
    void a_create_the_database_refuses_spends_no_number() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int voucher = sequenceOf(create(VOUCHERS, "{\"Ref\":\"A\"}", null));
        refuse(VOUCHERS, "{\"Ref\":\"A\"}", 409);
        refuse(VOUCHERS, "{\"Ref\":\"B\",\"Amount\":" + OVERFLOW + "}", 500);
        assertEquals(voucher + 1, sequenceOf(create(VOUCHERS, "{\"Ref\":\"B\"}", null)),
                "a create refused on an entity with no reference check spent a number");

        int first = sequenceOf(create(CLAIMS, "{\"Ref\":\"A\"}", 1));
        refuse(CLAIMS, "{\"Ref\":\"A\"}", 409);
        refuse(CLAIMS, "{\"Ref\":\"B\",\"Amount\":" + OVERFLOW + "}", 500);
        assertEquals(first + 1, sequenceOf(create(CLAIMS, "{\"Ref\":\"B\"}", 1)), "a create the power surface refused spent a number");

        refuse(MY_CLAIMS, "{\"Ref\":\"B\"}", 409);
        refuse(MY_CLAIMS, "{\"Ref\":\"C\",\"Amount\":" + OVERFLOW + "}", 500);
        assertEquals(first + 2, sequenceOf(create(MY_CLAIMS, "{\"Ref\":\"C\"}", 1)), "a create the my surface refused spent a number");

        refuse(PARTNER_CLAIMS, "{\"Ref\":\"C\"}", 409);
        refuse(PARTNER_CLAIMS, "{\"Ref\":\"D\",\"Amount\":" + OVERFLOW + "}", 500);
        assertEquals(first + 3, sequenceOf(create(PARTNER_CLAIMS, "{\"Ref\":\"D\"}", 1)),
                "a create the partner surface refused spent a number");

        // The create-from saves its header inside its own unit of work: Trip 2 maps the taken ref A, Trip 1
        // a fresh one with an amount past the column.
        refuse(CLAIM_FROM_TRIP, "{\"id\":2}", 500);
        refuse(CLAIM_FROM_TRIP, "{\"id\":1,\"values\":{\"Amount\":" + OVERFLOW + "}}", 500);
        assertEquals(first + 4, sequenceOf(create(CLAIMS, "{\"Ref\":\"E\"}", 1)), "a create-from the database refused spent a number");
    }

    /**
     * A {@code label:} that reads a {@code stampOn: create} number is stored with that number on the
     * create itself (#7822). The name used to be computed before the number was drawn, so every new
     * record kept an empty {@code Name} - the option text of every picker over it - until its next
     * write. Both insert shapes are driven: the voucher's plain insert and the claim's insert inside
     * the reference check's unit of work, on the power, my and partner surfaces and a create-from.
     */
    @Test
    void a_label_that_reads_the_number_is_stored_on_create() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        assertNameOnCreate(VOUCHERS, "{\"Ref\":\"N\"}", "");
        assertNameOnCreate(CLAIMS, "{\"Ref\":\"P\"}", " P");
        assertNameOnCreate(MY_CLAIMS, "{\"Ref\":\"M\"}", " M");
        assertNameOnCreate(PARTNER_CLAIMS, "{\"Ref\":\"Q\"}", " Q");
        // The create-from answers the saved claim: Trip 1 maps its name T-1 onto the claim's ref.
        assertNameOnCreate(CLAIM_FROM_TRIP, "{\"id\":1,\"values\":{\"Amount\":1}}", " T-1");
    }

    /** Creates a record and asserts its stored Name is its allocated number plus the given suffix. */
    private void assertNameOnCreate(String controller, String body, String suffix) {
        restAssuredExecutor.execute(() -> {
            Map<String, Object> created = given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(controller)
                                                 .then()
                                                 .statusCode(200)
                                                 .extract()
                                                 .jsonPath()
                                                 .getMap("$");
            String number = (String) created.get("Number");
            assertEquals(number + suffix, created.get("Name"), controller + " stored the label without the number it was given");
        }, TIMEOUT_SECONDS);
    }

    /** Posts a create the database must refuse with the given status. */
    private void refuse(String endpoint, String body, int status) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(endpoint)
                                                 .then()
                                                 .statusCode(status),
                TIMEOUT_SECONDS);
    }

    /** The sequence half of an allocated number - the three-letter prefix, then the padded counter. */
    private static int sequenceOf(String number) {
        return Integer.parseInt(number.substring(3));
    }

    /**
     * Creates a document and answers its allocated number, asserting the company the row ended up
     * carrying - the value the partition has to be resolved from.
     */
    private String create(String controller, String body, Integer expectedCompany) {
        AtomicReference<String> number = new AtomicReference<>();
        restAssuredExecutor.execute(() -> number.set(given().contentType("application/json")
                                                            .body(body)
                                                            .when()
                                                            .post(controller)
                                                            .then()
                                                            .statusCode(200)
                                                            .body("Company", equalTo(expectedCompany))
                                                            .extract()
                                                            .path("Number")),
                TIMEOUT_SECONDS);
        return number.get();
    }

    private void generateProject() {
        writeProjectFile("app.intent", INTENT_YAML);
        writeProjectFile(PROJECT + ".numbers", NUMBERS_JSON);
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

    private void writeProjectFile(String relativePath, String content) {
        String path = PROJECT_PATH + "/" + relativePath;
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
