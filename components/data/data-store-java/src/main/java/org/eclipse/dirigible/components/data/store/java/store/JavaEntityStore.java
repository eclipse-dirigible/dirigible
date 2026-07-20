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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.data.store.java.manager.JavaEntityManager;
import org.eclipse.dirigible.components.data.store.java.manager.RegisteredEntity;
import org.eclipse.dirigible.components.data.store.java.repository.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Public CRUD facade for Dirigible Java entities. Designed to be used directly from client
 * {@code JavaHandler} classes (resolve via {@code BeanProvider.getBean(JavaEntityStore.class)}).
 *
 * <p>
 * All methods are typed: take and return the user's own {@link Class}, not a string entity name.
 * Internally we go through Hibernate in dynamic-map mode, with {@link EntityBeanMapper} bridging
 * the two representations.
 *
 * <p>
 * Audit fields ({@code @CreatedAt}, {@code @UpdatedAt}, {@code @CreatedBy}, {@code @UpdatedBy}) are
 * populated automatically on {@link #save(Object)} / {@link #update(Object)} from
 * {@link UserFacade#getName()} and {@link Instant#now()}.
 */
@Component
public class JavaEntityStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaEntityStore.class);

    private final JavaEntityManager entityManager;

    /**
     * @param entityManager the manager that owns the dynamic Hibernate {@code SessionFactory}
     */
    @Autowired
    public JavaEntityStore(JavaEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Insert a new entity. The id field is back-filled when a generator produced it.
     *
     * @param <T> the entity type
     * @param entity the entity to insert
     * @return the same entity (with any generated identifier populated)
     */
    public <T> T save(T entity) {
        RegisteredEntity meta = resolve(entity.getClass());
        applyCreateAudit(entity, meta);
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);

        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            // Hibernate 7 removed the legacy save(...) overloads. persist() returns void; for
            // dynamic-map entities the generator-produced id is populated into `data` under the
            // id property's key. Read it back to mirror it onto the caller's typed bean.
            session.persist(meta.entityName(), data);
            tx.commit();
            Object generatedId = data.get(meta.idField()
                                              .getName());
            if (generatedId != null) {
                writeId(entity, meta, generatedId);
            }
            return entity;
        }
    }

    /**
     * Update an existing entity by id.
     *
     * @param <T> the entity type
     * @param entity the entity to update
     * @return the same entity
     */
    public <T> T update(T entity) {
        RegisteredEntity meta = resolve(entity.getClass());
        applyUpdateAudit(entity, meta);
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            // Hibernate 7: update(entityName, ...) is gone. merge() is the standardized
            // replacement — copies state from the detached map onto the managed instance.
            session.merge(meta.entityName(), data);
            tx.commit();
        }
        return entity;
    }

    /**
     * Update a single property of one entity row, touching nothing else — the primitive for
     * workflow/system write-backs (e.g. the process trigger persisting {@code ProcessId}). A full-row
     * {@link #update(Object)} writes every column from the caller's possibly stale snapshot and
     * silently reverts concurrent writes (a document's recalculated totals, a workflow status); this
     * targeted mutation cannot, because only the named column is in the statement. No audit stamping,
     * no events — a system column write, not a user edit.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary-key value
     * @param property the entity property to set (a plain identifier)
     * @param value the new value
     * @return the number of updated rows ({@code 0} when the id does not exist)
     */
    public <T> int updateProperty(Class<T> type, Object id, String property, Object value) {
        if (property == null || !PLAIN_PROPERTY.matcher(property)
                                               .matches()) {
            throw new IllegalArgumentException("Invalid property name: [" + property + "]");
        }
        RegisteredEntity meta = resolve(type);
        String idProperty = meta.idField()
                                .getName();
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            int updated = session
                                 .createMutationQuery(
                                         "update " + meta.entityName() + " set " + property + " = :value where " + idProperty + " = :id")
                                 .setParameter("value", value)
                                 .setParameter("id", id)
                                 .executeUpdate();
            tx.commit();
            return updated;
        }
    }

    /**
     * Update several properties of one entity row in a single statement, touching nothing else — the
     * multi-column sibling of {@link #updateProperty(Class, Object, String, Object)} for
     * workflow/system write-backs that persist more than one field (e.g. a user task's reviewed edits).
     * All named columns are written atomically by one mutation query; every other column is untouched,
     * so a concurrent write to an unrelated column cannot be reverted. No audit stamping, no events — a
     * system write, not a user edit.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary-key value
     * @param values the properties to set (plain identifiers) with their new values; iteration order is
     *        the statement's column order
     * @return the number of updated rows ({@code 0} when the id does not exist or {@code values} is
     *         empty)
     */
    public <T> int updateProperties(Class<T> type, Object id, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        StringBuilder assignments = new StringBuilder();
        int index = 0;
        for (String property : values.keySet()) {
            if (property == null || !PLAIN_PROPERTY.matcher(property)
                                                   .matches()) {
                throw new IllegalArgumentException("Invalid property name: [" + property + "]");
            }
            if (index > 0) {
                assignments.append(", ");
            }
            assignments.append(property)
                       .append(" = :value")
                       .append(index++);
        }
        RegisteredEntity meta = resolve(type);
        String idProperty = meta.idField()
                                .getName();
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            MutationQuery query =
                    session.createMutationQuery("update " + meta.entityName() + " set " + assignments + " where " + idProperty + " = :id");
            index = 0;
            for (Object value : values.values()) {
                query.setParameter("value" + index++, value);
            }
            int updated = query.setParameter("id", id)
                               .executeUpdate();
            tx.commit();
            return updated;
        }
    }

    /** Property names must be plain identifiers so nothing can be injected into the mutation HQL. */
    private static final Pattern PLAIN_PROPERTY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /**
     * Find by id. Throws {@link IllegalArgumentException} when not found — use {@link #findOne} for the
     * optional variant.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary key
     * @return the entity
     */
    public <T> T findById(Class<T> type, Object id) {
        return findOne(type, id).orElseThrow(
                () -> new IllegalArgumentException("No entity [" + type.getSimpleName() + "] with id [" + id + "]"));
    }

    /**
     * Find by id.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary key
     * @return the entity, or empty if not found
     */
    public <T> Optional<T> findOne(Class<T> type, Object id) {
        RegisteredEntity meta = resolve(type);
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            // Hibernate 7: get(entityName, ...) is deprecated-for-removal in favour of find(...).
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) session.find(meta.entityName(), id);
            if (data == null) {
                return Optional.empty();
            }
            return Optional.of(EntityBeanMapper.fromMap(type, data, meta));
        }
    }

    /**
     * @param <T> the entity type
     * @param type the entity class
     * @return every entity of the given type
     */
    public <T> List<T> findAll(Class<T> type) {
        return findAll(type, -1, -1);
    }

    /**
     * @param <T> the entity type
     * @param type the entity class
     * @param limit max rows to return; non-positive means unlimited
     * @param offset rows to skip; non-positive means none
     * @return the requested page
     */
    public <T> List<T> findAll(Class<T> type, int limit, int offset) {
        RegisteredEntity meta = resolve(type);
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Query<Map> query = session.createQuery("from " + meta.entityName(), Map.class);
            if (limit > 0) {
                query.setMaxResults(limit);
            }
            if (offset > 0) {
                query.setFirstResult(offset);
            }
            return mapRows(type, meta, query.getResultList());
        }
    }

    /**
     * Delete an entity instance.
     *
     * @param <T> the entity type
     * @param entity the entity to delete
     */
    public <T> void delete(T entity) {
        RegisteredEntity meta = resolve(entity.getClass());
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);
        Object id = data.get(meta.idField()
                                 .getName());
        if (id == null) {
            return;
        }
        removeById(meta, id);
    }

    /**
     * Delete an entity by primary key.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary-key value
     */
    public <T> void deleteById(Class<T> type, Object id) {
        removeById(resolve(type), id);
    }

    /**
     * Hibernate 7 removed {@code delete(entityName, ...)} entirely. The replacement is to load the
     * managed instance first and then call {@link Session#remove(Object)} on it — Hibernate routes to
     * the correct entity type via the persistence-context state of the loaded {@link Map}.
     */
    private void removeById(RegisteredEntity meta, Object id) {
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            Object managed = session.find(meta.entityName(), id);
            if (managed != null) {
                session.remove(managed);
            }
            tx.commit();
        }
    }

    /**
     * @param <T> the entity type
     * @param type the entity class
     * @return the number of stored entities of the given type
     */
    public <T> long count(Class<T> type) {
        RegisteredEntity meta = resolve(type);
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Long result = session.createQuery("select count(*) from " + meta.entityName(), Long.class)
                                 .getSingleResult();
            return result == null ? 0L : result;
        }
    }

    /**
     * Execute an HQL/JPQL query against the entity. Parameters are bound by name. The query must select
     * map projections (default for dynamic-map entities — {@code from <entityName>} works out of the
     * box).
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param hql the HQL/JPQL query string
     * @param parameters named parameter bindings; may be {@code null}
     * @return the query results
     */
    public <T> List<T> query(Class<T> type, String hql, Map<String, Object> parameters) {
        RegisteredEntity meta = resolve(type);
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Query<Map> q = session.createQuery(hql, Map.class);
            if (parameters != null) {
                parameters.forEach(q::setParameter);
            }
            return mapRows(type, meta, q.getResultList());
        }
    }

    /**
     * Find every entity of the given type matching a typed {@link Criteria}. Builds an HQL query from
     * the criteria's conditions and ordering and runs it through the same map-projection path as
     * {@link #query(Class, String, Map)} — values are bound as named parameters, never inlined.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param criteria the query criteria; {@code null} returns all rows
     * @return the matching entities
     */
    public <T> List<T> findAll(Class<T> type, Criteria criteria) {
        if (criteria == null) {
            return findAll(type);
        }
        RegisteredEntity meta = resolve(type);
        String hql = criteria.append("from " + meta.entityName());
        return query(type, hql, criteria.parameters());
    }

    /**
     * @return the number of currently registered entity types — useful in tests
     */
    public int registeredCount() {
        return entityManager.size();
    }

    private <T> List<T> mapRows(Class<T> type, RegisteredEntity meta, List<Map> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> out = new ArrayList<>(rows.size());
        for (Map row : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) row;
            out.add(EntityBeanMapper.fromMap(type, data, meta));
        }
        return out;
    }

    private RegisteredEntity resolve(Class<?> entityClass) {
        return entityManager.findForClass(entityClass)
                            .orElseThrow(() -> new IllegalStateException("Class [" + entityClass.getName()
                                    + "] is not a registered @Entity. "
                                    + "Ensure it has been picked up by the synchronizer (file present in the registry, no compile errors)."));
    }

    private void applyCreateAudit(Object entity, RegisteredEntity meta) {
        if (!meta.audit()
                 .any()) {
            return;
        }
        try {
            Instant now = Instant.now();
            String user = currentUserSafely();
            for (RegisteredEntity.PropertyInfo p : meta.properties()) {
                if (p.createdAt()) {
                    writeTemporal(entity, p.field(), now);
                }
                if (p.createdBy() && user != null) {
                    Field f = p.field();
                    f.setAccessible(true);
                    f.set(entity, user);
                }
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to apply create-audit fields: {}", e.getMessage());
        }
    }

    private void applyUpdateAudit(Object entity, RegisteredEntity meta) {
        if (!meta.audit()
                 .any()) {
            return;
        }
        try {
            Instant now = Instant.now();
            String user = currentUserSafely();
            for (RegisteredEntity.PropertyInfo p : meta.properties()) {
                if (p.updatedAt()) {
                    writeTemporal(entity, p.field(), now);
                }
                if (p.updatedBy() && user != null) {
                    Field f = p.field();
                    f.setAccessible(true);
                    f.set(entity, user);
                }
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to apply update-audit fields: {}", e.getMessage());
        }
    }

    private static String currentUserSafely() {
        try {
            return UserFacade.getName();
        } catch (RuntimeException e) {
            // In contexts without an authenticated user (background jobs, tests) UserFacade can
            // raise — we treat that as "no user available" and leave the field null.
            return null;
        }
    }

    private static void writeTemporal(Object entity, Field field, Instant instant) throws IllegalAccessException {
        field.setAccessible(true);
        Class<?> t = field.getType();
        if (t == Instant.class) {
            field.set(entity, instant);
        } else if (t == Timestamp.class) {
            field.set(entity, Timestamp.from(instant));
        } else if (t == java.util.Date.class) {
            field.set(entity, java.util.Date.from(instant));
        } else if (t == java.time.LocalDateTime.class) {
            field.set(entity, java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()));
        }
        // Other temporal types are ignored — the entity author chose an unsupported type.
    }

    private static void writeId(Object entity, RegisteredEntity meta, Object idValue) {
        try {
            Field id = meta.idField();
            id.setAccessible(true);
            Class<?> t = id.getType();
            if (idValue instanceof Number n) {
                if (t == int.class || t == Integer.class) {
                    id.set(entity, n.intValue());
                    return;
                }
                if (t == long.class || t == Long.class) {
                    id.set(entity, n.longValue());
                    return;
                }
            }
            if (t.isInstance(idValue)) {
                id.set(entity, idValue);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to back-fill generated id on {}: {}", entity.getClass()
                                                                            .getName(),
                    e.getMessage());
        }
    }

}
