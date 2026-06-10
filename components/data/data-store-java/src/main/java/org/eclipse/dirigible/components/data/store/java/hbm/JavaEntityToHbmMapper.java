/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.hbm;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.eclipse.dirigible.components.data.store.hbm.HbmXmlDescriptor;
import org.eclipse.dirigible.components.data.store.hbm.HbmXmlDescriptor.HbmIdDescriptor;
import org.eclipse.dirigible.components.data.store.hbm.HbmXmlDescriptor.HbmPropertyDescriptor;
import org.eclipse.dirigible.components.data.store.java.manager.RegisteredEntity;
import org.eclipse.dirigible.sdk.db.Column;
import org.eclipse.dirigible.sdk.db.CreatedAt;
import org.eclipse.dirigible.sdk.db.CreatedBy;
import org.eclipse.dirigible.sdk.db.Entity;
import org.eclipse.dirigible.sdk.db.GeneratedValue;
import org.eclipse.dirigible.sdk.db.GenerationType;
import org.eclipse.dirigible.sdk.db.Id;
import org.eclipse.dirigible.sdk.db.Table;
import org.eclipse.dirigible.sdk.db.Transient;
import org.eclipse.dirigible.sdk.db.UpdatedAt;
import org.eclipse.dirigible.sdk.db.UpdatedBy;

/**
 * Reflects over a client class annotated with Dirigible's {@link Entity} and produces:
 * <ol>
 * <li>A {@link HbmXmlDescriptor} ready to be serialised to Hibernate's HBM XML, and</li>
 * <li>A companion {@link RegisteredEntity} carrying the metadata the runtime store needs at
 * save/load time (id field, audit flags, column / property mapping).</li>
 * </ol>
 *
 * <p>
 * Mode: <b>dynamic-map</b>. The HBM XML never references the user's {@link Class} — it only uses
 * the entity name. This lets Hibernate run without needing to load the client class, sidestepping
 * the classloader question that would otherwise arise when Hibernate tried to introspect a class
 * defined in our {@code ClientClassLoader}.
 */
public final class JavaEntityToHbmMapper {

    private JavaEntityToHbmMapper() {}

    /**
     * Build the descriptor + registered-entity pair for a single entity class. Throws
     * {@link IllegalArgumentException} when the class is not a valid entity (missing {@link Entity}, no
     * {@link Id}, duplicate {@code @Id}, etc.).
     *
     * @param key the registration key (typically the entity FQN)
     * @param entityClass the client class annotated with {@link Entity}
     * @return the descriptor + registered-entity pair
     */
    public static Result map(String key, Class<?> entityClass) {
        Entity entityAnn = entityClass.getAnnotation(Entity.class);
        if (entityAnn == null) {
            throw new IllegalArgumentException("Class [" + entityClass.getName() + "] is not annotated with @Entity");
        }

        String entityName = entityAnn.name()
                                     .isEmpty() ? entityClass.getSimpleName() : entityAnn.name();
        Table tableAnn = entityClass.getAnnotation(Table.class);
        String tableName = (tableAnn != null && !tableAnn.name()
                                                         .isEmpty()) ? tableAnn.name() : entityName.toUpperCase(Locale.ROOT);

        // Walk fields (including inherited ones) in declaration order. LinkedHashMap preserves
        // the order, which produces deterministic HBM output — easier to test and diff.
        Map<String, Field> orderedFields = new LinkedHashMap<>();
        for (Class<?> c = entityClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isSynthetic() || java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                orderedFields.putIfAbsent(f.getName(), f);
            }
        }

        Field idField = null;
        String idColumn = null;
        String idType = null;
        String generatorClass = "assigned";

        List<RegisteredEntity.PropertyInfo> properties = new ArrayList<>();
        String createdAtProp = null;
        String updatedAtProp = null;
        String createdByProp = null;
        String updatedByProp = null;

