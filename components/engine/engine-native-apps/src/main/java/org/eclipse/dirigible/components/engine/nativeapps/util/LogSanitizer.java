/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.util;

/**
 * Strips control characters (CR/LF/etc.) from values before they are interleaved with platform log
 * output. Use at every sink that passes attacker- or untrusted-author-controlled strings to
 * {@code Logger.info/debug/...}, so a CR/LF in the input cannot forge a synthetic log entry.
 *
 * <p>
 * CodeQL "Log Injection" findings on native-apps loggers (HTTP {@code remoteUser}, request paths,
 * artefact-author-controlled fields, env-driven config) route through this helper. Keep using it in
 * new logging sites that touch inbound HTTP / artefact fields.
 */
public final class LogSanitizer {

    private LogSanitizer() {}

    /**
     * Returns a copy of {@code value} with all Unicode control characters replaced by {@code '_'},
     * suitable for inclusion in a log line. {@code null} is rendered as the literal string "null".
     */
    public static String sanitize(Object value) {
        if (value == null) {
            return "null";
        }
        return String.valueOf(value)
                     .replaceAll("\\p{Cntrl}", "_");
    }
}
