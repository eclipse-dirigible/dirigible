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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Tolerant parsing for the {@code java.time} values that reach the platform as JSON strings.
 * <p>
 * The generated REST layer is a boundary with parties that cannot be re-taught to send strict
 * ISO-8601: an {@code inbound:} webhook receives whatever shape the external system already emits,
 * and a browser form may pass a user-typed value through verbatim when its own conversion could not
 * parse it (WebKit's {@code new Date()} rejects the common {@code "2026-08-10 12:30"}). Rejecting
 * such a value with a 400 turns an obvious intent into a dead end, so the binding layers accept the
 * unambiguous near-ISO shapes too:
 * <ul>
 * <li>{@code "2026-08-10T12:30:00Z"} / {@code "…+03:00"} — strict ISO, as before;</li>
 * <li>{@code "2026-08-10 12:30"} / {@code "2026-08-10T12:30[:ss[.n]]"} — a zoneless local
 * date-time, interpreted at <b>UTC</b> (deterministic across server time zones);</li>
 * <li>{@code "2026-08-10"} — a bare date, midnight UTC when an instant is required.</li>
 * </ul>
 * For a date-only target the date part is taken <b>as written</b> ({@code "2026-08-10T22:00:00Z"} →
 * {@code 2026-08-10}), never shifted through a zone.
 * <p>
 * Each method returns {@code null} when the text matches none of these shapes, so a caller can fall
 * back to its stricter default and keep its original diagnostics for genuinely malformed input.
 */
public final class LenientJavaTime {

    private LenientJavaTime() {}

    /**
     * Parse an {@link Instant} from any accepted shape.
     *
     * @param text the raw string value
     * @return the instant, or {@code null} when the text matches no accepted shape
     */
    public static Instant parseInstant(String text) {
        String t = normalize(text);
        if (t == null) {
            return null;
        }
        try {
            return Instant.parse(t);
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return OffsetDateTime.parse(t)
                                 .toInstant();
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return LocalDateTime.parse(t)
                                .toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return LocalDate.parse(t)
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parse a {@link LocalDate} from any accepted shape. A date-time string contributes its date part
     * as written — no zone conversion is applied.
     *
     * @param text the raw string value
     * @return the date, or {@code null} when the text matches no accepted shape
     */
    public static LocalDate parseLocalDate(String text) {
        String t = normalize(text);
        if (t == null) {
            return null;
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            // fall through
        }
        if (t.length() > 10) {
            try {
                return LocalDate.parse(t.substring(0, 10));
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parse a {@link LocalDateTime} from any accepted shape. An offset/zoned string contributes its
     * local fields as written; a bare date becomes midnight.
     *
     * @param text the raw string value
     * @return the date-time, or {@code null} when the text matches no accepted shape
     */
    public static LocalDateTime parseLocalDateTime(String text) {
        String t = normalize(text);
        if (t == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(t);
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return OffsetDateTime.parse(t)
                                 .toLocalDateTime();
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return LocalDate.parse(t)
                            .atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Trim and turn the single space separating date and time into the ISO {@code 'T'}
     * ({@code "2026-08-10 12:30"} → {@code "2026-08-10T12:30"}); blank input becomes {@code null}.
     */
    private static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > 10 && t.charAt(10) == ' ') {
            t = t.substring(0, 10) + 'T' + t.substring(11);
        }
        return t;
    }
}
