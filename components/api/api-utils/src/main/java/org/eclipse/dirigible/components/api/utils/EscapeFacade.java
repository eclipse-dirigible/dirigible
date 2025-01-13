/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.utils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Component;

/**
 * The Class EscapeFacade.
 */
@Component
public class EscapeFacade {

    /**
     * Escape CSV.
     *
     * @param input the input
     * @return the escaped input
     */
    public static final String escapeCsv(String input) {
        return StringEscapeUtils.escapeCsv(input);
    }

    /**
     * Escape JavaScript.
     *
     * @param input the input
     * @return the escaped input
     */
    public static final String escapeJavascript(String input) {
        return StringEscapeUtils.escapeEcmaScript(input);
    }

    /**
     * Escape HTML 3.
     *
     * @param input the input
     * @return the escaped input
     */
    public static final String escapeHtml3(String input) {
        return StringEscapeUtils.escapeHtml3(input);
    }

    /**
     * Escape HTML 4.
     *
     * @param input the input
     * @return the escaped input
     */
    public static final String escapeHtml4(String input) {
        return StringEscapeUtils.escapeHtml4(input);
    }

    /**
     * Escape Java.
     *
     * @param input the input
     * @return the escaped input
     */
    public static final String escapeJava(String input) {
        return StringEscapeUtils.escapeJava(input);
    }

    /**
     * Escape JSON.
     *
     * @param input the input
     * @return the escaped input
     */
    public static final String escapeJson(String input) {
        return StringEscapeUtils.escapeJson(input);
    }

    /**
     * Escape XML.
     *
     * @param input the input
     * @return the escaped input
     */
    public static final String escapeXml(String input) {
        return StringEscapeUtils.escapeXml(input);
    }

    /**
     * Unescape CSV.
     *
     * @param input the input
     * @return the unescaped input
     */
    public static final String unescapeCsv(String input) {
        return StringEscapeUtils.unescapeCsv(input);
    }

    /**
     * Unescape JavaScript.
     *
     * @param input the input
     * @return the unescaped input
     */
    public static final String unescapeJavascript(String input) {
        return StringEscapeUtils.unescapeEcmaScript(input);
    }

    /**
     * Unescape HTML 3.
     *
     * @param input the input
     * @return the unescaped input
     */
    public static final String unescapeHtml3(String input) {
        return StringEscapeUtils.unescapeHtml3(input);
    }

    /**
     * Unescape HTML 4.
     *
     * @param input the input
     * @return the unescaped input
     */
    public static final String unescapeHtml4(String input) {
        return StringEscapeUtils.unescapeHtml4(input);
    }

    /**
     * Unescape Java.
     *
     * @param input the input
     * @return the unescaped input
     */
    public static final String unescapeJava(String input) {
        return StringEscapeUtils.unescapeJava(input);
    }

    /**
     * Unescape JSON.
     *
     * @param input the input
     * @return the unescaped input
     */
    public static final String unescapeJson(String input) {
        return StringEscapeUtils.unescapeJson(input);
    }

    /**
     * Unescape XML.
     *
     * @param input the input
     * @return the unescaped input
     */
    public static final String unescapeXml(String input) {
        return StringEscapeUtils.unescapeXml(input);
    }

}
