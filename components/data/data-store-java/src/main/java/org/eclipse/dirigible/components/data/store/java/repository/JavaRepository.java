/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.sdk.db.ConcurrentWriteException;
import org.eclipse.dirigible.sdk.db.DeleteRestrictionException;
import org.eclipse.dirigible.sdk.db.TargetAgreementRule;
import org.eclipse.dirigible.sdk.db.TargetDeleteRule;
import org.eclipse.dirigible.sdk.db.ValidationException;
import org.eclipse.dirigible.sdk.extensions.Extensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Typed CRUD facade for a single Dirigible {@code @Entity} type. Client code subclasses this,
 * supplying the entity {@link Class} via the protected constructor, and exposes a clean typed API
 * to controllers without ever touching {@link JavaEntityStore} directly.
 *
 * <p>
 * Usage sketch (client code; tags are shown as literals to keep this javadoc valid):
 * {@code @Repository class CountryRepository extends JavaRepository<Country> { ... }} and
 * {@code @Inject private CountryRepository countries;} inside a {@code @Controller}.
 *
 * <p>
 * The class is intentionally a thin wrapper: it forwards each call to the singleton
 * {@code JavaEntityStore}, resolved lazily through {@link BeanProvider} so the client class doesn't
 * need to be Spring-scanned. Repositories are stateless and reused across requests.
 *
 * @param <T> the entity type managed by this repository
 */
