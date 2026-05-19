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

/**
 * Clones {@code dirigiblelabs/sample-java-entity-decorators}, publishes it through the IDE, and
 * verifies that the {@code @Entity} / {@code @Repository} / {@code @Controller} stack from
 * {@code engine-java} + {@code data-store-java} is wired end-to-end: POSTing rows via the
 * controller persists them, the same controller lists them back, and the OpenAPI aggregator picks
 * up the controller's routes.
 */
public class JavaEntityDecoratorsSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String PROJECT = "sample-java-entity-decorators";

    /**
     * Controller base. {@code @Get("/")} and {@code @Post} both bind here; the HTTP method
     * disambiguates list vs. create.
     */
    private static final String CONTROLLER_BASE = "/services/java/" + PROJECT + "/demo/CountryController";

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-entity-decorators.git";
    }

    @Override
    protected void verifyProject() {
        restAssuredExecutor.execute(() -> {
            // Seed three rows via the controller's @Post @Body route — proves @Inject
            // CountryRepository resolved, the COUNTRIES table exists, and Jackson deserialised
            // each request body into a Country instance.
            postCountry("AF", "AFG", "004", "Afghanistan");
            postCountry("AL", "ALB", "008", "Albania");
            postCountry("DZ", "DZA", "012", "Algeria");

            // GET on the bare base URL → @Get("/") list route.
            given().when()
                   .get(CONTROLLER_BASE)
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"))
                   .body(containsString("Albania"))
                   .body(containsString("Algeria"));

            // GET /{id} — exercises @PathParam binding and JavaRepository.findById. Ids are
            // generated in insertion order starting at 1.
            given().when()
                   .get(CONTROLLER_BASE + "/1")
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"));

            // /services/openapi merges every stored OpenAPI artefact; engine-java contributes
            // one fragment per controller class at registration time.
            given().when()
                   .get("/services/openapi")
                   .then()
                   .statusCode(200)
                   .body(containsString(CONTROLLER_BASE))
                   .body(containsString(CONTROLLER_BASE + "/{id}"));
        });
    }

    private static void postCountry(String code2, String code3, String numericCode, String name) {
        String body = String.format("{\"code2\":\"%s\",\"code3\":\"%s\",\"numericCode\":\"%s\",\"name\":\"%s\"}", code2, code3, numericCode,
                name);
        given().contentType("application/json")
               .body(body)
               .when()
               .post(CONTROLLER_BASE)
               .then()
               .statusCode(200)
               .body(containsString(name));
    }

}
