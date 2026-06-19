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
 * An inbound webhook: expose an HTTP endpoint that ingests a JSON payload into an entity. The
 * "another system tells us" pattern of the declarative-glue catalog.
 *
 * <p>
 * Generates a client-Java {@code @Controller} with a {@code @Post} that deserializes the request
 * body into {@link #create} and saves it through the entity's repository. {@link #path} is the
 * endpoint suffix. Upsert / start-process actions are later increments.
 */
public class InboundIntent {

    private String name;
    private String path;
    private String create;

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
}
