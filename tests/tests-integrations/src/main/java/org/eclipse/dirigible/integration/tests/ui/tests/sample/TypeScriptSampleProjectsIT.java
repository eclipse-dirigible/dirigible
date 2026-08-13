/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;

/**
 * The TypeScript / JavaScript decorator samples, published together into one instance and verified
 * one sample per test. Counterpart of {@link JavaSampleProjectsIT}, which covers the client-Java
 * samples - the two families are kept apart because their entity samples share a table name (see
 * {@link SampleProjectsIT}).
 */
class TypeScriptSampleProjectsIT extends SampleProjectsIT {

    private static final String COUNTRIES_RESPONSE_BODY =
            """
                    [{"Code2":"AF","Numeric":"004","Code3":"AFG","Id":1,"$type$":"CountryEntity","Name":"Afghanistan"},{"Code2":"AL","Numeric":"008","Code3":"ALB","Id":2,"$type$":"CountryEntity","Name":"Albania"},{"Code2":"DZ","Numeric":"012","Code3":"DZA","Id":3,"$type$":"CountryEntity","Name":"Algeria"}]
                    """;

    /**
     * The expected OpenAPI document with the instance's own version replaced by
     * {@link #VERSION_PLACEHOLDER}. The served {@code info.version} is the real build version, which
     * changes on every release and every development bump - so it is normalised out of BOTH sides of
     * the comparison rather than pinned here. Pinning it made this test fail on both CI databases the
     * first night after the version stopped being served as a literal {@code ${project.version}}
     * (#6644).
     *
     * <p>
     * The document stays an exact match even though seven more samples share the instance:
     * {@code CountryController.ts} is the only {@code @Controller} in the family, and only a controller
     * contributes an OpenAPI fragment.
     */
    private static final String VERSION_PLACEHOLDER = "<version>";

    private static final String OPENAPI_RESPONSE_BODY =
            """
                    {"openapi":"3.0.1","info":{"title":"Applications Services Open API","description":"Services Open API provided by the applications","contact":{"name":"Eclipse Dirigible","url":"https://www.dirigible.io","email":"dirigible-dev@eclipse.org"},"license":{"name":"Eclipse Public License - v 2.0","url":"https://www.eclipse.org/legal/epl-v20.html"},"version":"<version>"},"servers":[{"url":"/services/ts"},{"url":"/services/ts"}],"security":[],"tags":[],"paths":{"/sample-entity-decorators/CountryController.ts/":{"get":{"tags":["CountryController"],"summary":"getAll CountryController ","operationId":"getAll","parameters":[],"responses":{"200":{"description":"Success","content":{"application/json":{"schema":{"type":"array","items":{"$ref":"#/components/schemas/CountryEntity"}}}}}}}}},"components":{"schemas":{"number":{"type":"number"},"CountryEntity":{"type":"object","properties":{"Code2":{"type":"string"},"Numeric":{"type":"string"},"Code3":{"type":"string"},"Id":{"type":"number","description":"My Id"},"Name":{"type":"string","description":"My Name"}},"description":"Sample Country Entity"},"string":{"type":"string"},"any":{"type":"object"}},"responses":{},"parameters":{},"examples":{},"requestBodies":{},"headers":{},"securitySchemes":{},"links":{},"callbacks":{}}}
                    """;

    /** {@code "version":"<anything>"} inside the OpenAPI {@code info} block. */
    private static final Pattern OPENAPI_VERSION = Pattern.compile("\"version\":\"[^\"]*\"");

    private static final String LIST_CUSTOMERS_RESPONSE_BODY = """
            List all customers:
            [
              {
                "address": "Sofia, Bulgaria",
                "name": "John",
                "id": 1,
                "$type$": "Customer"
              },
              {
                "address": "Varna, Bulgaria",
                "name": "Jane",
                "id": 2,
                "$type$": "Customer"
              },
              {
                "address": "Berlin, Germany",
                "name": "Matthias",
                "id": 3,
                "$type$": "Customer"
              }
            ]""";

