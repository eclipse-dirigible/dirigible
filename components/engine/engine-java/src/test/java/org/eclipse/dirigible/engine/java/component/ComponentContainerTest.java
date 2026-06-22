/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.component.Inject;
import org.junit.jupiter.api.Test;

import jakarta.annotation.PostConstruct;

/** Unit coverage for the client bean container: injection styles, naming, collections, cycles. */
class ComponentContainerTest {

    @Test
    void constructor_injection_wires_a_collaborator() {
        ComponentContainer container = TestComponentContainers.of(Engine.class, Car.class);

        Car car = container.get(Car.class)
                           .orElseThrow();
        assertSame(container.get(Engine.class)
                            .orElseThrow(),
                car.engine);
    }

    @Test
    void field_injection_wires_a_collaborator() {
        ComponentContainer container = TestComponentContainers.of(Engine.class, Dashboard.class);

        Dashboard dashboard = container.get(Dashboard.class)
                                       .orElseThrow();
        assertEquals(Engine.class, dashboard.engine.getClass());
    }

    @Test
    void collection_injection_gets_every_implementation() {
        ComponentContainer container = TestComponentContainers.of(EnglishGreeter.class, GermanGreeter.class, GreetingHub.class);

        GreetingHub hub = container.get(GreetingHub.class)
                                   .orElseThrow();
        assertEquals(2, hub.greeters.size());
        assertEquals(2, container.getAll(Greeter.class)
                                 .size());
    }

    @Test
    void default_bean_name_is_the_decapitalized_simple_name() {
        ComponentContainer container = TestComponentContainers.of(Engine.class);

        assertTrue(container.get("engine", Engine.class)
                            .isPresent());
    }

    @Test
    void explicit_bean_name_is_honoured() {
        ComponentContainer container = TestComponentContainers.of(NamedService.class);

        assertTrue(container.get("custom", NamedService.class)
                            .isPresent());
        assertFalse(container.get("namedService", NamedService.class)
                             .isPresent());
    }

    @Test
    void post_construct_runs_after_construction() {
        ComponentContainer container = TestComponentContainers.of(Initialised.class);

        assertTrue(container.get(Initialised.class)
                            .orElseThrow().ready);
    }

    @Test
    void construction_cycle_is_detected_and_the_beans_are_not_created() {
        ComponentContainer container = TestComponentContainers.of(Ping.class, Pong.class);

        assertTrue(container.get(Ping.class)
                            .isEmpty());
        assertTrue(container.get(Pong.class)
                            .isEmpty());
    }

    @Test
    void unsatisfied_dependency_leaves_the_bean_uncreated() {
        // Car needs Engine, which is absent from this generation.
        ComponentContainer container = TestComponentContainers.of(Car.class);

        assertTrue(container.get(Car.class)
                            .isEmpty());
    }

    // --- fixtures --------------------------------------------------------------------------------

    @Component
    static class Engine {
    }

    @Component
    static class Car {
        final Engine engine;

        Car(Engine engine) {
            this.engine = engine;
        }
    }

    @Component
    static class Dashboard {
        @Inject
        Engine engine;
    }

    interface Greeter {
    }

    @Component
    static class EnglishGreeter implements Greeter {
    }

    @Component
    static class GermanGreeter implements Greeter {
    }

    @Component
    static class GreetingHub {
        final List<Greeter> greeters;

        GreetingHub(List<Greeter> greeters) {
            this.greeters = greeters;
        }
    }

    @Component("custom")
    static class NamedService {
    }

    @Component
    static class Initialised {
        boolean ready;

        @PostConstruct
        void init() {
            ready = true;
        }
    }

    @Component
    static class Ping {
        final Pong pong;

        Ping(Pong pong) {
            this.pong = pong; // cycle: Ping needs Pong
        }
    }

    @Component
    static class Pong {
        final Ping ping;

        Pong(Ping ping) {
            this.ping = ping; // cycle: Pong needs Ping
        }
    }
}
