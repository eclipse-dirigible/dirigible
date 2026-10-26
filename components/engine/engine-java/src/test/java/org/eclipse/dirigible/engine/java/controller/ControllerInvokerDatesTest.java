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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.engine.java.component.TestComponentContainers;
import org.eclipse.dirigible.engine.java.controller.ControllerInvokerBindingTest.FakeResponse;
import org.eclipse.dirigible.sdk.http.Body;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.Post;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import tools.jackson.databind.ObjectMapper;

/**
 * The wire format of a date a controller answers (#8083): ISO-8601 by default, the Jackson
 * timestamps of earlier releases behind {@code DIRIGIBLE_JAVA_CONTROLLER_DATES_AS_TIMESTAMPS}. The
 * invoker is built through its Spring constructor, so the switch is read where production reads it.
 */
class ControllerInvokerDatesTest {

    private static final String SWITCH = DirigibleConfig.JAVA_CONTROLLER_DATES_AS_TIMESTAMPS.getKey();

    private static final Instant CREATED_AT = Instant.parse("2026-10-23T10:34:10.343660Z");

    private final ControllerRouter router = new ControllerRouter();

    private final ControllerClassConsumer consumer =
            new ControllerClassConsumer(router, Optional.empty(), TestComponentContainers.of(Tickets.class));

    @AfterEach
    void resetSwitch() {
        Configuration.remove(SWITCH);
    }

    @Test
    void anInstantIsAnsweredAsIso8601Utc() {
        String body = get(new ControllerInvoker(noSpringMapper()));

        assertTrue(body.contains("\"CreatedAt\":\"2026-10-23T10:34:10.343660Z\""), body);
    }

    @Test
    void aLocalDateIsAnsweredAsIsoDate() {
        String body = get(new ControllerInvoker(noSpringMapper()));

        assertTrue(body.contains("\"dueDate\":\"2026-11-02\""), body);
        assertTrue(body.contains("\"remindAt\":\"2026-11-02T09:30:00\""), body);
        assertTrue(body.contains("\"opensAt\":\"09:30:00\""), body);
    }

    @Test
    void theSwitchRestoresEpochTimestamps() {
        Configuration.set(SWITCH, "true");

        String body = get(new ControllerInvoker(noSpringMapper()));

        assertTrue(body.contains("\"CreatedAt\":1792751650.343660000"), body);
        assertTrue(body.contains("\"dueDate\":[2026,11,2]"), body);
        assertTrue(body.contains("\"remindAt\":[2026,11,2,9,30]"), body);
        assertTrue(body.contains("\"opensAt\":[9,30]"), body);
    }

    @Test
    void aNumericDateInTheBodyStillBinds() {
        FakeResponse response = invoke(new ControllerInvoker(noSpringMapper()), "echo",
                "{\"CreatedAt\":1792751650.343660000,\"dueDate\":[2026,11,2],\"remindAt\":\"2026-11-02 09:30\",\"opensAt\":\"09:30\"}");

        assertEquals(200, response.status(), response.body());
        assertTrue(response.body()
                           .contains("\"CreatedAt\":\"2026-10-23T10:34:10.343660Z\""),
                response.body());
        assertTrue(response.body()
                           .contains("\"dueDate\":\"2026-11-02\""),
                response.body());
    }

    /** A context with no ObjectMapper bean - the invoker falls back to a fresh mapper. */
    private static ObjectProvider<ObjectMapper> noSpringMapper() {
        return new StaticListableBeanFactory().getBeanProvider(ObjectMapper.class);
    }

    private String get(ControllerInvoker invoker) {
        FakeResponse response = invoke(invoker, "ticket", null);
        assertEquals(200, response.status(), response.body());
        return response.body();
    }

    private FakeResponse invoke(ControllerInvoker invoker, String methodName, String requestBody) {
        ControllerEntry entry = consumer.build(ControllerInvokerBindingTest.loaded(Tickets.class));
        Route route = entry.routes()
                           .stream()
                           .filter(r -> r.method()
                                         .getName()
                                         .equals(methodName))
                           .findFirst()
                           .orElseThrow();
        FakeResponse response = new FakeResponse();
        invoker.invoke(new RouteMatch(entry, route, Map.of()), ControllerInvokerBindingTest.mockRequest(requestBody), response);
        return response;
    }

    // --- fixtures --------------------------------------------------------------------------------

    @Controller
    static class Tickets {

        @Get("/ticket")
        public Ticket ticket() {
            Ticket ticket = new Ticket();
            ticket.CreatedAt = CREATED_AT;
            ticket.dueDate = LocalDate.of(2026, 11, 2);
            ticket.remindAt = LocalDateTime.of(2026, 11, 2, 9, 30);
            ticket.opensAt = LocalTime.of(9, 30);
            return ticket;
        }

        @Post("/echo")
        public Ticket echo(@Body Ticket ticket) {
            return ticket;
        }
    }

    static class Ticket {
        public Instant CreatedAt;
        public LocalDate dueDate;
        public LocalDateTime remindAt;
        public LocalTime opensAt;
    }
}
