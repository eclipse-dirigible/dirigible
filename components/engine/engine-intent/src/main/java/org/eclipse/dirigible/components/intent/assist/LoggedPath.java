/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

/**
 * A file path made safe to put in a log line.
 *
 * <p>
 * Every path this package logs is named by the caller - it arrives in the request body, or is the
 * name of a file somebody created in their workspace. A line break inside one would let a value
 * forge a second log entry, which is how a log stops being evidence. Everything else about the path
 * is kept, because a path that has been mangled beyond recognition is no use to whoever is reading
 * the line.
 */
final class LoggedPath {

    private LoggedPath() {}

    /**
     * Collapse the line breaks in a caller-named path.
     *
     * @param path the path, possibly {@code null}
     * @return the path with any CR/LF replaced, or {@code null}
     */
    static String of(String path) {
        return path == null ? null
                : path.replace('\r', '_')
                      .replace('\n', '_');
    }
}
