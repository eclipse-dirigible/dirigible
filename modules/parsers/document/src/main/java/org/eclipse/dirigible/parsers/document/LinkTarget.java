/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document;

import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * The target of an {@code href} attribute - the link a {@code <text>}, {@code <image>},
 * {@code <qrcode>} or a table {@code <column>} cell opens in the rendered document.
 *
 * <p>
 * Only four kinds of link are accepted, whatever the letter case of the scheme: a web address
 * ({@code http://}, {@code https://}), {@code mailto:} and {@code tel:}. Everything else is refused
 * - {@code javascript:}, {@code file:}, {@code data:}, a scheme-relative {@code //host}, a relative
 * path, a bare domain: a generated business document must not carry a link that runs a script or
 * opens a local file, and a value read from the data must not be able to pick the scheme.
 *
 * <p>
 * A value that is blank, or that is only its scheme ({@code mailto:} whose address placeholder
 * resolved empty), is <b>no link</b>, not an error: the element renders as if it had no
 * {@code href}.
 *
 * <p>
 * An accepted target is returned as a plain ASCII URI: an internationalized host name becomes its
 * {@code xn--} form, and every character outside the URI syntax - a space, a quote, a parenthesis,
 * a brace, Cyrillic - is percent-encoded as UTF-8. Existing percent-escapes are kept. That makes
 * the target safe to embed anywhere a renderer needs it (an XML attribute, an XSLT attribute value
 * template, the {@code url(...)} notation of XSL-FO, a PDF string).
 */
public final class LinkTarget {

    /** The name of the attribute. */
    public static final String ATTRIBUTE = "href";

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    /** The accepted prefixes, lower-case; a web address needs its {@code //}. */
    private static final List<String> PREFIXES = List.of(HTTPS, HTTP, "mailto:", "tel:");

    /** The URI punctuation emitted as it is; a quote, a parenthesis or a brace is not among it. */
    private static final String PUNCTUATION = "-._~:/?#[]@!$&*+,;=";

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private LinkTarget() {}

    /**
     * Whether the element takes an {@code href}: a {@code text}, an {@code image}, a {@code qrcode} and
     * a {@code column} (whose table cells are the link). On every other element the attribute is not a
     * link and is left alone.
     *
     * @param node the element
     * @return {@code true} when an {@code href} on it is a link
     */
    public static boolean appliesTo(Node node) {
        return node instanceof TextNode || node instanceof ImageNode || node instanceof QrCodeNode || node instanceof ColumnNode;
    }

    /**
     * Whether the raw attribute value still holds a placeholder, so that it can only be judged once the
     * data is bound.
     *
     * @param raw the attribute value as authored
     * @return {@code true} when it contains a <code>{{</code>
     */
    public static boolean isTemplated(String raw) {
        return raw != null && raw.contains("{{");
    }

    /**
     * Validates and encodes a link target.
     *
     * @param raw the attribute value, with its placeholders already resolved; may be {@code null}
     * @return the target as an ASCII URI, or an empty string when there is no link - the value is
     *         blank, or only a scheme
     * @throws IllegalArgumentException when the value is not an {@code http://}, {@code https://},
     *         {@code mailto:} or {@code tel:} link
     */
    public static String parse(String raw) {
        String value = raw == null ? "" : raw.strip();
        if (value.isEmpty()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        String prefix = PREFIXES.stream()
                                .filter(lower::startsWith)
                                .findFirst()
                                .orElseThrow(() -> invalid(raw));
        String rest = value.substring(prefix.length());
        if (rest.isEmpty()) {
            return "";
        }
        boolean web = HTTP.equals(prefix) || HTTPS.equals(prefix);
        if (web) {
            if (!isHostStart(rest.codePointAt(0))) {
                throw invalid(raw);
            }
            rest = withAsciiHost(rest);
        }
        StringBuilder target = new StringBuilder(value.length() + 16);
        target.append(value, 0, prefix.length());
        encode(target, rest);
        return target.toString();
    }

    private static IllegalArgumentException invalid(String raw) {
        return new IllegalArgumentException(
                "Invalid " + ATTRIBUTE + ": '" + raw + "' (expected an http://, https://, mailto: or tel: link)");
    }

    /** A web address names a host right after the {@code //}: a letter, a digit or an IPv6 bracket. */
    private static boolean isHostStart(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '[';
    }

    /**
     * The part of a web address after its {@code //} with an internationalized host name in its ASCII
     * ({@code xn--}) form. A host the IDNA rules reject is left as it is and percent-encoded with the
     * rest.
     */
    private static String withAsciiHost(String rest) {
        int authorityEnd = rest.length();
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '/' || c == '?' || c == '#' || c == '\\') {
                authorityEnd = i;
                break;
            }
        }
        int hostStart = rest.lastIndexOf('@', authorityEnd - 1) + 1;
        int hostEnd = authorityEnd;
        int port = rest.lastIndexOf(':', authorityEnd - 1);
        if (port >= hostStart && rest.charAt(hostStart) != '[') {
            hostEnd = port;
        }
        String host = rest.substring(hostStart, hostEnd);
        if (host.chars()
                .allMatch(c -> c < 0x80)) {
            return rest;
        }
        try {
            return rest.substring(0, hostStart) + IDN.toASCII(host) + rest.substring(hostEnd);
        } catch (IllegalArgumentException notAHostName) {
            return rest;
        }
    }

    private static void encode(StringBuilder target, String text) {
        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            int length = Character.charCount(codePoint);
            if (isUriCharacter(codePoint) || isPercentEscape(text, i)) {
                target.appendCodePoint(codePoint);
            } else {
                for (byte b : text.substring(i, i + length)
                                  .getBytes(StandardCharsets.UTF_8)) {
                    target.append('%')
                          .append(HEX[(b >> 4) & 0xF])
                          .append(HEX[b & 0xF]);
                }
            }
            i += length;
        }
    }

    private static boolean isUriCharacter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c < 0x80 && PUNCTUATION.indexOf(c) >= 0);
    }

    private static boolean isPercentEscape(String text, int index) {
        return text.charAt(index) == '%' && index + 2 < text.length() && isHexDigit(text.charAt(index + 1))
                && isHexDigit(text.charAt(index + 2));
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
