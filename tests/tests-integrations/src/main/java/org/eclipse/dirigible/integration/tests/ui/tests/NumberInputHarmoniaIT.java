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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

/**
 * #8115: a generated number field keeps a zero typed after the decimal point.
 *
 * <p>
 * The generated form binds a number input with {@code x-model.number} inside
 * {@code x-h-input-number}, as the Harmonia reference prescribes. Alpine writes the model back into
 * the input whenever the model changes, and up to Harmonia 3.6.1 that write landed on the focused
 * field too: a field reading {@code 1.0} with a model of {@code 1} was rewritten to {@code 1}, the
 * next digit made {@code 15}, and the record was saved with that number (codbex/harmonia#149, fixed
 * in 3.6.2). The generated source is byte-correct either way - the defect lives in the running
 * directive - so this drives a real form and reads the saved record back.
 *
 * <p>
 * The keystrokes matter. Typed straight, {@code 1.05} never changes the model on Linux Chrome (it
 * is {@code 1} from the first digit on), so nothing is written back and 3.6.1 passes as well.
 * Typing {@code 10} and inserting the point before the {@code 0} moves the model from {@code 10} to
 * {@code 1} while the field reads {@code 1.0}: the write-back fires, and only 3.6.2 keeps the zero.
 */
class NumberInputHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "decimalit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/decimalit/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/decimalit/api/product/ProductController";

    private static final String TYPED = "1.05";

    /** The column's own scale is explicit, so only the input can lose the typed zero. */
    private static final String INTENT_YAML = """
            name: decimalit
            entities:
              - name: Product
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, length: 60 }
                  - { name: price, type: decimal, precision: 18, scale: 2 }
            """;

    /** What the Price input shows and what the form holds for it, as {@code <shown>|<model>}. */
    private static final String PRICE = """
            var input = document.getElementById('f_Price');
            if (!input) return 'not-rendered';
            var form = Alpine.$data(input).form;
            return input.value + '|' + (form ? JSON.stringify(form.Price) : 'no-form');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_typed_decimal_keeps_its_zero_and_is_saved_as_typed() {
        generateAndPublish();

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();
        browser.openPath(APP + "/Product/create");
        assertNotEquals("not-rendered", poll(state -> !"not-rendered".equals(state)), "the Product form must render");

        // 10, then the point inserted before the 0: the model moves from 10 to 1 under a field that
        // reads 1.0 - the write-back that used to rewrite the field.
        Selenide.$("#f_Price")
                .sendKeys("10", Keys.ARROW_LEFT, ".");
        assertEquals("1.0|1", poll("1.0|1"::equals), "the field must keep the typed 1.0 while the model is 1");
        Selenide.$("#f_Price")
                .sendKeys(Keys.END, "5");
        String expected = TYPED + "|" + TYPED;
        assertEquals(expected, poll(expected::equals), "the field must show the typed decimal and the model must hold it");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Create");

        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> rows = given().when()
                                                    .get(API)
                                                    .then()
                                                    .statusCode(200)
                                                    .extract()
                                                    .jsonPath()
                                                    .getList("$");
            assertEquals(1, rows.size(), "the created Product must be listed, found: " + rows);
            Object saved = rows.get(0)
                               .get("Price");
            assertEquals(0, new BigDecimal(TYPED).compareTo(new BigDecimal(String.valueOf(saved))), "saved Price: " + saved);
        }, 30);
    }

    private String poll(Predicate<String> settled) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(PRICE);
            if (value != null && settled.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value == null ? "" : value;
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
