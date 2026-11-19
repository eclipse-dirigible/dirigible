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

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * An {@code Accept-Language} header listing more languages than Tomcat's {@code maxLocaleCount}
 * (10) is a 400, not the unhandled 500 {@code RequestContextFilter} turned it into (#8709). Driven
 * against the real connector, the only place the refusal is raised.
 */
// One Dirigible boot for the whole class: both methods are read-only.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AcceptLanguageLimitIT extends IntegrationTest {

    private static final String HEALTH = "/actuator/health/liveness";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void more_languages_than_the_connector_accepts_is_a_bad_request() {
        restAssuredExecutor.execute(() -> given().header("Accept-Language", "a,b,c,d,e,f,g,h,i,j,k")
                                                 .when()
                                                 .get(HEALTH)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", containsString("Accept-Language")));
    }

    @Test
    void as_many_languages_as_the_connector_accepts_is_served() {
        restAssuredExecutor.execute(() -> given().header("Accept-Language", "a,b,c,d,e,f,g,h,i,j")
                                                 .when()
                                                 .get(HEALTH)
                                                 .then()
                                                 .statusCode(200));
    }
}
