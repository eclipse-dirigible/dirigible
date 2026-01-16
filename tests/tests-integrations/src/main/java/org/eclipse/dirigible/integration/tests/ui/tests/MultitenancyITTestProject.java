/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import ch.qos.logback.classic.Level;
import io.restassured.http.ContentType;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.tests.base.BaseMultitenantTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.awaitility.AwaitilityExecutor;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.BrowserFactory;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.IDEFactory;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Lazy
@Component
class MultitenancyITTestProject extends BaseMultitenantTestProject {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultitenancyITTestProject.class);

    private static final String PROJECT_RESOURCES_PATH = "MultitenancyIT";
    private static final String UI_HOME_PATH = "/services/web/dashboard/index.html";
    private static final String TS_BASE_PATH = "/services/ts/" + PROJECT_RESOURCES_PATH + "/";
    private static final String READERS_VIEW_SERVICE_PATH = TS_BASE_PATH + "views/ReaderViewService.ts";
    private static final String DOCUMENTS_SERVICE_PATH = TS_BASE_PATH + "cmis/DocumentService.ts/documents";
    private static final String BOOKS_SERVICE_PATH = TS_BASE_PATH + "gen/edm/api/Books/BookService.ts";
    private static final String READERS_ODATA_ENTITY_PATH = "/odata/v2/Readers";

    private final BrowserFactory browserFactory;
    private final RestAssuredExecutor restAssuredExecutor;
    private final IDEFactory ideFactory;

    private final LogsAsserter testJobLogsAsserter;
    private final LogsAsserter eventListenerLogsAsserter;

    MultitenancyITTestProject(BrowserFactory browserFactory, EdmView edmView, RestAssuredExecutor restAssuredExecutor,
            IDEFactory ideFactory, ProjectUtil projectUtil) {
        super(PROJECT_RESOURCES_PATH, ideFactory.create(), projectUtil, edmView);
        this.browserFactory = browserFactory;
        this.restAssuredExecutor = restAssuredExecutor;
        this.ideFactory = ideFactory;

        this.testJobLogsAsserter = new LogsAsserter("app.test-job-handler.ts", Level.DEBUG);
        this.eventListenerLogsAsserter = new LogsAsserter("app.book-entity-events-handler.ts", Level.DEBUG);
    }

    @Override
    public void configure() {
        copyToWorkspace();
        generateEDM("edm.edm");
        publish();
    }

    @Override
    public void verify(DirigibleTestTenant tenant) {
        LOGGER.info("Verifying test project for tenant [{}]...", tenant);

        wrapVerification(this::verifyHomePageAccessibleByTenant, tenant, "verifyHomePageAccessibleByTenant");
        wrapVerification(this::verifyView, tenant, "verifyView");
        wrapVerification(this::verifyEdmGeneratedResources, tenant, "verifyEdmGeneratedResources");
        wrapVerification(this::verifyOData, tenant, "verifyOData");
        wrapVerification(this::verifyDocumentsAPI, tenant, "verifyDocumentsAPI");

        LOGGER.info("Test project for tenant [{}] has been verified successfully!", tenant);
    }

    private void verifyHomePageAccessibleByTenant(DirigibleTestTenant tenant) {
        Browser browser = browserFactory.createByHost(tenant.getHost());
        browser.openPath(UI_HOME_PATH);

        IDE ide = ideFactory.create(browser, tenant.getUsername(), tenant.getPassword());
        boolean forceLogin = !tenant.isDefaultTenant();
        ide.login(forceLogin);

        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Dashboard");
    }

    /**
     * Verifies indirectly:<br>
     * - MultitenancyIT/views/readers.view is created and it is working<br>
     * - MultitenancyIT/csvim/data.csvim is imported <br>
     * - default DB datasource is resolved correctly
     */
    private void verifyView(DirigibleTestTenant tenant) {

        restAssuredExecutor.execute(tenant, //
                () -> given().when()
                             .get(READERS_VIEW_SERVICE_PATH)
                             .then()
                             .statusCode(200)
                             .body("$", hasSize(2))
                             .body("[0].READER_FIRST_NAME", equalTo("Ivan"))
                             .body("[0].READER_LAST_NAME", equalTo("Ivanov"))
                             .body("[1].READER_FIRST_NAME", equalTo("Maria"))
                             .body("[1].READER_LAST_NAME", equalTo("Petrova")),
                25);
    }

    /**
     * Verifies indirectly:<br>
     * - edm generated schema is created<br>
     * - generated REST is created and it works<br>
     * - topic listener works<br>
     * - job has been executed<br>
     * - default DB datasource is resolved correctly
     */
    private void verifyEdmGeneratedResources(DirigibleTestTenant tenant) {
        restAssuredExecutor.execute(tenant, () -> verifyBookREST(tenant));
        verifyJobExecuted(tenant);
        verifyListenerExecuted(tenant);
    }

    private void verifyBookREST(DirigibleTestTenant tenant) {
        String title = "Title[" + tenant.getName() + "]";
        String author = "Author[" + tenant.getName() + "]";
        String jsonPayload = String.format("""
                {
                    "Title": "%s",
                    "Author": "%s"
                }
                """, title, author);

        given().contentType(ContentType.JSON)
               .body(jsonPayload)
               .when()
               .post(BOOKS_SERVICE_PATH)
               .then()
               .statusCode(201);

        given().when()
               .get(BOOKS_SERVICE_PATH)
               .then()
               .statusCode(200)
               .body("$", hasSize(1))
               .body("[0].Id", equalTo(1))
               .body("[0].Title", equalTo(title))
               .body("[0].Author", equalTo(author));
    }

    private void verifyJobExecuted(DirigibleTestTenant tenant) {
        String expectedMessage = "Job: found [1] books. Books: [[{\"Id\":1,\"Title\":\"Title[" + tenant.getName()
                + "]\",\"Author\":\"Author[" + tenant.getName() + "]\"}]]";
        verifyMessageLogged(expectedMessage, testJobLogsAsserter);
    }

    private void verifyMessageLogged(String expectedMessage, LogsAsserter logsAsserter) {
        String failMessage =
                "Couldn't find message [" + expectedMessage + "] in the logs. Logged messages: " + logsAsserter.getLoggedMessages();
        AwaitilityExecutor.execute(failMessage, () -> await().atMost(10, TimeUnit.SECONDS)
                                                             .until(() -> logsAsserter.containsMessage(expectedMessage, Level.INFO)));
    }

    private void verifyListenerExecuted(DirigibleTestTenant tenant) {
        String expectedMessage = "Listener: found [1] books. Books: [[{\"Id\":1,\"Title\":\"Title[" + tenant.getName()
                + "]\",\"Author\":\"Author[" + tenant.getName() + "]\"}]]";
        verifyMessageLogged(expectedMessage, eventListenerLogsAsserter);
    }

    private void verifyDocumentsAPI(DirigibleTestTenant tenant) {
        restAssuredExecutor.execute(tenant, () -> {
            given().when()
                   .get(DOCUMENTS_SERVICE_PATH)
                   .then()
                   .statusCode(200)
                   .body("$", hasSize(0));

            String documentName = "DOC_NAME_" + tenant.getId() + ".txt";
            String documentContent = "DOC_CONTENT_" + tenant.getId();
            String jsonPayload = String.format("""
                    {
                        "documentName": "%s",
                        "content": "%s"
                    }
                    """, documentName, documentContent);

            given().contentType(ContentType.JSON)
                   .body(jsonPayload)
                   .when()
                   .post(DOCUMENTS_SERVICE_PATH)
                   .then()
                   .statusCode(200);

            given().when()
                   .get(DOCUMENTS_SERVICE_PATH + "/" + documentName)
                   .then()
                   .statusCode(200)
                   .body(equalTo(JsonHelper.toJson(documentContent)));
        });
    }

    /**
     * Verifies indirectly:<br>
     * - MultitenancyIT/tables/reader.table is created<br>
     * - MultitenancyIT/csvim/data.csvim is imported <br>
     * - MultitenancyIT/odata/readers.odata is configured <br>
     * - OData is working<br>
     * - default DB datasource is resolved correctly
     */
    private void verifyOData(DirigibleTestTenant tenant) {
        restAssuredExecutor.execute(tenant, () -> {
            verifyCSVIMIsImported();
            verifyAddingNewReader(tenant);
        });
    }

    private void verifyCSVIMIsImported() {
        given().header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(READERS_ODATA_ENTITY_PATH)
               .then()
               .statusCode(200)
               .body("d.results", hasSize(2))
               .body("d.results[0].ReaderId", equalTo(1))
               .body("d.results[0].ReaderFirstName", equalTo("Ivan"))
               .body("d.results[0].ReaderLastName", equalTo("Ivanov"))
               .body("d.results[1].ReaderId", equalTo(2))
               .body("d.results[1].ReaderFirstName", equalTo("Maria"))
               .body("d.results[1].ReaderLastName", equalTo("Petrova"));
    }

    private void verifyAddingNewReader(DirigibleTestTenant tenant) {
        String firstName = "FirstName[" + tenant.getName() + "]";
        String lastName = "LastName[" + tenant.getName() + "]";
        String jsonPayload = String.format("""
                {
                    "ReaderId": 3,
                    "ReaderFirstName": "%s",
                    "ReaderLastName": "%s"
                }
                """, firstName, lastName);

        given().contentType(ContentType.JSON)
               .body(jsonPayload)
               .when()
               .post(READERS_ODATA_ENTITY_PATH)
               .then()
               .statusCode(201);

        given().header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(READERS_ODATA_ENTITY_PATH)
               .then()
               .statusCode(200)
               .body("d.results", hasSize(3))
               .body("d.results[2].ReaderId", equalTo(3))
               .body("d.results[2].ReaderFirstName", equalTo(firstName))
               .body("d.results[2].ReaderLastName", equalTo(lastName));
    }

}
