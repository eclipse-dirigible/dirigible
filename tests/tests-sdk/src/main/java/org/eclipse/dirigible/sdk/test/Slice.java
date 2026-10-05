/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.eclipse.dirigible.components.data.store.java.repository.JavaRepository;
import org.eclipse.dirigible.sdk.db.ValidationException;
import org.junit.jupiter.api.function.Executable;

/**
 * A test's handle on the application running in an {@link IntentSlice}: its components, its
 * entities' repositories, and what it sent. Declare it as a parameter of a test method.
 */
public final class Slice {

    private final SliceRuntime runtime;

    Slice(SliceRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * A component of the application - a generated repository or controller, a hand-written action,
     * delegate or service - exactly as the platform's bean container wires it.
     *
     * @param <T> the component type
     * @param type the component's class, or an interface it implements
     * @return the component
     * @throws IllegalArgumentException if the application has no such component
     */
    public <T> T bean(Class<T> type) {
        return runtime.container()
                      .get(type)
                      .orElseThrow(() -> new IllegalArgumentException("The application has no component of type " + type.getName()));
    }

    /**
     * The generated repository of an entity.
     *
     * @param <E> the entity type
     * @param entityClass the generated entity class
     * @return its repository
     * @throws IllegalArgumentException if the application has no repository for it
     */
    @SuppressWarnings("unchecked")
    public <E> JavaRepository<E> repository(Class<E> entityClass) {
        return runtime.container()
                      .getAll(JavaRepository.class)
                      .stream()
                      .filter(repository -> repository.getEntityClass() == entityClass)
                      .findFirst()
                      .map(repository -> (JavaRepository<E>) repository)
                      .orElseThrow(() -> new IllegalArgumentException("The application has no repository of " + entityClass.getName()));
    }

    /**
     * Starts a record of an entity, to be saved through its repository.
     *
     * @param <E> the entity type
     * @param entityClass the generated entity class
     * @return the record, every property unset
     */
    public <E> Given<E> given(Class<E> entityClass) {
        return new Given<>(repository(entityClass));
    }

    /**
     * Every message the application sent in this test so far - the events its repositories published
     * and the messages its own code sent - in order.
     *
     * @return the messages
     */
    public List<SentMessage> sent() {
        return runtime.sent();
    }

    /**
     * The payloads sent to one topic or queue in this test so far, in order.
     *
     * @param destination the topic or queue
     * @return the payloads
     */
    public List<String> sentTo(String destination) {
        return runtime.sent()
                      .stream()
                      .filter(message -> message.destination()
                                                .equals(destination))
                      .map(SentMessage::payload)
                      .toList();
    }

    /**
     * Asserts that a write is refused by a validation or a declared check, and returns the refusal -
     * whose message is what the caller of the REST API would read.
     *
     * @param write the write expected to be refused
     * @return the refusal
     */
    public static ValidationException expectRefused(Executable write) {
        return assertThrows(ValidationException.class, write, "Expected the write to be refused");
    }
}
