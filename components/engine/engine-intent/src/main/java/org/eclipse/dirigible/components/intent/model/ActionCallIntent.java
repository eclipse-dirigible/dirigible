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

import java.util.Map;

/**
 * The request an {@link ActionIntent} sends to the application's OWN endpoint (issue #8090) - a
 * hand-written controller that owns the business rule, so the button needs neither a page nor a
 * generated controller in between.
 *
 * <pre>
 * call:
 *   method: POST
 *   path: /services/java/helpdesk/custom/api/TicketApi/tickets/{id}/escalations
 *   body: { kind: MANUAL, level: "{level}", note: "{note}" }
 * </pre>
 *
 * {@code {id}} in the path is the clicked record's primary key; a body string that is exactly
 * {@code "{name}"} is that prompt input, typed, and {@code {name}} inside a longer string
 * interpolates as text. Every other body value is a constant, sent as written.
 */
public class ActionCallIntent {

    /** {@code POST} (default), {@code PUT} or {@code DELETE}. */
    private String method = "POST";

    /** An absolute same-origin path under {@code /services/}; {@code {id}} is its one placeholder. */
    private String path;

    /** The JSON body, or {@code null} to send none. */
    private Map<String, Object> body;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public void setBody(Map<String, Object> body) {
        this.body = body;
    }
}
