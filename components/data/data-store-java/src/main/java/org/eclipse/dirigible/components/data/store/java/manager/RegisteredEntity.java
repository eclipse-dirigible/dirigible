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

    public String key() {
        return key;
    }

    public Class<?> entityClass() {
        return entityClass;
    }

    public String entityName() {
        return entityName;
    }

    public String tableName() {
        return tableName;
    }

    public Field idField() {
        return idField;
    }

    public String idColumn() {
        return idColumn;
    }

    public List<PropertyInfo> properties() {
        return properties;
    }

    public AuditFlags audit() {
        return audit;
    }

    /** One persistent property on the entity. */
    public record PropertyInfo(Field field, String propertyName, String columnName, String hibernateType, Integer length, boolean nullable,
            Integer precision, Integer scale, boolean createdAt, boolean updatedAt, boolean createdBy, boolean updatedBy) {
    }

    /**
     * Convenience flags identifying which property names carry audit semantics. Lets the store fill
     * them in on save/update without re-walking annotations on every call.
     */
    public record AuditFlags(String createdAtProperty, String updatedAtProperty, String createdByProperty, String updatedByProperty) {
        public boolean any() {
            return createdAtProperty != null || updatedAtProperty != null || createdByProperty != null || updatedByProperty != null;
        }
    }

}
