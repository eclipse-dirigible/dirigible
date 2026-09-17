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
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentNumberingPartitionIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "numbering";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String REQUESTS = API + "/request/RequestController";
    private static final String TICKETS = API + "/ticket/TicketController";
    private static final long TIMEOUT_SECONDS = 90;

    /**
     * Both series are provisioned for the tenant at publish. Prefix + total width 8 → {@code REQ00001}
     * / {@code TCK00001}, so the assertions can read the sequence off the tail.
     */
    private static final String NUMBERS_JSON = """
            {"series": [
              {"name": "Partitioned Request", "prefix": "REQ", "size": 8},
              {"name": "Partitioned Ticket",  "prefix": "TCK", "size": 8}
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

            seeds:
              - name: companies
                entity: Company
                rows:
                  - { id: 1, name: Default }
                  - { id: 2, name: Second }
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
