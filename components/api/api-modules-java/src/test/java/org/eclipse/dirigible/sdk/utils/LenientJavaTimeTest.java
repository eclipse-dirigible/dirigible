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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class LenientJavaTimeTest {

    /**
     * The reported shape: a user typed "2026-08-10 12:30" into a browser form (WebKit's new Date()
     * rejects the space separator, so the client passed it through verbatim) and the generated
     * controller answered 400. A zoneless local date-time reads as UTC — deterministic across server
     * time zones.
     */
    @Test
    void instant_accepts_space_separated_local_date_time_as_utc() {
        assertEquals(Instant.parse("2026-08-10T12:30:00Z"), LenientJavaTime.parseInstant("2026-08-10 12:30"));
        assertEquals(Instant.parse("2026-08-10T12:30:45Z"), LenientJavaTime.parseInstant("2026-08-10 12:30:45"));
    }

    @Test
    void instant_accepts_strict_iso_and_offset_forms() {
        assertEquals(Instant.parse("2026-08-10T12:30:00Z"), LenientJavaTime.parseInstant("2026-08-10T12:30:00Z"));
        assertEquals(Instant.parse("2026-08-10T09:30:00Z"), LenientJavaTime.parseInstant("2026-08-10T12:30:00+03:00"));
        assertEquals(Instant.parse("2026-08-10T09:30:00Z"), LenientJavaTime.parseInstant("2026-08-10 12:30:00+03:00"));
    }

    @Test
    void instant_accepts_zoneless_t_form_and_bare_date() {
        assertEquals(Instant.parse("2026-08-10T12:30:00Z"), LenientJavaTime.parseInstant("2026-08-10T12:30"));
        assertEquals(Instant.parse("2026-08-10T00:00:00Z"), LenientJavaTime.parseInstant("2026-08-10"));
    }

    /**
     * The generated UI converts a picked date to a full ISO instant even when the target column is
     * date-only; the date part is taken AS WRITTEN, never shifted through a zone.
     */
    @Test
    void local_date_accepts_date_time_strings_taking_the_date_part_as_written() {
        assertEquals(LocalDate.of(2026, 8, 10), LenientJavaTime.parseLocalDate("2026-08-10"));
        assertEquals(LocalDate.of(2026, 8, 10), LenientJavaTime.parseLocalDate("2026-08-10T00:00:00.000Z"));
        assertEquals(LocalDate.of(2026, 8, 10), LenientJavaTime.parseLocalDate("2026-08-10 22:30"));
        assertEquals(LocalDate.of(2026, 8, 10), LenientJavaTime.parseLocalDate("2026-08-10T22:30:00+03:00"));
    }

    @Test
    void local_date_time_accepts_space_offset_and_bare_date_forms() {
        assertEquals(LocalDateTime.of(2026, 8, 10, 12, 30), LenientJavaTime.parseLocalDateTime("2026-08-10 12:30"));
        assertEquals(LocalDateTime.of(2026, 8, 10, 12, 30), LenientJavaTime.parseLocalDateTime("2026-08-10T12:30"));
        // an offset form contributes its local fields as written
        assertEquals(LocalDateTime.of(2026, 8, 10, 12, 30), LenientJavaTime.parseLocalDateTime("2026-08-10T12:30:00+03:00"));
        assertEquals(LocalDateTime.of(2026, 8, 10, 0, 0), LenientJavaTime.parseLocalDateTime("2026-08-10"));
    }

    /** Unrecognized input yields null so callers keep their stricter default and its diagnostics. */
    @Test
    void unrecognized_shapes_yield_null() {
        assertNull(LenientJavaTime.parseInstant("not a date"));
        assertNull(LenientJavaTime.parseInstant("10/08/2026 12:30"));
        assertNull(LenientJavaTime.parseInstant(""));
        assertNull(LenientJavaTime.parseInstant(null));
        assertNull(LenientJavaTime.parseLocalDate("12:30"));
        assertNull(LenientJavaTime.parseLocalDateTime("garbage"));
    }
}
