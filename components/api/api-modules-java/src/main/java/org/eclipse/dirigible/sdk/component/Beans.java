/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.engine.java.runtime.ClientBeanResolver;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;

/**
 * Programmatic access to beans from client Java code. Prefer constructor or {@link Inject @Inject}
 * injection — reach for {@code Beans} only when a dependency can't be expressed as an injection
 * point (a static context, a factory, conditional lookup).
 *
 * <p>
 * Resolution checks the client bean container first (your {@link Component @Component} /
 * {@code @Repository} / {@code @Controller} beans), then falls back to the platform's Spring beans
 * (the SDK services). This is the client-facing counterpart to the platform-internal
 * {@code org.eclipse.dirigible.components.base.spring.BeanProvider}, which client code should not
 * use directly.
 *
 * <p>
 * Example:
 *
 * <pre>
 * GreetingService greetings = Beans.get(GreetingService.class);
 * List&lt;OrderProcessor&gt; processors = Beans.getAll(OrderProcessor.class);
 * </pre>
 */
public final class Beans {

    private Beans() {}

    /**
     * Resolve a single bean assignable to {@code type} — client beans first, then platform beans.
     *
     * @param <T> the bean type
     * @param type the required type
     * @return the resolved bean
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if no bean matches
     */
    public static <T> T get(Class<T> type) {
        ClientBeanResolver resolver = clientResolver();
        if (resolver != null) {
            Optional<T> bean = resolver.get(type);
            if (bean.isPresent()) {
                return bean.get();
            }
        }
        return BeanProvider.getBean(type);
    }

    /**
     * Resolve a bean by name, assignable to {@code type} — client beans first, then platform beans.
     *
     * @param <T> the bean type
     * @param name the bean name
     * @param type the required type
     * @return the resolved bean
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if no bean matches
     */
    public static <T> T get(String name, Class<T> type) {
        ClientBeanResolver resolver = clientResolver();
        if (resolver != null) {
            Optional<T> bean = resolver.get(name, type);
            if (bean.isPresent()) {
                return bean.get();
            }
        }
        return BeanProvider.getBean(name, type);
    }

    /**
     * Resolve every bean assignable to {@code type} — client beans followed by platform beans. Useful
     * for plugin-style fan-out over all implementations of an interface.
     *
     * @param <T> the bean type
     * @param type the required type (typically an interface)
     * @return all matching beans; empty if none
     */
    public static <T> List<T> getAll(Class<T> type) {
        List<T> result = new ArrayList<>();
        ClientBeanResolver resolver = clientResolver();
        if (resolver != null) {
            result.addAll(resolver.getAll(type));
        }
        result.addAll(BeanProvider.getBeans(type));
        return result;
    }

    private static ClientBeanResolver clientResolver() {
        if (!BeanProvider.isInitialzed()) {
            return null;
        }
        return BeanProvider.getBean(ClientBeansHolder.class)
                           .current();
    }
}
