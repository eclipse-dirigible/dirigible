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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.store.config.CurrentTenantIdentifierResolverImpl;
import org.eclipse.dirigible.components.data.store.config.MultiTenantConnectionProviderImpl;
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

    /**
     * Hibernate multi-tenancy wiring, reused from the TypeScript {@code DataStore}. Without it the
     * dynamic {@link SessionFactory} binds a single datasource and every tenant's entity CRUD hits the
     * same schema (cross-tenant data leak); with it, Hibernate resolves the current tenant per session
     * and the connection provider hands back a tenant-routed connection at query time.
     */
    private final MultiTenantConnectionProviderImpl connectionProvider;

    private final CurrentTenantIdentifierResolverImpl tenantIdentifierResolver;

    /** Current registered entities, keyed by {@code <project>::<fqn>}. */
    private final Map<String, RegisteredEntity> registered = new LinkedHashMap<>();

    private final ReentrantLock rebuildLock = new ReentrantLock();

    private volatile SessionFactory sessionFactory;

    /**
     * @param dataSourcesManager source of the JDBC {@link DataSource} used by the dynamic
     *        {@link SessionFactory}
     * @param connectionProvider the Hibernate multi-tenant connection provider (tenant-routed
     *        connections)
     * @param tenantIdentifierResolver resolves the current tenant for each session
     */
    @Autowired
    public JavaEntityManager(DataSourcesManager dataSourcesManager, MultiTenantConnectionProviderImpl connectionProvider,
            CurrentTenantIdentifierResolverImpl tenantIdentifierResolver) {
        this.dataSourcesManager = dataSourcesManager;
        this.connectionProvider = connectionProvider;
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    /**
     * Register (or re-register) an entity class. Triggers a {@link SessionFactory} rebuild.
     *
     * @param key the registration key (typically {@code <project>::<fqn>})
     * @param entityClass the loaded entity class
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

    /**
     * Drop an entity from the runtime registry. The table itself is left in place.
     *
     * @param key the registration key to remove
     * @return the removed entry, or empty if no entry was registered under {@code key}
     */
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

    /**
     * Look up by registry key.
     *
     * @param key the registration key
     * @return the registered entry, or empty if none
     */
    public Optional<RegisteredEntity> find(String key) {
        return Optional.ofNullable(registered.get(key));
    }

    /**
     * Look up by entity {@link Class} — finds the registered entry whose
     * {@link RegisteredEntity#entityClass()} matches by identity, falling back to FQN equality so a
     * class loaded in a previous generation of the {@code ClientClassLoader} still resolves to its
     * current registration.
     *
     * @param entityClass the entity class to resolve
     * @return the registered entry, or empty if none matches
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

    /**
     * @return the current {@link SessionFactory}, lazily creating an empty one if needed
     */
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

    /**
     * @return the number of currently registered entities
     */
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

        // Pin the dialect explicitly (as the TypeScript DataStore does). With multi-tenancy the
        // connection used for boot-time metadata detection is not guaranteed to reflect the target
        // database, and an unresolved/wrong dialect breaks dialect-specific behaviour - notably MSSQL
        // IDENTITY key generation, where the insert would otherwise send an explicit NULL for the
        // auto-increment primary key instead of letting the database assign it.
        String dialect = detectHibernateDialect();
        if (dialect != null) {
            configuration.setProperty("hibernate.dialect", dialect);
        }

        for (RegisteredEntity entity : registered.values()) {
            // Re-map fresh through JavaEntityToHbmMapper rather than reusing reflection-derived
            // descriptors — keeps Java reflection objects out of the Hibernate metadata layer.
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
        // Enable Hibernate multi-tenancy (mirrors the TypeScript DataStore): the connection provider
        // resolves the datasource per connection at request time and the resolver scopes each session
        // to the current tenant, so Java-entity CRUD is isolated per tenant instead of all tenants
        // sharing the datasource captured when this factory was built.
        builder.applySetting(Environment.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        builder.applySetting(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
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
     * Detect the Hibernate dialect from the default datasource's metadata, mirroring the TypeScript
     * {@code DataStore}. Returns {@code null} when it cannot be determined, in which case Hibernate
     * falls back to its own auto-detection.
     *
     * @return the fully qualified Hibernate dialect class name, or {@code null}
     */
    private String detectHibernateDialect() {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            String productName = connection.getMetaData()
                                           .getDatabaseProductName();
            if (productName == null) {
                return null;
            }
            String name = productName.toLowerCase();
            if (name.contains("h2")) {
                return "org.hibernate.dialect.H2Dialect";
            }
            if (name.contains("postgres")) {
                return "org.hibernate.dialect.PostgreSQLDialect";
            }
            if (name.contains("mariadb")) {
                return "org.hibernate.dialect.MariaDBDialect";
            }
            if (name.contains("mysql")) {
                return "org.hibernate.dialect.MySQLDialect";
            }
            if (name.contains("microsoft sql server") || name.contains("mssql")) {
                return "org.hibernate.dialect.SQLServerDialect";
            }
            if (name.contains("hdb") || name.contains("hana")) {
                return "org.hibernate.dialect.HANADialect";
            }
            return null;
        } catch (SQLException e) {
            LOGGER.warn("Could not detect Hibernate dialect from datasource metadata", e);
            return null;
        }
    }

    /**
     * Close the held {@link SessionFactory} on shutdown.
     */
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
