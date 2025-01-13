/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scripting context facade is the centralized place where the different scripting facade providers
 * can register the request (sync) scoped objects.
 */
public class ThreadContextFacade {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ThreadContextFacade.class);

    /** The Constant STACKED_CONTEXT. */
    private static final ThreadLocal<Map<Integer, Map<String, Object>>> STACKED_CONTEXT = new ThreadLocal<>();

    /** The Constant STACKED_CLOSEABLES. */
    private static final ThreadLocal<Map<Integer, Map<String, AutoCloseable>>> STACKED_CLOSEABLES = new ThreadLocal<>();

    /** The Constant UUID_GENERATOR. */
    private static final AtomicLong UUID_GENERATOR = new AtomicLong(Long.MIN_VALUE);

    /** The Constant STACK_ID. */
    private static final ThreadLocal<Integer> STACK_ID = new ThreadLocal<>();

    /**
     * Initializes the context. This has to be called at the very first (as possible) place at the
     * service entry point
     */
    public static void setUp() {
        if (stackedContextIsEmpty()) {
            STACKED_CONTEXT.set(new HashMap<>());
        }
        if (stackedCloseablesIsEmpty()) {
            STACKED_CLOSEABLES.set(new HashMap<>());
        }

        Integer currentStackId = STACK_ID.get();
        if (currentStackId == null) {
            STACK_ID.set(0);
        } else {
            STACK_ID.set(currentStackId + 1);

        }

        STACKED_CONTEXT.get()
                       .put(STACK_ID.get(), collectParentObjects());
        STACKED_CLOSEABLES.get()
                          .put(STACK_ID.get(), new HashMap<>());

        if (logger.isTraceEnabled()) {
            logger.trace("Scripting context {} has been set up", Thread.currentThread()
                                                                       .hashCode());
        }
    }

    /**
     * Collect parent objects.
     *
     * @return the map
     */
    private static Map<String, Object> collectParentObjects() {
        Map<String, Object> allParentObjects = new HashMap<>();

        for (int parentStackId = STACK_ID.get() - 1; parentStackId >= 0; parentStackId -= 1) {
            Map<String, Object> parentStackObjects = STACKED_CONTEXT.get()
                                                                    .get(parentStackId);
            allParentObjects.putAll(parentStackObjects);
        }

        return allParentObjects;
    }

    /**
     * IMPORTANT! This have to be added at the finally block to clean up objects after the execution of
     * the service.
     */
    public static void tearDown() {
        Integer stackId = STACK_ID.get();
        if (stackId == null) {
            return;
        }

        if (stackedContextIsNotEmpty()) {
            STACKED_CONTEXT.get()
                           .get(stackId)
                           .clear();
        }

        if (stackedCloseablesIsNotEmpty()) {
            Map<String, AutoCloseable> CLOSEABLES = STACKED_CLOSEABLES.get()
                                                                      .get(stackId);
            for (Entry<String, AutoCloseable> closeable : CLOSEABLES.entrySet()) {
                try {
                    if (logger.isErrorEnabled()) {
                        logger.error("Object of type {} from the context {} has not been closed properly.", closeable.getValue()
                                                                                                                     .getClass()
                                                                                                                     .getCanonicalName(),
                                Thread.currentThread()
                                      .hashCode());
                    }
                    closeable.getValue()
                             .close();
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            STACKED_CLOSEABLES.get()
                              .get(stackId)
                              .clear();
        }

        if (stackId == 0) {
            STACKED_CONTEXT.remove();
            STACKED_CLOSEABLES.remove();
        }
        STACK_ID.set(stackId - 1);

        if (logger.isTraceEnabled()) {
            logger.trace("Scripting context {} has been torn down", Thread.currentThread()
                                                                          .hashCode());
        }
    }

    /**
     * Get a context scripting object.
     *
     * @param key the key
     * @return the value by this key
     * @throws ContextException in case of an error
     */
    public static Object get(String key) throws ContextException {
        checkContext();
        Map<Integer, Map<String, Object>> stackedContext = STACKED_CONTEXT.get();
        Map<String, Object> currentStack = stackedContext.get(STACK_ID.get());
        return currentStack.get(key);
    }

    /**
     * Set a context scripting object.
     *
     * @param value the value
     * @return the UUID of the object
     * @throws ContextException in case of an error
     */
    public static String set(Object value) throws ContextException {
        final String uuid = generateObjectId();
        set(uuid, value);
        return uuid;
    }

    /**
     * Set a context scripting object. If object with with this key exists, it will be replaced with the
     * new object
     *
     * @param key the key
     * @param value the value
     * @throws ContextException in case of an error
     */
    public static void set(String key, Object value) throws ContextException {
        checkContext();
        STACKED_CONTEXT.get()
                       .get(STACK_ID.get())
                       .put(key, value);
        if (logger.isTraceEnabled()) {
            logger.trace("Context object has been added to {} with key {}", Thread.currentThread()
                                                                                  .hashCode(),
                    key);
        }
    }

    /**
     * Remove a context scripting object.
     *
     * @param key the key
     * @throws ContextException in case of an error
     */
    public static void remove(String key) throws ContextException {
        checkContext();
        STACKED_CONTEXT.get()
                       .get(STACK_ID.get())
                       .remove(key);
        if (logger.isTraceEnabled()) {
            logger.trace("Context object has been removed - key {}", key);
        }
    }


    /**
     * Check context.
     *
     * @throws ContextException the context exception
     */
    private static void checkContext() throws ContextException {
        if (STACKED_CONTEXT.get() == null) {
            throw new ContextException("Context has not been initialized");
        }
    }

    /**
     * Check whether the facade is valid.
     *
     * @return yes, if it is valid
     */
    public static boolean isValid() {
        return (STACKED_CONTEXT.get() != null);
    }

    /**
     * Generate object id.
     *
     * @return the string
     */
    private static String generateObjectId() {
        return Long.toString(UUID_GENERATOR.incrementAndGet(), Character.MAX_RADIX);
    }

    /**
     * Add a closeable object to the map.
     *
     * @param closeable the closeable object
     */
    public static void addCloseable(AutoCloseable closeable) {
        if (STACKED_CLOSEABLES.get() != null) {
            STACKED_CLOSEABLES.get()
                              .get(STACK_ID.get())
                              .put(STACK_ID.get() + "_" + closeable.hashCode(), closeable);
            if (logger.isTraceEnabled()) {
                logger.trace("Closeable object has been added to {} with hash {}", Thread.currentThread()
                                                                                         .hashCode(),
                        closeable.hashCode());
            }
        }
    }

    /**
     * Remove a closeable object.
     *
     * @param closeable the closeable object
     */
    public static void removeCloseable(AutoCloseable closeable) {
        if (STACKED_CLOSEABLES.get() != null) {
            STACKED_CLOSEABLES.get()
                              .get(STACK_ID.get())
                              .remove(STACK_ID.get() + "_" + closeable.hashCode());
            if (logger.isTraceEnabled()) {
                logger.trace("Closeable object has been removed - hash {}", closeable.hashCode());
            }
        }
    }

    /**
     * Stacked context is empty.
     *
     * @return true, if successful
     */
    private static boolean stackedContextIsEmpty() {
        Map<Integer, Map<String, Object>> stackedContext = STACKED_CONTEXT.get();
        return stackedContext == null || stackedContext.isEmpty();
    }

    /**
     * Stacked context is not empty.
     *
     * @return true, if successful
     */
    private static boolean stackedContextIsNotEmpty() {
        Map<Integer, Map<String, Object>> stackedContext = STACKED_CONTEXT.get();
        return stackedContext != null && !stackedContext.isEmpty();
    }

    /**
     * Stacked closeables is empty.
     *
     * @return true, if successful
     */
    private static boolean stackedCloseablesIsEmpty() {
        Map<Integer, Map<String, AutoCloseable>> stackedCloseables = STACKED_CLOSEABLES.get();
        return stackedCloseables == null || stackedCloseables.isEmpty();
    }

    /**
     * Stacked closeables is not empty.
     *
     * @return true, if successful
     */
    private static boolean stackedCloseablesIsNotEmpty() {
        Map<Integer, Map<String, AutoCloseable>> stackedCloseables = STACKED_CLOSEABLES.get();
        return stackedCloseables != null && !stackedCloseables.isEmpty();
    }
}
