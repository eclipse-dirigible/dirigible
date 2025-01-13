/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.io.IOException;
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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * The Class ObjectStore.
 */
@Component
public class DataStore {

    /** The session factory. */
    private SessionFactory sessionFactory;

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    /** The data source. */
    private DataSource dataSource;

    /** The mappings. */
    private final Map<String, String> mappings = new HashMap<>();

    /**
     * Instantiates a new object store.
     *
     * @param datasourcesManager the datasources manager
     */
    @Autowired
    public DataStore(DataSourcesManager datasourcesManager) {
        this.datasourcesManager = datasourcesManager;
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
     * Gets the data source.
     *
     * @return the data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets the data source.
     *
     * @param dataSource the new data source
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
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
    public synchronized void initialize() {
        if (this.dataSource == null) {
            this.dataSource = datasourcesManager.getDefaultDataSource();
        }
        Configuration configuration = new Configuration().setProperty(Environment.SHOW_SQL, "true")
                                                         .setProperty("hibernate.hbm2ddl.auto", "update")
                                                         .setProperty("hibernate.current_session_context_class",
                                                                 "org.hibernate.context.internal.ThreadLocalSessionContext");

        mappings.forEach((k, v) -> addInputStreamToConfig(configuration, k, v));

        StandardServiceRegistryBuilder serviceRegistryBuilder = new StandardServiceRegistryBuilder();
        serviceRegistryBuilder.applySetting(Environment.DATASOURCE, getDataSource());
        serviceRegistryBuilder.applySetting(Environment.JAKARTA_JTA_DATASOURCE, getDataSource());
        serviceRegistryBuilder.applySettings(configuration.getProperties());

        StandardServiceRegistry serviceRegistry = serviceRegistryBuilder.build();

        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    }

    /**
     * Adds the input stream to config.
     *
     * @param configuration the configuration
     * @param key the key
     * @param value the value
     */
    private void addInputStreamToConfig(Configuration configuration, String key, String value) {
        try (InputStream inputStream = IOUtils.toInputStream(value, StandardCharsets.UTF_8)) {
            configuration.addInputStream(inputStream);
        } catch (RuntimeException | IOException ex) {
            throw new IllegalStateException("Failed to add input stream to configuration for [" + key + "]: [" + value + "]", ex);
        }
    }

    /**
     * Save.
     *
     * @param type the type
     * @param json the json
     */
    public void save(String type, String json) {
        save(type, json, getDataSource());
    }

    /**
     * Save.
     *
     * @param type the type
     * @param json the json
     * @param datasource the datasource
     */
    public void save(String type, String json, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Map object = JsonHelper.fromJson(json, Map.class);
            session.save(type, object);
            transaction.commit();
        }
    }

    /**
     * Save.
     *
     * @param type the type
     * @param object the object
     */
    public void save(String type, Map object) {
        save(type, object, getDataSource());
    }

    /**
     * Save.
     *
     * @param type the type
     * @param object the object
     * @param datasource the datasource
     */
    public void save(String type, Map object, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(type, object);
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
        delete(type, id, getDataSource());
    }

    /**
     * Delete.
     *
     * @param type the type
     * @param id the id
     * @param datasource the datasource
     */
    public void delete(String type, Serializable id, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
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
        return get(type, id, getDataSource());
    }

    /**
     * Gets the.
     *
     * @param type the type
     * @param id the id
     * @param datasource the datasource
     * @return the map
     */
    public Map get(String type, Serializable id, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
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
        return contains(type, json, getDataSource());
    }

    /**
     * Contains.
     *
     * @param type the type
     * @param json the json
     * @param datasource the datasource
     * @return true, if successful
     */
    public boolean contains(String type, String json, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
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
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {

            if (!mappings.containsKey(type)) {
                throw new IllegalArgumentException("There is not entity of type " + type);
            }
            Query query = entityManager.createQuery("from " + type + " c");
            return query.getResultList();
        }
    }

    /**
     * Query.
     *
     * @param query the query
     * @return the list
     */
    public List<Map> query(String query) {
        return query(query, getDataSource());
    }

    /**
     * Query.
     *
     * @param query the query
     * @param datasource the datasource
     * @return the list
     */
    public List<Map> query(String query, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            return session.createNativeQuery(query)
                          .list();
        }
    }

    // /**
    // * List.
    // *
    // * @param type the type
    // * @return the string
    // */
    // public String list(String type) {
    // try (Session session = sessionFactory.openSession()) {
    // List list = session.createSQLQuery("SELECT * FROM " + type).list();
    // return JsonHelper.toJson(list);
    // }
    // }

}


