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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.security.Roles;
import org.eclipse.dirigible.engine.java.controller.ControllerInvokerBindingTest.FakeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

class ControllerInvokerRolesTest {

    private final ControllerRouter router = new ControllerRouter();

    private final ControllerClassConsumer consumer = new ControllerClassConsumer(router, Optional.empty());

    private final ControllerInvoker invoker = new ControllerInvoker(new ObjectMapper());

    @Test
    void user_in_required_role_passes() {
        ControllerEntry entry = consumer.build(loaded(Restricted.class));
        Route route = entry.routes()
                           .get(0);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.isUserInRole("DEVELOPER")).thenReturn(false);
        when(req.isUserInRole("ADMINISTRATOR")).thenReturn(false);
        when(req.isUserInRole("OPERATOR")).thenReturn(true);

        FakeResponse resp = new FakeResponse();
        invoker.invoke(new RouteMatch(entry, route, Map.of()), req, resp);
        assertEquals(200, resp.status());
    }

    @Test
    void user_with_no_matching_role_is_forbidden() {
        ControllerEntry entry = consumer.build(loaded(Restricted.class));
        Route route = entry.routes()
                           .get(0);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteUser()).thenReturn("alice");
        // every isUserInRole call returns false by default.

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> invoker.invoke(new RouteMatch(entry, route, Map.of()), req, new FakeResponse()));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }

    @Test
    void developer_super_role_short_circuits_class_check() {
        ControllerEntry entry = consumer.build(loaded(Restricted.class));
        Route route = entry.routes()
                           .get(0);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.isUserInRole("DEVELOPER")).thenReturn(true);

        FakeResponse resp = new FakeResponse();
        invoker.invoke(new RouteMatch(entry, route, Map.of()), req, resp);
        assertEquals(200, resp.status());
    }

    @Test
    void method_level_roles_override_class_level() {
        ControllerEntry entry = consumer.build(loaded(MixedRoles.class));
        Route adminOnly = entry.routes()
                               .stream()
                               .filter(r -> r.method()
                                             .getName()
                                             .equals("adminThing"))
                               .findFirst()
                               .orElseThrow();
        assertEquals(java.util.List.of("ADMINISTRATOR"), adminOnly.roles());

        Route classRoleInherited = entry.routes()
                                        .stream()
                                        .filter(r -> r.method()
                                                      .getName()
                                                      .equals("operatorThing"))
                                        .findFirst()
                                        .orElseThrow();
        assertEquals(java.util.List.of("OPERATOR"), classRoleInherited.roles());
    }

    // --- fixtures --------------------------------------------------------------------------------

    @Controller
    @Roles({"OPERATOR"})
    static class Restricted {

        @Get("/secret")
        public String secret() {
            return "ok";
        }
    }

    @Controller
    @Roles({"OPERATOR"})
    static class MixedRoles {

        @Get("/admin")
        @Roles({"ADMINISTRATOR"})
        public String adminThing() {
            return "ok";
        }

        @Get("/op")
        public String operatorThing() {
            return "ok";
        }
    }

    private static org.eclipse.dirigible.engine.java.spi.LoadedClass loaded(Class<?> type) {
        return new org.eclipse.dirigible.engine.java.spi.LoadedClass("p", type.getName(), type, type.getClassLoader());
    }
}
