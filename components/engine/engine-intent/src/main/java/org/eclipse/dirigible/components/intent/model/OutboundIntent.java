/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An outbound departure: when an event fires, emit a message on a queue or a topic. The mirror of
 * {@link InboundIntent} - the "raise a business event for another system" pattern, next to
 * {@link IntegrationIntent}'s "call another system's API".
 *
 * <p>
 * The two are deliberately separate constructs rather than one with a transport switch: an
 * integration calls and is answered (so a failure is a failed call), while a departure is emitted
 * and forgotten (so a failure is a recorded, non-fatal miss). They share everything else -
 * {@link #event} is the same glue event axis (an entity lifecycle event or a process-step event,
 * with the optional {@code when} guard), and {@link #payload} is the same declared envelope.
 *
 * <p>
 * {@link #to} names exactly one channel. With no {@link #payload} the body is the record's JSON,
 * exactly what an integration forwards over HTTP today.
 *
 * <p>
 * <b>Delivery semantics, stated rather than implied:</b> the message is published after the write
 * that raised the event is persisted, and is NOT transactional with it - a failed publish is logged
 * and the write stands. There is no outbox, no exactly-once delivery and no ordering guarantee.
 */
public class OutboundIntent {

    private String name;
    private Map<String, Object> event = new LinkedHashMap<>();
    private OutboundTargetIntent to;
    private Map<String, Object> payload = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getEvent() {
        return event;
    }

    public void setEvent(Map<String, Object> event) {
        this.event = event == null ? new LinkedHashMap<>() : event;
    }

    public OutboundTargetIntent getTo() {
        return to;
    }

    public void setTo(OutboundTargetIntent to) {
        this.to = to;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new LinkedHashMap<>() : payload;
    }
}
