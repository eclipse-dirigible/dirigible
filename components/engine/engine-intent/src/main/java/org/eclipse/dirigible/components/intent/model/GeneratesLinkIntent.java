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
 * The {@code link:} of a {@code generates} action (issue #7748): one row of a composition child of
 * the SOURCE, written in the same unit of work as the created target and pointing at both - the
 * allocation a payment recorded FROM the invoice it pays makes against that invoice.
 *
 * <p>
 * The row's composition relation is set to the source's key and its to-one relation to the target
 * ({@link GeneratesIntent#getTo()}) to the created target's key; {@link #map} copies created-target
 * properties onto the row's fields. Both relations are derived from the link entity's declarations,
 * {@link #relation} naming the target-side one only when the entity holds more than one to-one to
 * the target. The row is saved through its own generated repository, so its checks and the roll-ups
 * over it run, and a refusal rolls the target back with it. Because the unit's events are published
 * only once it commits, a consumer of the target's create event already sees the row - a settlement
 * that would otherwise spread the new payment over the oldest open invoice finds nothing left to
 * spread.
 */
public class GeneratesLinkIntent {

    /** The link entity: a composition child of the create-from's source, in the same model. */
    private String entity;

    /**
     * The link entity's to-one relation to the target, named only when there is more than one; derived
     * otherwise.
     */
    private String relation;

    /** Link field (authored name) -> created-target property (authored name) whose value it takes. */
    private Map<String, String> map = new LinkedHashMap<>();

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public Map<String, String> getMap() {
        return map == null ? Map.of() : map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }
}
