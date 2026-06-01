/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.manager;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Resolved metadata for one Dirigible-annotated client {@code @Entity} class.
 *
 * <p>
 * Produced by {@code JavaEntityToHbmMapper}; held by {@link JavaEntityManager}; consumed by
 * {@code JavaEntityStore} when mapping between user beans and the dynamic-map representation
 * Hibernate uses internally.
 */
public final class RegisteredEntity {

    /** Logical key keyed in {@link JavaEntityManager}'s map of registered entities. */
    private final String key;

    /** The original loaded {@link Class} this entity reflects. */
    private final Class<?> entityClass;

    /** Hibernate entity-name — what we pass to {@code session.save(entityName, …)}. */
    private final String entityName;

    /** Database table name (already case-resolved). */
    private final String tableName;

    /** The {@code @Id}-annotated field. Non-null for any valid entity. */
    private final Field idField;

    /** The column name used for the id. */
    private final String idColumn;

    /** Persistent fields excluding {@code @Transient}; field name → property metadata. */
    private final List<PropertyInfo> properties;

    /** Field-level audit flags (created/updated at/by). */
    private final AuditFlags audit;

    /**
     * Capture the resolved metadata for one entity class.
     *
     * @param key the logical registration key
     * @param entityClass the original loaded class
     * @param entityName the Hibernate entity name
     * @param tableName the database table name
     * @param idField the reflective {@code @Id} field
     * @param idColumn the id column name
     * @param properties the persistent property metadata
     * @param audit which property names carry audit semantics
     */
    public RegisteredEntity(String key, Class<?> entityClass, String entityName, String tableName, Field idField, String idColumn,
            List<PropertyInfo> properties, AuditFlags audit) {
        this.key = key;
        this.entityClass = entityClass;
        this.entityName = entityName;
        this.tableName = tableName;
        this.idField = idField;
        this.idColumn = idColumn;
        this.properties = List.copyOf(properties);
        this.audit = audit;
    }

    /** @return the logical registration key */
    public String key() {
        return key;
    }

    /** @return the original loaded {@link Class} */
    public Class<?> entityClass() {
        return entityClass;
    }

    /** @return the Hibernate entity name */
    public String entityName() {
        return entityName;
    }

    /** @return the database table name */
    public String tableName() {
        return tableName;
    }

    /** @return the reflective {@code @Id} field */
    public Field idField() {
        return idField;
    }

    /** @return the id column name */
    public String idColumn() {
        return idColumn;
    }

    /** @return the persistent property metadata, in declaration order */
    public List<PropertyInfo> properties() {
        return properties;
    }

    /** @return which property names carry audit semantics */
    public AuditFlags audit() {
        return audit;
    }

    /**
     * One persistent property on the entity.
     *
     * @param field the reflective field
     * @param propertyName the Hibernate property name
     * @param columnName the database column name
     * @param hibernateType the Hibernate basic-type name
     * @param length max string length, or {@code null}
     * @param nullable whether the column accepts nulls
     * @param precision numeric precision, or {@code null}
     * @param scale numeric scale, or {@code null}
     * @param createdAt true if this property carries {@code @CreatedAt} semantics
     * @param updatedAt true if this property carries {@code @UpdatedAt} semantics
     * @param createdBy true if this property carries {@code @CreatedBy} semantics
     * @param updatedBy true if this property carries {@code @UpdatedBy} semantics
     */
    public record PropertyInfo(Field field, String propertyName, String columnName, String hibernateType, Integer length, boolean nullable,
            Integer precision, Integer scale, boolean createdAt, boolean updatedAt, boolean createdBy, boolean updatedBy) {
    }

    /**
     * Convenience flags identifying which property names carry audit semantics. Lets the store fill
     * them in on save/update without re-walking annotations on every call.
     *
     * @param createdAtProperty property name carrying {@code @CreatedAt}, or {@code null}
     * @param updatedAtProperty property name carrying {@code @UpdatedAt}, or {@code null}
     * @param createdByProperty property name carrying {@code @CreatedBy}, or {@code null}
     * @param updatedByProperty property name carrying {@code @UpdatedBy}, or {@code null}
     */
    public record AuditFlags(String createdAtProperty, String updatedAtProperty, String createdByProperty, String updatedByProperty) {
        /** @return whether any audit property is set */
        public boolean any() {
            return createdAtProperty != null || updatedAtProperty != null || createdByProperty != null || updatedByProperty != null;
        }
    }

}
