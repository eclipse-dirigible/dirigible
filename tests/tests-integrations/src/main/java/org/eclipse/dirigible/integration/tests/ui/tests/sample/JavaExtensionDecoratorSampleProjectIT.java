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

@org.junit.jupiter.api.Disabled("Temporarily disabled: clones the dirigiblelabs sample repo whose master is mid-migration to the new client-Java API (eclipse-dirigible/dirigible PR 6051). Re-enable once the matching sample PR is merged.")
public class JavaExtensionDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String PROJECT = "sample-java-extension-decorator";
    private static final String EXTENSION_CONSUMER_BASE = "/services/java/" + PROJECT + "/demo/extension/ExtensionConsumer";

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-extension-decorator.git";
    }

    @Override
    protected void verifyProject() {
        // The typed Extensions.find(SampleExtensionPoint.class) lookup returns
        // SampleContribution instances; the consumer maps each via describe() so the
        // /contributions response body carries the contribution's own string. Asserting on
        // that string also implicitly verifies the cast — only a real implementor reaches it.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(EXTENSION_CONSUMER_BASE + "/contributions")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Hello from SampleContribution!")));
    }

}
