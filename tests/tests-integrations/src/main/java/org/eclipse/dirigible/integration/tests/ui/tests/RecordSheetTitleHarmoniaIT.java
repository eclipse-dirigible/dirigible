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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;

import io.restassured.http.ContentType;

/**
 * #8570: the title of a manage list's record sheet keeps its height when the record's properties
 * overflow the sheet.
 *
 * <p>
 * The sheet body is a scrolling flex column, and its title is {@code truncate} ({@code overflow:
 * hidden}), so the title's automatic minimum height is 0: with the default {@code flex-shrink: 1}
 * the overflow was taken out of the title, which collapsed to 0 px instead of the body scrolling. A
 * layout question, so it needs a real browser - in a window short enough for the record not to fit.
 */
class RecordSheetTitleHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final int FIELD_COUNT = 40;

    private static final String PROJECT = "sheettitleit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/sheettitle/index.html#";
    private static final String PRODUCTS = "/services/java/" + PROJECT + "/gen/sheettitle/api/product/ProductController";

    private static final String PAGE = "Alpine.$data(document.querySelector('[x-data=\"ProductManageListPage\"]'))";

    /** The list as the user sees it once it has loaded: {@code <state>;rows=<n>;drawer=<open>}. */
    private static final String LIST_STATE = """
            var root = document.querySelector('[x-data="ProductManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading') return 'pending';
            return page.state + ';rows=' + page.items.length + ';drawer=' + page.detailsOpen;
            """;

    /**
     * The open sheet's title and body: {@code <title text>;overflows=<body scrolls>;title=<px>}, or
     * {@code no-sheet} while the title has not rendered its text.
     */
    private static final String SHEET_LAYOUT = """
            var title = document.querySelector('[x-h-sheet] h2');
            if (!title || !title.textContent.trim()) return 'no-sheet';
            var body = title.parentElement;
            return title.textContent.trim() + ';overflows=' + (body.scrollHeight > body.clientHeight)
                + ';title=' + Math.round(title.getBoundingClientRect().height);
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_record_sheet_keeps_its_title_when_the_record_overflows_the_sheet() {
        generateAndPublish();
        createProduct();
        ide.openHomePage();
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(1280, 600));
        browser.openPath(APP + "/Product");
        assertEquals("default;rows=1;drawer=false", poll(LIST_STATE, "default;rows=1;drawer=false"), "the list must render the product");

        // Open the product in the sheet, as a click on its row does.
        Selenide.executeJavaScript(PAGE + ".selectRow(" + PAGE + ".items[0]);");
        String layout = pollUntilRendered();
        assertTrue(layout.contains(";overflows=true;"),
                "the record's properties must overflow the sheet in this window, or the test proves nothing: " + layout);
        int titleHeight = titleHeight(layout);
        assertTrue(titleHeight > 0, "the sheet's title must keep its height while the body scrolls: " + layout);

        // Control: without shrink-0 the same title collapses - the window is short enough to matter.
        Selenide.executeJavaScript("document.querySelector('[x-h-sheet] h2').classList.remove('shrink-0');");
        assertEquals(0, titleHeight(Selenide.executeJavaScript(SHEET_LAYOUT)),
                "without shrink-0 the title must collapse in this window, else the assertion above guards nothing");
    }

    private static int titleHeight(String layout) {
        return Integer.parseInt(layout.substring(layout.lastIndexOf("title=") + "title=".length()));
    }

    private String pollUntilRendered() {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(SHEET_LAYOUT);
            if (!"no-sheet".equals(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
    }

    private String poll(String script, String expected) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(script);
            if (expected.equals(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
    }

    private void createProduct() {
        Map<String, Object> product = new LinkedHashMap<>();
        IntStream.rangeClosed(1, FIELD_COUNT)
                 .forEach(i -> product.put(String.format("Attribute%02d", i), "value " + i));
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(product)
                                                 .when()
                                                 .post(PRODUCTS)
                                                 .then()
                                                 .statusCode(200));
    }

    private static String intentYaml() {
        String fields = IntStream.rangeClosed(1, FIELD_COUNT)
                                 .mapToObj(i -> String.format("      - { name: attribute%02d, type: string }%n", i))
                                 .collect(Collectors.joining());
        return """
                name: sheettitle
                entities:
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """ + fields;
    }

    private void generateAndPublish() {
        String path = PROJECT_PATH + "/app.intent";
        byte[] content = intentYaml().getBytes(StandardCharsets.UTF_8);
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(content);
        } else {
            repository.createResource(path, content);
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
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(1920, 1080));
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
