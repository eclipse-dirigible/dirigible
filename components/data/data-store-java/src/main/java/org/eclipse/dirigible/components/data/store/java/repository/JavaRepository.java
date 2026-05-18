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
 * to controllers without ever touching {@link JavaEntityStore} directly:
 *
 * <pre>
 * {
 *     &#64;code
 *     &#64;Repository
 *     public class CountryRepository extends JavaRepository<Country> {
 *         public CountryRepository() {
 *             super(Country.class);
 *         }
 *     }
 *
 *     &#64;Controller
 *     public class CountryController {
 *         @Inject
 *         private CountryRepository countries;
 *
 *         &#64;Get("/list")
 *         public List<Country> list() {
 *             return countries.findAll();
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>
 * The class is intentionally a thin wrapper: it forwards each call to the singleton
 * {@code JavaEntityStore}, resolved lazily through {@link BeanProvider} so the client class doesn't
 * need to be Spring-scanned. Repositories are stateless and reused across requests.
 */
public abstract class JavaRepository<T> {

    private final Class<T> entityClass;

    protected JavaRepository(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass must not be null");
        }
        this.entityClass = entityClass;
    }

    /** The entity {@link Class} this repository operates on. */
    public final Class<T> getEntityClass() {
        return entityClass;
    }

    public T save(T entity) {
        return store().save(entity);
    }

    public T update(T entity) {
        return store().update(entity);
    }

    public T findById(Object id) {
        return store().findById(entityClass, id);
    }

    public Optional<T> findOne(Object id) {
        return store().findOne(entityClass, id);
    }

    public List<T> findAll() {
        return store().findAll(entityClass);
    }

    public List<T> findAll(int limit, int offset) {
        return store().findAll(entityClass, limit, offset);
    }

    public void delete(T entity) {
        store().delete(entity);
    }

    public void deleteById(Object id) {
        store().deleteById(entityClass, id);
    }

    public long count() {
        return store().count(entityClass);
    }

    /** Execute a named-parameter HQL/JPQL query bound to this repository's entity. */
    public List<T> query(String hql, Map<String, Object> parameters) {
        return store().query(entityClass, hql, parameters);
    }

    /**
     * The shared {@link JavaEntityStore} bean. Fetched lazily so the repository can be instantiated via
     * a no-arg constructor by {@code RepositoryClassConsumer} (the client class is not in Spring's
     * component scan).
     */
    protected JavaEntityStore store() {
        return BeanProvider.getBean(JavaEntityStore.class);
    }
}
