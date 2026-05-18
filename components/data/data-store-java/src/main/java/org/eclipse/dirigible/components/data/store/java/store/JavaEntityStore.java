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

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.data.store.java.manager.JavaEntityManager;
import org.eclipse.dirigible.components.data.store.java.manager.RegisteredEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;
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
 * Audit fields ({@code @CreatedAt}, {@code @UpdatedAt}, {@code @CreatedBy}, {@code @UpdatedBy})
 * are populated automatically on {@link #save(Object)} / {@link #update(Object)} from
 * {@link UserFacade#getName()} and {@link Instant#now()}.
 */
@Component
public class JavaEntityStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaEntityStore.class);

    private final JavaEntityManager entityManager;

    @Autowired
    public JavaEntityStore(JavaEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Insert a new entity. The id field is back-filled when a generator produced it. */
    public <T> T save(T entity) {
        RegisteredEntity meta = resolve(entity.getClass());
        applyCreateAudit(entity, meta);
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);

        try (Session session = entityManager.getSessionFactory()
                                             .openSession()) {
            Transaction tx = session.beginTransaction();
            Object generatedId = session.save(meta.entityName(), data);
            tx.commit();
            // Write the generator-produced id back onto the bean. Hibernate updates `data` too,
            // but the caller passed us a typed bean.
            if (generatedId != null) {
                writeId(entity, meta, generatedId);
            }
            return entity;
        }
    }

    /** Update an existing entity by id. */
    public <T> T update(T entity) {
        RegisteredEntity meta = resolve(entity.getClass());
        applyUpdateAudit(entity, meta);
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);
        try (Session session = entityManager.getSessionFactory()
                                             .openSession()) {
            Transaction tx = session.beginTransaction();
            session.update(meta.entityName(), data);
            tx.commit();
        }
        return entity;
    }

    /** Find by id. Throws {@link IllegalArgumentException} when not found — use {@link #findOne} for the optional variant. */
    public <T> T findById(Class<T> type, Object id) {
        return findOne(type, id).orElseThrow(() -> new IllegalArgumentException("No entity [" + type.getSimpleName() + "] with id [" + id + "]"));
    }

    public <T> Optional<T> findOne(Class<T> type, Object id) {
        RegisteredEntity meta = resolve(type);
        try (Session session = entityManager.getSessionFactory()
                                             .openSession()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) session.get(meta.entityName(), (java.io.Serializable) id);
            if (data == null) {
                return Optional.empty();
            }
            return Optional.of(EntityBeanMapper.fromMap(type, data, meta));
        }
    }

    public <T> List<T> findAll(Class<T> type) {
        return findAll(type, -1, -1);
    }

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

    public <T> void delete(T entity) {
        RegisteredEntity meta = resolve(entity.getClass());
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);
        try (Session session = entityManager.getSessionFactory()
                                             .openSession()) {
            Transaction tx = session.beginTransaction();
            session.delete(meta.entityName(), data);
            tx.commit();
        }
    }

    public <T> void deleteById(Class<T> type, Object id) {
        RegisteredEntity meta = resolve(type);
        try (Session session = entityManager.getSessionFactory()
                                             .openSession()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) session.get(meta.entityName(), (java.io.Serializable) id);
            if (data == null) {
                return;
            }
            Transaction tx = session.beginTransaction();
            session.delete(meta.entityName(), data);
            tx.commit();
        }
    }

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
     * Execute an HQL/JPQL query against the entity. Parameters are bound by name. The query must
     * select map projections (default for dynamic-map entities — {@code from <entityName>} works
     * out of the box).
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

    /** Number of currently registered entity types — useful in tests. */
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
                             .orElseThrow(() -> new IllegalStateException(
                                     "Class [" + entityClass.getName() + "] is not a registered @Entity. "
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
