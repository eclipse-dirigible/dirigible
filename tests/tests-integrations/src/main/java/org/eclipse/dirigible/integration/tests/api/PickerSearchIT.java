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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import io.restassured.http.ContentType;

/**
 * A relation picker over a large table lists a bounded first page and searches the rest on the
 * server (dirigible #8343): a sales invoice whose Customer picker listed 1,393 customers held its
 * page for 20 s. The generated controller's {@code /search} takes the {@code CONTAINS} condition
 * the picker sends as the user types - a case-insensitive substring of a text field, its wildcards
 * taken literally - and the generated form and document pages load their pickers through the shared
 * {@code pickerRows} / {@code searchPicker} instead of the whole target table. The helpers
 * themselves are run by {@code HarmoniaSearchedPickerIT}.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PickerSearchIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "picker-search";
    private static final String CUSTOMERS = "/services/java/" + PROJECT + "/gen/picker/api/customer/CustomerController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT = """
            name: picker
            description: picker-search fixture - pickers over a customer table

            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
              - name: Visit
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string,  length: 200 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: SalesInvoice
                function: Document
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string,  length: 200 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: SalesInvoiceLine
                function: DocumentItem
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string,  length: 200 }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void the_generated_pickers_search_their_target_on_the_server() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int acme = create("Acme Ltd");
        int acmePercent = create("ACME 100% Trading");
        int underscored = create("north_wind");
        create("Northwind");
        create("Other");

        // Case-insensitive, anywhere in the text.
        assertContains("acme", acme, acmePercent);
        assertContains("ltd", acme);
        // A wildcard the user types is a character, never a pattern.
        assertContains("100%", acmePercent);
        assertContains("_", underscored);
        assertContains("%", acmePercent);
        // The page a picker asks for is the page it gets.
        assertFound("{\"conditions\":[{\"propertyName\":\"Name\",\"operator\":\"CONTAINS\",\"value\":\"n\"}],\"$limit\":2}", 2);
        // Only a text field can be searched this way.
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"conditions\":[{\"propertyName\":\"Id\",\"operator\":\"CONTAINS\",\"value\":\"1\"}]}")
                                                 .post(CUSTOMERS + "/search")
                                                 .then()
                                                 .statusCode(400),
                TIMEOUT_SECONDS);

        assertPicker(contentOf("gen/picker/js/components/pages/Visit/VisitFormPage.js"),
                contentOf("gen/picker/views/Visit/Visit-form.html"));
        assertPicker(contentOf("gen/picker/js/components/pages/SalesInvoice/SalesInvoiceDocumentPage.js"),
                contentOf("gen/picker/views/SalesInvoice/SalesInvoice-document.html"));
    }

    private static void assertPicker(String page, String view) {
        assertTrue(page.contains("this.pickerRows('Customer', '/services/java/" + PROJECT + "/gen/picker/api/customer/CustomerController'"),
                "the Customer picker must list a bounded first page through pickerRows: " + page);
        assertFalse(page.contains("api.getAll('/services/java/" + PROJECT + "/gen/picker/api/customer/CustomerController'"),
                "the Customer picker must not load the whole customer table");
        assertTrue(page.contains("optionsFromCustomer(rows)"), "searchPicker maps the rows it finds through optionsFromCustomer");
        assertTrue(page.contains("await this.ensureHeldOptions();"), "a held customer beyond the first page must still show its label");
        assertTrue(view.contains("@input.debounce.300ms=\"searchPicker('Customer', $event.target.value)\""),
                "the Customer picker's search box must ask the server");
    }

    private void assertFound(String body, int size) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body(body)
                                                 .post(CUSTOMERS + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Id", hasSize(size)),
                TIMEOUT_SECONDS);
    }

    private void assertContains(String text, Integer... ids) {
        String body = "{\"conditions\":[{\"propertyName\":\"Name\",\"operator\":\"CONTAINS\",\"value\":\"" + text + "\"}]}";
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body(body)
                                                 .post(CUSTOMERS + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Id", containsInAnyOrder(ids)),
                TIMEOUT_SECONDS);
    }

    private int create(String name) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().when()
                                                        .contentType(ContentType.JSON)
                                                        .body("{\"Name\":\"" + name + "\"}")
                                                        .post(CUSTOMERS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(projectPath() + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private void generateProject() {
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

    private void writeIntent() {
        String path = projectPath() + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String projectPath() {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        if (repository.hasCollection(projectPath())) {
            repository.removeCollection(projectPath());
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
