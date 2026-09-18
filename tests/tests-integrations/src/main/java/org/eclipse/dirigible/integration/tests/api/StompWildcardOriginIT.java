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
import static org.hamcrest.Matchers.nullValue;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A wildcard in {@code DIRIGIBLE_CORS_ALLOWED_ORIGINS} serves bearer clients over HTTP from any
 * origin, but never reaches the STOMP handshake: a WebSocket handshake carries the session cookie,
 * so a page on any origin could otherwise open a STOMP session as a logged in user (#7415).
 */
// one boot for the class: the configuration is static and every case reads only
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StompWildcardOriginIT extends IntegrationTest {

    private static final String ANY_ORIGIN = "https://evil.example.com";

    @LocalServerPort
    private int port;

    @BeforeAll
    static void allowEveryOrigin() {
        // cleared by IntegrationTest.reloadConfigurations() once the class is done
        Configuration.set(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey(), "*");
    }

    @Test
    void thePlatformAnswersAPreflightFromAnyOriginWithoutCredentials() {
        given().port(port)
               .header("Origin", ANY_ORIGIN)
               .header("Access-Control-Request-Method", "GET")
               .when()
               .options("/services/core/version")
               .then()
               .statusCode(200)
               .header("Access-Control-Allow-Origin", ANY_ORIGIN)
               .header("Access-Control-Allow-Credentials", nullValue());
    }

    @Test
    void theStompEndpointRefusesTheOriginTheWildcardWouldAdmit() {
        given().port(port)
               .header("Origin", ANY_ORIGIN)
               .when()
               .get("/stomp/info")
               .then()
               .statusCode(403);
    }

    @Test
    void theStompEndpointStillServesSameOriginPages() {
        given().port(port)
               .when()
               .get("/stomp/info")
               .then()
               .statusCode(200);
    }
}
