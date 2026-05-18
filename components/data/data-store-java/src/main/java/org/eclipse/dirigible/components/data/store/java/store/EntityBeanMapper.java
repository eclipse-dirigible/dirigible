/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.store;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.components.data.store.java.manager.RegisteredEntity;

/**
 * Reflection-based converter between typed user beans and the {@code Map<String, Object>}
 * representation Hibernate uses in dynamic-map mode.
 *
 * <p>
 * Map keys are the entity's <b>property names</b> (i.e. the field names, since we never rename
 * properties — only their columns are renameable via {@code @Column.name}). Values are coerced
 * between Java types and the temporal types Hibernate hands us back from the JDBC driver
 * (e.g. {@code java.sql.Timestamp} → {@code Instant}).
 */
public final class EntityBeanMapper {

    private EntityBeanMapper() {}

    /**
     * Copy every persistent field of {@code bean} into a fresh {@link Map}. The id field is also
     * included (Hibernate needs it for {@code update} / {@code merge}).
     */
    public static Map<String, Object> toMap(Object bean, RegisteredEntity entity) {
        Map<String, Object> data = new HashMap<>();
        try {
            entity.idField()
                  .setAccessible(true);
            data.put(entity.idField()
                            .getName(),
                    entity.idField()
                          .get(bean));
            for (RegisteredEntity.PropertyInfo p : entity.properties()) {
                Field field = p.field();
                field.setAccessible(true);
                data.put(p.propertyName(), field.get(bean));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read fields of " + bean.getClass()
                                                                            .getName() + ": " + e.getMessage(),
                    e);
        }
        return data;
    }

    /**
     * Instantiate {@code type} via its no-arg constructor and assign every entry from {@code data}
     * to the matching field. Unknown keys are ignored. Values are coerced where the target field
     * type differs from the JDBC-returned type (e.g. {@code Timestamp} → {@code Instant}).
     */
    public static <T> T fromMap(Class<T> type, Map<String, Object> data, RegisteredEntity entity) {
        T instance;
        try {
            instance = type.getDeclaredConstructor()
                           .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Entity class [" + type.getName() + "] must declare a public no-arg constructor: " + e.getMessage(), e);
        }
        try {
            entity.idField()
                  .setAccessible(true);
            Object idValue = data.get(entity.idField()
                                             .getName());
            if (idValue != null) {
                entity.idField()
                      .set(instance, coerce(idValue, entity.idField()
                                                            .getType()));
            }
            for (RegisteredEntity.PropertyInfo p : entity.properties()) {
                Object v = data.get(p.propertyName());
                if (v == null) {
                    continue;
                }
                Field field = p.field();
                field.setAccessible(true);
                field.set(instance, coerce(v, field.getType()));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot write fields of " + type.getName() + ": " + e.getMessage(), e);
        }
        return instance;
    }

    /**
     * Best-effort type coercion to bridge gaps between what Hibernate returns and the user's
     * declared field type. Returns the value unchanged when no coercion is needed.
     */
    private static Object coerce(Object value, Class<?> target) {
        if (value == null || target.isInstance(value)) {
            return value;
        }
        // Numeric narrowing/widening
        if (value instanceof Number n) {
            if (target == int.class || target == Integer.class) {
                return n.intValue();
            }
            if (target == long.class || target == Long.class) {
                return n.longValue();
            }
            if (target == short.class || target == Short.class) {
                return n.shortValue();
            }
            if (target == byte.class || target == Byte.class) {
                return n.byteValue();
            }
            if (target == double.class || target == Double.class) {
                return n.doubleValue();
            }
            if (target == float.class || target == Float.class) {
                return n.floatValue();
            }
            if (target == BigDecimal.class) {
                return new BigDecimal(n.toString());
            }
            if (target == BigInteger.class) {
                return BigInteger.valueOf(n.longValue());
            }
        }
        // Temporal coercions
        if (target == Instant.class) {
            if (value instanceof Timestamp ts) {
                return ts.toInstant();
            }
            if (value instanceof Date d) {
                return d.toInstant();
            }
        }
        if (target == LocalDateTime.class && value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (target == LocalDate.class && value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (target == Date.class && value instanceof Instant i) {
            return Date.from(i);
        }
        if (target == Timestamp.class && value instanceof Instant i) {
            return Timestamp.from(i);
        }
        if (target == Timestamp.class && value instanceof LocalDateTime ldt) {
            return Timestamp.valueOf(ldt);
        }
        if (target == Instant.class && value instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault())
                      .toInstant();
        }
        // Boolean
        if (target == boolean.class || target == Boolean.class) {
            if (value instanceof Number n) {
                return n.intValue() != 0;
            }
            if (value instanceof String s) {
                return Boolean.parseBoolean(s);
            }
        }
        if (target == String.class) {
            return value.toString();
        }
        return value;
    }

}
