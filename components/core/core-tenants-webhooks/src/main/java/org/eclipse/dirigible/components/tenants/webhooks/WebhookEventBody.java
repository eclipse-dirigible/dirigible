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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The body of a webhook delivery - a thin event, naming what happened and none of the record's
 * content:
 *
 * <pre>
 * {"id": "&lt;event id&gt;", "type": "hr-Leave-Leave-transitioned", "tenant": "&lt;tenant id&gt;",
 *  "occurredAt": "2026-11-17T12:00:00Z", "data": {"id": 42, "uuid": "...", "status": 3}}
 * </pre>
 *
 * <p>
 * {@code data.uuid} only for an entity with a {@code Uuid} property, {@code data.status} only for
 * one with a {@code function: EntityStatus} relation. The receiver reads the record back through
 * its integration client, so a forged or replayed event can at most cause a harmless re-read, and
 * no personal data travels in a delivery.
 */
final class WebhookEventBody {

    private static final ObjectMapper JSON = new ObjectMapper();

    private WebhookEventBody() {}

    /**
     * Builds a body.
     *
     * @param eventId the event id
     * @param type the declared event type
     * @param tenantId the tenant
     * @param occurredAt when the event was recorded
     * @param entityPayload the entity event's payload - the JSON of the written row
     * @return the body, compact JSON
     */
    static String build(String eventId, WebhookEventType type, String tenantId, Instant occurredAt, String entityPayload) {
        JsonNode entity = parse(entityPayload);
        ObjectNode body = JSON.createObjectNode();
        body.put("id", eventId);
        body.put("type", type.type());
        body.put("tenant", tenantId);
        body.put("occurredAt", occurredAt.truncatedTo(ChronoUnit.MILLIS)
                                         .toString());
        ObjectNode data = body.putObject("data");
        copy(entity, type.idProperty(), data, "id");
        copy(entity, type.uuidProperty(), data, "uuid");
        copy(entity, type.statusProperty(), data, "status");
        return JSON.writeValueAsString(body);
    }

    private static void copy(JsonNode entity, String property, ObjectNode data, String field) {
        if (property == null) {
            return;
        }
        JsonNode value = entity.get(property);
        if (value != null && (value.isValueNode() || value.isNull())) {
            data.set(field, value);
        }
    }

    private static JsonNode parse(String payload) {
        if (payload == null) {
            return JSON.createObjectNode();
        }
        try {
            JsonNode parsed = JSON.readTree(payload);
            return parsed.isObject() ? parsed : JSON.createObjectNode();
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("An entity event's payload is not JSON", ex);
        }
    }
}
