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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The inline "New" beside a document's pickers opens the target's own create page when the model
 * name carries a hyphen (dirigible #8657).
 *
 * <p>
 * A hyphenated model keeps its web assets under the raw gen folder ({@code gen/cash-desk}) and its
 * controllers under the sanitized one ({@code gen/cash_desk}). The document header and its line
 * dialog derived the create page from the controller URL, so the dialog loaded
 * {@code gen/cash_desk/index.html} - the platform's Page Not Found. The generated document page and
 * its line-items register are run here, the URL each "New" opens is captured, and the published app
 * must serve it.
 */
class DocumentInlineCreateHyphenatedModelIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "inline-new-hyphen";
    private static final String GEN_FOLDER = "cash-desk";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String DOCUMENT_VIEW = "gen/" + GEN_FOLDER + "/views/CashOrder/CashOrder-document.html";
    private static final String DOCUMENT_PAGE = "gen/" + GEN_FOLDER + "/js/components/pages/CashOrder/CashOrderDocumentPage.js";
    private static final String ITEMS_REGISTER = "gen/" + GEN_FOLDER + "/js/components/pages/CashOrder/CashOrderItem.detail.js";
    private static final String APP_URL = "/services/web/" + PROJECT + "/gen/" + GEN_FOLDER + "/index.html";
    private static final String APP_JS = "/META-INF/dirigible/application-core/shell/js/app.js";
    private static final Pattern ADD_RELATED = Pattern.compile("addRelated\\('(\\w+)', '([^']*)', '(\\w+)'\\)");
    private static final long TIMEOUT_SECONDS = 90;

    /**
     * The reported shape: a document with a picker to another entity and one to itself, and lines with
     * a picker.
     */
    private static final String INTENT_YAML = """
            name: cash-desk
            entities:
              - name: CashDesk
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
              - name: Article
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
              - name: CashOrder
                function: Document
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 20, function: DocumentTitle }
                relations:
                  - { name: CashDesk, kind: manyToOne, to: CashDesk, required: true }
                  - { name: StornoOf, kind: manyToOne, to: CashOrder }
              - name: CashOrderItem
                function: DocumentItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: CashOrder, kind: manyToOne, to: CashOrder, composition: true, required: true }
                  - { name: Article,   kind: manyToOne, to: Article }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void every_new_beside_a_document_picker_opens_a_page_the_hyphenated_app_serves() throws Exception {
        generateProject();

        List<String> opened = openEveryInlineCreate();
        assertEquals(List.of(createUrl("CashDesk"), createUrl("CashOrder"), createUrl("Article")), opened,
                "each New must open the target's create page under the app's raw gen folder");

        publishProject();
        for (String url : opened) {
            String page = url.substring(0, url.indexOf('?'));
            restAssuredExecutor.execute(() -> given().when()
                                                     .get(page)
                                                     .then()
                                                     .statusCode(200),
                    TIMEOUT_SECONDS);
        }
    }

    private static String createUrl(String entity) {
        return APP_URL + "?embedded=1#/" + entity + "/create?embedded=1&dialog=1";
    }

    /**
     * Run the shipped app.js with the generated items register and document page, click every header
     * "New" exactly as the generated view binds it, then the line dialog's "New" for the Article
     * column, and return the URL each one handed the related-create dialog, in that order.
     */
    private List<String> openEveryInlineCreate() throws Exception {
        String view = contentOf(DOCUMENT_VIEW);
        try (Context context = Context.newBuilder("js")
                                      .allowAllAccess(true)
                                      .option("engine.WarnInterpreterOnly", "false")
                                      .build()) {
            context.eval("js",
                    "var window = this; var __opened = []; var __pages = {}; var __init = [];"
                            + " var console = { error: () => {}, warn: () => {}, log: () => {} };"
                            + " var document = { addEventListener: (type, cb) => { if (type === 'alpine:init') __init.push(cb); } };"
                            + " var Alpine = { data: (name, factory) => { __pages[name] = factory; }, directive: () => {},"
                            + "   store: () => ({ create: (url) => { __opened.push(url); } }) };"
                            + " var baseFormPage = () => ({}); var printActions = () => ({});");
            context.eval("js", readClasspath(APP_JS));
            context.eval("js", contentOf(ITEMS_REGISTER));
            context.eval("js", contentOf(DOCUMENT_PAGE));
            context.eval("js", "__init.forEach(cb => cb()); var page = __pages['CashOrderDocumentPage']();"
                    + " page.itemsDef = App.detailsFor('CashOrder').find(d => d.entity === 'CashOrderItem');");

            Value page = context.getBindings("js")
                                .getMember("page");
            Matcher click = ADD_RELATED.matcher(view);
            int headerPickers = 0;
            while (click.find()) {
                page.invokeMember("addRelated", click.group(1), click.group(2), click.group(3));
                headerPickers++;
            }
            assertEquals(2, headerPickers, "the document header must offer New beside its CashDesk and StornoOf pickers");
            context.eval("js", "page.addRelatedItem(page.editColumns.find(c => c.name === 'Article'));");

            Value urls = context.getBindings("js")
                                .getMember("__opened");
            List<String> opened = new ArrayList<>();
            for (long i = 0; i < urls.getArraySize(); i++) {
                opened.add(urls.getArrayElement(i)
                               .asString());
            }
            assertTrue(opened.stream()
                             .noneMatch(url -> url.contains("/cash_desk/")),
                    "a New must never open the sanitized controller folder: " + opened);
            return opened;
        }
    }

    private void generateProject() {
        IResource existing = repository.getResource(PROJECT_PATH + "/app.intent");
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(PROJECT_PATH + "/app.intent", INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
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

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private static String readClasspath(String resource) throws Exception {
        try (InputStream in = DocumentInlineCreateHyphenatedModelIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
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
