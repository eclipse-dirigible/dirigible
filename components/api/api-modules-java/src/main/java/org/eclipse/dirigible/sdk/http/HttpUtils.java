/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Small content-type and query-string helpers — recognising JSON / XML media types, URL-encoding /
 * decoding a {@link Map} of parameters. Saves writing the {@code &}-joining loop for the dozenth
 * time and centralises the answer to "is this content-type JSON?" so future RFC additions (custom
 * {@code +json} suffixes etc.) flow through every caller.
 * <p>
 * Implemented inline against the JDK — no platform facade — because the operations are pure and the
 * result must match the equivalent TS helpers exactly.
 */
public final class HttpUtils {

    private HttpUtils() {}

    public static boolean isContentTypeJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.contains("application/json") || normalized.endsWith("+json");
    }

    public static boolean isContentTypeXml(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.contains("application/xml") || normalized.contains("text/xml") || normalized.endsWith("+xml");
    }

    public static String toQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public static Map<String, String> fromQueryString(String queryString) {
        Map<String, String> result = new LinkedHashMap<>();
        if (queryString == null || queryString.isEmpty()) {
            return result;
        }
        String input = queryString.charAt(0) == '?' ? queryString.substring(1) : queryString;
        for (String pair : input.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = idx < 0 ? pair : pair.substring(0, idx);
            String value = idx < 0 ? "" : pair.substring(idx + 1);
            try {
                result.put(URLDecoder.decode(key, StandardCharsets.UTF_8.name()), URLDecoder.decode(value, StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException("UTF-8 encoding not supported", ex);
            }
        }
        return result;
    }
}
