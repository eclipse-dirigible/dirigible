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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
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
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
     * Runs a block of entity work as ONE unit: every write and read this thread makes through the store
     * inside it joins a single session and a single transaction, which commits when the block returns
     * and rolls back whole when it throws — for any failure the block raises, an {@link Error}
     * included.
     *
     * <p>
     * Without it each store call is its own transaction, so a multi-write operation that fails halfway
     * leaves the writes that already succeeded behind — a document minted with no lines, its source
     * already marked as billed. Reads inside the block go through the same session, so the block sees
     * its own uncommitted writes (the status a create-from just flipped, the master a line was just
     * attached to).
     *
     * <p>
     * The events those writes record ride the same transaction, as they always did, and are handed to
     * the broker only once the whole unit has committed — never for work that was rolled back. Calls
     * nest: an inner {@code inUnitOfWork} joins the outer one, which alone owns the commit.
     *
     * <p>
     * What is deliberately NOT in the unit: the change history ({@code History}), document-number
     * allocation and the outbox table's own DDL each run on their own connection, so a rolled-back unit
     * can leave a history row or consume a number. Both are append-only records of an attempt, not
     * business state.
     *
     * @param <R> the block's result type
     * @param work the block to run
     * @return whatever the block returned
     */
    public <R> R inUnitOfWork(Supplier<R> work) {
        UnitOfWork joined = UNIT_OF_WORK.get();
        if (joined != null) {
            return work.get();
        }
        UnitOfWork unit = new UnitOfWork(entityManager.getSessionFactory()
                                                      .openSession());
        UNIT_OF_WORK.set(unit);
        try {
            R result;
            try {
                result = work.get();
                unit.transaction.commit();
            } catch (Throwable ex) {
                // Throwable, not RuntimeException: an Error — an assertion, a StackOverflow from a
                // handler that recursed, an OOME — is exactly the failure that must not leave the
                // unit's writes behind, and it would otherwise reach the finally below and close the
                // session with the transaction still active, making the outcome depend on what the
                // pool does with such a connection rather than on an explicit rollback.
                rollback(unit.transaction, ex);
                throw ex;
            }
            unit.dispatch();
            return result;
        } finally {
            UNIT_OF_WORK.remove();
            unit.session.close();
        }
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
        return write((session, events) -> {
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
            events.add(outbox.record(session, eventsOf(eventTopic, entity, additionalEvents)));
            return entity;
        });
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
        return write((session, events) -> {
            // Hibernate 7: update(entityName, ...) is gone. merge() is the standardized
            // replacement — copies state from the detached map onto the managed instance.
            session.merge(meta.entityName(), data);
            events.add(outbox.record(session, eventsOf(eventTopic, entity, additionalEvents)));
            return entity;
        });
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
        return write((session, events) -> {
            flushBeforeMutation(session);
            int updated = session
                                 .createMutationQuery(
                                         "update " + meta.entityName() + " set " + property + " = :value where " + idProperty + " = :id")
                                 .setParameter("value", value)
                                 .setParameter("id", id)
                                 .executeUpdate();
            evictAfterMutation(session, meta, id);
            return updated;
        });
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
        return write((session, events) -> {
            flushBeforeMutation(session);
            MutationQuery query =
                    session.createMutationQuery("update " + meta.entityName() + " set " + assignments + " where " + idProperty + " = :id");
            int parameter = 0;
            for (Object value : values.values()) {
                query.setParameter("value" + parameter++, value);
            }
            int updated = query.setParameter("id", id)
                               .executeUpdate();
            evictAfterMutation(session, meta, id);
            events.add(outbox.record(session, updated == 0 ? List.of()
                    : eventsOf(eventTopic, eventTopic == null ? null : readInTransaction(session, type, meta, id), additionalEvents)));
            return updated;
        });
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
        return read(session -> {
            Map<String, Object> data = mapById(session, meta, id);
            if (data == null) {
                return Optional.empty();
            }
            return Optional.of(EntityBeanMapper.fromMap(type, data, meta));
        });
    }

    /**
     * Loads one row as its dynamic-map by id, seeing the caller's own uncommitted writes.
     *
     * <p>
     * Outside a unit of work each store call is its own session and the row is already committed, so a
     * plain {@code session.find} serves it (Hibernate 7: {@code get(entityName, ...)} is
     * deprecated-for-removal in favour of {@code find}). Inside a unit the row may have been persisted
     * earlier in the SAME session, and there {@code find} by id does NOT return it: an
     * {@code IDENTITY}-generated dynamic-map entity that has been flushed once (as it is the moment any
     * later write in the block flushes the session) is not served back by a subsequent {@code find} on
     * its id — it returns {@code null}. That is what made a generated document's synchronous total
     * recompute reload its own just-created header as {@code null} and give up before summing a single
     * line, so the header committed with the zeros it was inserted with while the identical line
     * recomputed correctly when POSTed on its own connection (issue #7096). An HQL query by id, run
     * after a flush, reads through to the row the block wrote — the read-your-own-writes guarantee the
     * unit makes — so inside a unit that is the path taken.
     */
    private static Map<String, Object> mapById(Session session, RegisteredEntity meta, Object id) {
        if (UNIT_OF_WORK.get() == null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> found = (Map<String, Object>) session.find(meta.entityName(), id);
            return found;
        }
        session.flush();
        List<Map> rows = session.createQuery("from " + meta.entityName() + " where " + meta.idField()
                                                                                           .getName()
                + " = :id", Map.class)
                                .setParameter("id", id)
                                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rows.get(0);
        return data;
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
        return read(session -> {
            Query<Map> query = session.createQuery("from " + meta.entityName(), Map.class);
            if (limit > 0) {
                query.setMaxResults(limit);
            }
            if (offset > 0) {
                query.setFirstResult(offset);
            }
            return mapRows(type, meta, query.getResultList());
        });
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
     * {@link #save(Object, String)} for what that buys. The payload is the row as it was read inside
     * the deleting transaction, not the caller's instance: a caller legitimately holds a partial
     * snapshot — an id and the couple of fields their code cares about — and publishing that hands
     * every delete reaction a payload whose unset columns are null. The abort listener that stops a
     * record's running process reads {@code ProcessIds} off it and would silently no-op, leaving the
     * process alive after its record. Before the removal the row is still there to be read, so it is.
     *
     * @param <T> the entity type
     * @param entity the entity to delete
     * @param eventTopic the topic to publish the deleted row on; {@code null} publishes nothing
     */
    public <T> void delete(T entity, String eventTopic) {
        RegisteredEntity meta = resolve(entity.getClass());
        Map<String, Object> data = EntityBeanMapper.toMap(entity, meta);
        Object id = data.get(meta.idField()
                                 .getName());
        if (id == null) {
            return;
        }
        removeById(entity.getClass(), meta, id, eventTopic);
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
        removeById(type, resolve(type), id, eventTopic);
    }

    /**
     * Hibernate 7 removed {@code delete(entityName, ...)} entirely. The replacement is to load the
     * managed instance first and then call {@link Session#remove(Object)} on it — Hibernate routes to
     * the correct entity type via the persistence-context state of the loaded {@link Map}.
     *
     * <p>
     * The instance loaded here is also the event payload, so a delete describes exactly what was
     * removed however the caller addressed it.
     */
    private void removeById(Class<?> type, RegisteredEntity meta, Object id, String eventTopic) {
        prepareOutbox(eventTopic != null);
        write((session, events) -> {
            Object managed = session.find(meta.entityName(), id);
            Object deleted = eventTopic == null ? null : toBean(type, meta, managed);
            if (managed != null) {
                session.remove(managed);
            }
            events.add(outbox.record(session, eventsOf(eventTopic, deleted, List.of())));
            return null;
        });
    }

    /**
     * @param <T> the entity type
     * @param type the entity class
     * @return the number of stored entities of the given type
     */
    public <T> long count(Class<T> type) {
        RegisteredEntity meta = resolve(type);
        return read(session -> {
            Long result = session.createQuery("select count(*) from " + meta.entityName(), Long.class)
                                 .getSingleResult();
            return result == null ? 0L : result;
        });
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
        return read(session -> {
            Query<Map> q = session.createQuery(hql, Map.class);
            if (parameters != null) {
                parameters.forEach(q::setParameter);
            }
            return mapRows(type, meta, q.getResultList());
        });
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
     * transaction's own uncommitted changes (the just-written row a targeted update publishes on its
     * topic). Goes through {@link #mapById} so a row written earlier in the same unit is seen here too.
     */
    private static <T> T readInTransaction(Session session, Class<T> type, RegisteredEntity meta, Object id) {
        return toBean(type, meta, mapById(session, meta, id));
    }

    @SuppressWarnings("unchecked")
    private static <T> T toBean(Class<T> type, RegisteredEntity meta, Object data) {
        return data == null ? null : EntityBeanMapper.fromMap(type, (Map<String, Object>) data, meta);
    }

    /**
     * Flushes the session's pending writes before a bulk mutation query runs inside a unit of work. A
     * targeted update is an {@code update … where id = :id} executed straight against the connection;
     * Hibernate does not first flush the block's still-in-memory {@code persist}s, so without this the
     * statement would run ahead of the very rows it depends on — the master a line was just attached
     * to, a header just created. Outside a unit there is nothing pending on this session, so it no-ops.
     */
    private static void flushBeforeMutation(Session session) {
        if (UNIT_OF_WORK.get() != null) {
            session.flush();
        }
    }

    /**
     * Drops the session's cached copy of the row a bulk mutation query just wrote, inside a unit of
     * work — the symmetric half of {@link #flushBeforeMutation(Session)}. A targeted update executes
     * straight against the connection and never touches the persistence context, so the instance the
     * session is holding for that id still carries the PRE-mutation values; and an HQL entity query by
     * id resolves to the already-managed instance rather than to the row it just read, so the follow-up
     * read hands back exactly those stale values. That made a topic-bearing
     * {@link #updateProperties(Class, Object, Map, String)} publish the OLD totals from inside a unit
     * (a create-from copying lines and then re-summing the header), and a generated repository's
     * {@code history} trail record {@code before == after} — an entry saying the write happened with
     * nothing moved (issue #7135). Evicting after the mutation makes the next read reload the row, so
     * the payload really is the row as the statement left it. It runs after
     * {@link #flushBeforeMutation} and the mutation itself, so there is nothing pending on the instance
     * to lose. Outside a unit each call has its own session with no cached instance, so it no-ops.
     */
    private static void evictAfterMutation(Session session, RegisteredEntity meta, Object id) {
        if (UNIT_OF_WORK.get() == null) {
            return;
        }
        PersistenceContext context = session.unwrap(SharedSessionContractImplementor.class)
                                            .getPersistenceContextInternal();
        for (Map.Entry<EntityKey, EntityHolder> held : new ArrayList<>(context.getEntityHoldersByKey()
                                                                              .entrySet())) {
            if (!meta.entityName()
                     .equals(held.getKey()
                                 .getEntityName())
                    || !sameId(held.getKey()
                                   .getIdentifier(),
                            id)) {
                continue;
            }
            Object managed = held.getValue()
                                 .getEntity();
            if (managed != null) {
                session.evict(managed);
            }
        }
    }

    /**
     * Whether a key the session filed the row under is the id this mutation wrote. The store's ids
     * arrive as whatever the caller had — an {@code Integer} literal from a path parameter, a
     * {@code String} from a query — while the session keys the row under the mapped identifier type, so
     * the two can denote one row and still not be {@code equals}. Their textual forms decide it then,
     * which is exact for the integral and string keys an entity id is.
     */
    private static boolean sameId(Object key, Object id) {
        if (Objects.equals(key, id)) {
            return true;
        }
        return key != null && id != null && String.valueOf(key)
                                                  .equals(String.valueOf(id));
    }

    /**
     * Runs a write on the thread's open unit of work when there is one — its transaction commits it,
     * and its collector dispatches the events afterwards — otherwise in a transaction of its own.
     */
    private <R> R write(Write<R> work) {
        UnitOfWork joined = UNIT_OF_WORK.get();
        if (joined != null) {
            return work.apply(joined.session, joined.events);
        }
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            Transaction tx = session.beginTransaction();
            List<EventOutbox.Batch> events = new ArrayList<>(1);
            R result;
            try {
                result = work.apply(session, events);
                tx.commit();
            } catch (Throwable ex) {
                // Throwable for the same reason as in inUnitOfWork: an Error must roll this write
                // back rather than ride out on the try-with-resources close.
                rollback(tx, ex);
                throw ex;
            }
            dispatch(events);
            return result;
        }
    }

    /**
     * Runs a read on the thread's open unit of work when there is one — so it sees that unit's own
     * uncommitted writes — otherwise on a session of its own.
     */
    private <R> R read(Function<Session, R> work) {
        UnitOfWork joined = UNIT_OF_WORK.get();
        if (joined != null) {
            return work.apply(joined.session);
        }
        try (Session session = entityManager.getSessionFactory()
                                            .openSession()) {
            return work.apply(session);
        }
    }

    private static void dispatch(List<EventOutbox.Batch> events) {
        for (EventOutbox.Batch batch : events) {
            batch.dispatch();
        }
    }

    /** A store write: the session to write on, and where to collect the events it recorded. */
    @FunctionalInterface
    private interface Write<R> {

        R apply(Session session, List<EventOutbox.Batch> events);
    }

    /** The session, transaction and pending event batches of an open {@link #inUnitOfWork} block. */
    private static final class UnitOfWork {

        private final Session session;

        private final Transaction transaction;

        private final List<EventOutbox.Batch> events = new ArrayList<>();

        private UnitOfWork(Session session) {
            this.session = session;
            this.transaction = session.beginTransaction();
        }

        private void dispatch() {
            JavaEntityStore.dispatch(events);
        }
    }

    /**
     * The unit of work the current thread has open, if any. Thread-bound rather than passed around
     * because the writers that must join it — a generated repository, a calculated-field action — are
     * reached through their own objects and never see the caller's session.
     */
    private static final ThreadLocal<UnitOfWork> UNIT_OF_WORK = new ThreadLocal<>();

    /**
     * Rolls back a transaction whose work failed, keeping the original failure as the one the caller
     * sees. A rollback that itself fails is attached to it as suppressed rather than replacing it.
     */
    private static void rollback(Transaction tx, Throwable cause) {
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
