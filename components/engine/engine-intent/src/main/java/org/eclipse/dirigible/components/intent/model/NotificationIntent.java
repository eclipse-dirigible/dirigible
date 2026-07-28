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
 * It is a self-contained reaction (event -> send). {@link #event} is a free-form map carrying
 * exactly one of {@code onCreate} / {@code onUpdate} / {@code onDelete} naming a declared entity,
 * plus an optional {@code when} guard expression. (The key is {@code event}, not {@code on} - YAML
 * 1.1 resolves a bare {@code on} to the boolean {@code true}.) The generator emits an annotated
 * client-Java {@code @Listener} that binds to the entity's event topic and sends via the SDK;
 * {@link #to} is a direct field of the event entity or a literal address.
 *
 * <p>
 * The same shape is the reusable <b>notify block</b>: it is embedded in a
 * {@code schedules[].notify}, a {@code transitions[].notify} and a {@code serviceTask}'s
 * {@code args.notify} - the three places an intent can act - where {@link #name} / {@link #event}
 * are unused (the call site IS the event) and {@link #attach} may name the document to send along.
 * {@code attach: print} renders the record's {@code .print} template to PDF server-side and
 * attaches it, which is how a business document (an invoice to its customer, a payslip to its
 * employee) is mailed declaratively.
 */
public class NotificationIntent {

    private String name;
    private Map<String, Object> event = new LinkedHashMap<>();
    private String channel = "email";
    private String to;
    private String subject;
    private String body;
    /**
     * Optional document to attach: {@code print} renders the record's {@code .print} template to PDF
     * (through the entity's generated print feeder) and attaches it. Blank = a plain-text message.
     */
    private String attach;
    /** The print template language for {@link #attach} (a {@code languages:} code); defaults to en. */
    private String language;

    /**
     * Read an <b>embedded</b> notify block off a free-form map - a process step's {@code args.notify},
     * whose args are untyped by design. {@code name} / {@code event} stay unset: the call site is the
     * event.
     *
     * @param raw the map, or any other value (including {@code null}) for "no notify block"
     * @return the notify block, or {@code null} when {@code raw} is not a map
     */
    public static NotificationIntent fromMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        NotificationIntent notify = new NotificationIntent();
        notify.setTo(string(map.get("to")));
        notify.setSubject(string(map.get("subject")));
        notify.setBody(string(map.get("body")));
        notify.setAttach(string(map.get("attach")));
        notify.setLanguage(string(map.get("language")));
        String channel = string(map.get("channel"));
        if (channel != null) {
            notify.setChannel(channel);
        }
        return notify;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

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

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
