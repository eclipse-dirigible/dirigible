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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonTest {

    /**
     * A bare {@code new Gson()} throws InaccessibleObjectException on these java.time fields under JDK
     * 17+ (LocalDate#year, Instant#seconds, ...); the helper must serialize them as ISO-8601 strings
     * and round-trip them back. The generated entities use all of these: {@code date} -> LocalDate,
     * {@code time} -> LocalTime, {@code timestamp}/audit -> Instant.
     */
    @Test
    void roundtrips_java_time_fields_a_bare_gson_cannot_serialize() {
        Holder holder = new Holder();
        holder.date = LocalDate.of(2026, 6, 17);
        holder.dateTime = LocalDateTime.of(2026, 6, 17, 13, 30, 9);
        holder.time = LocalTime.of(13, 30, 9);
        holder.instant = Instant.parse("2026-06-17T13:30:09Z");
        holder.name = "Member";

        String json = Json.stringify(holder);
        assertTrue(json.contains("2026-06-17"), "the date should serialize as an ISO-8601 string");

        Holder back = Json.parse(json, Holder.class);
        assertEquals(holder.date, back.date);
        assertEquals(holder.dateTime, back.dateTime);
        assertEquals(holder.time, back.time);
        assertEquals(holder.instant, back.instant);
        assertEquals(holder.name, back.name);
    }

    /**
     * An {@code inbound:} webhook parses whatever shape the external system emits — a near-ISO
     * date-time ("2026-08-10 12:30") must bind instead of failing the whole ingest, and a full ISO
     * instant aimed at a date-only field contributes its date part as written (see LenientJavaTime).
     */
    @Test
    void parses_near_iso_date_time_strings_an_inbound_webhook_receives() {
        Holder back = Json.parse(
                "{\"instant\":\"2026-08-10 12:30\",\"date\":\"2026-08-10T00:00:00.000Z\"," + "\"dateTime\":\"2026-08-10 12:30\"}",
                Holder.class);
        assertEquals(Instant.parse("2026-08-10T12:30:00Z"), back.instant);
        assertEquals(LocalDate.of(2026, 8, 10), back.date);
        assertEquals(LocalDateTime.of(2026, 8, 10, 12, 30), back.dateTime);
    }

    /**
     * A query's rows are untyped maps: {@code parse} reads their numbers as doubles, which cut a
     * {@code DECIMAL(18, 2)} amount to about 15 significant digits (#8487); {@code parseExact} keeps
     * every digit the JSON carries.
     */
    @Test
    void parse_exact_reads_an_untyped_number_as_the_decimal_it_is_written_as() {
        String rows = "[{\"Amount\":1234567890123456.78,\"Count\":2}]";

        Map<?, ?> exact = (Map<?, ?>) Json.parseExact(rows, List.class)
                                          .get(0);
        assertEquals(new BigDecimal("1234567890123456.78"), exact.get("Amount"));
        assertEquals(new BigDecimal("2"), exact.get("Count"));

        Map<?, ?> approximate = (Map<?, ?>) Json.parse(rows, List.class)
                                                .get(0);
        assertEquals(1.2345678901234568E15, approximate.get("Amount"));
    }

    static class Holder {
        public LocalDate date;
        public LocalDateTime dateTime;
        public LocalTime time;
        public Instant instant;
        public String name;
    }
}
