/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The signature scheme, pinned by the test vector the documentation carries: a receiver in any
 * language checks its own verification against these values.
 */
class WebhookSignerTest {

    /** The test vector. */
    static final String SECRET = "whsec_dirigible_test_vector_secret";
    static final String PREVIOUS_SECRET = "whsec_dirigible_previous_secret_xx";
    static final long TIMESTAMP = 1763380800L;
    static final String BODY = "{\"id\":\"3f1c2a54-8a0e-4d0b-9c5e-2f6a1b7d9e10\",\"type\":\"hr-Leave-Leave-transitioned\","
            + "\"tenant\":\"acme\",\"occurredAt\":\"2026-11-17T12:00:00Z\",\"data\":{\"id\":42,\"status\":3}}";
    static final String EXPECTED = "8123b05cc7b122a4bf2eeb154d1fdfc0d8726c30f52791d0d494e83382d2941b";
    static final String EXPECTED_PREVIOUS = "0011e970851bdccd2b1331ae74d9520427b4abe1fd59b52ac0b9e18964e682c6";

    @Test
    void signsTheTimestampDotBodyWithHmacSha256InLowerCaseHex() {
        assertEquals(EXPECTED, WebhookSigner.sign(SECRET, TIMESTAMP, BODY));
    }

    @Test
    void theHeaderCarriesOneSignaturePerActiveSecretCurrentFirst() {
        assertEquals("v1=" + EXPECTED, WebhookSigner.signatureHeader(List.of(SECRET), TIMESTAMP, BODY));
        assertEquals("v1=" + EXPECTED + ",v1=" + EXPECTED_PREVIOUS,
                WebhookSigner.signatureHeader(List.of(SECRET, PREVIOUS_SECRET), TIMESTAMP, BODY));
    }

    @Test
    void aWrongSecretOrAnotherTimestampSignsDifferently() {
        assertNotEquals(EXPECTED, WebhookSigner.sign(SECRET + "x", TIMESTAMP, BODY));
        assertNotEquals(EXPECTED, WebhookSigner.sign(SECRET, TIMESTAMP + 1, BODY));
        assertNotEquals(EXPECTED, WebhookSigner.sign(SECRET, TIMESTAMP, BODY + " "));
    }
}
