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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;

public class StoreAPISampleProjectIT extends SampleProjectRepositoryIT {

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

    @Override
    protected void verifyProject() {
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
                });
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-store-api.git";
    }

}

