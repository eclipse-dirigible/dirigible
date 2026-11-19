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
 * A report's anti-join (dirigible #7914): the report keeps only the source rows for which NO row of
 * {@link #entity} matches - an issued document with no journal entry, a payment with no allocation,
 * an entry whose date falls in no fiscal year of its company. Emitted as a correlated
 * {@code NOT EXISTS (SELECT 1 FROM <entity> WHERE ...)} in the report's {@code WHERE}.
 *
 * <p>
 * The shape is the one an {@code exists} check and a {@code resolves:} lookup already use:
 * {@link #match} pairs a property of the target with a property of the report's source, and
 * {@link #between} is a {@code resolves:}-style validity window.
 */
public class ReportNotExistsIntent {

    /** The entity whose matching rows must be absent - this model's, or one of {@link #model}. */
    private String entity;
    /** The {@code uses:} alias of the model owning {@link #entity}; omitted for this model's own. */
    private String model;
    /**
     * Equality keys: a property of {@link #entity} (a field, or a to-one compared by its foreign key)
     * to the property of the report's source it must equal - the source's own field or to-one, or a
     * one-hop {@code Relation.field}. Every pair must hold on the same target row.
     */
    private Map<String, String> match = new LinkedHashMap<>();
    /**
     * An optional validity window: {@code start} / {@code end} name the target's period bounds and
     * {@code value} the source's date the period must cover. A missing bound on a target row is
     * open-ended, both bounds are inclusive and compared as dates.
     */
    private Map<String, String> between = new LinkedHashMap<>();
    /** An optional predicate over the target's own fields, narrowing which of its rows count. */
    private String filter;

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /** Whether {@link #entity} is owned by another model. */
    public boolean isCrossModel() {
        return model != null && !model.isBlank();
    }

    public Map<String, String> getMatch() {
        return match;
    }

    public void setMatch(Map<String, String> match) {
        this.match = match == null ? new LinkedHashMap<>() : match;
    }

    public Map<String, String> getBetween() {
        return between;
    }

    public void setBetween(Map<String, String> between) {
        this.between = between == null ? new LinkedHashMap<>() : between;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
