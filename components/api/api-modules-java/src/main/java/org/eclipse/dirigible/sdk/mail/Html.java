/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.mail;

/**
 * Makes a value safe to interpolate into an HTML mail body. A generated notify block with an
 * {@code html:} alternative passes every resolved placeholder through {@link #escape(Object)}, so a
 * value coming out of business data - a customer named {@code Smith & Sons <Ltd>} - is shown as
 * written instead of being read as markup; the authored markup around it is sent as written.
 */
public final class Html {

    private Html() {}

    /**
     * Escape a value for HTML text and attribute content: {@code &}, {@code <}, {@code >}, {@code "}
     * and {@code '} become character references. The value is rendered exactly as string concatenation
     * renders it, so the HTML part carries the same text as the plain part.
     *
     * @param value the resolved value, may be {@code null}
     * @return the escaped text
     */
    public static String escape(Object value) {
        String text = String.valueOf(value);
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
