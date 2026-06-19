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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.sdk.component.Inject;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Immutable, reflectively-derived recipe for a single client bean: its name, the constructor the
 * container should call, the {@link Inject @Inject} fields to set after construction, and any
 * {@code @PostConstruct} / {@code @PreDestroy} lifecycle methods. All reflection is resolved once,
 * when the definition is built, and cached for the life of the generation.
 */
final class BeanDefinition {

    private final String name;
    private final Class<?> type;
    private final Constructor<?> constructor;
    private final List<Field> injectFields;
    private final List<Method> postConstructMethods;
    private final List<Method> preDestroyMethods;

    BeanDefinition(String name, Class<?> type) {
        this.name = name;
        this.type = type;
        this.constructor = selectConstructor(type);
        this.constructor.setAccessible(true);
        this.injectFields = collectInjectFields(type);
        this.postConstructMethods = collectLifecycleMethods(type, PostConstruct.class);
        this.preDestroyMethods = collectLifecycleMethods(type, PreDestroy.class);
    }

    String name() {
        return name;
    }

    Class<?> type() {
        return type;
    }

    Constructor<?> constructor() {
        return constructor;
    }

    List<Field> injectFields() {
        return injectFields;
    }

    List<Method> postConstructMethods() {
        return postConstructMethods;
    }

    List<Method> preDestroyMethods() {
        return preDestroyMethods;
    }

    /**
     * Pick the constructor to wire: the sole constructor if there is one, otherwise the single
     * {@code @Inject} constructor, otherwise a no-arg constructor. Anything else is a configuration
     * error the developer must resolve.
     */
    private static Constructor<?> selectConstructor(Class<?> type) {
        Constructor<?>[] ctors = type.getDeclaredConstructors();
        if (ctors.length == 1) {
            return ctors[0];
        }
        List<Constructor<?>> annotated = new ArrayList<>();
        Constructor<?> noArg = null;
        for (Constructor<?> ctor : ctors) {
            if (ctor.isAnnotationPresent(Inject.class)) {
                annotated.add(ctor);
            }
            if (ctor.getParameterCount() == 0) {
                noArg = ctor;
            }
        }
        if (annotated.size() == 1) {
            return annotated.get(0);
        }
        if (annotated.size() > 1) {
            throw new BeanContainerException("Bean [" + type.getName() + "] declares multiple @Inject constructors; annotate exactly one.");
        }
        if (noArg != null) {
            return noArg;
        }
        throw new BeanContainerException(
                "Bean [" + type.getName() + "] must have a single constructor, one @Inject constructor, or a public no-arg constructor.");
    }

    private static List<Field> collectInjectFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> walk = type;
        while (walk != null && walk != Object.class) {
            for (Field field : walk.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            walk = walk.getSuperclass();
        }
        return fields;
    }

    private static List<Method> collectLifecycleMethods(Class<?> type, Class<? extends Annotation> annotation) {
        List<Method> methods = new ArrayList<>();
        Class<?> walk = type;
        while (walk != null && walk != Object.class) {
            for (Method method : walk.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation) && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    methods.add(method);
                }
            }
            walk = walk.getSuperclass();
        }
        return methods;
    }
}
