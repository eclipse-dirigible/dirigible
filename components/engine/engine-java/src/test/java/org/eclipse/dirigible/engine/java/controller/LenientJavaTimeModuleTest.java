/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * The mapper is assembled the same way {@link ControllerInvoker} builds its body binder:
 * ServiceLoader-discovered modules (jsr310) plus {@link LenientJavaTimeModule} on top.
 */
class LenientJavaTimeModuleTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                                                          .registerModule(new LenientJavaTimeModule());

    /** The reported failure: a user-typed "2026-08-10 12:30" reaching an Instant field verbatim. */
    @Test
    void space_separated_date_time_binds_into_an_instant_field() throws Exception {
        Fine fine = mapper.readValue("{\"ViolationAt\":\"2026-08-10 12:30\"}", Fine.class);
        assertEquals(Instant.parse("2026-08-10T12:30:00Z"), fine.ViolationAt);
    }

    @Test
    void strict_iso_and_zoneless_forms_bind_into_an_instant_field() throws Exception {
        assertEquals(Instant.parse("2026-08-10T09:30:00Z"),
                mapper.readValue("{\"ViolationAt\":\"2026-08-10T12:30:00+03:00\"}", Fine.class).ViolationAt);
        assertEquals(Instant.parse("2026-08-10T12:30:00Z"),
                mapper.readValue("{\"ViolationAt\":\"2026-08-10T12:30\"}", Fine.class).ViolationAt);
        assertEquals(Instant.parse("2026-08-10T00:00:00Z"), mapper.readValue("{\"ViolationAt\":\"2026-08-10\"}", Fine.class).ViolationAt);
    }

    /**
     * The generated UI turns a picked date into a full ISO instant even when the target column is
     * date-only — the date part binds as written.
     */
    @Test
    void full_iso_instant_binds_into_a_local_date_field() throws Exception {
        Fine fine = mapper.readValue("{\"DeclaredAt\":\"2026-08-10T00:00:00.000Z\"}", Fine.class);
        assertEquals(LocalDate.of(2026, 8, 10), fine.DeclaredAt);
    }

    @Test
    void space_separated_date_time_binds_into_a_local_date_time_field() throws Exception {
        Fine fine = mapper.readValue("{\"At\":\"2026-08-10 12:30\"}", Fine.class);
        assertEquals(LocalDateTime.of(2026, 8, 10, 12, 30), fine.At);
    }

    /** Non-string shapes keep the standard jsr310 behavior (epoch seconds for Instant). */
    @Test
    void epoch_number_still_binds_through_the_default_deserializer() throws Exception {
        Fine fine = mapper.readValue("{\"ViolationAt\":1786712345}", Fine.class);
        assertEquals(Instant.ofEpochSecond(1786712345L), fine.ViolationAt);
    }

    /** Garbage still fails with the standard Jackson diagnostics — leniency is not silence. */
    @Test
    void malformed_string_still_fails_with_jackson_diagnostics() {
        assertThrows(InvalidFormatException.class, () -> mapper.readValue("{\"ViolationAt\":\"10/08/2026 12:30\"}", Fine.class));
    }

    static class Fine {
        public Instant ViolationAt;
        public LocalDate DeclaredAt;
        public LocalDateTime At;
    }
}
