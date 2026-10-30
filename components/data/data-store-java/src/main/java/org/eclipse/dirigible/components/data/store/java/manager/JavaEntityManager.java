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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
 * A rebuild re-maps every registered entity and runs {@code hbm2ddl} over all their tables, so
 * installing a whole generation one rebuild per entity is quadratic. {@code EntityClassConsumer}
 * therefore installs a generation inside {@link #beginBatch()} / {@link #endBatch()}: within a
 * batch {@link #register} and {@link #unregister} only change the registrations, and the batch ends
 * with a single rebuild, during which the previous {@link SessionFactory} keeps serving. Should
 * that rebuild fail, the entities that break it are found and dropped, so one broken entity does
 * not take the others down with it.
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

    /**
     * Metadata views for callers whose entity {@link Class} belongs to an OLDER client-classloader
     * generation than the currently registered one — e.g. a Flowable-cached delegate instance that
     * resolved its classes before its module was republished. Keyed by the caller's class.
     *
     * <p>
     * Deliberately NOT a {@code WeakHashMap}: each value re-reflects the entity against the caller's
     * class and therefore strongly references that class (through its {@code Field}s), which would pin
     * the key and make weak-key collection impossible — the map would leak the retired
     * {@code ClientClassLoader} forever. The cache is instead invalidated explicitly on every
     * {@link #rebuildSessionFactory()}: a generation change makes the prior generation's views
     * obsolete, and once dropped the retired classloader becomes collectable.
     */
    private final Map<Class<?>, RegisteredEntity> staleGenerationViews = Collections.synchronizedMap(new HashMap<>());

    private final ReentrantLock rebuildLock = new ReentrantLock();

    /** Open {@link #beginBatch()} calls not yet closed by {@link #endBatch()}; guarded by the lock. */
    private int batchDepth;

    /** Whether a registration changed inside the open batch; guarded by the lock. */
    private boolean batchDirty;

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
     * Open a batch: until the matching {@link #endBatch()}, {@link #register} and {@link #unregister}
     * change the registrations without rebuilding the {@link SessionFactory}. Batches nest; only the
     * outermost {@link #endBatch()} rebuilds.
     */
    public void beginBatch() {
        rebuildLock.lock();
        try {
            batchDepth++;
        } finally {
            rebuildLock.unlock();
        }
    }

    /**
     * Close a batch opened by {@link #beginBatch()}. Closing the outermost one rebuilds the
     * {@link SessionFactory} once if any registration changed inside it. An entity that breaks that
     * rebuild is dropped from the registrations and logged, and the others are installed without it.
     *
     * @throws IllegalStateException if no batch is open
     */
    public void endBatch() {
        rebuildLock.lock();
        try {
            if (batchDepth == 0) {
                throw new IllegalStateException("endBatch() called without a matching beginBatch()");
            }
            batchDepth--;
            if (batchDepth == 0 && batchDirty) {
                batchDirty = false;
                rebuildDroppingBrokenEntities();
            }
        } finally {
            rebuildLock.unlock();
        }
    }

    /**
     * Register (or re-register) an entity class. Outside a batch this rebuilds the
     * {@link SessionFactory}, and a failed rebuild leaves the registrations as they were; inside a
     * batch the rebuild is left to {@link #endBatch()}.
     *
     * @param key the registration key (typically {@code <project>::<fqn>})
     * @param entityClass the loaded entity class
     * @return the resolved {@link RegisteredEntity} metadata
     */
    public RegisteredEntity register(String key, Class<?> entityClass) {
        rebuildLock.lock();
        try {
            RegisteredEntity entity = JavaEntityToHbmMapper.map(key, entityClass)
                                                           .registered();
            RegisteredEntity previous = registered.put(key, entity);
            if (batchDepth > 0) {
                batchDirty = true;
            } else {
                try {
                    rebuildSessionFactory();
                } catch (RuntimeException e) {
                    if (previous == null) {
                        registered.remove(key);
                    } else {
                        registered.put(key, previous);
                    }
                    throw e;
                }
            }
            LOGGER.info("Registered Java entity [{}] → table [{}]", entity.entityName(), entity.tableName());
            return entity;
        } finally {
            rebuildLock.unlock();
        }
    }

    /**
     * Drop an entity from the runtime registry. The table itself is left in place. Outside a batch this
     * rebuilds the {@link SessionFactory}; inside a batch the rebuild is left to {@link #endBatch()}.
     *
     * @param key the registration key to remove
     * @return the removed entry, or empty if no entry was registered under {@code key}
     */
    public Optional<RegisteredEntity> unregister(String key) {
        rebuildLock.lock();
        try {
            RegisteredEntity removed = registered.remove(key);
            if (removed != null) {
                if (batchDepth > 0) {
                    batchDirty = true;
                } else {
                    rebuildSessionFactory();
                }
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

    /** Test seam: seed a registration without touching the Hibernate {@link SessionFactory}. */
    void registerWithoutRebuild(String key, Class<?> entityClass) {
        registered.put(key, JavaEntityToHbmMapper.map(key, entityClass)
                                                 .registered());
    }

    /**
     * Look up by entity {@link Class} — finds the registered entry whose
     * {@link RegisteredEntity#entityClass()} matches by identity, falling back to FQN equality so a
     * class loaded in a previous generation of the {@code ClientClassLoader} still resolves to its
     * current registration.
     *
     * <p>
     * The FQN fallback must NOT return the current registration as-is: its reflective surface
     * ({@code Field}s, constructor) belongs to the CURRENT generation's class and cannot be applied to
     * the caller's instances — {@code EntityBeanMapper} would throw {@code IllegalArgumentException}
     * mixing a bean of one generation with fields of another (the shape a Flowable-cached delegate hits
     * after its module is republished, stalling the workflow job on every retry). Instead the same
     * entity is re-reflected against the CALLER's class: persistence is Hibernate dynamic-map
     * (name-keyed), so the entity/table names line up and only the reflective surface differs per
     * generation.
     *
     * @param entityClass the entity class to resolve
     * @return the registered entry (or a caller-generation view of it), or empty if none matches
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
                String key = r.key();
                return Optional.of(staleGenerationViews.computeIfAbsent(entityClass, clazz -> {
                    LOGGER.info(
                            "Entity [{}] resolved for a previous-generation class (caller classloader [{}], registered [{}])"
                                    + " - serving a caller-generation metadata view",
                            clazz.getName(), clazz.getClassLoader(), r.entityClass()
                                                                      .getClassLoader());
                    return JavaEntityToHbmMapper.map(key, clazz)
                                                .registered();
                }));
            }
        }
        return Optional.empty();
    }

    /**
     * Look up by the fully-qualified name of the entity class, for a caller that names an entity it
     * does not compile against - a to-one reference to a record another model generates (#8100).
     *
     * @param entityClassName the entity class's fully-qualified name
     * @return the registered entry, or empty if no registered entity has that class name
     */
    public Optional<RegisteredEntity> findForClassName(String entityClassName) {
        for (RegisteredEntity r : registered.values()) {
            if (r.entityClass()
                 .getName()
                 .equals(entityClassName)) {
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
        // Any registration change retires the prior client-classloader generation. Drop the stale
        // metadata views now: each strongly references a caller class from an older generation, so
        // leaving them in place would pin that generation's ClientClassLoader indefinitely.
        staleGenerationViews.clear();
        install(buildSessionFactory(List.copyOf(registered.values())));
    }

    /**
     * Rebuild over every registration; should that fail, find the entities that break it. The factory
     * is built again from nothing, adding one entity at a time, and an entity whose addition fails is
     * dropped from the registrations. That costs one build per entity - what every generation install
     * cost before batching - and only after a failed rebuild. If not even an empty factory can be
     * built, the database rather than an entity is at fault: the registrations are kept for the next
     * rebuild and the previous factory keeps serving.
     */
    private void rebuildDroppingBrokenEntities() {
        RuntimeException failure;
        try {
            rebuildSessionFactory();
            return;
        } catch (RuntimeException e) {
            failure = e;
        }
        LOGGER.warn("Building the SessionFactory over all [{}] registered Java entities failed - isolating the entities that break it",
                registered.size(), failure);

        SessionFactory current;
        try {
            current = buildSessionFactory(List.of());
        } catch (RuntimeException e) {
            failure.addSuppressed(e);
            throw failure;
        }
        List<RegisteredEntity> accepted = new ArrayList<>();
        for (RegisteredEntity candidate : List.copyOf(registered.values())) {
            accepted.add(candidate);
            try {
                SessionFactory next = buildSessionFactory(List.copyOf(accepted));
                close(current);
                current = next;
            } catch (RuntimeException e) {
                accepted.remove(accepted.size() - 1);
                registered.remove(candidate.key());
                LOGGER.error("Java entity [{}] (table [{}]) breaks the SessionFactory and is dropped until its class is published again",
                        candidate.entityName(), candidate.tableName(), e);
            }
        }
        install(current);
    }

    /** Swap in {@code next} and close the factory it replaces. */
    private void install(SessionFactory next) {
        // Close-after-swap: for our hot-reload use case there are no long-lived open sessions, so
        // closing the replaced factory right away is safe.
        SessionFactory old = sessionFactory;
        sessionFactory = next;
        close(old);
    }

    private static void close(SessionFactory factory) {
        if (factory == null) {
            return;
        }
        try {
            factory.close();
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to close a replaced SessionFactory cleanly", e);
        }
    }

    /**
     * Build a {@link SessionFactory} mapping exactly {@code entities}; {@code hbm2ddl} creates or
     * extends their tables. Package-private as a test seam.
     *
     * @param entities the entities to map
     * @return the new factory; the caller owns it
     */
    SessionFactory buildSessionFactory(List<RegisteredEntity> entities) {
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

        for (RegisteredEntity entity : entities) {
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
        try {
            return configuration.buildSessionFactory(serviceRegistry);
        } catch (RuntimeException e) {
            // A built factory owns its registry and releases it on close; a failed build does not.
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
            throw e;
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
