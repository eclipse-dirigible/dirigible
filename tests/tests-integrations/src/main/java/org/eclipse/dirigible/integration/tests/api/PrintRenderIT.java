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
import java.util.Base64;

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
    private static final String IMAGE_TEMPLATE_LOCATION = "/" + PROJECT + "/doc/Templates/ImageDoc/Print/en/standard.print";
    private static final String LOGO_LOCATION = "/" + PROJECT + "/doc/Templates/Print/logo.png";
    private static final String CONTROLLER_LOCATION = "/" + PROJECT + "/api/PrintTestController.java";
    private static final String TEMPLATE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + TEMPLATE_LOCATION;
    private static final String BG_TEMPLATE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + BG_TEMPLATE_LOCATION;
    private static final String IMAGE_TEMPLATE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + IMAGE_TEMPLATE_LOCATION;
    private static final String LOGO_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + LOGO_LOCATION;
    private static final String CONTROLLER_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + CONTROLLER_LOCATION;

    private static final String ENDPOINT = "/services/java/" + PROJECT + "/api/PrintTestController/render";
    private static final String LANGUAGE_ENDPOINT = "/services/java/" + PROJECT + "/api/PrintTestController/renderDefaultLanguage";
    private static final String IMAGE_ENDPOINT = "/services/java/" + PROJECT + "/api/PrintTestController/renderWithImage";

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

    /**
     * The logo on a printed document, end to end: a PNG shipped under the project's {@code doc/} folder
     * is seeded into the tenant CMS as any other file, the template names it by its CMS path, and the
     * render inlines the bytes so the PDF really carries an image object. The same template also names
     * a logo that does NOT exist - the render must still succeed and simply carry one image, because a
     * tenant that has not uploaded its logo yet is the everyday state and must not lose its invoice.
     */
    @Test
    void embedsAContentStoreImageAndSkipsAMissingOne() {
        repository.createResource(LOGO_PATH, Base64.getDecoder()
                                                   .decode(ONE_PIXEL_PNG_BASE64),
                false, "image/png", true);
        repository.createResource(IMAGE_TEMPLATE_PATH, IMAGE_TEMPLATE.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
        repository.createResource(CONTROLLER_PATH, controllerSource().getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(IMAGE_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("\"head\": \"%PDF"))
                                                 .body(containsString("\"images\": 1")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @AfterEach
    void cleanup() {
        boolean any = false;
        for (String path : new String[] {TEMPLATE_PATH, BG_TEMPLATE_PATH, IMAGE_TEMPLATE_PATH, LOGO_PATH, CONTROLLER_PATH}) {
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
                        <!-- Alternative operands: the local twin is absent from the payload, so the
                             render must fall through to the canonical field and still produce a PDF. -->
                        <field label="Customer">{{document.customerLocal|document.customer}}</field>
                    </section>
                    <table source="items">
                        <column width="2*">{{name}}</column>
                        <column width="*" align="right">{{amount}}</column>
                    </table>
                    <total align="right">{{document.total}}</total>
                </page>
            </document>
            """;

    /**
     * A 1x1 red PNG - the smallest valid image, so the assertion is about the pipeline, not the file.
     */
    private static final String ONE_PIXEL_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC";

    private static final String IMAGE_TEMPLATE = """
            <document id="image-doc">
                <page>
                    <image src="Templates/Print/logo.png" width="120"/>
                    <image src="Templates/Print/missing.png" width="120"/>
                    <section>
                        <field label="Number">{{document.number}}</field>
                    </section>
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

                    @Get("/renderWithImage")
                    public String renderWithImage() {
                        String data = "{\\"document\\":{\\"number\\":\\"INV-003\\"},\\"items\\":[]}";
                        byte[] pdf = Print.render("ImageDoc", "en", data);
                        String content = new String(pdf, StandardCharsets.ISO_8859_1);
                        int images = 0;
                        int at = content.indexOf("/Subtype /Image");
                        while (at >= 0) {
                            images++;
                            at = content.indexOf("/Subtype /Image", at + 1);
                        }
                        int n = Math.min(5, pdf.length);
                        String head = new String(pdf, 0, n, StandardCharsets.ISO_8859_1);
                        return "{\\"images\\": " + images + ", \\"head\\": \\"" + head + "\\"}";
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
