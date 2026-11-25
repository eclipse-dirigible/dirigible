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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
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
import io.restassured.path.json.JsonPath;

/**
 * #7820: the prompt dialog of a create-from whose target is no detail of the button's view reads in
 * the user's language. Its captions ride the action descriptor ({@code promptColumns}), and each
 * carries the key the target's own form is translated by - the catalog of the model declaring the
 * property: this module's for a local standalone target, the owner's for a cross-model one.
 *
 * <p>
 * This needs a real browser: the captions are resolved by the shared shell runtime through the i18n
 * catalogs, in the language the user picked - and the owner's catalog is one the consuming app's
 * shell loads only because the dialog asks for it.
 */
class GeneratesPromptI18nHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String WORKSPACE = "workspace";
    private static final String OWNER = "gpi18npay";
    private static final String CONSUMER = "gpi18ninv";
    private static final String APP = "/services/web/" + CONSUMER + "/gen/" + CONSUMER + "/index.html#";
    private static final String INVOICES = "/services/java/" + CONSUMER + "/gen/" + CONSUMER + "/api/invoice/InvoiceController";

    /** The owner module: the payment a consuming module records. */
    private static final String OWNER_INTENT = """
            name: gpi18npay
            languages: [en, bg]
            entities:
              - name: Payment
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: amount,    type: decimal }
                  - { name: reference, type: string }
            """;

    /**
     * The consumer: one prompted create-from into the owner's payment, one into a standalone note of
     * its own.
     */
    private static final String CONSUMER_INTENT = """
            name: gpi18ninv
            languages: [en, bg]
            uses:
              - { model: gpi18npay }
            entities:
              - name: Invoice
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: number,  type: string }
                  - { name: balance, type: decimal }
              - name: Note
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string }
            generates:
              - name: record-payment
                from: Invoice
                to: Payment
                uses: gpi18npay
                label: Record Payment
                prompt:
                  - { field: amount, required: true, default: balance }
                  - { field: reference }
              - name: add-note
                from: Invoice
                to: Note
                label: Add Note
                prompt:
                  - { field: text, required: true }
            """;

    /**
     * Opens the prompt dialog of the action ending in {@code %1$s} on invoice {@code %2$s} once the
     * store has loaded it, then returns the dialog's captions, joined by {@code |}.
     */
    private static final String OPEN_PROMPT = """
            var s = window.Alpine && Alpine.store('customActions');
            if (!s) return 'pending';
            if (!s.promptOpen) {
              var a = (s.actions || []).find(x => x.id.endsWith('-%1$s'));
              if (a) s.trigger(a, %2$s, { Id: %2$s, Balance: 10 });
              return 'pending';
            }
            if (!s.promptAction.id.endsWith('-%1$s')) return 'pending';
            return [...document.querySelectorAll('[data-prompt-dialog] label[x-h-label]')]
                .filter(l => l.offsetParent !== null).map(l => l.innerText.trim()).join('|');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_prompt_captions_read_in_the_language_the_user_picked() {
        generate(OWNER, OWNER_INTENT);
        generate(CONSUMER, CONSUMER_INTENT);

        // The descriptor names each caption's key, and the generated en-US catalog of the model that
        // declares the property carries it - with the English label the dialog falls back to.
        assertCaptionKey("record-payment", "Amount", OWNER + ":" + OWNER + "-model.t.PAYMENT_AMOUNT", OWNER, "Amount");
        assertCaptionKey("record-payment", "Reference", OWNER + ":" + OWNER + "-model.t.PAYMENT_REFERENCE", OWNER, "Reference");
        assertCaptionKey("add-note", "Text", CONSUMER + ":" + CONSUMER + "-model.t.NOTE_TEXT", CONSUMER, "Text");

        // A translator's bg-BG catalogs - each module translates its own properties.
        write(OWNER, "i18n/bg-BG/" + OWNER + ".model.json",
                "{ \"" + OWNER + "-model\": { \"t\": { \"PAYMENT_AMOUNT\": \"Сума\", \"PAYMENT_REFERENCE\": \"Основание\" } } }");
        write(CONSUMER, "i18n/bg-BG/" + CONSUMER + ".model.json",
                "{ \"" + CONSUMER + "-model\": { \"t\": { \"NOTE_TEXT\": \"Бележка\" } } }");
        // The platform registers en-US only; a deployment that speaks Bulgarian registers the locale.
        write(CONSUMER, "configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Български\", \"common\": \"/" + CONSUMER + "/i18n/bg-BG/common.json\" }");
        write(CONSUMER, "i18n/bg-BG/common.json", "{}");
        write(CONSUMER, "extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + CONSUMER + "/configs/bg-BG.json\" }");
        publish(OWNER);
        publish(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();
        int invoiceId = createInvoice();

        ide.openHomePage();
        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");
        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');");
        browser.openPath(APP + "/Invoice/" + invoiceId + "/edit");
        Selenide.refresh();

        // The owner's captions: its catalog is a namespace the consuming app's shell does not load.
        assertEquals("Сума|Основание", openPrompt("record-payment", invoiceId, "Сума"),
                "a cross-model target's captions must be translated from its owner's catalog");
        Selenide.executeJavaScript("Alpine.store('customActions').cancelPrompt();");
        assertEquals("Бележка", openPrompt("add-note", invoiceId, "Бележка"),
                "a standalone target's caption must be translated from this module's catalog");
        // Run with the required input empty: the dialog names it by the same caption.
        assertEquals("Бележка", Selenide.executeJavaScript("var s = Alpine.store('customActions'); s.promptRun(); return s.promptError;"),
                "a missing input must be named by its translated caption");
    }

    /** Opens the action's prompt dialog through the store and waits for its captions to settle. */
    private static String openPrompt(String action, int invoiceId, String translated) {
        return poll(OPEN_PROMPT.formatted(action, invoiceId), captions -> captions.startsWith(translated));
    }

    private void assertCaptionKey(String action, String column, String key, String project, String label) {
        JsonPath descriptor = JsonPath.from(read(CONSUMER, action + "-generate-action.json"));
        assertEquals(key, descriptor.getString("promptColumns.find { it.name == '" + column + "' }.tkey"),
                "the " + column + " caption must carry the key its target's form is translated by");
        JsonPath catalog = JsonPath.from(read(project, "i18n/en-US/" + project + ".model.json"));
        String path = key.substring(key.indexOf(':') + 1);
        String model = path.substring(0, path.indexOf('.'));
        assertEquals(label, catalog.getString("'" + model + "'" + path.substring(model.length())),
                "the generated catalog must carry [" + key + "]");
    }

    private int createInvoice() {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(Map.of("Number", "INV-1", "Balance", 10))
                                                        .when()
                                                        .post(INVOICES)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .jsonPath()
                                                        .getInt("Id")));
        return id.get();
    }

    private static String poll(String script, Predicate<String> settled) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(script);
            if (value != null && !"pending".equals(value) && settled.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    private String read(String project, String path) {
        return new String(repository.getResource(projectPath(project) + "/" + path)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private void write(String project, String path, String content) {
        String full = projectPath(project) + "/" + path;
        if (repository.hasResource(full)) {
            repository.getResource(full)
                      .setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(full, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void generate(String project, String yaml) {
        write(project, "app.intent", yaml);
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

    private void publish(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    @AfterEach
    void cleanup() {
        Selenide.executeJavaScript("localStorage.removeItem('codbex.harmonia.language');");
        Configuration.remove(DirigibleConfig.APPLICATION_LANGUAGES.getKey());
        for (String project : List.of(CONSUMER, OWNER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(greaterThanOrEqualTo(200)));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
