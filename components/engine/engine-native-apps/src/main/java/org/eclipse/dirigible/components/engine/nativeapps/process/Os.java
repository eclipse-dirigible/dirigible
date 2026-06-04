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
import java.util.Optional;

/**
 * The host operating systems the native-app command resolver knows about. {@link #UNSUPPORTED} is
 * the sentinel returned by {@link OsCommandResolver#currentOs()} when the running platform is none
 * of mac / linux / windows — a runtime fact, not an authoring mistake. The platform stays
 * functional on UNSUPPORTED hosts; it just declines to pick any native-app command.
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
     * Strictly parses a {@code command.os} JSON token. Returns the matching {@link Os} for one of mac /
     * linux / windows (case-insensitive); throws {@link IllegalArgumentException} for null, blank, or
     * anything else. Use this when the value comes from an authored {@code .native-app} file and a typo
     * should fail loud at the layer that owns the artefact (e.g. a future synchronizer-side validation
     * step). For the runtime per-request matching path where an unknown token should simply not match,
     * use {@link #fromTokenIfKnown}.
     */
    static Os fromToken(String token) {
        return fromTokenIfKnown(token).orElseThrow(() -> new IllegalArgumentException(
                "Unknown OS token [" + token + "] — must be one of mac / linux / windows (case-insensitive)."));
    }

    /**
     * Leniently parses a {@code command.os} JSON token. Returns {@link Optional#empty()} for null,
     * blank, or any unrecognised value; otherwise the matching {@link Os}. Used by
     * {@link OsCommandResolver#pickForCurrentOs} so a command tagged for an OS the platform doesn't
     * know about silently doesn't match — without paying an exception per request.
     */
    static Optional<Os> fromTokenIfKnown(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalised = token.toLowerCase(Locale.ROOT);
        for (Os os : values()) {
            if (os != UNSUPPORTED && os.token.equals(normalised)) {
                return Optional.of(os);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return token;
    }
}
