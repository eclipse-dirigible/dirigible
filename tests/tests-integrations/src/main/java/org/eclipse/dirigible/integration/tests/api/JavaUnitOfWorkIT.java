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
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@code UnitOfWork} makes several entity writes one transaction: a failure anywhere in the block
 * leaves none of them behind.
 *
 * <p>
 * Each repository call is otherwise its own transaction, which is what let a create-from commit an
 * invoice header and flip its source to INVOICED and then fail on a line - a document that exists,
 * counts as the period's billing, and is missing what it was for (issue #7069). The control case
 * here runs the same two writes without the block, so the assertion is about the block and not
 * about the database happening to refuse both.
 *
 * <p>
 * The second test covers the read-your-own-writes half the block promises for the write-then-query-
 * then-targeted-update pattern every generated document repository runs: a header and its lines are
 * written, the lines are queried back by foreign key and the header re-summed through a targeted
 * mutation, all inside one unit. The header reload had to see the row the block just created and
 * the lines query the rows it just wrote, or the recompute wrote a total of zero over a document
 * whose lines were right (issue #7096).
 */
class JavaUnitOfWorkIT extends IntegrationTest {

    private static final String PROJECT = "JavaUnitOfWorkIT";
    private static final String CONTROLLER = "/services/java/" + PROJECT + "/ledger/EntryController";
    private static final String DOCUMENTS = "/services/java/" + PROJECT + "/ledger/DocumentController";
    private static final String[] TABLE_NAMES = {"UOW_LEDGER_ENTRY", "UOW_LINE", "UOW_DOCUMENT"};
    private static final long TIMEOUT_SECONDS = 30;
    private static final String ECHO_QUEUE = "uow-it-updated-echo";
    private static final long RECEIVE_TIMEOUT_MILLIS = 2000;

    @Autowired
    private IRepository repository;

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void a_failed_write_takes_the_whole_unit_of_work_with_it() {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // The first call, so it retries until the freshly compiled route is registered.
        assertGet(CONTROLLER + "/unit/pass/committed", 200, "written");
        assertGet(CONTROLLER + "/count/committed", 200, "2");

        // The refused second write rolls the first one back with it.
        assertGet(CONTROLLER + "/unit/fail/rolledback", 500);
        assertGet(CONTROLLER + "/count/rolledback", 200, "0");

        // Without the block the same pair leaves the first write behind - which is the defect, and
        // what makes the assertion above about the unit of work rather than about the failure.
        assertGet(CONTROLLER + "/nounit/fail/leftbehind", 500);
        assertGet(CONTROLLER + "/count/leftbehind", 200, "1");

        // A read inside the block sees the block's own uncommitted writes, so a guard that re-reads
        // the row it just wrote behaves as it would after a commit.
        assertGet(CONTROLLER + "/unit/reads/visible", 200, "visible");
    }

    @Test
    void a_document_summed_inside_the_unit_commits_with_the_sum_of_its_lines() {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // Header, two lines and a re-sum after each line, all in one block: the lines query inside the
        // block must see both lines the block wrote, and the targeted update of the header must find
        // the row the block inserted...
        String[] observed = new String[1];
        restAssuredExecutor.execute(() -> observed[0] = given().when()
                                                               .get(DOCUMENTS + "/document/unit/summed")
                                                               .then()
                                                               .statusCode(200)
                                                               .body(containsString("header=true lines=2 updated=1"))
                                                               .extract()
                                                               .asString(),
                TIMEOUT_SECONDS);
        String id = observed[0].substring("id=".length(), observed[0].indexOf(' '));

        // ...and what commits is the sum, not the zero the header was inserted with (issue #7096: a
        // create-from with items produced an invoice whose lines were right and whose total was 0).
        assertGet(DOCUMENTS + "/document/" + id + "/total", 200, "12");

        // The same chain in the shape a generated create-from has: a targeted flip on another entity
        // first, then a header and lines that each record their create event in the outbox.
        restAssuredExecutor.execute(() -> observed[0] = given().when()
                                                               .get(DOCUMENTS + "/document/unit/events/summed")
                                                               .then()
                                                               .statusCode(200)
                                                               .body(containsString("header=true lines=2 updated=1"))
                                                               .extract()
                                                               .asString(),
                TIMEOUT_SECONDS);
        String withEvents = observed[0].substring("id=".length(), observed[0].indexOf(' '));
        assertGet(DOCUMENTS + "/document/" + withEvents + "/total", 200, "12");
    }

    @Test
    void a_read_after_a_targeted_write_in_the_same_unit_sees_the_row_the_statement_left() {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // The topic-bearing re-sum, with the header loaded earlier in the same unit. The whole exchange
        // retries because a topic keeps nothing for a subscriber that is not there yet, and the client
        // classes are compiled and subscribed asynchronously.
        String[] observed = new String[1];
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .ignoreExceptions()
                  .until(() -> {
                      drainEcho();
                      restAssuredExecutor.execute(() -> observed[0] = given().when()
                                                                             .get(DOCUMENTS + "/document/unit/topic/summed")
                                                                             .then()
                                                                             .statusCode(200)
                                                                             .extract()
                                                                             .asString(),
                              TIMEOUT_SECONDS);
                      String payload = receiveEcho();
                      if (payload == null) {
                          return false;
                      }
                      observed[0] = observed[0] + " payload=" + payload;
                      return true;
                  });

        // The two reads the history block makes around the write: before it the header still carries the
        // zero it was inserted with, after it the sum of the lines - a trail entry that records a change
        // instead of before == after.
        assertTrue(observed[0].contains("updated=1 before=0 after=12"),
                "the read after the targeted write must be the row the statement left: " + observed[0]);

        // ...and the payload the topic carried is that same row, not the one the session was holding -
        // a roll-up or a notify {placeholder} downstream computes from exactly this.
        assertTrue(observed[0].contains("\"total\":12"), "the '-updated' payload must carry the written total: " + observed[0]);

        // What committed is the sum too, so the assertions above are about the read and not about a
        // mutation that never landed.
        String id = observed[0].substring("id=".length(), observed[0].indexOf(' '));
        assertGet(DOCUMENTS + "/document/" + id + "/total", 200, "12");
    }

    /** Empties the echo queue so an assertion cannot read a previous attempt's message. */
    private void drainEcho() {
        while (receiveEcho() != null) {
            // keep reading until the queue is empty
        }
    }

    private String receiveEcho() {
        try {
            return MessagingFacade.receiveFromQueue(ECHO_QUEUE, RECEIVE_TIMEOUT_MILLIS);
        } catch (RuntimeException nothingYet) {
            return null;
        }
    }

    private void assertGet(String path, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(path)
                                                 .then()
                                                 .statusCode(expectedStatus),
                TIMEOUT_SECONDS);
    }

    private void assertGet(String path, int expectedStatus, String expectedBody) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(path)
                                                 .then()
                                                 .statusCode(expectedStatus)
                                                 .body(containsString(expectedBody)),
                TIMEOUT_SECONDS);
    }

    /**
     * The fixture files go away with the Dirigible folder the base class wipes per test class; the
     * tables themselves would survive a local run against an unclean target and carry their rows into
     * the next one.
     */
    @AfterEach
    void dropTable() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            for (String table : TABLE_NAMES) {
                statement.execute("DROP TABLE IF EXISTS \"" + table + "\"");
            }
        }
    }
}
