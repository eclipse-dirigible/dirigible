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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.component.Inject;
import org.junit.jupiter.api.Test;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Unit coverage for {@code createUnmanaged} — wiring a client class the container does NOT own,
 * which is what a client {@code JavaDelegate} is: Flowable instantiates it, so it can never be a
 * bean. The fixtures are therefore deliberately <em>not</em> {@code @Component}.
 */
class ComponentContainerUnmanagedTest {

    @Test
    void constructor_injection_wires_an_instance_the_container_does_not_own() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class);

        ConstructorDelegate delegate = container.createUnmanaged(ConstructorDelegate.class)
                                                .orElseThrow();

        assertSame(container.get(RateProvider.class)
                            .orElseThrow(),
                delegate.rates);
    }

    @Test
    void field_injection_wires_an_instance_the_container_does_not_own() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class);

        FieldDelegate delegate = container.createUnmanaged(FieldDelegate.class)
                                          .orElseThrow();

        assertSame(container.get(RateProvider.class)
                            .orElseThrow(),
                delegate.rates);
    }

    @Test
    void collection_injection_gets_every_contribution_in_registration_order() {
        ComponentContainer container = TestComponentContainers.of(EnglishGreeter.class, GermanGreeter.class);

        CollectionDelegate delegate = container.createUnmanaged(CollectionDelegate.class)
                                               .orElseThrow();

        assertEquals(List.of(EnglishGreeter.class, GermanGreeter.class), delegate.greeters.stream()
                                                                                          .map(Object::getClass)
                                                                                          .toList());
    }

    @Test
    void post_construct_runs_on_an_unmanaged_instance() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class);

        assertTrue(container.createUnmanaged(ConstructorDelegate.class)
                            .orElseThrow().ready);
    }

    @Test
    void pre_destroy_is_never_invoked_because_nothing_owns_the_instance() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class);

        assertFalse(container.createUnmanaged(ConstructorDelegate.class)
                             .orElseThrow().closed);
    }

    @Test
    void the_instance_is_not_registered_as_a_bean() {
        ComponentContainer container = TestComponentContainers.of(EnglishGreeter.class, GermanGreeter.class);

        container.createUnmanaged(CollectionDelegate.class)
                 .orElseThrow();

        assertTrue(container.get(CollectionDelegate.class)
                            .isEmpty());
        assertTrue(container.instanceOf(CollectionDelegate.class)
                            .isEmpty());
        assertTrue(container.get("collectionDelegate", CollectionDelegate.class)
                            .isEmpty());
        // Registering it would also have made it a candidate for every List<Greeter> injection point.
        assertEquals(2, container.getAll(Greeter.class)
                                 .size());
    }

    @Test
    void every_call_yields_a_fresh_instance_sharing_the_collaborator_singleton() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class);

        ConstructorDelegate first = container.createUnmanaged(ConstructorDelegate.class)
                                             .orElseThrow();
        ConstructorDelegate second = container.createUnmanaged(ConstructorDelegate.class)
                                              .orElseThrow();

        assertNotSame(first, second);
        assertSame(first.rates, second.rates);
    }

    @Test
    void a_class_with_nothing_to_wire_is_left_to_the_callers_own_instantiation() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class);

        // The whole backward-compatibility rule: every delegate that worked before injection existed
        // keeps being built by its caller's plain no-arg constructor.
        assertTrue(container.createUnmanaged(PlainDelegate.class)
                            .isEmpty());
    }

    @Test
    void a_post_construct_only_class_is_still_wired_so_its_callback_is_not_silently_dropped() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class);

        assertTrue(container.createUnmanaged(PostConstructOnlyDelegate.class)
                            .orElseThrow().ready);
    }

    @Test
    void an_unsatisfied_dependency_is_refused_and_is_not_a_rebuild_error() {
        ComponentContainer container = TestComponentContainers.of();

        BeanContainerException exception =
                assertThrows(BeanContainerException.class, () -> container.createUnmanaged(ConstructorDelegate.class));

        assertTrue(exception.getMessage()
                            .contains(RateProvider.class.getName()),
                exception.getMessage());
        // It is the step's failure, not the generation's: nothing is reported to the Problems view.
        assertTrue(container.wiringErrors()
                            .isEmpty());
    }

    @Test
    void an_ambiguous_dependency_is_refused_rather_than_guessed() {
        ComponentContainer container = TestComponentContainers.of(EnglishGreeter.class, GermanGreeter.class);

        BeanContainerException exception =
                assertThrows(BeanContainerException.class, () -> container.createUnmanaged(AmbiguousDelegate.class));

        assertTrue(exception.getMessage()
                            .contains("Ambiguous dependency"),
                exception.getMessage());
        assertTrue(exception.getMessage()
                            .contains("englishGreeter"),
                exception.getMessage());
    }

    @Test
    void a_field_name_matching_a_bean_name_disambiguates() {
        ComponentContainer container = TestComponentContainers.of(EnglishGreeter.class, GermanGreeter.class);

        NameHintedDelegate delegate = container.createUnmanaged(NameHintedDelegate.class)
                                               .orElseThrow();

        assertEquals(GermanGreeter.class, delegate.germanGreeter.getClass());
    }

    // --- fixtures (not @Component: a delegate is never a bean) ------------------------------------

    @Component
    static class RateProvider {
    }

    interface Greeter {
    }

    @Component
    static class EnglishGreeter implements Greeter {
    }

    @Component
    static class GermanGreeter implements Greeter {
    }

    static class ConstructorDelegate {
        final RateProvider rates;
        boolean ready;
        boolean closed;

        ConstructorDelegate(RateProvider rates) {
            this.rates = rates;
        }

        @PostConstruct
        void init() {
            ready = true;
        }

        @PreDestroy
        void close() {
            closed = true;
        }
    }

    static class FieldDelegate {
        @Inject
        RateProvider rates;
    }

    static class CollectionDelegate {
        @Inject
        List<Greeter> greeters;
    }

    static class PlainDelegate {
    }

    static class PostConstructOnlyDelegate {
        boolean ready;

        @PostConstruct
        void init() {
            ready = true;
        }
    }

    static class AmbiguousDelegate {
        @Inject
        Greeter greeter;
    }

    static class NameHintedDelegate {
        @Inject
        Greeter germanGreeter;
    }
}
