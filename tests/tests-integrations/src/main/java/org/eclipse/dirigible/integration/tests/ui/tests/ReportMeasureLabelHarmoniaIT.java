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
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * A captioned report measure (#8671, #8621), in the browser: {@code { of: "count(*)", label: Lines
 * }} heads the report page's column and the dashboard list tile's column "Lines", while the
 * report's REST rows keep the derived key {@code Count} - and a bare measure keeps its derived
 * caption.
 */
class ReportMeasureLabelHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "reportmeasurelabelit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/shop/index.html";
    private static final String PAGE = "/services/web/" + PROJECT + "/gen/soldproducts/reports/SoldProducts/index.html";
    private static final String API = "/services/java/" + PROJECT + "/gen/soldproducts/api/reports/SoldProductsController";

    private static final String INTENT_YAML = """
            name: shop
            entities:
              - name: Product
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: OrderLine
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: product, kind: manyToOne, to: Product }
            reports:
              - name: SoldProducts
                source: OrderLine
                dimensions: [product.name]
                measures: [{ of: "count(*)", label: Lines }, "sum(quantity)"]
                widget: { kind: list, limit: 5 }
            seeds:
              - name: products
                entity: Product
                rows:
                  - { id: 1, name: Bolt }
              - name: order-lines
                entity: OrderLine
                rows:
                  - { id: 1, quantity: 3, product: 1 }
                  - { id: 2, quantity: 4, product: 1 }
            """;

    /** The report page's column headers once its rows rendered, joined by '|'. */
    private static final String PAGE_HEADERS = """
            var root = document.querySelector('[x-data="reportPage"]');
            if (!root || !window.Alpine || !Alpine.$data(root).rows.length) return '';
            return Array.from(root.querySelectorAll('thead th')).map(function (th) { return th.textContent.trim(); }).join('|');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeEach
    void generateAndPublish() {
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

    @Test
    void the_caption_heads_the_column_and_the_row_keeps_its_key() {
        // The REST rows keep the key derived from the expression: an existing reader is unaffected.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0]", hasKey("Count"))
                                                 .body("[0]", not(hasKey("Lines"))),
                60);

        ide.openHomePage();
        browser.openPath(PAGE);
        String headers = poll();
        assertEquals("Product Name|Lines|Sum Quantity", headers, "the captioned measure is headed by its label, the bare one as before");

        // The dashboard's list tile heads the same column from the same catalog entry.
        browser.openPath(APP + "#/");
        // The sign-in redirect drops the fragment, so the dashboard is opened again once the session
        // exists.
        browser.openPath(APP + "#/");
        Selenide.$x("//th[normalize-space()='Lines']")
                .shouldBe(Condition.visible);
        assertTrue(Selenide.$$x("//th[normalize-space()='Count']")
                           .isEmpty(),
                "the tile must not head the column with its row key");
    }

    private String poll() {
        long deadline = System.currentTimeMillis() + 60_000;
        String value = "";
        do {
            value = Selenide.executeJavaScript(PAGE_HEADERS);
            if (value != null && !value.isEmpty()) {
                return value;
            }
            Selenide.sleep(250);
        } while (System.currentTimeMillis() < deadline);
        return value;
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
