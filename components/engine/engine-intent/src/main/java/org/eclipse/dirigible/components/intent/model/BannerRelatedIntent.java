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
 * The related rows a record-page banner reads (dirigible #8371): one of the entity's own
 * {@code related:} registers, the row of it the condition is tested on, and the condition.
 *
 * <pre>
 * related: { entity: Shipment, row: latest, when: "status == FAILED" }
 * </pre>
 */
public class BannerRelatedIntent {

    /**
     * The register's source entity - one of the enclosing entity's {@code related:} entries. Required.
     */
    private String entity;

    /** The register's {@code via:}, when the entity declares several registers of that source. */
    private String via;

    /**
     * {@code latest} - only the row with the highest primary key is tested; {@code any} - the banner
     * holds when any row matches. Required.
     */
    private String row;

    /**
     * The condition over the source's own fields and to-one relations, in the {@code visibleWhen}
     * grammar, with status names resolved in the source's terms. Required.
     */
    private Object when;

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

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public Object getWhen() {
        return when;
    }

    public void setWhen(Object when) {
        this.when = when;
    }
}
