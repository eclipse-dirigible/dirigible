/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.api.helpers;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DateTimeUtils.
 */
public class DateTimeUtils {

    /** The Constant dateFormatter. */
    public static final DateTimeFormatter dateFormatter =
            new DateTimeFormatterBuilder().appendOptional(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                          .appendOptional(DateTimeFormatter.ISO_INSTANT)
                                          .appendOptional(DateTimeFormatter.ofPattern("M/d/yyyy"))
                                          .appendOptional(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyyMMdd"))
                                          .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                          .appendOptional(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
                                          .appendOptional(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                          .toFormatter(Locale.ENGLISH);

    /** The Constant timeFormatter. */
    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("[HH:mm:ss.SSSSSS][yyyy-MM-dd]['T'][HH:mm:ss[.SSS][ Z]['Z']]", Locale.ENGLISH);

    /** The Constant datetimeFormatter. */
    private static final DateTimeFormatter datetimeFormatter =
            new DateTimeFormatterBuilder().appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]['Z']"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSSSSS"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSS"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss[.SSS][ Z]"))
                                          .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS][ Z]"))
                                          .appendOptional(DateTimeFormatter.ofPattern("dd[ ]MMM[ ]yyyy:HH:mm:ss.SSS[ Z]", Locale.ENGLISH))
                                          .toFormatter(Locale.ENGLISH);

    private static final Logger LOGGER = LoggerFactory.getLogger(DateTimeUtils.class);

    /**
     * Numberize.
     *
     * @param value the value
     * @return the string
     */
    private String numberize(String value) {
        if (StringUtils.isEmpty(value)) {
            value = "0";
        }
        return value;
    }

    public static Optional<Timestamp> optionallyParseDateTime(String value) {
        try {
            return Optional.of(parseDateTime(value));
        } catch (DateTimeParseException ex) {
            LOGGER.debug("[{}] cannot be parsed to date time", value, ex);
            return Optional.empty();
        }
    }

    /**
     * Parses the date time.
     *
     * <p>
     * A value carrying an explicit UTC/offset marker (e.g. the ISO-8601 {@code 2024-11-21T13:15:10Z}
     * that {@code Instant}/{@code OffsetDateTime} produce) is resolved to its real instant first.
     * Without this, {@code datetimeFormatter}'s {@code ['Z']} is a literal character to strip, not the
     * zone designator - the result was a zone-less {@link LocalDateTime} that
     * {@link Timestamp#valueOf(LocalDateTime)} then anchored to the JVM's default time zone, silently
     * shifting every such value by that zone's UTC offset (e.g. -2h on a server running with
     * {@code TZ=Europe/Sofia}) instead of preserving the moment the value actually named. A value with
     * no zone marker keeps the previous, zone-less behavior.
     *
     * @param value the value
     * @return the timestamp
     */
    public static Timestamp parseDateTime(String value) {
        value = sanitize(value);
        Instant instant = tryParseAsInstant(value);
        if (instant != null) {
            return Timestamp.from(instant);
        }
        value = timezonize(value);
        return Timestamp.valueOf(LocalDateTime.parse(value, datetimeFormatter));
    }

    /**
     * Try to resolve a value that carries an explicit UTC/offset marker to its real instant, e.g.
     * {@code 2024-11-21T13:15:10Z} or {@code 2024-11-21T13:15:10+02:00}.
     *
     * @param value the value
     * @return the instant, or {@code null} if the value carries no zone/offset marker
     */
    private static Instant tryParseAsInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            // not the plain "...Z" ISO instant form - fall through to try an explicit offset
        }
        try {
            return OffsetDateTime.parse(value)
                                 .toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * Sanitize.
     *
     * @param value the value
     * @return the string
     */
    private static String sanitize(String value) {
        if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        if (value != null && value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.trim();
    }

    /**
     * Timezonize.
     *
     * @param value the value
     * @return the string
     */
    private static String timezonize(String value) {
        if (value != null && value.indexOf('.') == value.length() - 8) {
            value = value.substring(0, value.indexOf('.') + 4) + " +" + value.substring(value.indexOf('.') + 4);
        }
        return value;
    }

    public static Optional<Date> optionallyParseDate(String value) {
        try {
            return Optional.of(parseDate(value));
        } catch (DateTimeException ex) {
            LOGGER.debug("[{}] cannot be parsed to date", value, ex);
            return Optional.empty();
        }
    }

    /**
     * Parses the date.
     *
     * @param value the value
     * @return the date
     */
    public static Date parseDate(String value) {
        value = sanitize(value);
        try {
            return Date.valueOf(LocalDate.parse(value, dateFormatter));
        } catch (DateTimeParseException ex) {
            throw new DateTimeException("Failed to parse [" + value + "] using date formatter " + dateFormatter, ex);
        }
    }

    public static Optional<Time> optionallyParseTime(String value) {
        try {
            return Optional.of(parseTime(value));
        } catch (DateTimeException ex) {
            LOGGER.debug("[{}] cannot be parsed to time", value, ex);
            return Optional.empty();
        }
    }

    /**
     * Parses the time.
     *
     * @param value the value
     * @return the time
     */
    public static Time parseTime(String value) {
        value = sanitize(value);
        value = timezonize(value);
        try {
            return Time.valueOf(LocalTime.parse(value, timeFormatter));
        } catch (DateTimeParseException ex) {
            throw new DateTimeException("Failed to parse [" + value + "] using time formatter " + timeFormatter, ex);
        }
    }
}

