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
 * An outbound integration: when an entity event fires, call an external HTTP endpoint. The "tell
 * another system" pattern of the declarative-glue catalog.
 *
 * <p>
 * {@link #event} is the same {@code onCreate}/{@code onUpdate}/{@code onDelete} binding the
 * notifications use. The generated {@code @Listener} forwards the entity JSON (the event payload)
 * to {@link #url} via {@link #method}. The URL is a literal or {@code @config:KEY} (read
 * server-side from the configuration). Custom body mapping and headers are a later increment.
 */
public class IntegrationIntent {

    private String name;
    private Map<String, Object> event = new LinkedHashMap<>();
    private String method = "POST";
    private String url;

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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
