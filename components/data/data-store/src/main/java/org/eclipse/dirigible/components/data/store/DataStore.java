/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.store.config.CurrentTenantIdentifierResolverImpl;
import org.eclipse.dirigible.components.data.store.config.MultiTenantConnectionProviderImpl;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The Class ObjectStore.
 */
@Component
@Scope("singleton")
public class DataStore {

    // private ApplicationContext applicationContext;
    //
    // private static final String EMF_BEAN_NAME = "dynamicEntityManagerFactory";

    private SessionFactory sessionFactory;

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    private final MultiTenantConnectionProviderImpl connectionProvider;

    private final CurrentTenantIdentifierResolverImpl tenantIdentifierResolver;

    /** The mappings. */
    private final Map<String, String> mappings = new HashMap<>();

    /**
     * Instantiates a new object store.
     *
     * @param datasourcesManager the datasources manager
     */
    @Autowired
    public DataStore(// ApplicationContext applicationContext,
            DataSourcesManager datasourcesManager, MultiTenantConnectionProviderImpl connectionProvider,
            CurrentTenantIdentifierResolverImpl tenantIdentifierResolver) {
        // this.applicationContext = applicationContext;
        this.datasourcesManager = datasourcesManager;
        this.connectionProvider = connectionProvider;
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    /**
     * Gets the datasources manager.
     *
     * @return the datasources manager
     */
    public DataSourcesManager getDatasourcesManager() {
        return datasourcesManager;
    }

    /**
     * Adds the mapping.
     *
     * @param name the name
     * @param content the content
     */
    public void addMapping(String name, String content) {
        mappings.put(name, content);
    }

    /**
     * Removes the mapping.
     *
     * @param name the name
     */
    public void removeMapping(String name) {
        mappings.remove(name);
    }

    /**
     * Initialize.
     */
    public synchronized void recreate() {
        DataSource dataSource = datasourcesManager.getDefaultDataSource();
        Configuration configuration = new Configuration().setProperty(Environment.SHOW_SQL, "true")
                                                         .setProperty("hibernate.hbm2ddl.auto", "update")
                                                         .setProperty("hibernate.current_session_context_class",
                                                                 "org.hibernate.context.internal.ThreadLocalSessionContext");


        // em.setJpaVendorAdapter(jpaVendorAdapter());

        mappings.forEach((k, v) -> addInputStreamToConfig(configuration, k, v));

        StandardServiceRegistryBuilder serviceRegistryBuilder = new StandardServiceRegistryBuilder();
        serviceRegistryBuilder.applySetting(Environment.DATASOURCE, dataSource);
        serviceRegistryBuilder.applySetting(Environment.JAKARTA_JTA_DATASOURCE, dataSource);
        serviceRegistryBuilder.applySetting(Environment.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        serviceRegistryBuilder.applySetting(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        serviceRegistryBuilder.applySettings(configuration.getProperties());

        StandardServiceRegistry serviceRegistry = serviceRegistryBuilder.build();

        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    }

    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            recreate();
        }
        return sessionFactory;
    }

    /**
     * Adds the input stream to config.
     *
     * @param configuration the configuration
     * @param key the key
     * @param value the value
     */
    private void addInputStreamToConfig(Configuration configuration, String key, String value) {
        try (InputStream inputStream = IOUtils.toInputStream(EntityTransformer.fromEntity(value), StandardCharsets.UTF_8)) {
            configuration.addInputStream(inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to add input stream to configuration for [" + key + "]: [" + value + "]", ex);
        }
    }

    /**
     * Save.
     *
     * @param type the type
     * @param json the json
     * @return the identifier
     */
    public Object save(String type, String json) {
        Map object = JsonHelper.fromJson(json, Map.class);
        return save(type, object);
    }

    /**
     * Save.
     *
     * @param type the type
     * @param object the object
     * @return the identifier
     */
    public Object save(String type, Map object) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            Object id = session.save(type, object);
            transaction.commit();
            return id;
        }
    }

    /**
     * Save or update.
     *
     * @param type the type
     * @param json the json
     */
    public void upsert(String type, String json) {
        Map object = JsonHelper.fromJson(json, Map.class);
        upsert(type, object);
    }

