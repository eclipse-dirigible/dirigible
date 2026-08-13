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

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * End-to-end test for server-side print rendering ({@code sdk.print.Print} -> {@code PrintFacade}).
 * Seeds a {@code .print} template under a project's {@code doc/} folder (the CmsSeedSynchronizer
 * mirrors it into the tenant CMS) and drops a client-Java {@code @Controller} that calls
 * {@code Print.render} with a {@code {document, items}} payload, then asserts over HTTP (in the
 * caller's tenant scope) that the response is a valid PDF. Exercises the CMS template lookup + the
 * SDK bean bridge + the render pipeline in the same tenant scope the generated snapshot delegate
 * will use.
 */
// One Dirigible boot for the whole class: each method cleans up after itself (or is read-only), so
// the per-method context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PrintRenderIT extends IntegrationTest {

    private static final String PROJECT = "print-render-it";

    /** Registry-relative locations: the seeded template and the client-Java controller source. */
    private static final String TEMPLATE_LOCATION = "/" + PROJECT + "/doc/Templates/TestDoc/Print/en/standard.print";
    private static final String BG_TEMPLATE_LOCATION = "/" + PROJECT + "/doc/Templates/LangDoc/Print/bg/standard.print";
    private static final String CONTROLLER_LOCATION = "/" + PROJECT + "/api/PrintTestController.java";
    private static final String TEMPLATE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + TEMPLATE_LOCATION;
    private static final String BG_TEMPLATE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + BG_TEMPLATE_LOCATION;
    private static final String CONTROLLER_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + CONTROLLER_LOCATION;

    private static final String ENDPOINT = "/services/java/" + PROJECT + "/api/PrintTestController/render";
    private static final String LANGUAGE_ENDPOINT = "/services/java/" + PROJECT + "/api/PrintTestController/renderDefaultLanguage";

    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void rendersSeededTemplateToPdf() {
        repository.createResource(TEMPLATE_PATH, TEMPLATE.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
        repository.createResource(CONTROLLER_PATH, controllerSource().getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();

        // The controller renders the seeded template server-side and reports the PDF's size + magic
        // (a client-Java @Controller returns text/plain, so assert on the raw body, not a JSON path).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("\"head\": \"%PDF"))
                                                 .body(containsString("\"size\":")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    /**
     * The mint-time language fallback: with the application language set configured to bg-first (the
     * tenant's primary language), a render through {@code Print.defaultLanguage()} must pick the bg
     * template. The proof is loud by construction - ONLY a bg template is seeded, so resolving to
     * anything else fails the render (no such template) instead of returning a wrong-language PDF.
     */
    @Test
    void defaultLanguageRenderPicksTheConfiguredLanguage() {
        Configuration.set("DIRIGIBLE_APPLICATION_LANGUAGES", "bg,en");
        try {
            repository.createResource(BG_TEMPLATE_PATH, TEMPLATE.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
            repository.createResource(CONTROLLER_PATH, controllerSource().getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
            synchronizationProcessor.forceProcessSynchronizers();

            restAssuredExecutor.execute(() -> given().when()
                                                     .get(LANGUAGE_ENDPOINT)
                                                     .then()
                                                     .statusCode(200)
                                                     .body(containsString("\"language\": \"bg\""))
                                                     .body(containsString("\"head\": \"%PDF")),
                    ASSERTION_TIMEOUT_SECONDS);
        } finally {
            Configuration.remove("DIRIGIBLE_APPLICATION_LANGUAGES");
        }
    }

    @AfterEach
    void cleanup() {
        boolean any = false;
        for (String path : new String[] {TEMPLATE_PATH, BG_TEMPLATE_PATH, CONTROLLER_PATH}) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                any = true;
            }
        }
        if (any) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private static final String TEMPLATE = """
            <document id="test-doc">
                <page>
                    <section>
                        <field label="Number">{{document.number}}</field>
                        <field label="Customer">{{document.customer}}</field>
                    </section>
                    <table source="items">
                        <column width="2*">{{name}}</column>
                        <column width="*" align="right">{{amount}}</column>
                    </table>
                    <total align="right">{{document.total}}</total>
                </page>
            </document>
            """;

    private static String controllerSource() {
        return """
                package api;

                import java.nio.charset.StandardCharsets;

                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Get;
                import org.eclipse.dirigible.sdk.print.Print;

                @Controller
                public class PrintTestController {

                    @Get("/render")
                    public String render() {
                        String data = "{\\"document\\":{\\"number\\":\\"INV-001\\",\\"customer\\":\\"ACME Ltd.\\",\\"total\\":\\"123.45\\"},\\"items\\":[{\\"name\\":\\"Widget\\",\\"amount\\":\\"100.00\\"}]}";
                        byte[] pdf = Print.render("TestDoc", "en", data);
                        int n = Math.min(5, pdf.length);
                        String head = new String(pdf, 0, n, StandardCharsets.ISO_8859_1);
                        return "{\\"size\\": " + pdf.length + ", \\"head\\": \\"" + head + "\\"}";
                    }

                    @Get("/renderDefaultLanguage")
                    public String renderDefaultLanguage() {
                        String data = "{\\"document\\":{\\"number\\":\\"INV-002\\",\\"customer\\":\\"ACME Ltd.\\",\\"total\\":\\"1.00\\"},\\"items\\":[{\\"name\\":\\"Widget\\",\\"amount\\":\\"1.00\\"}]}";
                        String language = Print.defaultLanguage();
                        byte[] pdf = Print.render("LangDoc", language, data);
                        int n = Math.min(5, pdf.length);
                        String head = new String(pdf, 0, n, StandardCharsets.ISO_8859_1);
                        return "{\\"language\\": \\"" + language + "\\", \\"head\\": \\"" + head + "\\"}";
                    }
                }
                """;
    }

}
