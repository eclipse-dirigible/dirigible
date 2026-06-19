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
 * A declarative notification: send a message when an entity event fires. The first concrete pattern
 * of the declarative-glue catalog (see the engine guide's "Planned: declarative glue").
 *
 * <p>
 * It is a self-contained reaction (event -> send). {@link #on} is a free-form map carrying exactly
 * one of {@code onCreate} / {@code onUpdate} / {@code onDelete} naming a declared entity, plus an
 * optional {@code when} guard expression - the same shape a process trigger uses. The generator (a
 * later increment) emits an annotated client-Java {@code @Listener} that binds to the entity's
 * event topic and sends via the SDK; {@link #to} is a resolver path (e.g. {@code member.email}).
 */
public class NotificationIntent {

    private String name;
    private Map<String, Object> on = new LinkedHashMap<>();
    private String channel = "email";
    private String to;
    private String subject;
    private String body;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getOn() {
        return on;
    }

    public void setOn(Map<String, Object> on) {
        this.on = on == null ? new LinkedHashMap<>() : on;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
