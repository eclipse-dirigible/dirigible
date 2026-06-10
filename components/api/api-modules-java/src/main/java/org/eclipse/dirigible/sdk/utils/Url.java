/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.utils;

import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.DecoderException;
import org.eclipse.dirigible.components.api.utils.UrlFacade;

/**
 * URL component encoding helpers. {@link #encode(String)} / {@link #decode(String)} use the UTF-8
 * percent-encoding rules; the {@code escape*} family exposes the variants needed when building
 * paths or {@code application/x-www-form-urlencoded} bodies where the standard URL rules differ
 * slightly.
 * <p>
 * The class is named {@code Url} (lower-case d) to avoid a name clash with {@link java.net.URL} in
 * callers that statically import the JDK type — both names coexist on the import list cleanly this
 * way.
 */
public final class Url {

    private Url() {}

    public static String encode(String input) throws UnsupportedEncodingException {
        return UrlFacade.encode(input);
    }

    public static String encode(String input, String charset) throws UnsupportedEncodingException {
        return UrlFacade.encode(input, charset);
    }

    public static String decode(String input) throws DecoderException, UnsupportedEncodingException {
        return UrlFacade.decode(input);
    }

    public static String decode(String input, String charset) throws DecoderException, UnsupportedEncodingException {
        return UrlFacade.decode(input, charset);
    }

    public static String escape(String input) {
        return UrlFacade.escape(input);
    }

    public static String escapePath(String input) {
        return UrlFacade.escapePath(input);
    }

    public static String escapeForm(String input) {
        return UrlFacade.escapeForm(input);
    }
}
