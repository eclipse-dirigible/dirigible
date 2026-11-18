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

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * The thin event: the record's id, {@code Uuid} and status, nothing else of it.
 */
class WebhookEventBodyTest {

    private static final Instant AT = Instant.parse("2026-11-17T12:00:00Z");

    private static final String ROW = "{\"Id\":42,\"Uuid\":\"6b1e\",\"Status\":3,\"Employee\":\"ann@acme.test\",\"Note\":\"surgery\","
            + "\"__derivedWrite\":[\"Balance\"]}";

    @Test
    void carriesOnlyTheIdTheUuidAndTheStatus() {
        WebhookEventType type = new WebhookEventType("hr-Leave-Leave-transitioned", "transitioned", "hr", "Leave", "Id", "Uuid", "Status");
        assertEquals(
                "{\"id\":\"evt-1\",\"type\":\"hr-Leave-Leave-transitioned\",\"tenant\":\"acme\",\"occurredAt\":\"2026-11-17T12:00:00Z\","
                        + "\"data\":{\"id\":42,\"uuid\":\"6b1e\",\"status\":3}}",
                WebhookEventBody.build("evt-1", type, "acme", AT, ROW));
    }

    @Test
    void leavesOutWhatTheEntityDoesNotHave() {
        WebhookEventType type = new WebhookEventType("hr-Leave-Leave", "created", "hr", "Leave", "Id", null, null);
        assertEquals("{\"id\":\"evt-2\",\"type\":\"hr-Leave-Leave\",\"tenant\":\"acme\",\"occurredAt\":\"2026-11-17T12:00:00Z\","
                + "\"data\":{\"id\":42}}", WebhookEventBody.build("evt-2", type, "acme", AT, ROW));
    }
}
