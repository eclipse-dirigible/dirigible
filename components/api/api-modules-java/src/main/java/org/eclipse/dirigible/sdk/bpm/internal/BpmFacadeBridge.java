/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.bpm.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Reflective bridge to {@code org.eclipse.dirigible.components.api.bpm.BpmFacade}. The SDK module
 * intentionally does <em>not</em> declare a compile-time dependency on {@code api-bpm} — that link
 * would close the cycle {@code engine-java → api-modules-java → api-bpm → engine-bpm-flowable →
 * engine-java} the moment {@code engine-java} starts using the SDK annotations published here.
 * <p>
 * At runtime {@code api-bpm} is always present (it ships with the platform via {@code group-api}),
 * so the reflective lookups succeed; the only observable difference is that any breakage shows up
 * the first time a caller hits a method, rather than at compile time.
 * <p>
 * Lookups are cached per ({@code methodName}, parameter-type-tuple) pair — annotations on the
 * underlying facade are stable for a JVM lifetime, so a {@link ConcurrentHashMap} suffices.
 */
public final class BpmFacadeBridge {

    private static final String FACADE_FQN = "org.eclipse.dirigible.components.api.bpm.BpmFacade";

    private static final ConcurrentMap<String, Method> METHODS = new ConcurrentHashMap<>();

    private static final Class<?> FACADE = loadFacade();

    private BpmFacadeBridge() {}

    private static Class<?> loadFacade() {
        try {
            return Class.forName(FACADE_FQN);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("BpmFacade not found on classpath — api-bpm must be present at runtime", ex);
        }
    }

    /**
     * Invokes a static method on {@code BpmFacade} and casts the result to the caller-supplied type.
     * Argument types are inferred from the runtime classes of {@code args}, with {@code null} treated
     * as {@link Object}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = METHODS.computeIfAbsent(cacheKey(methodName, parameterTypes), key -> resolve(methodName, parameterTypes));
            return (T) method.invoke(null, args);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new IllegalStateException("BpmFacade." + methodName + " failed", cause);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot access BpmFacade." + methodName, ex);
        }
    }

    private static Method resolve(String methodName, Class<?>[] parameterTypes) {
        try {
            return FACADE.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("BpmFacade." + methodName + parameterTypeNames(parameterTypes) + " not found", ex);
        }
    }

    private static String cacheKey(String methodName, Class<?>[] parameterTypes) {
        StringBuilder sb = new StringBuilder(methodName);
        sb.append('(');
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(parameterTypes[i].getName());
        }
        sb.append(')');
        return sb.toString();
    }

    private static String parameterTypeNames(Class<?>[] parameterTypes) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameterTypes[i].getSimpleName());
        }
        return sb.append(')')
                 .toString();
    }
}
