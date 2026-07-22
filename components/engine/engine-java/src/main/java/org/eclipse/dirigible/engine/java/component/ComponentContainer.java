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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.engine.java.runtime.ClientBeanResolver;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.component.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * The IoC container for client beans. One instance lives for the life of the platform but rebuilds
 * its bean set on every {@code ClientClassLoader} generation: it discovers every
 * {@link Component @Component}-(meta-)annotated class, derives a {@link BeanDefinition}, and
 * eagerly instantiates singletons with recursive <em>constructor injection</em> (plus
 * {@code @Inject} field injection and {@code @PostConstruct} callbacks), detecting construction
 * cycles. The behaviour consumers ({@code @Controller}, {@code @Scheduled}, {@code @Listener},
 * {@code @Websocket}, {@code @Extension}) then fetch the ready instances via
 * {@link #instanceOf(Class)} rather than instantiating client classes themselves.
 *
 * <p>
 * Implements {@link ClientBeanResolver} and publishes itself into {@link ClientBeansHolder} so the
 * SDK facade {@code org.eclipse.dirigible.sdk.component.Beans} can resolve client beans.
 *
 * <p>
 * Threading: {@link #rebuild(Collection)} runs on the single synchronization thread and publishes
 * an immutable singleton snapshot through a {@code volatile} field; lookups happen lock-free on
 * HTTP dispatch threads.
 */
@org.springframework.stereotype.Component
public class ComponentContainer implements ClientBeanResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentContainer.class);

    /** Definitions of the live generation, in registration order. */
    private volatile List<BeanDefinition> definitions = List.of();

    /** name → singleton for the live generation (immutable snapshot, registration order). */
    private volatile Map<String, Object> singletons = Map.of();

    /** runtime class → singleton, for O(1) {@link #instanceOf(Class)} during the load pass. */
    private volatile Map<Class<?>, Object> instancesByType = Map.of();

    /** client class FQN → wiring error from the last rebuild (so the synchronizer can surface it). */
    private volatile Map<String, String> wiringErrors = Map.of();

    public ComponentContainer(ClientBeansHolder holder) {
        holder.swap(this);
    }

    /**
     * Wiring errors from the last {@link #rebuild(Collection)} keyed by client class FQN — an
     * unsatisfied/ambiguous dependency, a construction cycle, a duplicate bean name or a throwing
     * constructor. The synchronizer projects these onto the IDE Problems view so a developer sees the
     * failure on the offending source file, not only in the server log.
     *
     * @return an immutable FQN → message map (empty if the last rebuild had no wiring errors)
     */
    public Map<String, String> wiringErrors() {
        return wiringErrors;
    }

    /**
     * Re-create the whole client bean set for a new generation. Builds and instantiates the new beans
     * first, publishes them atomically, then tears down the previous generation (so reads transition
     * cleanly old → new). Per-bean failures are logged and skipped — one bad bean never aborts the
     * rebuild.
     *
     * @param loaded every loaded client class of the new generation (beans are filtered out internally)
     */
    public synchronized void rebuild(Collection<LoadedClass> loaded) {
        List<BeanDefinition> previousDefinitions = definitions;
        Map<String, Object> previousSingletons = singletons;

        Map<String, BeanDefinition> byName = new LinkedHashMap<>();
        List<BeanDefinition> ordered = new ArrayList<>();
        Map<String, String> errors = new LinkedHashMap<>();
        ClassLoader loader = null;
        for (LoadedClass info : loaded) {
            if (info == null) {
                continue;
            }
            Class<?> type = info.type();
            if (!isBean(type)) {
                continue;
            }
            loader = info.loader();
            try {
                String name = beanName(type);
                BeanDefinition existing = byName.get(name);
                if (existing != null) {
                    String message = "Duplicate client bean name [" + name + "] also used by [" + existing.type()
                                                                                                          .getName()
                            + "]; keeping the first. Use @Component(\"...\") to disambiguate.";
                    LOGGER.error(message);
                    errors.put(type.getName(), message);
                    continue;
                }
                BeanDefinition definition = new BeanDefinition(name, type);
                byName.put(name, definition);
                ordered.add(definition);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to define client bean [{}]: {}", type.getName(), e.getMessage(), e);
                errors.put(type.getName(), e.getMessage());
            }
        }

        Map<String, Object> created = new LinkedHashMap<>();
        ClassLoader previousTccl = Thread.currentThread()
                                         .getContextClassLoader();
        if (loader != null) {
            Thread.currentThread()
                  .setContextClassLoader(loader);
        }
        try {
            for (BeanDefinition definition : ordered) {
                try {
                    getOrCreate(definition, byName, ordered, created, new ArrayDeque<>());
                } catch (RuntimeException e) {
                    LOGGER.error("Failed to instantiate client bean [{}] ([{}]): {}", definition.name(), definition.type()
                                                                                                                   .getName(),
                            e.getMessage(), e);
                    errors.put(definition.type()
                                         .getName(),
                            e.getMessage());
                }
            }
        } finally {
            Thread.currentThread()
                  .setContextClassLoader(previousTccl);
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        Map<Class<?>, Object> byType = new LinkedHashMap<>();
        for (BeanDefinition definition : ordered) {
            Object instance = created.get(definition.name());
            if (instance != null) {
                snapshot.put(definition.name(), instance);
                byType.put(instance.getClass(), instance);
            }
        }
        this.definitions = List.copyOf(ordered);
        this.singletons = java.util.Collections.unmodifiableMap(snapshot);
        this.instancesByType = java.util.Collections.unmodifiableMap(byType);
        this.wiringErrors = Map.copyOf(errors);

        destroy(previousDefinitions, previousSingletons);
        LOGGER.info("Client bean container rebuilt: {} bean(s).", snapshot.size());
    }

    private Object getOrCreate(BeanDefinition definition, Map<String, BeanDefinition> byName, List<BeanDefinition> ordered,
            Map<String, Object> created, Deque<String> inCreation) {
        Object existing = created.get(definition.name());
        if (existing != null) {
            return existing;
        }
        if (inCreation.contains(definition.name())) {
            throw new BeanContainerException("Constructor injection cycle detected: " + String.join(" -> ", inCreation) + " -> "
                    + definition.name() + ". Break the cycle (e.g. inject a collaborator lazily via Beans.get).");
        }
        inCreation.addLast(definition.name());
        try {
            Constructor<?> constructor = definition.constructor();
            Parameter[] parameters = constructor.getParameters();
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                args[i] = resolve(parameters[i].getType(), parameters[i].getParameterizedType(), parameterName(parameters[i]),
                        definition.type(), byName, ordered, created, inCreation);
            }
            Object instance;
            try {
                instance = constructor.newInstance(args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new BeanContainerException("Constructor of [" + definition.type()
                                                                                .getName()
                        + "] threw: " + cause.getMessage(), cause);
            } catch (ReflectiveOperationException e) {
                throw new BeanContainerException("Cannot instantiate [" + definition.type()
                                                                                    .getName()
                        + "]: " + e.getMessage(), e);
            }
            created.put(definition.name(), instance);
            injectFields(definition, instance, byName, ordered, created, inCreation);
            invokePostConstruct(definition, instance);
            return instance;
        } finally {
            inCreation.removeLast();
        }
    }

    private void injectFields(BeanDefinition definition, Object instance, Map<String, BeanDefinition> byName, List<BeanDefinition> ordered,
            Map<String, Object> created, Deque<String> inCreation) {
        for (Field field : definition.injectFields()) {
            Object value = resolve(field.getType(), field.getGenericType(), field.getName(), definition.type(), byName, ordered, created,
                    inCreation);
            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new BeanContainerException("Cannot set @Inject field [" + definition.type()
                                                                                          .getName()
                        + "." + field.getName() + "]: " + e.getMessage(), e);
            }
        }
    }

    /** Resolve one injection point — a collection of all matches, or the single matching bean. */
    private Object resolve(Class<?> rawType, Type genericType, String nameHint, Class<?> owner, Map<String, BeanDefinition> byName,
            List<BeanDefinition> ordered, Map<String, Object> created, Deque<String> inCreation) {
        if (Collection.class.isAssignableFrom(rawType)) {
            Class<?> element = elementType(genericType);
            List<Object> values = new ArrayList<>();
            for (BeanDefinition candidate : ordered) {
                if (element.isAssignableFrom(candidate.type())) {
                    values.add(getOrCreate(candidate, byName, ordered, created, inCreation));
                }
            }
            return Set.class.isAssignableFrom(rawType) ? new LinkedHashSet<>(values) : values;
        }
        List<BeanDefinition> candidates = new ArrayList<>();
        for (BeanDefinition candidate : ordered) {
            if (rawType.isAssignableFrom(candidate.type())) {
                candidates.add(candidate);
            }
        }
        if (candidates.size() == 1) {
            return getOrCreate(candidates.get(0), byName, ordered, created, inCreation);
        }
        if (candidates.isEmpty()) {
            throw new BeanContainerException("No client bean of type [" + rawType.getName() + "] to inject into [" + owner.getName()
                    + "]. Declare it as @Component, or use Beans.get(...) for a platform service.");
        }
        if (nameHint != null) {
            BeanDefinition named = byName.get(nameHint);
            if (named != null && rawType.isAssignableFrom(named.type())) {
                return getOrCreate(named, byName, ordered, created, inCreation);
            }
        }
        List<String> names = candidates.stream()
                                       .map(BeanDefinition::name)
                                       .toList();
        throw new BeanContainerException("Ambiguous dependency of type [" + rawType.getName() + "] for [" + owner.getName()
                + "]: candidates " + names + ". Use a more specific type or match the parameter/field name to a bean name.");
    }

    private void invokePostConstruct(BeanDefinition definition, Object instance) {
        for (Method method : definition.postConstructMethods()) {
            try {
                method.invoke(instance);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new BeanContainerException("@PostConstruct [" + definition.type()
                                                                                .getName()
                        + "." + method.getName() + "] threw: " + cause.getMessage(), cause);
            } catch (IllegalAccessException e) {
                throw new BeanContainerException("Cannot invoke @PostConstruct [" + definition.type()
                                                                                              .getName()
                        + "." + method.getName() + "]: " + e.getMessage(), e);
            }
        }
    }

    private static void destroy(List<BeanDefinition> previousDefinitions, Map<String, Object> previousSingletons) {
        for (int i = previousDefinitions.size() - 1; i >= 0; i--) {
            BeanDefinition definition = previousDefinitions.get(i);
            Object instance = previousSingletons.get(definition.name());
            if (instance == null) {
                continue;
            }
            for (Method method : definition.preDestroyMethods()) {
                try {
                    method.invoke(instance);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    LOGGER.error("@PreDestroy [{}.{}] threw: {}", definition.type()
                                                                            .getName(),
                            method.getName(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * The single bean instance whose runtime class is exactly {@code type} — used by the behaviour
     * consumers to fetch the bean the container already built for a loaded class.
     *
     * @param type the concrete client class
     * @return the bean, or empty if it is not a bean or failed to instantiate
     */
    public Optional<Object> instanceOf(Class<?> type) {
        return Optional.ofNullable(instancesByType.get(type));
    }

    /**
     * Resolve the current singleton by its class's fully-qualified name - the runtime lookup a
     * scheduled Java job needs, which only carries the handler FQN (not the {@link Class}, which may
     * have been reloaded since registration).
     *
     * @param fqn the fully-qualified class name
     * @return the current instance, or empty if no such bean is loaded
     */
    public Optional<Object> instanceOfClassName(String fqn) {
        for (Map.Entry<Class<?>, Object> entry : instancesByType.entrySet()) {
            if (entry.getKey()
                     .getName()
                     .equals(fqn)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> get(Class<T> type) {
        List<T> all = getAll(type);
        return all.size() == 1 ? Optional.of(all.get(0)) : Optional.empty();
    }

    @Override
    public <T> Optional<T> get(String name, Class<T> type) {
        Object instance = singletons.get(name);
        if (instance != null && type.isInstance(instance)) {
            return Optional.of(type.cast(instance));
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Object instance : singletons.values()) {
            if (type.isInstance(instance)) {
                result.add(type.cast(instance));
            }
        }
        return result;
    }

    private static boolean isBean(Class<?> type) {
        return AnnotatedElementUtils.hasAnnotation(type, Component.class);
    }

    private static String beanName(Class<?> type) {
        Component component = AnnotatedElementUtils.findMergedAnnotation(type, Component.class);
        if (component != null && !component.value()
                                           .isEmpty()) {
            return component.value();
        }
        return java.beans.Introspector.decapitalize(type.getSimpleName());
    }

    private static String parameterName(Parameter parameter) {
        return parameter.isNamePresent() ? parameter.getName() : null;
    }

    private static Class<?> elementType(Type genericType) {
        if (genericType instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if (arguments.length == 1) {
                Type argument = arguments[0];
                if (argument instanceof Class<?> clazz) {
                    return clazz;
                }
                if (argument instanceof WildcardType wildcard && wildcard.getUpperBounds().length > 0
                        && wildcard.getUpperBounds()[0] instanceof Class<?> bound) {
                    return bound;
                }
            }
        }
        return Object.class;
    }
}
