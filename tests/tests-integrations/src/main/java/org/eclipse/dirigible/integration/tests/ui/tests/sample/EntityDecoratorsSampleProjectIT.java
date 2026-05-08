/*
 * Copyright (c) 2022 codbex or an codbex affiliate company and contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 codbex or an codbex affiliate company and contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;

public class EntityDecoratorsSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String COUNTRIES_RESPONSE_BODY =
            """
                    [{"Code2":"AF","Numeric":"004","Code3":"AFG","Id":1,"$type$":"CountryEntity","Name":"Afghanistan"},{"Code2":"AL","Numeric":"008","Code3":"ALB","Id":2,"$type$":"CountryEntity","Name":"Albania"},{"Code2":"DZ","Numeric":"012","Code3":"DZA","Id":3,"$type$":"CountryEntity","Name":"Algeria"}]
                    """;

    private static final String OPENAPI_RESPONSE_BODY =
            """
                    {"openapi":"3.0.1","info":{"title":"Applications Services Open API","description":"Services Open API provided by the applications","contact":{"name":"Eclipse Dirigible","url":"https://www.dirigible.io","email":"dirigible-dev@eclipse.org"},"license":{"name":"Eclipse Public License - v 2.0","url":"https://www.eclipse.org/legal/epl-v20.html"},"version":"${project.version}"},"servers":[{"url":"/services/ts"},{"url":"/services/ts"}],"security":[],"tags":[],"paths":{"/sample-entity-decorators/CountryController.ts/":{"get":{"tags":["CountryController"],"summary":"getAll CountryController ","operationId":"getAll","parameters":[],"responses":{"200":{"description":"Success","content":{"application/json":{"schema":{"type":"array","items":{"$ref":"#/components/schemas/CountryEntity"}}}}}}}}},"components":{"schemas":{"number":{"type":"number"},"CountryEntity":{"type":"object","properties":{"Code2":{"type":"string"},"Numeric":{"type":"string"},"Code3":{"type":"string"},"Id":{"type":"number","description":"My Id"},"Name":{"type":"string","description":"My Name"}},"description":"Sample Country Entity"},"string":{"type":"string"},"any":{"type":"object"}},"responses":{},"parameters":{},"examples":{},"requestBodies":{},"headers":{},"securitySchemes":{},"links":{},"callbacks":{}}}
                    """;

    @Override
    protected void verifyProject() {
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
                    given().when()
                           .get("/services/openapi")
                           .then()
                           .statusCode(200)
                           .body(equalToCompressingWhiteSpace(OPENAPI_RESPONSE_BODY));
                });
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-entity-decorators.git";
    }

}
