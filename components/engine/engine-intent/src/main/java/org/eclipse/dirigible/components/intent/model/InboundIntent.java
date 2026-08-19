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
 * An inbound ingest: something outside the application hands us a JSON record and we create it. The
 * "another system tells us" pattern of the declarative-glue catalog.
 *
 * <p>
 * The payload is saved into {@link #create} through that entity's repository; only where it arrives
 * from differs. {@link #path} declares the HTTP shape - a client-Java {@code @Controller} with a
 * {@code @Post} at that path. {@link #source} declares a non-HTTP one: a messaging destination (a
 * {@code MessageHandler} bound to the queue/topic) or a drop folder (a {@code JobHandler} polling
 * it on the declared cron). Exactly one of the two is declared. Upsert / start-process actions are
 * later increments.
 *
 * <p>
 * Without a {@link #map}, the payload deserializes straight into the entity - which works only when
 * the sender's JSON already <em>is</em> the entity, field for field. A real arrival contract is an
 * envelope, so {@link #map} projects its keys onto the entity's own, and {@link #accept} gates on
 * the envelope's type and version:
 *
 * <pre>
 * accept: { type: user.assignment.requested, version: 1 }
 * map:
 *   messageId: messageId
 *   tenant:    { lookup: Tenant, by: tenantId, from: tenantId }
 * </pre>
 *
 * A {@code lookup} value resolves a <b>business key to a relation</b> - the single most common
 * requirement of any arrival, and on its own the reason a modelled arrival still needed a
 * hand-written consumer. A non-matching {@code accept} is acknowledged and ignored with a warning
 * (never failed into redelivery), while an unresolvable lookup rejects the arrival rather than
 * storing a null relation.
 */
public class InboundIntent {

    private String name;
    private String path;
    private String create;
    private InboundSourceIntent source;
    private Map<String, Object> accept = new LinkedHashMap<>();
    private Map<String, Object> map = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCreate() {
        return create;
    }

    public void setCreate(String create) {
        this.create = create;
    }

    public InboundSourceIntent getSource() {
        return source;
    }

    public void setSource(InboundSourceIntent source) {
        this.source = source;
    }

    public Map<String, Object> getAccept() {
        return accept == null ? new LinkedHashMap<>() : accept;
    }

    public void setAccept(Map<String, Object> accept) {
        this.accept = accept;
    }

    public Map<String, Object> getMap() {
        return map == null ? new LinkedHashMap<>() : map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }
}