    private static final String COMPLEX_CUSTOMERS_RESPONSE_BODY = """

            Select customers with first name John:
            [
              {
                "address": "Sofia, Bulgaria",
                "name": "John",
                "id": 1,
                "$type$": "Customer"
              }
            ]

            Select native customers with first name John:
            [
              {
                "customer_id": 1,
                "customer_address": "Sofia, Bulgaria",
                "customer_name": "John"
              }
            ]

            Find customers by Example:
            [
              {
                "address": "Sofia, Bulgaria",
                "name": "John",
                "id": 1,
                "$type$": "Customer"
              }
            ]

            List customers with filter options:
            [
              {
                "address": "Varna, Bulgaria",
                "name": "Jane",
                "id": 2,
                "$type$": "Customer"
              },
              {
                "address": "Sofia, Bulgaria",
                "name": "John",
                "id": 1,
                "$type$": "Customer"
              }
            ]

            Select customers with first name starts with J:
            [
              {
                "address": "Sofia, Bulgaria",
                "name": "John",
                "id": 1,
                "$type$": "Customer"
              },
              {
                "address": "Varna, Bulgaria",
                "name": "Jane",
                "id": 2,
                "$type$": "Customer"
              }
            ]

            Select customers with first name starts with M with typed query:
            [
              {
                "address": "Berlin, Germany",
                "name": "Matthias",
                "id": 3,
                "$type$": "Customer"
              }
            ]

            Select customers with first name starts with M with named query:
            [
              {
                "address": "Berlin, Germany",
                "name": "Matthias",
                "id": 3,
                "$type$": "Customer"
              }
            ]

            Select customers with first name in ['John', 'Jane'] with named query:
            [
              {
                "address": "Sofia, Bulgaria",
                "name": "John",
                "id": 1,
                "$type$": "Customer"
              },
              {
                "address": "Varna, Bulgaria",
                "name": "Jane",
                "id": 2,
                "$type$": "Customer"
              }
            ]""";

    private static final String ADMIN_USERNAME = "adm1";
    private static final String ADMIN_PASS = "adm1-pass";

    private static final String UNAUTHORIZED_USER_USERNAME = "unathorized-usr";
    private static final String UNAUTHORIZED_USER_PASS = "unathorized-usr-pass";

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    protected List<String> getRepositoryUrls() {
        return List.of( //
                "https://github.com/dirigiblelabs/sample-component-decorators.git", //
                "https://github.com/dirigiblelabs/sample-entity-decorators.git", //
                "https://github.com/dirigiblelabs/sample-extension-decorator.git", //
                "https://github.com/dirigiblelabs/sample-job-decorator.git", //
                "https://github.com/dirigiblelabs/sample-listener-decorator.git", //
                "https://github.com/dirigiblelabs/sample-roles-decorator.git", //
                "https://github.com/dirigiblelabs/sample-store-api.git", //
                "https://github.com/dirigiblelabs/sample-websocket-decorator.git");
    }

