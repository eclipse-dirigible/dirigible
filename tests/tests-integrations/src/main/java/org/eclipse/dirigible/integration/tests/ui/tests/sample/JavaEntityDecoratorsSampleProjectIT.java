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
 * verifies the {@code @Entity} / {@code @Repository} / {@code @Controller} annotation stack with
 * CSVIM-seeded country CRUD and OpenAPI registration.
 */
@org.junit.jupiter.api.Disabled("Temporarily disabled: clones the dirigiblelabs sample repo whose master is mid-migration to the new client-Java API (eclipse-dirigible/dirigible PR 6051). Re-enable once the matching sample PR is merged.")
public class JavaEntityDecoratorsSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String PROJECT = "sample-java-entity-decorators";
    private static final String CONTROLLER_BASE = "/services/java/" + PROJECT + "/demo/CountryController";

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-entity-decorators.git";
    }

    @Override
    protected void verifyProject() {
        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(CONTROLLER_BASE)
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"))
                   .body(containsString("Albania"))
                   .body(containsString("Algeria"));

            given().when()
                   .get(CONTROLLER_BASE + "/1")
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"));

            given().when()
                   .get("/services/openapi")
                   .then()
                   .statusCode(200)
                   .body(containsString(CONTROLLER_BASE))
                   .body(containsString(CONTROLLER_BASE + "/{id}"));
        });
    }

}
