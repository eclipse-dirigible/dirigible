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
import static org.hamcrest.Matchers.equalTo;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;

/**
 * Pins the reason of a {@code ResponseStatusException} into the JSON error body.
 * <p>
 * The authored message is the only thing that tells a caller which of several 400s it got, and it
 * is what a user interface has left to show; there are ~200 throw sites across the platform relying
 * on it. It reaches the body through the {@code include-message} error attribute option, whose
 * property Spring Boot 4.0 renamed from {@code server.error.include-message} to
 * {@code spring.web.error.include-message} - deprecating the old key at level {@code error}, i.e.
 * not binding it at all. The platform kept the old spelling and so answered every REST error with a
 * bare status phrase, silently: the property was still loaded, still visible on
 * {@code /actuator/env}, and simply had no effect.
 * <p>
 * Two unrelated endpoints are driven on purpose - a rename regression is platform-wide, so a single
 * endpoint's assertion would not distinguish it from that endpoint's own error handling.
 */
// One Dirigible boot for the whole class: both methods are read-only, so the per-method context
// reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RestErrorMessageIT extends IntegrationTest {

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void not_found_carries_its_reason() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/messaging-monitoring/queues/no-such-queue-6994/messages")
                                                 .then()
                                                 .statusCode(404)
                                                 .body("message", equalTo("Queue [no-such-queue-6994] not found")));
    }

    @Test
    void bad_request_carries_its_reason() {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body("{}")
                                                 .when()
                                                 .post("/services/ide/java-lsp/diagnostics")
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", equalTo("Missing 'workspace'")));
    }

}
