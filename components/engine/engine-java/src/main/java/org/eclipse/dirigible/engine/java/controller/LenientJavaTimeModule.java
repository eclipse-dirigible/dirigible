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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.dirigible.sdk.utils.LenientJavaTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

/**
 * Jackson module making {@code @Body} binding tolerant of near-ISO date/time strings for the three
 * {@code java.time} types the generated entities use ({@code timestamp} → {@link Instant},
 * {@code date} → {@link LocalDate}, plus {@link LocalDateTime} for hand-written controllers).
 * <p>
 * A generated controller is a boundary with parties that cannot be re-taught strict ISO-8601: an
 * {@code inbound:} webhook forwards whatever the external system emits, and a browser form can pass
 * a user-typed {@code "2026-08-10 12:30"} through verbatim (WebKit's {@code new Date()} rejects the
 * space separator, so the client-side ISO conversion is skipped). The accepted shapes and their
 * semantics (zoneless local date-time reads as UTC; a date-only target takes the date part as
 * written) are defined by {@link LenientJavaTime}.
 * <p>
 * Only STRING values take the lenient path — and it is attempted first, since it also covers strict
 * ISO. Everything else (epoch numbers, {@code [y,m,d]} arrays) and any string no lenient shape
 * matches falls through to the standard jsr310 deserializer, preserving its behavior and its
 * diagnostics for genuinely malformed input.
 */
class LenientJavaTimeModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    LenientJavaTimeModule() {
        super("dirigible-lenient-java-time");
        addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
            @Override
            public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    Instant lenient = LenientJavaTime.parseInstant(p.getText());
                    if (lenient != null) {
                        return lenient;
                    }
                }
                return InstantDeserializer.INSTANT.deserialize(p, ctxt);
            }
        });
        addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    LocalDate lenient = LenientJavaTime.parseLocalDate(p.getText());
                    if (lenient != null) {
                        return lenient;
                    }
                }
                return LocalDateDeserializer.INSTANCE.deserialize(p, ctxt);
            }
        });
        addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    LocalDateTime lenient = LenientJavaTime.parseLocalDateTime(p.getText());
                    if (lenient != null) {
                        return lenient;
                    }
                }
                return LocalDateTimeDeserializer.INSTANCE.deserialize(p, ctxt);
            }
        });
    }
}
