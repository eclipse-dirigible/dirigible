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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.data.store.java.repository.JavaRepository;

/**
 * A record of one entity being put together for a test:
 * {@code slice.given(InvoiceLineEntity.class).with("Quantity", 1).with("UnitPrice", "2.90").saved()}.
 * Properties are the generated entity's own field names. A value is converted to the field's type
 * when it is a number or text naming one - {@code "2.90"} for a {@code BigDecimal},
 * {@code "2026-01-31"} for a {@code LocalDate} - so a money value can be written exactly, without a
 * {@code double} on the way.
 *
 * @param <E> the entity type
 */
public final class Given<E> {

    private final JavaRepository<E> repository;
    private final E entity;

    Given(JavaRepository<E> repository) {
        this.repository = repository;
        this.entity = instantiate(repository.getEntityClass());
    }

    /**
     * Sets one property of the record.
     *
     * @param property the entity field, e.g. {@code "UnitPrice"}
     * @param value the value, or a number or text convertible to the field's type
     * @return this
     * @throws IllegalArgumentException if the entity has no such field, or the value does not fit it
     */
    public Given<E> with(String property, Object value) {
        Field field = field(property);
        try {
            field.set(entity, convert(value, field));
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException("Cannot set [" + property + "] of " + entity.getClass()
                                                                                           .getName(),
                    ex);
        }
        return this;
    }

    /**
     * Saves the record through its generated repository - its calculated fields, validations, checks
     * and events applying exactly as on a REST create.
     *
     * @return the saved record, its generated key populated
     */
    public E saved() {
        return repository.save(entity);
    }

    /**
     * The record as put together so far, not saved - for a call to a hand-written action directly.
     *
     * @return the unsaved record
     */
    public E entity() {
        return entity;
    }

    private Field field(String property) {
        Class<E> type = repository.getEntityClass();
        try {
            Field field = type.getField(property);
            if (!Modifier.isStatic(field.getModifiers())) {
                return field;
            }
        } catch (NoSuchFieldException ex) {
            // reported below, with the fields the entity does have
        }
        String properties = Arrays.stream(type.getFields())
                                  .filter(candidate -> !Modifier.isStatic(candidate.getModifiers()))
                                  .map(Field::getName)
                                  .collect(Collectors.joining(", "));
        throw new IllegalArgumentException(type.getSimpleName() + " has no property [" + property + "]; it has: " + properties);
    }

    private static Object convert(Object value, Field field) {
        Class<?> type = field.getType();
        if (value == null || type.isInstance(value)) {
            return value;
        }
        String text = value.toString();
        try {
            if (value instanceof Number || value instanceof CharSequence) {
                if (type == BigDecimal.class) {
                    return new BigDecimal(text);
                }
                if (type == Integer.class) {
                    return Integer.valueOf(text);
                }
                if (type == Long.class) {
                    return Long.valueOf(text);
                }
                if (type == Double.class) {
                    return Double.valueOf(text);
                }
            }
            if (value instanceof CharSequence) {
                if (type == LocalDate.class) {
                    return LocalDate.parse(text);
                }
                if (type == LocalDateTime.class) {
                    return LocalDateTime.parse(text);
                }
                if (type == Instant.class) {
                    return Instant.parse(text);
                }
                if (type == Boolean.class) {
                    return Boolean.valueOf(text);
                }
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("[" + text + "] is not a " + type.getSimpleName() + " for [" + field.getName() + "]", ex);
        }
        throw new IllegalArgumentException("Cannot assign a " + value.getClass()
                                                                     .getSimpleName()
                + " to [" + field.getName() + "] of type " + type.getSimpleName());
    }

    private static <E> E instantiate(Class<E> type) {
        try {
            return type.getDeclaredConstructor()
                       .newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
            throw new IllegalStateException(
                    "Cannot create a " + type.getName() + " - a generated entity has a public no-argument constructor", ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("The constructor of " + type.getName() + " failed", ex.getCause());
        }
    }
}
