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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.component.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * "A {@code JavaDelegate} must NOT be a {@code @Component}" — where and how often the container
 * states that rule. It belongs on the Problems view at publish, in front of the developer who wrote
 * the annotation; the execution-time WARN is a fallback that must not restate the same fact on
 * every step execution.
 */
class ComponentContainerDelegateRuleTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void captureContainerLog() {
        logger = (Logger) LoggerFactory.getLogger(ComponentContainer.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        // The suppressed repeats are DEBUG, so the assertions can only see them with DEBUG enabled.
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void releaseContainerLog() {
        logger.detachAppender(appender);
        logger.setLevel(null);
    }

    @Test
    void a_bean_that_is_also_a_delegate_is_a_wiring_warning_of_the_generation() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class, ComponentDelegate.class);

        String warning = container.wiringWarnings()
                                  .get(ComponentDelegate.class.getName());

        assertEquals(List.of(ComponentDelegate.class.getName()), List.copyOf(container.wiringWarnings()
                                                                                      .keySet()));
        assertTrue(warning.contains(ComponentDelegate.class.getName()), warning);
        assertTrue(warning.contains("must NOT be a @Component"), warning);
        assertTrue(warning.contains("org.flowable.engine.delegate.JavaDelegate"), warning);
    }

    @Test
    void the_warning_does_not_fail_the_bean_it_is_about() {
        // It is a warning and not a wiring error on purpose: the bean is built and usable, so the
        // artefact must not go FAILED - only the Problems entry appears.
        ComponentContainer container = TestComponentContainers.of(RateProvider.class, ComponentDelegate.class);

        assertTrue(container.instanceOf(ComponentDelegate.class)
                            .isPresent());
        assertTrue(container.wiringErrors()
                            .isEmpty());
    }

    @Test
    void a_delegate_inherited_through_a_super_interface_is_still_found() {
        ComponentContainer container = TestComponentContainers.of(IndirectComponentDelegate.class);

        assertTrue(container.wiringWarnings()
                            .containsKey(IndirectComponentDelegate.class.getName()));
    }

    @Test
    void a_bean_that_is_not_a_delegate_is_not_warned_about() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class);

        assertTrue(container.wiringWarnings()
                            .isEmpty());
    }

    @Test
    void a_removed_annotation_clears_the_warning_of_the_previous_generation() {
        ComponentContainer container = new ComponentContainer(new ClientBeansHolder());
        container.rebuild(generation(RateProvider.class, ComponentDelegate.class));

        container.rebuild(generation(RateProvider.class));

        assertTrue(container.wiringWarnings()
                            .isEmpty());
    }

    @Test
    void the_execution_time_warning_is_logged_once_per_class_per_generation() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class, ComponentDelegate.class);
        appender.list.clear(); // the rebuild's own publish-time WARN is asserted separately

        container.createUnmanaged(ComponentDelegate.class);
        container.createUnmanaged(ComponentDelegate.class);
        container.createUnmanaged(ComponentDelegate.class);

        // On the ${JavaTask} path this runs for every execution of the step, forever.
        assertEquals(1, ruleLines(Level.WARN).size(), () -> "expected exactly one WARN, got: " + appender.list);
        assertEquals(2, ruleLines(Level.DEBUG).size(), () -> "expected the repeats at DEBUG, got: " + appender.list);
    }

    @Test
    void the_execution_time_warning_names_the_class_and_the_rule() {
        ComponentContainer container = TestComponentContainers.of(RateProvider.class, ComponentDelegate.class);
        appender.list.clear();

        container.createUnmanaged(ComponentDelegate.class);

        String message = ruleLines(Level.WARN).get(0);
        assertTrue(message.contains(ComponentDelegate.class.getName()), message);
        assertTrue(message.contains("must NOT be a @Component"), message);
        // It checked @Component on a class being wired unmanaged, not the JavaDelegate interface, so
        // it must not claim the class is a delegate.
        assertFalse(message.contains("is a JavaDelegate annotated"), message);
    }

    @Test
    void a_republish_says_it_again_because_the_developer_just_changed_the_class() {
        ComponentContainer container = new ComponentContainer(new ClientBeansHolder());
        container.rebuild(generation(RateProvider.class, ComponentDelegate.class));
        container.createUnmanaged(ComponentDelegate.class);

        container.rebuild(generation(RateProvider.class, ComponentDelegate.class));
        appender.list.clear();
        container.createUnmanaged(ComponentDelegate.class);

        assertEquals(1, ruleLines(Level.WARN).size(), () -> "expected the new generation to warn again, got: " + appender.list);
    }

    /**
     * The captured messages of {@code level} that state the rule (not e.g. the rebuilt-container INFO).
     */
    private List<String> ruleLines(Level level) {
        return appender.list.stream()
                            .filter(event -> event.getLevel() == level)
                            .map(ILoggingEvent::getFormattedMessage)
                            .filter(message -> message.contains("must NOT be a @Component"))
                            .toList();
    }

    private static List<LoadedClass> generation(Class<?>... classes) {
        return Arrays.stream(classes)
                     .map(type -> new LoadedClass("p", type.getName(), type, type.getClassLoader()))
                     .toList();
    }

    // --- fixtures --------------------------------------------------------------------------------

    @Component
    static class RateProvider {
    }

    /** The mistake the rule forbids: a delegate annotated {@code @Component}. */
    @Component
    static class ComponentDelegate implements org.flowable.engine.delegate.JavaDelegate {

        @Inject
        RateProvider rates;
    }

    interface AuditedDelegate extends org.flowable.engine.delegate.JavaDelegate {
    }

    /** Same mistake, one interface further away — the search has to walk the hierarchy. */
    @Component
    static class IndirectComponentDelegate implements AuditedDelegate {
    }
}
