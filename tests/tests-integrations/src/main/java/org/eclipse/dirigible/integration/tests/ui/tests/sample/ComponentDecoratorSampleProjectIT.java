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

public class ComponentDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    @Override
    protected void verifyProject() {
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/ts/sample-component-decorators/OrderProcessor.ts")
                             .then()
                             .statusCode(200)
                             .body(equalToCompressingWhiteSpace("Do Payment: {\"status\":\"OK\",\"data\":\"123.45\"}")));
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-component-decorators.git";
    }

}
