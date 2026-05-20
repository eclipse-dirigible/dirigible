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
 * {@code engine-java} + {@code data-store-java} reads the project's CSVIM-seeded data end-to-end:
 * the {@code SAMPLE_COUNTRY} table is created and populated from {@code data/countries.csv}, the
 * {@code @Inject}-ed {@code CountryRepository} lists those rows through {@code @Get("/")}, a single
 * row is fetched through {@code @Get("/{id}")} / {@code @PathParam}, and the OpenAPI aggregator
 * picks up the controller's routes.
 */
public class JavaEntityDecoratorsSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String PROJECT = "sample-java-entity-decorators";

    /** Controller base — {@code @Get("/")} lists, {@code @Get("/{id}")} fetches one. */
    private static final String CONTROLLER_BASE = "/services/java/" + PROJECT + "/demo/CountryController";

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-entity-decorators.git";
    }

    @Override
    protected void verifyProject() {
        restAssuredExecutor.execute(() -> {
            // GET on the bare base URL → @Get("/") list route. Proves @Inject CountryRepository
            // resolved and the SAMPLE_COUNTRY table was seeded from data/countries.csv via CSVIM.
            given().when()
                   .get(CONTROLLER_BASE)
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"))
                   .body(containsString("Albania"))
                   .body(containsString("Algeria"));

            // GET /{id} — exercises @PathParam binding and JavaRepository.findById against the
            // seeded COUNTRY_ID primary key (countries.csv assigns Afghanistan id 1).
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

}
