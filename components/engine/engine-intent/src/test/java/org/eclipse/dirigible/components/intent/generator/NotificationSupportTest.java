/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.junit.jupiter.api.Test;

class NotificationSupportTest {

    private static NotificationIntent on(String kind, String entity) {
        NotificationIntent notification = new NotificationIntent();
        notification.setEvent(new java.util.LinkedHashMap<>(Map.of(kind, entity)));
        return notification;
    }

    @Test
    void resolvesEventAndTopicSuffix() {
        assertEquals("onUpdate", NotificationSupport.eventKind(on("onUpdate", "Loan")));
        assertEquals("Loan", NotificationSupport.eventEntity(on("onUpdate", "Loan")));
        assertEquals("", NotificationSupport.topicSuffix("onCreate"));
        assertEquals("-updated", NotificationSupport.topicSuffix("onUpdate"));
        assertEquals("-deleted", NotificationSupport.topicSuffix("onDelete"));
    }

    @Test
    void recipientIsLiteralOrFieldAccess() {
        assertEquals("\"ops@example.com\"", NotificationSupport.recipientExpression("ops@example.com"));
        assertEquals("entity.Email", NotificationSupport.recipientExpression("email"));
    }

    @Test
    void interpolatesPlaceholdersIntoStringExpression() {
        assertEquals("\"Welcome \" + entity.Name", NotificationSupport.interpolate("Welcome {name}"));
        assertEquals("\"\" + entity.Name", NotificationSupport.interpolate("{name}"));
        assertEquals("\"plain text\"", NotificationSupport.interpolate("plain text"));
        assertEquals("\"Loan \" + entity.Id + \" approved\"", NotificationSupport.interpolate("Loan {id} approved"));
    }

    @Test
    void guardTranslatesSingleComparisonElseFiresAlways() {
        assertEquals("true", NotificationSupport.guard(null));
        assertEquals("true", NotificationSupport.guard("  "));
        assertEquals("java.util.Objects.equals(entity.Status, \"APPROVED\")", NotificationSupport.guard("status == 'APPROVED'"));
        assertEquals("!java.util.Objects.equals(entity.Status, \"CLOSED\")", NotificationSupport.guard("status != 'CLOSED'"));
        // Unsupported (multi-term) expressions degrade to firing on every event rather than emitting broken
        // Java.
        assertEquals("true", NotificationSupport.guard("amount > 10 and status == 'X'"));
    }
}
