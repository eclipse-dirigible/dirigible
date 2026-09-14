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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a document's built-in <b>Duplicate</b> action does to the copied header, beyond the drops it
 * has always made (identity, audit columns, status, document number, read-only and aggregate
 * fields, which are not authorable).
 *
 * <p>
 * Authored as the object form of the entity's {@code duplicable} key; the shorthand
 * {@code duplicable: true} is the empty object, so an entity that says nothing keeps today's
 * behaviour exactly - every ordinary user field is copied.
 *
 * <pre>
 * duplicable:
 *   defaults: { date: now }        # constants written into the clone
 *   reset: [due, taxEventDate]     # dropped, so the entity's own create-time rule refills them
 * </pre>
 *
 * <p>
 * The two keys compose and never overlap: {@code reset} is for a field that HAS a create-time rule
 * (a {@code calculatedActionOnCreate}, a {@code defaultValue}) and must be handed back to it - a
 * copied value would be respected by that rule and stick; {@code defaults} is for a field that has
 * none, where the copy needs a value stated here. {@code now} renders today in the field's own
 * shape, the same token and the same rendering {@code generates.defaults} uses.
 */
public class DuplicateIntent {

    /**
     * Constants written into the cloned header after the resets, by the entity's own field / to-one
     * relation name. {@code now} is today in the field's shape ({@code date} -> {@code YYYY-MM-DD}, a
     * {@code month} field -> {@code YYYY-MM}, a {@code week} field -> {@code YYYY-Www}); any other
     * value is a literal coerced to the property's type.
     */
    private Map<String, String> defaults = new LinkedHashMap<>();

    /**
     * The entity's own field / to-one relation names dropped from the clone, so the create it posts
     * fills them exactly as it would on a hand-made document.
     */
    private List<String> reset = new ArrayList<>();

    /** The constants written into the clone, keyed by authored property name; never null. */
    public Map<String, String> getDefaults() {
        return defaults == null ? new LinkedHashMap<>() : defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults == null ? new LinkedHashMap<>() : defaults;
    }

    /** The authored property names dropped from the clone; never null. */
    public List<String> getReset() {
        return reset == null ? new ArrayList<>() : reset;
    }

    public void setReset(List<String> reset) {
        this.reset = reset == null ? new ArrayList<>() : reset;
    }
}