    /**
     * Save or update.
     *
     * @param type the type
     * @param object the object
     */
    public void upsert(String type, Map object) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(type, object);
            transaction.commit();
        }
    }

    /**
     * Update.
     *
     * @param type the type
     * @param json the json
     */
    public void update(String type, String json) {
        Map object = JsonHelper.fromJson(json, Map.class);
        update(type, json);
    }

    /**
     * Update.
     *
     * @param type the type
     * @param object the object
     */
    public void update(String type, Map object) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.update(type, object);
            transaction.commit();
        }
    }

    /**
     * Delete.
     *
     * @param type the type
     * @param id the id
     */
    public void delete(String type, Serializable id) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            Object object = get(type, id);
            session.delete(type, object);
            transaction.commit();
        }
    }

    /**
     * Gets the.
     *
     * @param type the type
     * @param id the id
     * @return the map
     */
    public Map get(String type, Serializable id) {
        try (Session session = getSessionFactory().openSession()) {
            return (Map) session.get(type, id);
        }
    }

    /**
     * Contains.
     *
     * @param type the type
     * @param json the json
     * @return true, if successful
     */
    public boolean contains(String type, String json) {
        try (Session session = getSessionFactory().openSession()) {
            Map object = JsonHelper.fromJson(json, Map.class);
            return session.contains(type, object);
        }
    }

    /**
     * List.
     *
     * @param type the type
     * @return the list
     */
    public List<Map> list(String type) {
        try (Session session = getSessionFactory().openSession()) {

            Query<Map> query = session.createQuery("from " + type + " c", Map.class);
            return query.getResultList();
        }
    }

    /**
     * Count.
     *
     * @param type the type
     * @return the count
     */
    public long count(String type) {
        try (Session session = getSessionFactory().openSession()) {
            Query<Map> query = session.createQuery("from " + type + " c", Map.class);
            return query.getResultCount();
        }
    }

    /**
     * List with filter.
     *
     * @param type the type
     * @param options the options
     * @return the list
     */
    public List<Map> list(String type, QueryOptions options) {
        try (Session session = getSessionFactory().openSession()) {
            List<Map> matchingItems = DynamicQueryFilter.list(session, type, options);
            return matchingItems;
        }
    }

    /**
     * List with filter.
     *
     * @param type the type
     * @param options the options
     * @return the list
     */
    public List<Map> list(String type, String options) {
        if (options != null) {
            QueryOptions queryOptions = JsonHelper.fromJson(options, QueryOptions.class);
            return list(type, queryOptions);
        }
        return list(type);
    }

    /**
     * Count with filter.
     *
     * @param type the type
     * @param options the options
     * @return the count
     */
    public long count(String type, QueryOptions options) {
        try (Session session = getSessionFactory().openSession()) {
            long count = DynamicQueryFilter.count(session, type, options);
            return count;
        }
    }

    /**
     * Count with filter.
     *
     * @param type the type
     * @param options the options
     * @return the count
     */
    public long count(String type, String options) {
        if (options != null) {
            QueryOptions queryOptions = JsonHelper.fromJson(options, QueryOptions.class);
            return count(type, queryOptions);
        }
        return count(type);
    }

    /**
     * Find by example.
     *
     * @param type the type
     * @param json the json
     * @param limit the limit
     * @param offset the offset
     * @return the list
     */
    public List<Map> findByExample(String type, String json, int limit, int offset) {
        Map object = JsonHelper.fromJson(json, Map.class);
        List result = DynamicCriteriaFinder.findByExampleDynamic(getSessionFactory(), type, object, limit, offset);
        return result;
    }

    /**
     * Query.
     *
     * @param query the query
     * @param limit the limit
     * @param offset the offset
     * @return the list
     */
    public List<Map> query(String query, int limit, int offset) {
        try (Session session = getSessionFactory().openSession()) {
            Query<Map> queryObject = session.createQuery(query, Map.class);
            if (limit > 0) {
                queryObject.setMaxResults(limit);
            }
            if (offset >= 0) {
                queryObject.setFirstResult(offset);
            }
            return queryObject.getResultList();
        }
    }

    /**
     * Query.
     *
     * @param query the query
     * @return the list
     */
    public List<Map> queryNative(String query) {
        try (Session session = getSessionFactory().openSession()) {
            return session.createNativeQuery(query, Map.class)
                          .list();
        }
    }

}
