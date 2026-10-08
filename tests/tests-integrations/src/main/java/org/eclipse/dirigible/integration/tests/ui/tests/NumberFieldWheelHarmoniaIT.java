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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.WheelInput;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

/**
 * Scrolling over a focused number field scrolls the page and never changes the value (issue #7754).
 * A browser steps a focused {@code <input type="number">} on a wheel event over it and swallows the
 * event, so an Amount typed as 23 was saved as 18 by a user who only meant to scroll.
 *
 * <p>
 * This needs a real browser and a real wheel: the stepping is the browser's default action for a
 * trusted wheel event, which no synthetic {@code dispatchEvent} reproduces.
 */
class NumberFieldWheelHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "number-wheel-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/payments/index.html#";

    private static final int WHEEL_STEPS = 5;
    private static final int WHEEL_DELTA = 100;

    /** A number field first, then enough fields that the form overflows a laptop-sized window. */
    private static final String INTENT_YAML = """
            name: payments
            entities:
              - name: CustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                  - { name: reference, type: string }
                  - { name: payer, type: string }
                  - { name: payerIban, type: string }
                  - { name: payerBank, type: string }
                  - { name: payerEmail, type: string }
                  - { name: payerPhone, type: string }
                  - { name: street, type: string }
                  - { name: city, type: string }
                  - { name: postalCode, type: string }
                  - { name: country, type: string }
                  - { name: paidOn, type: date }
                  - { name: method, type: string }
                  - { name: fee, type: decimal }
                  - { name: note, type: text }
                  - { name: internalNote, type: text }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_wheel_over_a_focused_number_field_scrolls_the_page_and_keeps_the_value() {
        generateAndPublish();
        ide.openHomePage();
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(1280, 720));

        browser.openPath(APP + "/CustomerPayment/create");
        SelenideElement amount = Selenide.$("#f_Amount")
                                         .shouldBe(Condition.visible);
        amount.click();
        amount.sendKeys("23");
        assertEquals("23", amount.getValue());
        assertTrue((Boolean) Selenide.executeJavaScript("return document.activeElement === document.getElementById('f_Amount')"),
                "the Amount field must be focused before the wheel");
        long before = scrollTop();

        WebElement field = amount.toWebElement();
        for (int i = 0; i < WHEEL_STEPS; i++) {
            new Actions(WebDriverRunner.getWebDriver()).scrollFromOrigin(WheelInput.ScrollOrigin.fromElement(field), 0, WHEEL_DELTA)
                                                       .perform();
            Selenide.sleep(100);
        }
        Selenide.sleep(500);

        assertEquals("23", amount.getValue(), "the wheel over the focused Amount must not step its value");
        long after = scrollTop();
        assertTrue(after > before,
                "the wheel over the focused Amount must scroll the page: #app.scrollTop was " + before + ", is " + after);

        // The keyboard still steps the field.
        amount.click();
        amount.sendKeys(Keys.ARROW_UP);
        assertEquals("24", amount.getValue(), "the arrow keys must still step a number field");
    }

    private static long scrollTop() {
        Number top = Selenide.executeJavaScript("return document.getElementById('app').scrollTop");
        return top == null ? 0 : top.longValue();
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
