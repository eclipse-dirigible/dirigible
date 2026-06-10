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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.sdk.http.Body;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Delete;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.PathParam;
import org.eclipse.dirigible.sdk.http.Post;
import org.eclipse.dirigible.sdk.http.QueryParam;
import org.eclipse.dirigible.sdk.security.Roles;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class ControllerClassConsumerBuildTest {

    private final ControllerRouter router = new ControllerRouter();

    private final ControllerClassConsumer consumer = new ControllerClassConsumer(router, Optional.empty());

    @Test
    void accepts_only_controller_annotated_classes() {
        assertTrue(consumer.accepts(SampleController.class));
        assertTrue(!consumer.accepts(NotAController.class));
    }

    @Test
    void build_walks_all_http_methods_and_captures_path_templates() {
        ControllerEntry entry = consumer.build(loaded(SampleController.class));
        List<Route> routes = entry.routes();
        assertEquals(4, routes.size());

        Route list = routeByName(routes, "list");
        assertEquals(HttpMethod.GET, list.httpMethod());
        assertEquals("/list", list.pathTemplate());
        assertEquals(1, list.paramBindings()
                            .size());
        assertEquals(ParamBinding.Kind.QUERY, list.paramBindings()
                                                  .get(0)
                                                  .kind());

        Route byId = routeByName(routes, "byId");
        assertEquals(HttpMethod.GET, byId.httpMethod());
        assertEquals("/{id}", byId.pathTemplate());
        assertEquals(ParamBinding.Kind.PATH, byId.paramBindings()
                                                 .get(0)
                                                 .kind());

        Route create = routeByName(routes, "create");
        assertEquals(HttpMethod.POST, create.httpMethod());
        assertEquals("", create.pathTemplate());
        assertEquals(ParamBinding.Kind.BODY, create.paramBindings()
                                                   .get(0)
                                                   .kind());
        // Method-level @Roles overrides class-level.
        assertEquals(List.of("ADMINISTRATOR"), create.roles());

        Route delete = routeByName(routes, "remove");
        assertEquals(HttpMethod.DELETE, delete.httpMethod());
        assertEquals(List.of("DEVELOPER"), delete.roles());
    }

    @Test
    void registering_a_controller_also_implementing_javahandler_is_skipped() {
        // No throw, just ignored — the router stays empty.
        consumer.onClassLoaded(loaded(HybridConfusion.class));
        assertEquals(0, router.size());
    }

    @Test
    void duplicate_routes_keep_the_first_declared() {
        ControllerEntry entry = consumer.build(loaded(DuplicateRoutes.class));
        assertEquals(1, entry.routes()
                             .size());
    }

    @Test
    void controller_with_no_routes_is_not_registered() {
        consumer.onClassLoaded(loaded(EmptyController.class));
        assertEquals(0, router.size());
    }

    @Test
    void controller_without_no_arg_constructor_is_rejected() {
        // onClassLoaded catches the exception and logs; the router stays empty.
        consumer.onClassLoaded(loaded(NoArgFreeController.class));
        assertEquals(0, router.size());
    }

    @Test
    void context_parameters_are_auto_bound_by_type() {
        ControllerEntry entry = consumer.build(loaded(ContextOnlyController.class));
        Route route = entry.routes()
                           .get(0);
        assertEquals(ParamBinding.Kind.CTX_REQUEST, route.paramBindings()
                                                         .get(0)
                                                         .kind());
        assertEquals(ParamBinding.Kind.CTX_RESPONSE, route.paramBindings()
                                                          .get(1)
                                                          .kind());
    }

    @Test
    void multiple_body_annotations_are_rejected() {
        assertThrows(IllegalStateException.class, () -> consumer.build(loaded(TwoBodiesController.class)));
    }

    // --- fixtures --------------------------------------------------------------------------------

    @Controller
    @Roles({"DEVELOPER"})
    static class SampleController {

        @Get("/list")
        public List<String> list(@QueryParam("limit") Integer limit) {
            return List.of();
        }

        @Get("/{id}")
        public String byId(@PathParam("id") Long id) {
            return id.toString();
        }

        @Post
        @Roles({"ADMINISTRATOR"})
        public String create(@Body String body) {
            return body;
        }

        @Delete
        public void remove() {}
    }

    static class NotAController {
    }

    @Controller
    static class HybridConfusion implements JavaHandler {

        @Get
        public String stuff() {
            return "x";
        }

        @Override
        public void handle(HttpServletRequest req, HttpServletResponse resp) {}
    }

    @Controller
    static class DuplicateRoutes {

        @Get("/x")
        public String a() {
            return "a";
        }

        @Get("/x")
        public String b() {
            return "b";
        }
    }

    @Controller
    static class EmptyController {

        public String notARoute() {
            return "no";
        }
    }

    @Controller
    static class NoArgFreeController {

        @SuppressWarnings("unused")
        private final String dep;

        NoArgFreeController(String dep) {
            this.dep = dep;
        }

        @Get
        public String x() {
            return "x";
        }
    }

    @Controller
    static class ContextOnlyController {

        @Get("/x")
        public void run(HttpServletRequest req, HttpServletResponse resp) {}
    }

    @Controller
    static class TwoBodiesController {

        @Post
        public String x(@Body String a, @Body String b) {
            return a + b;
        }
    }

    // --- helpers ---------------------------------------------------------------------------------

    private static LoadedClass loaded(Class<?> type) {
        return new LoadedClass("p", type.getName(), type, type.getClassLoader());
    }

    private static Route routeByName(List<Route> routes, String methodName) {
        return routes.stream()
                     .filter(r -> r.method()
                                   .getName()
                                   .equals(methodName))
                     .findFirst()
                     .orElseThrow();
    }
}
