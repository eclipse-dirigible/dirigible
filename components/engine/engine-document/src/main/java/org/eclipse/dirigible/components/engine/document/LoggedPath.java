/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

/**
 * A content-store path made safe to put in a log line.
 *
 * <p>
 * A print template's {@code <image src>} is authored by a developer, but a placeholder inside it
 * resolves against the print payload, which arrives in a request body - so the path this module
 * logs when an image cannot be read is request-derived. A line break inside a logged value forges a
 * second log entry, which is how a log stops being evidence. Everything else about the path is
 * kept: a path mangled beyond recognition is no use to whoever is reading the line asking why the
 * logo did not print.
 */
final class LoggedPath {

    private LoggedPath() {}

    /**
     * Collapse the line breaks of a path.
     *
     * @param path the path, possibly {@code null}
     * @return the path with every line break replaced, or {@code null}
     */
    static String of(String path) {
        return path == null ? null : path.replaceAll("\\R", "_");
    }
}
