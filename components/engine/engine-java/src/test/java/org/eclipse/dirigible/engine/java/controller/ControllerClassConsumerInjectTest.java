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
import java.util.Optional;

import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.component.TestComponentContainers;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.component.Inject;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.junit.jupiter.api.Test;

/**
 * Verifies that controllers fetch their fully-injected instance from the {@link ComponentContainer}
 * — both {@code @Inject} field injection and constructor injection, including inherited fields.
 */
class ControllerClassConsumerInjectTest {

    @Test
    void inject_fields_are_satisfied_from_the_container() throws Exception {
        ControllerClassConsumer consumer = consumerWith(FakeService.class, WithInjectedField.class);

        ControllerEntry entry = consumer.build(loaded(WithInjectedField.class));

        Field field = WithInjectedField.class.getDeclaredField("service");
        field.setAccessible(true);
        assertEquals(FakeService.class, field.get(entry.instance())
                                             .getClass());
    }

    @Test
    void constructor_dependencies_are_satisfied_from_the_container() throws Exception {
        ControllerClassConsumer consumer = consumerWith(FakeService.class, WithConstructorDependency.class);

        ControllerEntry entry = consumer.build(loaded(WithConstructorDependency.class));

        assertSame(FakeService.class, ((WithConstructorDependency) entry.instance()).service.getClass());
    }

    @Test
    void controller_with_unsatisfied_dependency_is_not_built() {
        // FakeService is absent from the generation, so the container can't construct the controller.
        ControllerClassConsumer consumer = consumerWith(WithConstructorDependency.class);
        assertThrows(IllegalStateException.class, () -> consumer.build(loaded(WithConstructorDependency.class)));
    }

    @Test
    void controller_without_any_dependency_is_built() throws Exception {
        ControllerClassConsumer consumer = consumerWith(NoInjections.class);

        ControllerEntry entry = consumer.build(loaded(NoInjections.class));

        assertEquals(1, entry.routes()
                             .size());
        Field f = NoInjections.class.getDeclaredField("untouched");
        f.setAccessible(true);
        assertNull(f.get(entry.instance()));
    }

    @Test
    void inject_fields_on_a_superclass_are_also_satisfied() throws Exception {
        ControllerClassConsumer consumer = consumerWith(FakeService.class, SubclassController.class);

        ControllerEntry entry = consumer.build(loaded(SubclassController.class));

        Field field = ParentController.class.getDeclaredField("inheritedService");
        field.setAccessible(true);
        assertEquals(FakeService.class, field.get(entry.instance())
                                             .getClass());
    }

    // --- helpers ---------------------------------------------------------------------------------

    private static ControllerClassConsumer consumerWith(Class<?>... beans) {
        ComponentContainer container = TestComponentContainers.of(beans);
        return new ControllerClassConsumer(new ControllerRouter(), Optional.empty(), container);
    }

    private static LoadedClass loaded(Class<?> type) {
        return new LoadedClass("p", type.getName(), type, type.getClassLoader());
    }

    // --- fixtures --------------------------------------------------------------------------------

    @Component
    static class FakeService {
    }

    @Controller
    static class WithInjectedField {
        @Inject
        private FakeService service;

        @Get("/x")
        public String x() {
            return service == null ? "null" : "ok";
        }
    }

    @Controller
    static class WithConstructorDependency {
        final FakeService service;

        WithConstructorDependency(FakeService service) {
            this.service = service;
        }

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
