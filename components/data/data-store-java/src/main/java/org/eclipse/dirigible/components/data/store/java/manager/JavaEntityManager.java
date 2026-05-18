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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.store.hbm.HbmXmlDescriptor;
import org.eclipse.dirigible.components.data.store.java.hbm.JavaEntityToHbmMapper;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Owns the dynamic Hibernate {@link SessionFactory} that backs Java entities declared via
 * Dirigible's {@code @Entity} annotation.
 *
 * <p>
 * Whenever an entity class enters or leaves the runtime (via {@code EntityClassConsumer}), the
 * factory is rebuilt with the current set of HBM mappings. The rebuild is synchronized; only one
 * thread can be reconfiguring at any time. Reads against {@link #getSessionFactory()} block briefly
 * during a rebuild, which is acceptable because rebuilds run only on the synchronization thread and
 * are bounded by the number of registered entities.
 *
 * <p>
 * Schema management is delegated to Hibernate's {@code hbm2ddl.auto = update} — the same default
 * the TypeScript {@code DataStore} uses: tables and columns are created or extended, never dropped,
 * so removing an entity file from the registry does not lose data.
 */
@Component
public class JavaEntityManager implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaEntityManager.class);

    private final DataSourcesManager dataSourcesManager;

    /** Current registered entities, keyed by {@code <project>::<fqn>}. */
    private final Map<String, RegisteredEntity> registered = new LinkedHashMap<>();

    private final ReentrantLock rebuildLock = new ReentrantLock();

    private volatile SessionFactory sessionFactory;

    @Autowired
    public JavaEntityManager(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    /**
     * Register (or re-register) an entity class. Triggers a {@link SessionFactory} rebuild.
     *
     * @return the resolved {@link RegisteredEntity} metadata after the rebuild
     */
    public RegisteredEntity register(String key, Class<?> entityClass) {
        rebuildLock.lock();
        try {
            JavaEntityToHbmMapper.Result mapped = JavaEntityToHbmMapper.map(key, entityClass);
            registered.put(key, mapped.registered());
            rebuildSessionFactory();
            LOGGER.info("Registered Java entity [{}] → table [{}]", mapped.registered()
                                                                          .entityName(),
                    mapped.registered()
                          .tableName());
            return mapped.registered();
        } finally {
            rebuildLock.unlock();
        }
    }

    /** Drop an entity from the runtime registry. The table itself is left in place. */
    public Optional<RegisteredEntity> unregister(String key) {
        rebuildLock.lock();
        try {
            RegisteredEntity removed = registered.remove(key);
            if (removed != null) {
                rebuildSessionFactory();
                LOGGER.info("Unregistered Java entity [{}] (table [{}] kept in place)", removed.entityName(), removed.tableName());
            }
            return Optional.ofNullable(removed);
        } finally {
            rebuildLock.unlock();
        }
    }

    /** Look up by registry key. */
    public Optional<RegisteredEntity> find(String key) {
        return Optional.ofNullable(registered.get(key));
    }

    /**
     * Look up by entity {@link Class} — finds the registered entry whose
     * {@link RegisteredEntity#entityClass()} matches by identity, falling back to FQN equality so a
     * class loaded in a previous generation of the {@code ClientClassLoader} still resolves to its
     * current registration.
     */
    public Optional<RegisteredEntity> findForClass(Class<?> entityClass) {
        for (RegisteredEntity r : registered.values()) {
            if (r.entityClass() == entityClass) {
                return Optional.of(r);
            }
        }
        for (RegisteredEntity r : registered.values()) {
            if (r.entityClass()
                 .getName()
                 .equals(entityClass.getName())) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** Get the current {@link SessionFactory}, lazily creating an empty one if needed. */
    public SessionFactory getSessionFactory() {
        SessionFactory sf = sessionFactory;
        if (sf == null) {
            rebuildLock.lock();
            try {
                if (sessionFactory == null) {
                    rebuildSessionFactory();
                }
                sf = sessionFactory;
            } finally {
                rebuildLock.unlock();
            }
        }
        return sf;
    }

    public int size() {
        return registered.size();
    }

    private void rebuildSessionFactory() {
        // Capture for close-after-swap; Hibernate sessions in flight can finish against the old
        // factory before it's released — but for our hot-reload use case we don't have long-lived
        // open sessions, so closing here is safe.
        SessionFactory old = sessionFactory;

        Configuration configuration = new Configuration().setProperty(Environment.HBM2DDL_AUTO, "update")
                                                         .setProperty(Environment.SHOW_SQL, "false");

        for (RegisteredEntity entity : registered.values()) {
            HbmXmlDescriptor.HbmIdDescriptor idDesc = new HbmXmlDescriptor.HbmIdDescriptor(entity.idField()
                                                                                                 .getName(),
                    entity.idColumn(), hibernateTypeOf(entity.idField()
                                                             .getType()),
                    "native");
            // Re-map fresh — we don't want to leak Java reflection objects into the Hibernate
            // metadata layer. JavaEntityToHbmMapper.map is the canonical builder.
            JavaEntityToHbmMapper.Result mapped = JavaEntityToHbmMapper.map(entity.key(), entity.entityClass());
            String xml = mapped.descriptor()
                               .serialize();
            try (InputStream in = IOUtils.toInputStream(xml, StandardCharsets.UTF_8)) {
                configuration.addInputStream(in);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to add HBM mapping for [" + entity.entityName() + "]: " + e.getMessage(), e);
            }
        }

        DataSource dataSource = dataSourcesManager.getDefaultDataSource();

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
        builder.applySetting(Environment.JAKARTA_JTA_DATASOURCE, dataSource);
        builder.applySettings(configuration.getProperties());

        StandardServiceRegistry serviceRegistry = builder.build();
        SessionFactory next = configuration.buildSessionFactory(serviceRegistry);
        sessionFactory = next;

        if (old != null) {
            try {
                old.close();
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to close prior SessionFactory cleanly: {}", e.getMessage());
            }
        }
    }

    /**
     * Minimal Java→Hibernate type table for the id column (the mapper has the full version). The id
     * type is the only one we look up outside the mapper itself.
     */
    private static String hibernateTypeOf(Class<?> javaType) {
        if (javaType == String.class) {
            return "string";
        }
        if (javaType == long.class || javaType == Long.class) {
            return "long";
        }
        if (javaType == int.class || javaType == Integer.class) {
            return "integer";
        }
        if (javaType == java.util.UUID.class) {
            return "uuid";
        }
        return "string";
    }

    @Override
    public void destroy() {
        if (sessionFactory != null) {
            try {
                sessionFactory.close();
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to close SessionFactory on shutdown: {}", e.getMessage());
            }
        }
    }

}
