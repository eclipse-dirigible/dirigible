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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.sdk.component.Inject;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.engine.java.spi.DependencyResolver;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.Test;

class ControllerClassConsumerInjectTest {

    @Test
    void inject_fields_are_satisfied_from_dependency_resolver() throws Exception {
        FakeService fake = new FakeService();
        ControllerClassConsumer consumer = consumerWith(fake);

        ControllerEntry entry = consumer.build(loaded(WithInjectedService.class));

        Object controller = entry.instance();
        Field field = WithInjectedService.class.getDeclaredField("service");
        field.setAccessible(true);
        assertSame(fake, field.get(controller));
    }

    @Test
    void inject_field_without_resolver_match_fails_fast() {
        ControllerClassConsumer consumer = consumerWith();
        assertThrows(IllegalStateException.class, () -> consumer.build(loaded(WithInjectedService.class)));
    }

    @Test
    void controller_without_any_inject_fields_is_built_when_resolver_is_empty() throws Exception {
        ControllerClassConsumer consumer = consumerWith();
        ControllerEntry entry = consumer.build(loaded(NoInjections.class));
        // No fields needed injection; the controller instance exists and the @Get route registered.
        assertEquals(1, entry.routes()
                             .size());
        Field f = NoInjections.class.getDeclaredField("untouched");
        f.setAccessible(true);
        assertNull(f.get(entry.instance()));
    }

    @Test
    void inject_fields_on_superclass_are_also_satisfied() throws Exception {
        FakeService fake = new FakeService();
        ControllerClassConsumer consumer = consumerWith(fake);

        ControllerEntry entry = consumer.build(loaded(SubclassController.class));

        Object controller = entry.instance();
        Field field = ParentController.class.getDeclaredField("inheritedService");
        field.setAccessible(true);
        assertSame(fake, field.get(controller));
    }

    @Test
    void first_resolver_that_matches_wins() throws Exception {
        FakeService first = new FakeService();
        FakeService second = new FakeService();
        ControllerClassConsumer consumer = new ControllerClassConsumer(new ControllerRouter(), Optional.empty(),
                List.of(typedResolver(FakeService.class, first), typedResolver(FakeService.class, second)));

        ControllerEntry entry = consumer.build(loaded(WithInjectedService.class));
        Field field = WithInjectedService.class.getDeclaredField("service");
        field.setAccessible(true);
        assertSame(first, field.get(entry.instance()));
    }

    // --- helpers ---------------------------------------------------------------------------------

    private static ControllerClassConsumer consumerWith(FakeService... services) {
        List<DependencyResolver> resolvers = services.length == 0 ? List.of() : List.of(typedResolver(FakeService.class, services[0]));
        return new ControllerClassConsumer(new ControllerRouter(), Optional.empty(), resolvers);
    }

    private static DependencyResolver typedResolver(Class<?> type, Object instance) {
        return requested -> requested == type ? Optional.of(instance) : Optional.empty();
    }

    private static LoadedClass loaded(Class<?> type) {
        return new LoadedClass("p", type.getName(), type, type.getClassLoader());
    }

    // --- fixtures --------------------------------------------------------------------------------

    static class FakeService {
    }

    @Controller
    static class WithInjectedService {
        @Inject
        private FakeService service;

        @Get("/x")
        public String x() {
            return service == null ? "null" : "ok";
        }
    }

    @Controller
    static class NoInjections {
        @SuppressWarnings("unused")
        private FakeService untouched;

        @Get
        public String hello() {
            return "hi";
        }
    }

    abstract static class ParentController {
        @Inject
        protected FakeService inheritedService;
    }

    @Controller
    static class SubclassController extends ParentController {
        @Get("/inherit")
        public String byInheritance() {
            return inheritedService == null ? "null" : "ok";
        }
    }
}
