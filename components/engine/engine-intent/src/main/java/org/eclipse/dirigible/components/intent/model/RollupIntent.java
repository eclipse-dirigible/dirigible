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
 * A denormalized roll-up counter: maintain an integer {@link #field} on a parent entity equal to
 * the number of {@link #entity} (child) rows pointing at it through the {@link #via} to-one
 * relation.
 *
 * <p>
 * The generator emits two client-Java {@code @Listener}s (on the child's create and delete events)
 * that recompute the count for the affected parent and write it back. The count is recomputed from
 * the store on each event (not blindly incremented), so it self-heals; under high write concurrency
 * it is eventually consistent rather than transactionally exact.
 */
public class RollupIntent {

    private String name;
    private String entity;
    private String via;
    private String field;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