public abstract class JavaRepository<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaRepository.class);

    /**
     * The records being deleted on this thread, keyed by entity class and id - so a cascade that comes
     * back round to one of them stops there instead of recursing for ever.
     */
    private static final ThreadLocal<Set<String>> DELETING = ThreadLocal.withInitial(HashSet::new);

    private final Class<T> entityClass;

    /**
     * Subclass-only constructor that pins the entity type this repository operates on.
     *
     * @param entityClass the entity class; must not be {@code null}
     */
    protected JavaRepository(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass must not be null");
        }
        this.entityClass = entityClass;
    }

    /**
     * @return the entity {@link Class} this repository operates on
     */
    public final Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * Insert a new entity instance.
     *
     * @param entity the entity to insert
     * @return the saved entity (with any generated identifier populated)
     */
    public T save(T entity) {
        return store().save(entity);
    }

    /**
     * Insert a new entity instance and publish it on the given topic — atomically. The event is
     * recorded in the tenant's event outbox inside the insert's own transaction, so the row and its
     * event commit together; the broker sees the event only once the row is durable, and a broker that
     * refuses it leaves the entry for the relay to retry instead of failing this call.
     *
     * @param entity the entity to insert
     * @param eventTopic the topic to publish the saved entity on
     * @return the saved entity (with any generated identifier populated)
     */
    public T save(T entity, String eventTopic) {
        return store().save(entity, eventTopic);
    }

    /**
     * Insert a new entity instance, publishing it on the given topic plus any further events the write
     * emits about other rows — e.g. a create-from announcing its source's completed transition only
     * once the document that transition was about exists. All of them share the insert's transaction.
     *
     * @param entity the entity to insert
     * @param eventTopic the topic to publish the saved entity on; {@code null} publishes nothing
     * @param additionalEvents further events to record with the same write
     * @return the saved entity (with any generated identifier populated)
     */
    public T save(T entity, String eventTopic, List<DomainEvent> additionalEvents) {
        return store().save(entity, eventTopic, additionalEvents);
    }

    /**
     * Update an existing entity instance.
     *
     * @param entity the entity to update
     * @return the updated entity
     */
    public T update(T entity) {
        requireTargetAgreement(entity);
        return store().update(entity);
    }

    /**
     * Update an existing entity instance and publish it on the given topic — atomically, see
     * {@link #save(Object, String)}.
     *
     * @param entity the entity to update
     * @param eventTopic the topic to publish the updated entity on
     * @return the updated entity
     */
    public T update(T entity, String eventTopic) {
        requireTargetAgreement(entity);
        return store().update(entity, eventTopic);
    }

    /**
     * Update an existing entity instance, publishing it on the given topic plus any further events the
     * write emits about other rows — an aggregate's {@code "-rekeyed"} notice about the tuple the row
     * just left. All of them share the update's transaction.
     *
     * @param entity the entity to update
     * @param eventTopic the topic to publish the updated entity on
     * @param additionalEvents further events to record with the same write
     * @return the updated entity
     */
    public T update(T entity, String eventTopic, List<DomainEvent> additionalEvents) {
        requireTargetAgreement(entity);
        return store().update(entity, eventTopic, additionalEvents);
    }

    /**
     * Update a single property of one row, touching nothing else — the workflow/system write-back
     * primitive (the process trigger persisting {@code ProcessId}, a minted document number). Unlike
     * {@link #update(Object)}, which writes every column from the caller's snapshot and can silently
     * revert a concurrent write (a document's recalculated totals, a workflow status), this statement
     * carries only the named column. No validations, no events, no translation overlay — reserve it for
     * system columns; user data goes through the generated repository's normal write path.
     *
     * @param id the primary-key value
     * @param property the entity property to set (a plain identifier)
     * @param value the new value
     * @return the number of updated rows ({@code 0} when the id does not exist)
     */
    public int updateProperty(Object id, String property, Object value) {
        requireTargetAgreement(id, Map.of(property, value == null ? NULL : value));
        return store().updateProperty(entityClass, id, property, value);
    }

    /**
     * Update several properties of one row in a single atomic statement, touching nothing else — the
     * multi-column sibling of {@link #updateProperty(Object, String, Object)} for workflow/system
     * write-backs that persist more than one field (e.g. a user task's reviewed edits). Every column
     * not named in {@code values} is untouched, so a concurrent write to an unrelated column cannot be
     * reverted. No validations, no events, no translation overlay — reserve it for system writes; user
     * data goes through the generated repository's normal write path.
     *
     * @param id the primary-key value
     * @param values the properties to set (plain identifiers) with their new values
     * @return the number of updated rows ({@code 0} when the id does not exist or {@code values} is
     *         empty)
     */
    public int updateProperties(Object id, Map<String, Object> values) {
        // Deliberately NOT delegating to the event-carrying overload: a generated repository overrides
        // both, and its recalculation path reaches the plain write through `super` precisely to bypass
        // the semantics it adds. Re-dispatching here would drag them back in.
        requireTargetAgreement(id, values);
        return store().updateProperties(entityClass, id, values, null);
    }

    /**
     * Targeted multi-column write that publishes the resulting row on the given topic — atomically, see
     * {@link #save(Object, String)}. This is the derived-column path (roll-up totals, keyed
     * aggregates): every column not named is left alone, and the event still fires so downstream
     * reactions keep cascading.
     *
     * <p>
     * A generated repository that adds semantics to targeted writes (declarative checks, a stored
     * display name) overrides <em>this</em> method, so both {@link #updateProperties(Object, Map)} and
     * the event-carrying path go through it.
     *
     * @param id the primary-key value
     * @param values the properties to set (plain identifiers) with their new values
     * @param eventTopic the topic to publish the resulting row on; {@code null} publishes nothing
     * @return the number of updated rows ({@code 0} when the id does not exist or {@code values} is
     *         empty)
     */
    public int updateProperties(Object id, Map<String, Object> values, String eventTopic) {
        requireTargetAgreement(id, values);
        return store().updateProperties(entityClass, id, values, eventTopic);
    }

    /**
     * Targeted multi-column write publishing the resulting row on the given topic plus any further
     * events the write emits about other rows — an aggregate's {@code "-rekeyed"} notice about the
     * tuple the row just left. All of them share the mutation's transaction and are recorded only when
     * the row actually existed to be written.
     *
     * @param id the primary-key value
     * @param values the properties to set (plain identifiers) with their new values
     * @param eventTopic the topic to publish the resulting row on; {@code null} publishes nothing
     * @param additionalEvents further events to record with the same write
     * @return the number of updated rows ({@code 0} when the id does not exist or {@code values} is
     *         empty)
     */
    public int updateProperties(Object id, Map<String, Object> values, String eventTopic, List<DomainEvent> additionalEvents) {
        requireTargetAgreement(id, values);
        return store().updateProperties(entityClass, id, values, eventTopic, additionalEvents);
    }

    /**
     * Locks this entity's row until the current unit of work ends, so no other writer can change it
     * between this unit's read of the row and its write - see
     * {@link JavaEntityStore#lockForUpdate(Class, Object)}. Call it inside {@link UnitOfWork#run}.
     *
     * @param id the primary-key value
     * @return {@code true} when the row exists and is now locked, {@code false} when there is no such
     *         row
     */
    public boolean lockForUpdate(Object id) {
        return store().lockForUpdate(entityClass, id);
    }

    /**
     * Look up an entity by primary key. An absent id is an ordinary outcome — a dangling foreign key an
     * event handler should skip, a path parameter a controller should answer {@code 404} for — so it
     * reads back as {@code null} rather than as a thrown exception. Callers that require the row to
     * exist use {@link #findOne(Object)} and choose their own failure, e.g.
     * {@code findOne(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND))}.
     *
     * @param id the primary-key value
     * @return the entity, or {@code null} if not found
     */
    public T findById(Object id) {
        return store().findById(entityClass, id);
    }

    /**
     * Look up an entity by primary key — the {@link Optional} variant of {@link #findById(Object)}, for
     * callers that chain the absent case (an {@code orElseThrow} carrying their own status, an
     * {@code orElseGet} default).
     *
     * @param id the primary-key value
     * @return an optional carrying the entity if it exists
     */
    public Optional<T> findOne(Object id) {
        return store().findOne(entityClass, id);
    }

    /**
     * @return every entity of this repository's type
     */
    public List<T> findAll() {
        return store().findAll(entityClass);
    }

    /**
     * Paginated variant of {@link #findAll()}.
     *
     * @param limit max rows to return
     * @param offset rows to skip
     * @return the requested page
     */
    public List<T> findAll(int limit, int offset) {
        return store().findAll(entityClass, limit, offset);
    }

    /**
     * The same page in a declared order - the generated list endpoint's default row order
     * ({@code orderBy:}, dirigible #7727). Ordering in the database is what keeps paging coherent: an
     * unordered query is free to return a different slice on every call.
     *
     * @param limit max rows to return; non-positive means unlimited
     * @param offset rows to skip; non-positive means none
     * @param orderBy the {@code order by} fragment over the alias {@code e}, without the keywords
     * @return the requested page
     */
    public List<T> findAll(int limit, int offset, String orderBy) {
        return store().findAll(entityClass, limit, offset, orderBy);
    }

    /**
     * Find every entity matching a typed {@link Criteria} — the type-safe alternative to
     * {@link #query(String, Map)}. Conditions are combined with {@code AND}; values are bound as
     * parameters.
     *
     * @param criteria the query criteria
     * @return the matching entities
     */
    public List<T> findAll(Criteria criteria) {
        return store().findAll(entityClass, criteria);
    }

    /**
     * Delete an entity instance, applying the delete rules of the records that reference it - see
     * {@link #requireDeletable(Object)}.
     *
     * @param entity the entity to delete
     */
    public void delete(T entity) {
        deleteApplyingRules(store().idOf(entity), () -> store().delete(entity));
    }

    /**
     * Delete an entity instance and publish it on the given topic — atomically, see
     * {@link #save(Object, String)}. The payload is the row as it was read inside the deleting
     * transaction, so a caller holding a partial snapshot still announces the whole row. The delete
     * rules of the records that reference it apply, see {@link #requireDeletable(Object)}.
     *
     * @param entity the entity to delete
     * @param eventTopic the topic to publish the deleted row on
     */
    public void delete(T entity, String eventTopic) {
        deleteApplyingRules(store().idOf(entity), () -> store().delete(entity, eventTopic));
    }

    /**
     * Delete an entity by primary key, applying the delete rules of the records that reference it - see
     * {@link #requireDeletable(Object)}.
     *
     * @param id the primary-key value
     */
    public void deleteById(Object id) {
        deleteApplyingRules(id, () -> store().deleteById(entityClass, id));
    }

    /**
     * Delete an entity by primary key and publish the deleted row on the given topic — atomically, see
     * {@link #save(Object, String)}. The payload is the row as it was read inside the deleting
     * transaction. The delete rules of the records that reference it apply, see
     * {@link #requireDeletable(Object)}.
     *
     * @param id the primary-key value
     * @param eventTopic the topic to publish the deleted row on
     */
    public void deleteById(Object id, String eventTopic) {
        deleteApplyingRules(id, () -> store().deleteById(entityClass, id, eventTopic));
    }

    /**
     * Refuses the delete of a record while a record referencing it through a relation that RESTRICTS
     * its delete still exists (intent {@code whenTargetDeleted: restrict}, the default for a to-one,
     * dirigible #7547). Every delete runs it before anything is written; a generated repository whose
     * own delete writes first - a composition master deleting its children - calls it at the very top,
     * so the refusal arrives before any of those writes (the history trail is not part of the
     * transaction, and would otherwise record deletes that were rolled back).
     *
     * <p>
     * The rules come from the REFERENCING side: this entity's own generation cannot know which entities
     * point at it - another model may - so each referencing repository contributes a
     * {@link TargetDeleteRule}, and this asks all of them. A read-then-act guard, like every reference
     * check the platform generates: a referencing row inserted between this count and the delete is not
     * seen.
     *
     * @param id the primary key of the record about to be deleted
     * @throws DeleteRestrictionException while a restricting reference to it exists
     */
    protected void requireDeletable(Object id) {
        requireDeletable(id, deleteRules());
    }

    private void requireDeletable(Object id, List<TargetDeleteRule> rules) {
        if (id == null) {
            return;
        }
        String target = entityClass.getName();
        for (TargetDeleteRule rule : rules) {
            int referencing = rule.countRestricting(target, id);
            if (referencing > 0) {
                throw new DeleteRestrictionException("This " + label() + " is referenced by " + referencing + " " + rule.referencingLabel()
                        + " record(s) and cannot be deleted");
            }
        }
    }

    /**
     * The delete itself, with the referencing records' rules around it: first every restriction is
     * checked, then - in ONE transaction with the delete - every rule releases its references (a
     * {@code nullify} clears the foreign key, a {@code cascade} deletes the referencing record through
     * its own repository, its own rules applying in turn). A cascade that comes back round to a record
     * already being deleted on this thread stops there, so a cycle of cascading relations terminates.
     *
     * <p>
     * A cascade loads the referencing rows before it deletes them, and another writer may replace them
     * in between - a posting handler rewriting a journal entry's lines on its own listener thread - so
     * the delete finds a row it loaded already gone. The whole transaction is then rolled back and,
     * when this delete owns it (no unit of work was open around it), run ONCE more from a fresh read:
     * the other writer has committed by then, so the cascade sees the rows as they are now. A second
     * collision, or one inside a caller's own unit of work - whose earlier writes this delete cannot
     * repeat - reaches the caller as the {@link ConcurrentWriteException} itself, a 409 (dirigible
     * #7716).
     *
     * @param id the primary key of the record to delete
     * @param delete the store call that deletes it
     */
    private void deleteApplyingRules(Object id, Runnable delete) {
        List<TargetDeleteRule> rules = deleteRules();
        if (id == null || rules.isEmpty()) {
            delete.run();
            return;
        }
        String key = entityClass.getName() + "#" + id;
        Set<String> deleting = DELETING.get();
        if (!deleting.add(key)) {
            return; // already being deleted further up this cascade
        }
        try {
            if (store().isInUnitOfWork()) {
                releaseAndDelete(id, rules, delete);
                return;
            }
            try {
                releaseAndDelete(id, rules, delete);
            } catch (ConcurrentWriteException collision) {
                LOGGER.debug(
                        "Deleting [{}] collided with a concurrent write to the records referencing it - retrying once from a fresh read",
                        key, collision);
                releaseAndDelete(id, rules, delete);
            }
        } finally {
            deleting.remove(key);
            if (deleting.isEmpty()) {
                DELETING.remove();
            }
        }
    }

    /**
     * One attempt at the delete: the restrictions checked, then the references released and the record
     * deleted in ONE transaction.
     */
    private void releaseAndDelete(Object id, List<TargetDeleteRule> rules, Runnable delete) {
        requireDeletable(id, rules);
        String target = entityClass.getName();
        UnitOfWork.run(() -> {
            rules.forEach(rule -> rule.release(target, id));
            delete.run();
        });
    }

    /**
     * Every contributed delete rule. Outside a running platform there is no bean container, so no
     * contribution either.
     */
    private static List<TargetDeleteRule> deleteRules() {
        return BeanProvider.isInitialzed() ? Extensions.find(TargetDeleteRule.class) : List.of();
    }

    /**
     * Refuses a full-row update that changes a property a referencing record relies on (intent
     * {@code checks: agree} on a junction owned by another model, dirigible #7701) - see
     * {@link TargetAgreementRule}. The stored row is read only when some contribution relies on a
     * property of this entity, and a contribution is asked only about a property the update actually
     * changes, so an ordinary save that carries the stored values back costs no junction query. The
     * stored row is read through the store, not through a subclass's read: a multilingual repository
     * overlays a translation there, and a translated value compared with the stored one reads as a
     * change.
     *
     * <p>
     * A read-then-act guard, like every other reference check the platform generates: a referencing row
     * inserted between this lookup and the write is not seen.
     *
     * @param entity the entity about to be written
     * @throws ValidationException carrying the contribution's message when the update is refused
     */
    private void requireTargetAgreement(T entity) {
        List<TargetAgreementRule> rules = agreementRules();
        if (rules.isEmpty() || entity == null) {
            return;
        }
        String target = entityClass.getName();
        Object id = null;
        T stored = null;
        for (TargetAgreementRule rule : rules) {
            for (String property : rule.agreedProperties(target)) {
                if (stored == null) {
                    id = store().idOf(entity);
                    stored = id == null ? null : store().findById(entityClass, id);
                    if (stored == null) {
                        return; // no stored row - nothing references it yet
                    }
                }
                Field field = fieldOf(property);
                if (field != null && !same(read(field, stored), read(field, entity))) {
                    refuseIfRelied(rule, target, id, property);
                }
            }
        }
    }

    /**
     * The targeted-write twin of {@link #requireTargetAgreement(Object)}: only the named columns
     * change, so only those are compared with the stored row.
     *
     * @param id the primary key of the row about to be written
     * @param values the properties the write sets
     * @throws ValidationException carrying the contribution's message when the write is refused
     */
    private void requireTargetAgreement(Object id, Map<String, Object> values) {
        List<TargetAgreementRule> rules = agreementRules();
        if (rules.isEmpty() || id == null || values == null || values.isEmpty()) {
            return;
        }
        String target = entityClass.getName();
        T stored = null;
        for (TargetAgreementRule rule : rules) {
            for (String property : rule.agreedProperties(target)) {
                if (!values.containsKey(property)) {
                    continue;
                }
                if (stored == null) {
                    stored = store().findById(entityClass, id);
                    if (stored == null) {
                        return; // no stored row - the write updates nothing
                    }
                }
                Field field = fieldOf(property);
                Object value = values.get(property);
                if (field != null && !same(read(field, stored), value == NULL ? null : value)) {
                    refuseIfRelied(rule, target, id, property);
                }
            }
        }
    }

    private static void refuseIfRelied(TargetAgreementRule rule, String target, Object id, String property) {
        String refusal = rule.refusal(target, id, property);
        if (refusal != null) {
            throw new ValidationException(refusal);
        }
    }

    /** Stands for a {@code null} value in an immutable map - {@code Map.of} refuses null values. */
    private static final Object NULL = new Object();

    /**
     * Two values of one property compared as the column holds them: numbers by value, so a decimal read
     * back with another scale, or an id bound as a {@code Long} against an {@code Integer} field, is
     * not a change.
     */
    private static boolean same(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())) == 0;
        }
        if (left instanceof Number || right instanceof Number) {
            return Objects.equals(Objects.toString(left, null), Objects.toString(right, null));
        }
        return Objects.equals(left, right);
    }

    /** The entity's field backing the property, or null when it has none. */
    private Field fieldOf(String property) {
        for (Class<?> type = entityClass; type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(property);
                field.trySetAccessible();
                return field;
            } catch (NoSuchFieldException e) {
                // keep looking up the hierarchy
            }
        }
        return null;
    }

    private static Object read(Field field, Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read " + field.getName() + " of " + entity.getClass()
                                                                                              .getName(),
                    e);
        }
    }

    /**
     * Every contributed agreement rule. Outside a running platform there is no bean container, so no
     * contribution either.
     */
    private static List<TargetAgreementRule> agreementRules() {
        return BeanProvider.isInitialzed() ? Extensions.find(TargetAgreementRule.class) : List.of();
    }

    /** This entity as a refusal names it - {@code ExpenseCategoryEntity} reads "Expense Category". */
    private String label() {
        String name = entityClass.getSimpleName();
        if (name.endsWith("Entity") && name.length() > "Entity".length()) {
            name = name.substring(0, name.length() - "Entity".length());
        }
        return name.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
    }

    /**
     * Count the entities matching a typed {@link Criteria}, in the database - without loading them.
     *
     * @param criteria the query criteria
     * @return the number of matching entities
     */
    public long count(Criteria criteria) {
        return store().count(entityClass, criteria);
    }

    /**
     * @return the number of stored entities of this repository's type
     */
    public long count() {
        return store().count(entityClass);
    }

    /**
     * Execute a named-parameter HQL/JPQL query bound to this repository's entity.
     *
     * @param hql the query string
     * @param parameters named parameter bindings
     * @return the query results
     */
    public List<T> query(String hql, Map<String, Object> parameters) {
        return store().query(entityClass, hql, parameters);
    }

    /**
     * The shared {@link JavaEntityStore} bean. Fetched lazily so the repository (a client bean built by
     * the engine's component container, not a Spring-scanned bean) can reach the platform store.
     *
     * @return the platform {@link JavaEntityStore} singleton
     */
    protected JavaEntityStore store() {
        return BeanProvider.getBean(JavaEntityStore.class);
    }
}
