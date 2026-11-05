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
 * A field's {@code unique:} - a single-column business key.
 *
 * <pre>
 * fields:
 *   - { name: code,     type: string, unique: true }                     # a UNIQUE column
 *   - { name: username, type: string, unique: { ignoreCase: true } }     # QALT1 collides with qalt1
 * </pre>
 *
 * The shorthand {@code unique: true} is the empty mapping. {@code ignoreCase} (dirigible #8095)
 * also refuses a value another row holds in a different letter case - a login or an e-mail address
 * - while the stored value keeps the case it was typed in.
 */
public class FieldUniqueIntent {

    /** Whether two values that differ only in letter case are the same key. */
    private boolean ignoreCase;

    public boolean isIgnoreCase() {
        return ignoreCase;
    }
}
