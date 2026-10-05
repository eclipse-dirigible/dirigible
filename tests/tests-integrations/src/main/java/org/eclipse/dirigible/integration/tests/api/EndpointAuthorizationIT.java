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
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * End-to-end proof that the {@code @RolesAllowed} annotations guarding the platform's endpoints are
 * actually ENFORCED, not merely present.
 * <p>
 * Only a few path prefixes carry role rules in the central URL configuration; every other endpoint
 * under {@code /services/**} is authenticated there and relies on its own {@code @RolesAllowed},
 * which Spring honours only while JSR-250 method security is switched on. When that switch was
 * bound to the basic-authentication configuration, every single-sign-on profile turned it off and
 * the annotations silently stopped guarding anything - the administrative surfaces below were open
 * to any authenticated user, with no error anywhere. This test locks the enforcement in place.
 */
// One Dirigible boot for the whole class: each method cleans up after itself, so the per-method
// context reset inherited from IntegrationTest would only add ~10s of boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EndpointAuthorizationIT extends IntegrationTest {

    private static final String PLAIN_USER = "endpoint-authz-it-user";
    private static final String ADMIN_USER = "endpoint-authz-it-admin";
    private static final String PASSWORD = "endpoint-authz-it-password";
    private static final String FOREIGN_ORIGIN = "https://evil.example.org";
    /** The shape of a generated create endpoint - the filter refuses before any handler resolves it. */
    private static final String GENERATED_CREATE_ENDPOINT = "/services/ts/endpoint-authz-it/gen/invoices/api/Invoice/InvoiceController.ts";

    /**
     * Administrative surfaces that must never answer a role-less but authenticated caller.
     * <p>
     * The two {@code /services/data/} entries are now rejected by the URL layer as well (that prefix
     * gained a role gate), so they assert the outcome rather than which layer produced it. The three
     * below them have no URL rule and are therefore the ones that still prove method security is
     * switched on - keep at least one such path here.
     */
    private static final String[] PRIVILEGED_ENDPOINTS = { //
            "/services/data/metadata/", // database metadata (the Database perspective)
            "/services/data/sources", // data source definitions - carry credentials
            "/services/core/configurations", // configuration values
            "/services/security/tenants", // tenant administration
            "/services/core/extensions"}; // extension inventory

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void privileged_endpoints_reject_an_authenticated_user_without_a_platform_role() {
        securityUtil.ensureUserInDefaultTenant(PLAIN_USER, PASSWORD);

        for (String endpoint : PRIVILEGED_ENDPOINTS) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .get(endpoint)
                                                     .then()
                                                     .statusCode(403),
                    PLAIN_USER, PASSWORD);
        }
    }

    @Test
    void privileged_endpoints_admit_an_administrator() {
        securityUtil.ensureUserInDefaultTenant(ADMIN_USER, PASSWORD, Roles.RoleNames.ADMINISTRATOR);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations")
                                                 .then()
                                                 .statusCode(200),
                ADMIN_USER, PASSWORD);
    }

    /**
     * The database export dumps live in the CMS. They were reachable anonymously through the public CMS
     * mapping; now the CMS is served only from the secured path, and the exports folder inside it
     * additionally requires the roles that may produce an export.
     */
    @Test
    void database_exports_are_not_readable_without_an_administrative_role() {
        securityUtil.ensureUserInDefaultTenant(PLAIN_USER, PASSWORD);

        // the anonymous CMS mapping is gone entirely, so the path resolves to no handler at all
        given().when()
               .get("/public/cms/__EXPORTS/dump.zip")
               .then()
               .statusCode(404);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/cms/__EXPORTS/dump.zip")
                                                 .then()
                                                 .statusCode(403),
                PLAIN_USER, PASSWORD);

        // ... and the roles that MAY read an export still get past the guard (the export download in
        // the IDE shell must keep working - this request only fails later, on the missing document)
        securityUtil.ensureUserInDefaultTenant(ADMIN_USER, PASSWORD, Roles.RoleNames.ADMINISTRATOR);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/cms/__EXPORTS/dump.zip")
                                                 .then()
                                                 .statusCode(not(403)),
                ADMIN_USER, PASSWORD);
    }

    /**
     * Data transfer copies data between datasources, and its websocket handler declares no roles at all
     * - so until it was gated at the URL layer, any authenticated user could drive one. The handshake
     * is an ordinary HTTP request, so it passes the security filter chain and a role-less caller is
     * rejected there, before any upgrade is attempted.
     */
    @Test
    void data_transfer_is_not_drivable_without_a_platform_role() {
        securityUtil.ensureUserInDefaultTenant(PLAIN_USER, PASSWORD);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/websockets/data/transfer")
                                                 .then()
                                                 .statusCode(403),
                PLAIN_USER, PASSWORD);
    }

    /**
     * A task's variables carry its business payload, so reading them is scoped to the caller's own
     * inbox exactly like acting on the task - a bare task id must not address them.
     */
    @Test
    void task_variables_are_not_readable_for_a_foreign_task() {
        securityUtil.ensureUserInDefaultTenant(PLAIN_USER, PASSWORD);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/inbox/tasks/1234567890/variables")
                                                 .then()
                                                 .statusCode(403),
                PLAIN_USER, PASSWORD);
    }

    /**
     * A page of another site that a logged in user visits can post a form to a generated create
     * endpoint, and the browser attaches the session cookie: CSRF tokens are disabled on every chain.
     * The cross-site request filter refuses such a write on the browser's own word -
     * {@code Sec-Fetch-Site}, or {@code Origin} where an older browser sends no fetch metadata - before
     * any endpoint or authentication sees it, while the same write from the platform's own page passes
     * (#7644).
     */
    @Test
    void a_cross_site_form_post_with_the_session_cookie_is_refused() {
        securityUtil.ensureUserInDefaultTenant(ADMIN_USER, PASSWORD, Roles.RoleNames.ADMINISTRATOR);
        String session = formLoginSession(ADMIN_USER, PASSWORD);

        given().cookie("JSESSIONID", session)
               .header("Sec-Fetch-Site", "cross-site")
               .header("Origin", FOREIGN_ORIGIN)
               .contentType(ContentType.URLENC)
               .formParam("Name", "forged")
               .when()
               .post(GENERATED_CREATE_ENDPOINT)
               .then()
               .statusCode(403);

        given().cookie("JSESSIONID", session)
               .header("Origin", FOREIGN_ORIGIN)
               .contentType(ContentType.URLENC)
               .formParam("Name", "forged")
               .when()
               .post(GENERATED_CREATE_ENDPOINT)
               .then()
               .statusCode(403);

        given().cookie("JSESSIONID", session)
               .header("Sec-Fetch-Site", "same-origin")
               .contentType(ContentType.JSON)
               .body("{\"Name\":\"own\"}")
               .when()
               .post(GENERATED_CREATE_ENDPOINT)
               .then()
               .statusCode(not(403));
    }

    /**
     * The session cookie keeps itself out of cross-site posts and scripts, and every response tells the
     * browser not to frame the page for another site, not to sniff it and not to leak its URL (#7644).
     */
    @Test
    void the_session_cookie_and_the_responses_carry_the_browser_protections() {
        securityUtil.ensureUserInDefaultTenant(ADMIN_USER, PASSWORD, Roles.RoleNames.ADMINISTRATOR);

        Response login = formLogin(ADMIN_USER, PASSWORD);
        login.then()
             .header("Set-Cookie", containsStringIgnoringCase("SameSite=Lax"))
             .header("Set-Cookie", containsStringIgnoringCase("HttpOnly"));

        given().when()
               .get("/index.html")
               .then()
               .header("X-Frame-Options", "SAMEORIGIN")
               .header("Referrer-Policy", "strict-origin-when-cross-origin")
               .header("X-Content-Type-Options", "nosniff");
        // an authenticated answer carries them as well
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations")
                                                 .then()
                                                 .statusCode(200)
                                                 .header("X-Frame-Options", "SAMEORIGIN")
                                                 .header("Referrer-Policy", "strict-origin-when-cross-origin"),
                ADMIN_USER, PASSWORD);
    }

    private static Response formLogin(String user, String password) {
        return given().redirects()
                      .follow(false)
                      .contentType(ContentType.URLENC)
                      .formParam("username", user)
                      .formParam("password", password)
                      .when()
                      .post("/login");
    }

    private static String formLoginSession(String user, String password) {
        String session = formLogin(user, password).getCookie("JSESSIONID");
        assertNotNull(session, "the form login answers with the session cookie");
        return session;
    }
}
