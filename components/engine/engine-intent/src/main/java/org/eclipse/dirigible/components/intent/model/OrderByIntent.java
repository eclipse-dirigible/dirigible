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
 * One term of an entity's {@code orderBy:} - the default row order of its list, its
 * {@code hierarchy:} tree siblings and every picker that targets it (issue #7727).
 *
 * <p>
 * Authored either as a bare property name ({@code orderBy: [number]}, ascending) or as the full
 * form ({@code orderBy: [{ field: date, dir: desc }, number]}); the shorthand is expanded to this
 * shape on the raw tree before the typed mapping, so both share one class.
 */
public class OrderByIntent {

    /**
     * The property the rows are ordered by - a field or a to-one relation of the entity, in intent
     * notation (case-insensitive). Mandatory.
     */
    private String field;

    /** {@code asc} (the default) or {@code desc}. */
    private String dir;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    /** The direction as the generated {@code ORDER BY} spells it - {@code asc} unless {@code desc}. */
    public String direction() {
        return dir != null && "desc".equalsIgnoreCase(dir.trim()) ? "desc" : "asc";
    }
}
