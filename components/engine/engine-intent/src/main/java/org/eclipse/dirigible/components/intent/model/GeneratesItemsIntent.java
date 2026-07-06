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
 * The composition-child half of a {@link GeneratesIntent}: for each source item row (the child of
 * {@link GeneratesIntent#getFrom()}) a target item row is created and re-pointed at the newly
 * created target master. {@link #from} / {@link #to} are the child entity names; {@link #map} /
 * {@link #defaults} follow the same semantics as on the parent (source copy vs
 * {@code now}/literal). The foreign key back to the master is set automatically - it must not be
 * listed in {@link #map}.
 */
public class GeneratesItemsIntent {

    /** The source item entity (the composition child of the source master). */
    private String from;

    /** The target item entity (the composition child of the target master). */
    private String to;

    /** Target item property -> source item property. */
    private Map<String, String> map = new LinkedHashMap<>();

    /** Target item property -> {@code now} or a literal value. */
    private Map<String, String> defaults = new LinkedHashMap<>();

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map == null ? new LinkedHashMap<>() : map;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults == null ? new LinkedHashMap<>() : defaults;
    }
}
