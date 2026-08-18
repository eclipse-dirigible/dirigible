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

/**
 * An inbound ingest: something outside the application hands us a JSON record and we create it. The
 * "another system tells us" pattern of the declarative-glue catalog.
 *
 * <p>
 * The payload always deserializes into {@link #create} and is saved through that entity's
 * repository; only where it arrives from differs. {@link #path} declares the HTTP shape - a
 * client-Java {@code @Controller} with a {@code @Post} at that path. {@link #source} declares a
 * non-HTTP one: a messaging destination (a {@code MessageHandler} bound to the queue/topic) or a
 * drop folder (a {@code JobHandler} polling it on the declared cron). Exactly one of the two is
 * declared. Upsert / start-process actions are later increments.
 */
public class InboundIntent {

    private String name;
    private String path;
    private String create;
    private InboundSourceIntent source;

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
}
