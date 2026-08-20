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
import org.eclipse.dirigible.components.data.store.java.outbox.EventOutbox;
import org.eclipse.dirigible.components.data.store.java.repository.Criteria;
import org.eclipse.dirigible.components.data.store.java.repository.DomainEvent;
import org.eclipse.dirigible.sdk.utils.Json;
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

    private final EventOutbox outbox;

    /**
     * @param entityManager the manager that owns the dynamic Hibernate {@code SessionFactory}
     * @param outbox the transactional outbox that records a write's events alongside the write
     */
    @Autowired
    public JavaEntityStore(JavaEntityManager entityManager, EventOutbox outbox) {
        this.entityManager = entityManager;
        this.outbox = outbox;
    }

    /**
     * Insert a new entity. The id field is back-filled when a generator produced it.
     *
     * @param <T> the entity type
     * @param entity the entity to insert
     * @return the same entity (with any generated identifier populated)
     */
    public <T> T save(T entity) {
        return save(entity, null);
    }

    /**
     * Insert a new entity and publish it on the given topic, atomically: the event is recorded in the
     * tenant's outbox inside the insert's own transaction, so the row and its event commit together,
     * and the broker only sees the event once the row is durable. A broker that refuses it leaves the
     * entry for the relay instead of failing this call.
     *
     * @param <T> the entity type
     * @param entity the entity to insert
     * @param eventTopic the topic to publish the saved entity on; {@code null} publishes nothing
     * @return the same entity (with any generated identifier populated)
     */
    public <T> T save(T entity, String eventTopic) {
        return save(entity, eventTopic, List.of());
    }

    /**
     * Insert a new entity, publishing it on the given topic plus any further events the write emits
     * about other rows — e.g. a create-from announcing its source's completed transition only once the
     * document that transition was about exists. All of them share the insert's transaction.
     *
     * @param <T> the entity type
     * @param entity the entity to insert
     * @param eventTopic the topic to publish the saved entity on; {@code null} publishes nothing
     * @param additionalEvents further events to record with the same write
     * @return the same entity (with any generated identifier populated)
     */
    public <T> T save(T entity, String eventTopic, List<DomainEvent> additionalEvents) {
        RegisteredEntity meta = resolve(entity.getClass());
        applyCreateAudit(entity, meta);
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);
        prepareOutbox(eventTopic != null || !additionalEvents.isEmpty());

        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            EventOutbox.Batch events;
            try {
                // Hibernate 7 removed the legacy save(...) overloads. persist() returns void; for
                // dynamic-map entities the generator-produced id is populated into `data` under the
                // id property's key. Read it back to mirror it onto the caller's typed bean.
                session.persist(meta.entityName(), data);
                Object generatedId = data.get(meta.idField()
                                                  .getName());
                if (generatedId != null) {
                    // Back-filled before the event is recorded: the payload must carry the identifier
                    // the row was actually inserted with.
                    writeId(entity, meta, generatedId);
                }
                events = outbox.record(session, eventsOf(eventTopic, entity, additionalEvents));
                tx.commit();
            } catch (RuntimeException ex) {
                rollback(tx, ex);
                throw ex;
            }
            events.dispatch();
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
        return update(entity, null, List.of());
    }

    /**
     * Update an existing entity and publish it on the given topic, atomically — see
     * {@link #save(Object, String)} for what that buys.
     *
     * @param <T> the entity type
     * @param entity the entity to update
     * @param eventTopic the topic to publish the updated entity on; {@code null} publishes nothing
     * @return the same entity
     */
    public <T> T update(T entity, String eventTopic) {
        return update(entity, eventTopic, List.of());
    }

    /**
     * Update an existing entity, publishing it on the given topic plus any further events the write
     * emits about other rows — an aggregate's {@code "-rekeyed"} notice about the tuple it just left.
     * All of them share the update's transaction.
     *
     * @param <T> the entity type
     * @param entity the entity to update
     * @param eventTopic the topic to publish the updated entity on; {@code null} publishes nothing
     * @param additionalEvents further events to record with the same write
     * @return the same entity
     */
    public <T> T update(T entity, String eventTopic, List<DomainEvent> additionalEvents) {
        RegisteredEntity meta = resolve(entity.getClass());
        applyUpdateAudit(entity, meta);
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);
        prepareOutbox(eventTopic != null || !additionalEvents.isEmpty());
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            EventOutbox.Batch events;
            try {
                // Hibernate 7: update(entityName, ...) is gone. merge() is the standardized
                // replacement — copies state from the detached map onto the managed instance.
                session.merge(meta.entityName(), data);
                events = outbox.record(session, eventsOf(eventTopic, entity, additionalEvents));
                tx.commit();
            } catch (RuntimeException ex) {
                rollback(tx, ex);
                throw ex;
            }
            events.dispatch();
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
        return updateProperties(type, id, values, null);
    }

    /**
     * Targeted multi-column write that publishes the resulting row on the given topic, atomically — the
     * derived-column path (a roll-up total, a keyed aggregate) that must both leave every other column
     * alone and keep the {@code "-updated"} contract downstream reactions cascade on.
     *
     * <p>
     * The payload is read back on the write's own connection after the mutation and before the commit,
     * so it is the row as this statement left it — not a re-read that a concurrent write could have
     * moved on in the meantime.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary-key value
     * @param values the properties to set (plain identifiers) with their new values
     * @param eventTopic the topic to publish the resulting row on; {@code null} publishes nothing
     * @return the number of updated rows ({@code 0} when the id does not exist or {@code values} is
     *         empty)
     */
    public <T> int updateProperties(Class<T> type, Object id, Map<String, Object> values, String eventTopic) {
        return updateProperties(type, id, values, eventTopic, List.of());
    }

    /**
     * Targeted multi-column write publishing the resulting row on the given topic plus any further
     * events the write emits about other rows — an aggregate's {@code "-rekeyed"} notice about the
     * tuple the row just left. All of them share the mutation's transaction and are recorded only when
     * the row actually existed to be written.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary-key value
     * @param values the properties to set (plain identifiers) with their new values
     * @param eventTopic the topic to publish the resulting row on; {@code null} publishes nothing
     * @param additionalEvents further events to record with the same write
     * @return the number of updated rows ({@code 0} when the id does not exist or {@code values} is
     *         empty)
     */
    public <T> int updateProperties(Class<T> type, Object id, Map<String, Object> values, String eventTopic,
            List<DomainEvent> additionalEvents) {
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
        prepareOutbox(eventTopic != null || !additionalEvents.isEmpty());
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            int updated;
            EventOutbox.Batch events;
            try {
                MutationQuery query = session.createMutationQuery(
                        "update " + meta.entityName() + " set " + assignments + " where " + idProperty + " = :id");
                index = 0;
                for (Object value : values.values()) {
                    query.setParameter("value" + index++, value);
                }
                updated = query.setParameter("id", id)
                               .executeUpdate();
                events = outbox.record(session, updated == 0 ? List.of()
                        : eventsOf(eventTopic, eventTopic == null ? null : readInTransaction(session, type, meta, id), additionalEvents));
                tx.commit();
            } catch (RuntimeException ex) {
                rollback(tx, ex);
                throw ex;
            }
            events.dispatch();
            return updated;
        }
    }

    /** Property names must be plain identifiers so nothing can be injected into the mutation HQL. */
    private static final Pattern PLAIN_PROPERTY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /**
     * Find by id, or {@code null} when there is no such row — an absent id is an ordinary outcome of a
     * lookup, not a failure. Callers that require the row to exist use {@link #findOne} and decide the
     * failure themselves (a {@code 404} at a controller boundary, a skip in an event handler).
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary key
     * @return the entity, or {@code null} if not found
     */
    public <T> T findById(Class<T> type, Object id) {
        return findOne(type, id).orElse(null);
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
        delete(entity, null);
    }

    /**
     * Delete an entity instance and publish it on the given topic, atomically — see
     * {@link #save(Object, String)} for what that buys.
     *
     * @param <T> the entity type
     * @param entity the entity to delete
     * @param eventTopic the topic to publish the deleted entity on; {@code null} publishes nothing
     */
    public <T> void delete(T entity, String eventTopic) {
        RegisteredEntity meta = resolve(entity.getClass());
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);
        Object id = data.get(meta.idField()
                                 .getName());
        if (id == null) {
            return;
        }
        // The payload is the caller's instance: it is what they asked to delete, and after the delete
        // there is no row left to read it from.
        removeById(entity.getClass(), meta, id, eventTopic, entity);
    }

    /**
     * Delete an entity by primary key.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary-key value
     */
    public <T> void deleteById(Class<T> type, Object id) {
        deleteById(type, id, null);
    }

    /**
     * Delete an entity by primary key and publish the deleted row on the given topic, atomically. The
     * payload is the row as it was read inside the deleting transaction, so it describes exactly what
     * was removed.
     *
     * @param <T> the entity type
     * @param type the entity class
     * @param id the primary-key value
     * @param eventTopic the topic to publish the deleted row on; {@code null} publishes nothing
     */
    public <T> void deleteById(Class<T> type, Object id, String eventTopic) {
        removeById(type, resolve(type), id, eventTopic, null);
    }

    /**
     * Hibernate 7 removed {@code delete(entityName, ...)} entirely. The replacement is to load the
     * managed instance first and then call {@link Session#remove(Object)} on it — Hibernate routes to
     * the correct entity type via the persistence-context state of the loaded {@link Map}.
     *
     * @param payload the event payload when the caller already holds the row; {@code null} reads it
     *        from the transaction before removing it
     */
    private void removeById(Class<?> type, RegisteredEntity meta, Object id, String eventTopic, Object payload) {
        prepareOutbox(eventTopic != null);
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            EventOutbox.Batch events;
            try {
                Object managed = session.find(meta.entityName(), id);
                Object deleted = payload != null ? payload : toBean(type, meta, managed);
                if (managed != null) {
                    session.remove(managed);
                }
                events = outbox.record(session, eventsOf(eventTopic, deleted, List.of()));
                tx.commit();
            } catch (RuntimeException ex) {
                rollback(tx, ex);
                throw ex;
            }
            events.dispatch();
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

    /**
     * Creates the tenant's outbox table if this is the first event it records — outside the write
     * transaction, so its DDL can never poison one.
     */
    private void prepareOutbox(boolean hasEvents) {
        if (hasEvents) {
            outbox.prepare();
        }
    }

    /**
     * Builds the events a write records: the written row on its own topic, plus whatever the caller
     * wants published about other rows. A missing row publishes nothing rather than the string
     * {@code "null"}.
     */
    private static List<DomainEvent> eventsOf(String topic, Object entity, List<DomainEvent> additional) {
        if (topic == null && additional.isEmpty()) {
            return List.of();
        }
        List<DomainEvent> events = new ArrayList<>(additional.size() + 1);
        if (topic != null && entity != null) {
            events.add(new DomainEvent(topic, Json.stringify(entity)));
        }
        events.addAll(additional);
        return events;
    }

    /**
     * Reads a row on the given session — inside the caller's open transaction, so it sees that
     * transaction's own uncommitted changes.
     */
    private static <T> T readInTransaction(Session session, Class<T> type, RegisteredEntity meta, Object id) {
        return toBean(type, meta, session.find(meta.entityName(), id));
    }

    @SuppressWarnings("unchecked")
    private static <T> T toBean(Class<T> type, RegisteredEntity meta, Object data) {
        return data == null ? null : EntityBeanMapper.fromMap(type, (Map<String, Object>) data, meta);
    }

    /**
     * Rolls back a transaction whose work failed, keeping the original failure as the one the caller
     * sees.
     */
    private static void rollback(Transaction tx, RuntimeException cause) {
        try {
            if (tx.isActive()) {
                tx.rollback();
            }
        } catch (RuntimeException ex) {
            cause.addSuppressed(ex);
        }
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
