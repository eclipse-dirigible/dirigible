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

import org.eclipse.dirigible.components.api.utils.EscapeFacade;

/**
 * Output escaping helpers covering the contexts that templates and emitters commonly target — CSV
 * cells, HTML 3/4 attribute and text content, JavaScript string literals, Java string literals,
 * JSON values, and XML text. Each escape has a matching {@code unescape} for round-trip decoding
 * when the source is text produced by the same alphabet (it is <em>not</em> a general HTML / JSON
 * parser).
 * <p>
 * Reach for these when generating output by string concatenation; for structured output (assembled
 * JSON via Jackson, XML via SAX/DOM, HTML via a templating engine) the surrounding library already
 * takes care of escaping correctly.
 */
public final class Escape {

    private Escape() {}

    public static String escapeCsv(String input) {
        return EscapeFacade.escapeCsv(input);
    }

    public static String unescapeCsv(String input) {
        return EscapeFacade.unescapeCsv(input);
    }

    public static String escapeJavascript(String input) {
        return EscapeFacade.escapeJavascript(input);
    }

    public static String unescapeJavascript(String input) {
        return EscapeFacade.unescapeJavascript(input);
    }

    public static String escapeHtml3(String input) {
        return EscapeFacade.escapeHtml3(input);
    }

    public static String unescapeHtml3(String input) {
        return EscapeFacade.unescapeHtml3(input);
    }

    public static String escapeHtml4(String input) {
        return EscapeFacade.escapeHtml4(input);
    }

    public static String unescapeHtml4(String input) {
        return EscapeFacade.unescapeHtml4(input);
    }

    public static String escapeJava(String input) {
        return EscapeFacade.escapeJava(input);
    }

    public static String unescapeJava(String input) {
        return EscapeFacade.unescapeJava(input);
    }

    public static String escapeJson(String input) {
        return EscapeFacade.escapeJson(input);
    }

    public static String unescapeJson(String input) {
        return EscapeFacade.unescapeJson(input);
    }

    public static String escapeXml(String input) {
        return EscapeFacade.escapeXml(input);
    }

    public static String unescapeXml(String input) {
        return EscapeFacade.unescapeXml(input);
    }
}