    @Test
    void componentDecorator() {
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/ts/sample-component-decorators/OrderProcessor.ts")
                             .then()
                             .statusCode(200)
                             .body(equalToCompressingWhiteSpace("Do Payment: {\"status\":\"OK\",\"data\":\"123.45\"}")));
    }

    @Test
    void entityDecorators() {
        restAssuredExecutor.execute( //
                () -> {
                    RequestSpecification requestSpec = new RequestSpecBuilder().setUrlEncodingEnabled(false)
                                                                               .build();

                    // Use the spec where encoding is disabled otherwise limit param is encoded and skipped by the code
                    given().spec(requestSpec)
                           .queryParam("$limit", 3)
                           .get("/services/ts/sample-entity-decorators/CountryController.ts")
                           .then()
                           .statusCode(200)
                           .body(equalToCompressingWhiteSpace(COUNTRIES_RESPONSE_BODY));

                    // TODO: documentation texts from @Document annotations are not added to the open api response. Fix
                    // this issue and adapt the test.
                    String openApi = given().when()
                                            .get("/services/openapi")
                                            .then()
                                            .statusCode(200)
                                            .extract()
                                            .asString();
                    assertThat("the served OpenAPI document should match, ignoring the instance's own version", //
                            withoutVersion(openApi), equalToCompressingWhiteSpace(OPENAPI_RESPONSE_BODY));
                }, 60);
    }

    /**
     * The OpenAPI document with the instance's own {@code info.version} normalised to
     * {@link #VERSION_PLACEHOLDER}, so the assertion survives every release and development version
     * bump.
     *
     * @param openApi the served document
     * @return the document with its version normalised
     */
    private static String withoutVersion(String openApi) {
        return OPENAPI_VERSION.matcher(openApi)
                              .replaceFirst("\"version\":\"" + VERSION_PLACEHOLDER + "\"");
    }

    @Test
    void extensionDecorator() {
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/ts/sample-extension-decorator/OrderDiscount.ts")
                             .then()
                             .statusCode(200)
                             .body(equalToCompressingWhiteSpace("\"Discount: 5\"")));
    }

    @Test
    void jobDecorator() {
        LogsAsserter consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);

        // The job's cron fires every 10 seconds, so it logs again regardless of how long ago the
        // sample was published - the asserter only sees messages logged after it attached.
        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(3, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("MyJob executed!", Level.INFO));
    }

    @Test
    void listenerDecorator() {
        LogsAsserter consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);

        restAssuredExecutor.execute( //
                () -> given().get("/services/js/sample-listener-decorator/OrderListenerTrigger.js")
                             .then()
                             .statusCode(200));

        consoleLogAsserter.containsMessage("Hello from the OrderListener Trigger! Message: [ I am a message created at:", Level.INFO);
        consoleLogAsserter.containsMessage("Processing message event: [ I am a message created at:", Level.INFO);
    }

    @Test
    void rolesDecorator() {
        LogsAsserter consoleErrorLogAsserter = new LogsAsserter("app.err", Level.INFO);

        securityUtil.ensureUserInDefaultTenant(ADMIN_USERNAME, ADMIN_PASS, Roles.ADMINISTRATOR.getRoleName());
        restAssuredExecutor.execute(this::verifyAuthorizedUserAccess, ADMIN_USERNAME, ADMIN_PASS);

        securityUtil.ensureUserInDefaultTenant(UNAUTHORIZED_USER_USERNAME, UNAUTHORIZED_USER_PASS);
        restAssuredExecutor.execute(() -> verifyUnauthorizedUserAccess(consoleErrorLogAsserter), UNAUTHORIZED_USER_USERNAME,
                UNAUTHORIZED_USER_PASS);
    }

    private void verifyAuthorizedUserAccess() {
        // set default parser to enforce restassured json body validations
        // since the response doesn't specify the content type
        given().when()
               .get("/services/ts/sample-roles-decorator/RolesCheck.ts")
               .then()
               .statusCode(200)
               .using()
               .defaultParser(Parser.JSON)
               .body("message", equalTo("Roles Check"))
               .body("user", equalTo(ADMIN_USERNAME));
    }

    private void verifyUnauthorizedUserAccess(LogsAsserter consoleErrorLogAsserter) {
        given().when()
               .get("/services/ts/sample-roles-decorator/RolesCheck.ts")
               .then()
               .statusCode(500);

        consoleErrorLogAsserter.assertLoggedMessage("Current user [" + UNAUTHORIZED_USER_USERNAME
                + "] is not allowed to call module [RolesCheck]. Required some of roles [ADMINISTRATOR]", Level.ERROR);
    }

    @Test
    void storeApi() {
        // Retry-on-AssertionError: if a late sync cycle re-registers the Customer entity and
        // rebuilds its table between the init and the list (dropping the just-inserted rows and
        // resetting the identity counter), the next attempt re-inits and converges.
        restAssuredExecutor.execute( //
                () -> {
                    given().when()
                           .get("/services/ts/sample-store-api/InitCustomers.ts")
                           .then()
                           .statusCode(200);

                    given().when()
                           .get("/services/ts/sample-store-api/ListCustomers.ts")
                           .then()
                           .statusCode(200)
                           .body(equalToCompressingWhiteSpace(LIST_CUSTOMERS_RESPONSE_BODY));

                    given().when()
                           .get("/services/ts/sample-store-api/ComplexQueries.ts")
                           .then()
                           .statusCode(200)
                           .body(equalToCompressingWhiteSpace(COMPLEX_CUSTOMERS_RESPONSE_BODY));
                }, 90);
    }

    @Test
    void websocketDecorator() {
        LogsAsserter consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);

        // The only verification in the family that drives the browser. Every test gets a fresh Chrome
        // (the base closes the driver after each), so the session has to be established here - the
        // sample's page is behind authentication and would otherwise render the login form.
        ide.openHomePage();

        browser.openPath("/services/web/sample-websocket-decorator/order-websocket-page.html");

        browser.enterTextInElementById("fromInput", "Test user");
        browser.clickOnElementById("connectBtn");

        browser.enterTextInElementById("textInput", "A test message");
        browser.clickOnElementById("sendMessage");

        browser.assertElementExistsByIdAndContainsText("response", "Test user: Hello from OrderWebsocket! [A test message]");

        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("Message received: A test message, from: Test user", Level.INFO));
    }

}
