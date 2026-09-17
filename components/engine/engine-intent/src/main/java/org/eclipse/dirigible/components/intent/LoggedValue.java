/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent;

/**
 * A caller-authored value made safe to put in a log line.
 *
 * <p>
 * Nearly everything this module logs is named by the developer: an entity, a report or a process
 * name, a file path, a workspace or project segment of the request URL. Since the dry-run
 * validation endpoint, all of it can also arrive straight in a request body. A line break inside
 * such a value would let it forge a second log entry, which is how a log stops being evidence - so
 * every log argument that carries a model-derived or request-derived string goes through
 * {@link #of(Object)}. Everything else about the value is kept, because a name mangled beyond
 * recognition is no use to whoever is reading the line.
 */
public final class LoggedValue {

    private LoggedValue() {}

    /**
     * Collapse the line breaks in a caller-authored value.
     *
     * @param value the value, possibly {@code null}
     * @return the value's string form with every line break replaced, or {@code null}
     */
    public static String of(Object value) {
        return value == null ? null
                : String.valueOf(value)
                        .replaceAll("\\R", "_");
    }
}
