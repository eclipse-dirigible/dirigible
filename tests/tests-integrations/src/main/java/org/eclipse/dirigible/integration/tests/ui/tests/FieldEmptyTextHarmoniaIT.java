/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * #8605: {@code emptyText:} names what an empty value reads as. A member with no loan limit reads
 * "Loan limit: No limit" in the Details rail - the row and the block shown, not hidden - and an
 * empty expiry or nickname reads "Never" / "Not set" on the record page and in the list instead of
 * the em dash; a loan not yet returned reads "Not returned" in the member's composition panel. The
 * text is the placeholder of the nickname input, and it is never stored. A member whose values are
 * set prints them as before.
 */
class FieldEmptyTextHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "emptytextit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/lending/index.html#";
    private static final String MEMBERS = "/services/java/" + PROJECT + "/gen/lending/api/member/MemberController";
    private static final String LOANS = "/services/java/" + PROJECT + "/gen/lending/api/member/LoanController";

    private static final String INTENT_YAML = """
            name: lending
            entities:
              - name: Member
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 80, required: true }
                  - { name: maxLoans,  type: integer, readOnly: true, label: Loan limit, emptyText: No limit }
                  - { name: expiresOn, type: date, label: Membership expires, emptyText: Never }
                  - { name: nickname,  type: string, length: 40, emptyText: Not set }
              - name: Loan
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: title,      type: string, length: 80 }
                  - { name: returnedOn, type: date, label: Returned on, emptyText: Not returned }
                relations:
                  - { name: Member, kind: manyToOne, to: Member, composition: true }
            """;

    /** The visible text of the generated app's page, or {@code pending} until the record has loaded. */
    private static final String RECORD_TEXT = """
            var root = document.querySelector('[x-data="MemberFormPage"]');
            if (!root || !window.Alpine || !Alpine.$data(root).loadedRecord) return 'pending';
            return root.innerText;
            """;

    /** The visible text of the list's table, or {@code pending} until it has rendered a row. */
    private static final String LIST_TEXT = """
            var cell = document.querySelector('table tbody tr td');
            return cell ? cell.closest('table').innerText : 'pending';
            """;

    /** The nickname input's placeholder on the edit form, or {@code pending} until it renders. */
    private static final String NICKNAME_PLACEHOLDER = """
            var input = document.getElementById('f_Nickname');
            return input ? (input.getAttribute('placeholder') || '') : 'pending';
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void an_empty_value_reads_as_its_empty_text_on_every_surface() {
        generateAndPublish();
        int empty = createMember(Map.of("Name", "Ada"));
        createLoan(empty, "Dune");
        int filled = createMember(Map.of("Name", "Grace", "ExpiresOn", "2027-01-31", "Nickname", "Amazing"));
        storeMaxLoans(filled, 5);
        ide.openHomePage();

        browser.openPath(APP + "/Member/" + empty + "/preview");
        String record = pollUntil(RECORD_TEXT, text -> !"pending".equals(text) && text.contains("Dune"));
        assertTrue(record.contains("Loan limit") && record.contains("No limit"),
                "the rail must show the Loan limit row with its emptyText, not hide it: " + record);
        assertTrue(record.contains("Membership expires") && record.contains("Never"),
                "an empty expiry must read Never on the record page: " + record);
        assertTrue(record.contains("Not set"), "an empty nickname must read Not set on the record page: " + record);
        assertTrue(record.contains("Not returned"), "a loan without a return date must read Not returned in the panel: " + record);

        // Only the hash changes between two records of one route, so the page is reloaded to read the other
        // one.
        browser.openPath(APP + "/Member/" + filled + "/preview");
        Selenide.refresh();
        String filledRecord = pollUntil(RECORD_TEXT, text -> !"pending".equals(text) && text.contains("Amazing"));
        assertTrue(filledRecord.contains("Loan limit") && filledRecord.contains("5"), "a set limit prints as before: " + filledRecord);
        assertFalse(filledRecord.contains("No limit") || filledRecord.contains("Never") || filledRecord.contains("Not set"),
                "a set value must never print its emptyText: " + filledRecord);

        browser.openPath(APP + "/Member");
        String list = pollUntil(LIST_TEXT, text -> !"pending".equals(text) && text.contains("Ada") && text.contains("Grace"));
        assertTrue(list.contains("No limit") && list.contains("Never") && list.contains("Not set"),
                "the list cells must read the emptyTexts for the empty member: " + list);
        assertTrue(list.contains("Amazing"), "the filled member's values print as before: " + list);

        browser.openPath(APP + "/Member/" + empty + "/edit");
        Selenide.refresh();
        assertEquals("Not set", pollUntil(NICKNAME_PLACEHOLDER, value -> !"pending".equals(value)),
                "the emptyText is the placeholder of the nickname input");

        AtomicReference<Map<String, Object>> served = new AtomicReference<>();
        restAssuredExecutor.execute(() -> served.set(given().when()
                                                            .get(MEMBERS + "/" + empty)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()
                                                            .getMap("")));
        assertNull(served.get()
                         .get("Nickname"),
                "the emptyText is never stored: " + served.get());
        assertNull(served.get()
                         .get("MaxLoans"),
                "the emptyText is never stored: " + served.get());
    }

    private int createMember(Map<String, Object> body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(body)
                                                        .when()
                                                        .post(MEMBERS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private void createLoan(int member, String title) {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("Member", member, "Title", title))
                                                 .when()
                                                 .post(LOANS)
                                                 .then()
                                                 .statusCode(200));
    }

    /** The loan limit is read-only - a REST create drops it - so it is written as a job would. */
    private void storeMaxLoans(int id, int maxLoans) {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement update =
                        connection.prepareStatement("UPDATE \"LENDING_MEMBER\" SET \"MEMBER_MAX_LOANS\" = ? WHERE \"MEMBER_ID\" = ?")) {
            update.setInt(1, maxLoans);
            update.setInt(2, id);
            assertEquals(1, update.executeUpdate(), "the seeded member must exist");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to store the loan limit", ex);
        }
    }

    private String pollUntil(String script, Predicate<String> done) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script);
            if (value != null && done.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
    }

    private void generateAndPublish() {
        String path = PROJECT_PATH + "/app.intent";
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
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
