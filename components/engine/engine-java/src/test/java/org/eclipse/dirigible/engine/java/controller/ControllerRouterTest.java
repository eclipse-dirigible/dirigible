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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class ControllerRouterTest {

    private final ControllerRouter router = new ControllerRouter();

    @Test
    void unknown_request_yields_empty_match() {
        assertTrue(router.match(HttpMethod.GET, "any", "/missing/Controller/x")
                         .isEmpty());
    }

    @Test
    void registered_controller_matches_get_with_literal_suffix() {
        ControllerEntry entry = entry("demo", "demo.Hello", List.of(route(HttpMethod.GET, "/ping")));
        router.register(entry);

        RouteMatch match = router.match(HttpMethod.GET, "demo", "/demo/Hello/ping")
                                 .orElseThrow();
        assertEquals(entry.fqn(), match.entry()
                                       .fqn());
        assertEquals(HttpMethod.GET, match.route()
                                          .httpMethod());
    }

    @Test
    void method_must_match() {
        ControllerEntry entry = entry("p", "demo.Hello", List.of(route(HttpMethod.GET, "/ping")));
        router.register(entry);
        assertTrue(router.match(HttpMethod.POST, "p", "/demo/Hello/ping")
                         .isEmpty());
    }

    @Test
    void project_isolation() {
        router.register(entry("a", "demo.Hello", List.of(route(HttpMethod.GET, "/ping"))));
        assertTrue(router.match(HttpMethod.GET, "b", "/demo/Hello/ping")
                         .isEmpty());
    }

    @Test
    void path_placeholder_is_extracted() {
        router.register(entry("p", "demo.Hello", List.of(route(HttpMethod.GET, "/items/{id}"))));
        RouteMatch match = router.match(HttpMethod.GET, "p", "/demo/Hello/items/42")
                                 .orElseThrow();
        assertEquals("42", match.pathParameters()
                                .get("id"));
    }

    @Test
    void empty_suffix_matches_base_path_with_and_without_trailing_slash() {
        router.register(entry("p", "demo.Hello", List.of(route(HttpMethod.POST, ""))));
        assertTrue(router.match(HttpMethod.POST, "p", "/demo/Hello")
                         .isPresent());
        assertTrue(router.match(HttpMethod.POST, "p", "/demo/Hello/")
                         .isPresent());
    }

    @Test
    void longer_basepath_wins_when_two_controllers_overlap() {
        ControllerEntry shorter = entry("p", "demo.Outer", List.of(route(HttpMethod.GET, "/x")));
        ControllerEntry longer = entry("p", "demo.Outer.Inner", List.of(route(HttpMethod.GET, "/x")));
        router.register(shorter);
        router.register(longer);

        // /demo/Outer/Inner/x — both prefixes match; longer one must be picked.
        RouteMatch match = router.match(HttpMethod.GET, "p", "/demo/Outer/Inner/x")
                                 .orElseThrow();
        assertEquals(longer.fqn(), match.entry()
                                        .fqn());
    }

    @Test
    void unregister_removes_entry() {
        ControllerEntry entry = entry("p", "demo.Hello", List.of(route(HttpMethod.GET, "/x")));
        router.register(entry);
        router.unregister("p", "demo.Hello");
        assertEquals(0, router.size());
        assertTrue(router.match(HttpMethod.GET, "p", "/demo/Hello/x")
                         .isEmpty());
    }

    @Test
    void literal_route_preferred_over_placeholder_route() {
        ControllerEntry entry =
                entry("p", "demo.Hello", List.of(route(HttpMethod.GET, "/items/{id}"), route(HttpMethod.GET, "/items/featured")));
        router.register(entry);

        RouteMatch match = router.match(HttpMethod.GET, "p", "/demo/Hello/items/featured")
                                 .orElseThrow();
        assertEquals("/items/featured", match.route()
                                             .pathTemplate());
    }

    // --- helpers ---------------------------------------------------------------------------------

    private static Route route(HttpMethod method, String template) {
        PathPattern.Compiled c = PathPattern.compile(template);
        return new Route(method, template, c.pattern(), c.placeholders(), null, List.of(), List.of());
    }

    private static ControllerEntry entry(String project, String fqn, List<Route> routes) {
        return new ControllerEntry(project, fqn, ControllerRouter.fqnToBasePath(fqn), new Object(), routes);
    }
}
