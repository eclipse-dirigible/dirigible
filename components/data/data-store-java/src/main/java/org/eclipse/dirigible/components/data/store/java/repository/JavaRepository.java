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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;

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
     * Update an existing entity instance.
     *
     * @param entity the entity to update
     * @return the updated entity
     */
    public T update(T entity) {
        return store().update(entity);
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
        return store().updateProperty(entityClass, id, property, value);
    }

    /**
     * Look up an entity by primary key.
     *
     * @param id the primary-key value
     * @return the entity, or {@code null} if not found
     */
    public T findById(Object id) {
        return store().findById(entityClass, id);
    }

    /**
     * Look up an entity by primary key.
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
     * Delete an entity instance.
     *
     * @param entity the entity to delete
     */
    public void delete(T entity) {
        store().delete(entity);
    }

    /**
     * Delete an entity by primary key.
     *
     * @param id the primary-key value
     */
    public void deleteById(Object id) {
        store().deleteById(entityClass, id);
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
