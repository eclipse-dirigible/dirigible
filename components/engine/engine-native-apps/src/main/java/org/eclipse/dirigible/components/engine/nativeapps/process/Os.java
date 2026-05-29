/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.process;

import java.util.Locale;

/**
 * The host operating systems the native-app command resolver knows about. {@link #UNSUPPORTED} is
 * returned both when {@link OsCommandResolver#currentOs()} runs on a platform we don't recognise
 * and when a {@code command.os} token in a {@code .native-app} file doesn't parse cleanly — it
 * never matches anything in {@link OsCommandResolver#pickForCurrentOs}, so an unrecognised host
 * silently has no commands to pick.
 *
 * <p>
 * {@code toString()} is overridden to return the lower-case JSON token so error messages and log
 * lines that interpolate the value match the schema authors actually write in their
 * {@code .native-app} files (e.g. {@code "mac"}, not {@code "MAC"}).
 */
enum Os {

    MAC("mac"), LINUX("linux"), WINDOWS("windows"), UNSUPPORTED("unsupported");

    private final String token;

    Os(String token) {
        this.token = token;
    }

    /**
     * Maps the JSON {@code command.os} string to an {@link Os}. Case-insensitive. Returns
     * {@link #UNSUPPORTED} for null, blank, or unrecognised input — the matcher in
     * {@link OsCommandResolver#pickForCurrentOs} treats UNSUPPORTED as never-equal so unknown tokens
     * simply don't pick.
     */
    static Os fromToken(String token) {
        if (token == null || token.isBlank()) {
            return UNSUPPORTED;
        }
        String normalised = token.toLowerCase(Locale.ROOT);
        for (Os os : values()) {
            if (os != UNSUPPORTED && os.token.equals(normalised)) {
                return os;
            }
        }
        return UNSUPPORTED;
    }

    @Override
    public String toString() {
        return token;
    }
}