        for (Field field : orderedFields.values()) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }

            if (field.isAnnotationPresent(Id.class)) {
                if (idField != null) {
                    throw new IllegalArgumentException("Entity [" + entityClass.getName() + "] declares multiple @Id fields");
                }
                idField = field;
                Column col = field.getAnnotation(Column.class);
                idColumn = (col != null && !col.name()
                                               .isEmpty()) ? col.name() : field.getName();
                idType = hibernateTypeFor(field.getType());
                GeneratedValue gen = field.getAnnotation(GeneratedValue.class);
                if (gen != null) {
                    generatorClass = generatorClassFor(gen.strategy());
                }
                continue;
            }

            Column col = field.getAnnotation(Column.class);
            String propertyName = field.getName();
            String columnName = (col != null && !col.name()
                                                    .isEmpty()) ? col.name() : propertyName;
            String type = hibernateTypeFor(field.getType());

            Integer length = (col != null) ? col.length() : null;
            boolean nullable = col == null || col.nullable();
            Integer precision = (col != null && col.precision() > 0) ? col.precision() : null;
            Integer scale = (col != null && col.scale() > 0) ? col.scale() : null;

            boolean createdAt = field.isAnnotationPresent(CreatedAt.class);
            boolean updatedAt = field.isAnnotationPresent(UpdatedAt.class);
            boolean createdBy = field.isAnnotationPresent(CreatedBy.class);
            boolean updatedBy = field.isAnnotationPresent(UpdatedBy.class);

            if (createdAt) {
                createdAtProp = propertyName;
            }
            if (updatedAt) {
                updatedAtProp = propertyName;
            }
            if (createdBy) {
                createdByProp = propertyName;
            }
            if (updatedBy) {
                updatedByProp = propertyName;
            }

            properties.add(new RegisteredEntity.PropertyInfo(field, propertyName, columnName, type, length, nullable, precision, scale,
                    createdAt, updatedAt, createdBy, updatedBy));
        }

        if (idField == null) {
            throw new IllegalArgumentException("Entity [" + entityClass.getName() + "] is missing an @Id field");
        }

        HbmIdDescriptor idDesc = new HbmIdDescriptor(idField.getName(), idColumn, idType, generatorClass);
        HbmXmlDescriptor descriptor = new HbmXmlDescriptor(entityName, tableName, idDesc);
        for (RegisteredEntity.PropertyInfo p : properties) {
            descriptor.addProperty(new HbmPropertyDescriptor(p.propertyName(), p.columnName(), p.hibernateType(),
                    p.length() != null && p.length() > 0 ? p.length() : null, p.nullable(), null, p.precision(), p.scale()));
        }

        RegisteredEntity registered = new RegisteredEntity(key, entityClass, entityName, tableName, idField, idColumn, properties,
                new RegisteredEntity.AuditFlags(createdAtProp, updatedAtProp, createdByProp, updatedByProp));

        return new Result(descriptor, registered);
    }

    private static String generatorClassFor(GenerationType strategy) {
        return switch (strategy) {
            case AUTO -> "native";
            case IDENTITY -> "identity";
            case SEQUENCE -> "sequence";
            case TABLE -> "increment";
        };
    }

    private static String hibernateTypeFor(Class<?> javaType) {
        if (javaType == String.class) {
            return "string";
        }
        if (javaType == long.class || javaType == Long.class) {
            return "long";
        }
        if (javaType == int.class || javaType == Integer.class) {
            return "integer";
        }
        if (javaType == short.class || javaType == Short.class) {
            return "short";
        }
        if (javaType == byte.class || javaType == Byte.class) {
            return "byte";
        }
        if (javaType == boolean.class || javaType == Boolean.class) {
            return "boolean";
        }
        if (javaType == double.class || javaType == Double.class) {
            return "double";
        }
        if (javaType == float.class || javaType == Float.class) {
            return "float";
        }
        if (javaType == BigDecimal.class) {
            return "big_decimal";
        }
        if (javaType == BigInteger.class) {
            return "big_integer";
        }
        if (javaType == java.sql.Date.class) {
            return "date";
        }
        if (javaType == java.sql.Time.class) {
            return "time";
        }
        if (javaType == java.sql.Timestamp.class || javaType == Date.class) {
            return "timestamp";
        }
        // java.time.* types need their FQN in HBM XML — Hibernate's basic-type registry resolves them
        // by class name, but the short names ("Instant" / "LocalDate" / "LocalDateTime") aren't
        // registered and Hibernate falls back to Class.forName("Instant"), which throws.
        if (javaType == LocalDateTime.class) {
            return "java.time.LocalDateTime";
        }
        if (javaType == Instant.class) {
            return "java.time.Instant";
        }
        if (javaType == LocalDate.class) {
            return "java.time.LocalDate";
        }
        if (javaType == UUID.class) {
            return "uuid";
        }
        if (javaType == byte[].class) {
            return "binary";
        }
        throw new IllegalArgumentException(
                "Unsupported Java field type [" + javaType.getName() + "]; supported: primitives, String, BigDecimal, "
                        + "BigInteger, java.util.Date, java.sql.{Date,Time,Timestamp}, java.time.{Instant,LocalDate,LocalDateTime}, "
                        + "UUID, byte[]");
    }

    /**
     * Result of {@link #map(String, Class)}.
     *
     * @param descriptor the HBM XML descriptor
     * @param registered the runtime metadata for the entity
     */
    public record Result(HbmXmlDescriptor descriptor, RegisteredEntity registered) {
    }

}
