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
import static org.hamcrest.Matchers.containsString;

import io.restassured.http.ContentType;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end IT for the {@code local} native-app kind. Clones
 * {@code dirigiblelabs/sample-library-local-native-app}, publishes it through the IDE, and verifies
 * the platform:
 * <ol>
 * <li>Registers the {@code library-admin} role from {@code roles.roles}.</li>
 * <li>Enforces the role on {@code /rest/api/v1} via {@code security.exposedPaths.scopes} —
 * unauthorised callers get {@code 403}.</li>
 * <li>Spawns the Node process on demand and proxies CRUD calls through with the basic-auth header
 * injected by Dirigible once the caller is granted {@code library-admin}.</li>
 * </ol>
 *
 * <p>
 * Counterpart to {@code RemoteNativeAppIT} which exercises the {@code remote} kind.
 */
public class SampleLibraryLocalNativeAppIT extends SampleProjectRepositoryIT {

    private static final String API_ROOT = "/services/native-apps-proxy/v1/library-native-app-nodejs/rest/api/v1";

    private static final String BASE = API_ROOT + "/books";

    private static final String LIBRARY_INFO = API_ROOT + "/library";

    private static final String LIBRARY_ADMIN_ROLE = "library-admin";

    private static final String READER_USERNAME = "library-reader";
    private static final String READER_PASSWORD = "library-reader-pass";

    /**
     * Values declared in the sample repo's {@code .native-app} via the start-command
     * {@code arguments[]} ({@code --library-address}, {@code --library-phone}). The IT asserts that GET
     * {@code /library} returns exactly these values, proving start arguments reach the spawned Node
     * process and influence its runtime configuration.
     */
    private static final String EXPECTED_LIBRARY_ADDRESS = "42 Wallaby Way, Sydney";
    private static final String EXPECTED_LIBRARY_PHONE = "+61-2-9999-0042";

    @Autowired
    private SecurityUtil securityUtil;

    @BeforeAll
    static void allowTimeForFirstRunBootstrap() {
        // The artefact's start command runs `npm install --silent ... && npm run build:start` on
        // first spawn, which dwarfs the platform's 30 s default ready timeout when node_modules has
        // to be fetched. Five minutes is comfortably above worst-case cold-cache install + tsc on
        // CI runners; the lazy-start filter polls every 200 ms so a fast warm run still wins early.
        DirigibleConfig.NATIVE_APP_READY_TIMEOUT_MS.setIntValue(5 * 60 * 1000);
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-library-local-native-app.git";
    }

    @Override
    protected void verifyProject() {
        // The default 'admin' user holds DEVELOPER + ADMINISTRATOR, which short-circuits the scope
        // check in ExposedPathFilter — so we can't use it to exercise the negative path. Create a
        // dedicated user with no privileged roles, then watch the proxy flip from 403 to 200/201
        // once 'library-admin' is granted.
        securityUtil.createUserInDefaultTenant(READER_USERNAME, READER_PASSWORD);

        // 1. Without the library-admin role, every call to the whitelisted path must be rejected.
        restAssuredExecutor.execute(this::expectForbiddenForReader, READER_USERNAME, READER_PASSWORD);

        // 2. Grant the role and the very next call must succeed end-to-end through the proxy.
        String defaultTenantId = DirigibleTestTenant.createDefaultTenant()
                                                    .getId();
        securityUtil.assignRoleToUser(READER_USERNAME, defaultTenantId, LIBRARY_ADMIN_ROLE);

        restAssuredExecutor.execute(this::exerciseBookCrud, READER_USERNAME, READER_PASSWORD);

        // 3. Independent of CRUD: assert the .native-app's start arguments reached the spawned
        // Node process. Lives outside exerciseBookCrud because it tests the artefact's
        // arguments[] propagation contract, not the book CRUD behaviour.
        restAssuredExecutor.execute(this::assertStartArgumentsReachedNodeProcess, READER_USERNAME, READER_PASSWORD);
    }

    private void expectForbiddenForReader() {
        given().when()
               .get(BASE)
               .then()
               .statusCode(403);
    }

    private void exerciseBookCrud() {
        given().contentType(ContentType.JSON)
               .body("{\"title\":\"The Hobbit\",\"author\":\"J.R.R. Tolkien\",\"isbn\":\"9780547928227\"}")
               .when()
               .post(BASE)
               .then()
               .statusCode(anyOf(200, 201))
               .body(containsString("The Hobbit"));

        given().when()
               .get(BASE)
               .then()
               .statusCode(200)
               .body(containsString("The Hobbit"));
    }

    /**
     * The {@code .native-app}'s {@code lifecycle.start.commands[].arguments[]} include
     * {@code --library-address} / {@code --library-phone}; the Node app surfaces those at
     * {@code /library}. Asserting on the response proves the artefact's {@code arguments[]} entries
     * reach the spawned process and influence its runtime configuration.
     */
    private void assertStartArgumentsReachedNodeProcess() {
        given().when()
               .get(LIBRARY_INFO)
               .then()
               .statusCode(200)
               .body(containsString(EXPECTED_LIBRARY_ADDRESS))
               .body(containsString(EXPECTED_LIBRARY_PHONE));
    }

    private static org.hamcrest.Matcher<Integer> anyOf(int a, int b) {
        return org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.equalTo(a), org.hamcrest.Matchers.equalTo(b));
    }
}
