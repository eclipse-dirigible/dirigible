/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.config;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import javax.sql.DataSource;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.Locker;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.store.PListStore;
import org.apache.activemq.store.jdbc.JDBCPersistenceAdapter;
import org.apache.activemq.store.jdbc.LeaseDatabaseLocker;
import org.apache.activemq.store.jdbc.Statements;
import org.apache.activemq.store.kahadb.plist.PListStoreImpl;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

/**
 * The messaging beans backed by the platform's own in-process ActiveMQ broker - the default mode,
 * active whenever no external broker is configured. Its counterpart is
 * {@link ExternalBrokerMessagingConfig}; the two are mutually exclusive by construction.
 *
 * The broker keeps its messages either in a local KahaDB directory, which belongs to this process
 * alone, or - with {@code DIRIGIBLE_MESSAGING_USE_DEFAULT_DATABASE} - in the SystemDB, which every
 * instance of the deployment shares. A shared store admits exactly one broker at a time, so the
 * second half of this class is about the instance that is not it yet.
 */
@Configuration
@Conditional(EmbeddedMessagingBrokerCondition.class)
class EmbeddedBrokerMessagingConfig {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedBrokerMessagingConfig.class);

    /** The Constant CONNECTOR_URL_ATTACH. */
    private static final String CONNECTOR_URL_ATTACH = "vm://localhost?create=false";

    /** The Constant CONNECTOR_URL. */
    private static final String CONNECTOR_URL = "vm://localhost";

    /** The Constant LOCATION_TEMP_STORE. */
    private static final String LOCATION_TEMP_STORE = "./target/dirigible/kahadb";

    /** The table types a schema probe is interested in. */
    private static final String[] TABLE_TYPE = {"TABLE"};

    /**
     * How long a durable subscription may stay offline before the broker discards it, and how often
     * that sweep runs. A durable subscription outlives its subscriber by design - that is what stops a
     * message published during a republish from being lost - so nothing reclaims one whose handler was
     * deleted rather than reloaded. The consumer cannot do it: a class deleted while the server was
     * down is never reported as unloaded at all, so there is no moment at which to unsubscribe. A
     * generous timeout collects those orphans without ever expiring a subscription that a restart or a
     * republish is about to reconnect.
     */
    private static final long OFFLINE_DURABLE_SUBSCRIBER_TIMEOUT = Duration.ofDays(7)
                                                                           .toMillis();

    /** The Constant OFFLINE_DURABLE_SUBSCRIBER_TASK_SCHEDULE. */
    private static final long OFFLINE_DURABLE_SUBSCRIBER_TASK_SCHEDULE = Duration.ofHours(1)
                                                                                 .toMillis();

    /**
     * How long this instance's claim on the shared message store stays valid without being renewed -
     * and, because the lease locker reuses the value, how long it waits between attempts to take a
     * lease that somebody else holds. An instance that dies without releasing its lease therefore
     * blocks its successor for at most this long, deterministically, instead of for as long as the
     * database takes to notice a dead client.
     */
    private static final long SHARED_STORE_LEASE_DURATION = Duration.ofSeconds(30)
                                                                    .toMillis();

    /**
     * How often the master renews its lease. It must be comfortably shorter than the lease itself, or a
     * slow renewal lets the lease expire under a living master and a second broker joins it on the same
     * store.
     */
    private static final long SHARED_STORE_LEASE_KEEP_ALIVE = Duration.ofSeconds(10)
                                                                      .toMillis();

    /**
     * How far this instance's clock may drift from the database's before the lease arithmetic is done
     * against the database's clock instead. A lease is an absolute instant, so without this every
     * instance measures the others' leases with its own clock and a fast one declares a living master
     * expired.
     */
    private static final int SHARED_STORE_MAX_CLOCK_DRIFT_MILLIS = 1000;

    /**
     * Caps how long a single lease statement may block in the database. Without it a lease attempt can
     * wait forever on a row lock left behind by a broker of an older version, which took the store with
     * {@code SELECT ... FOR UPDATE} rather than with a lease.
     */
    private static final int SHARED_STORE_LOCK_QUERY_TIMEOUT_SECONDS = 10;

    /**
     * How long the context waits for an asynchronously starting broker before carrying on without it.
     * An uncontended instance - the normal case - takes the store well within this, and so reaches the
     * same state a synchronous start would have left it in.
     */
    private static final long SHARED_STORE_START_GRACE = Duration.ofSeconds(5)
                                                                 .toMillis();

    /**
     * Creates the active MQ connection factory.
     *
     * @return the active MQ connection factory
     */
    @Bean
    ActiveMQConnectionFactory createActiveMQConnectionFactory() {
        return new ActiveMQConnectionFactory(CONNECTOR_URL_ATTACH);
    }

    /**
     * Creates the broker service.
     *
     * @param dataSource the data source
     * @return the broker service
     */
    @Bean("ActiveMQBroker")
    BrokerService createBrokerService(@Qualifier("SystemDB") DataSource dataSource) {
        try {
            BrokerService broker = new BrokerService();
            boolean sharedStore = configureStore(broker, dataSource);
            broker.setPersistent(true);
            broker.setUseJmx(false);
            broker.setOfflineDurableSubscriberTimeout(OFFLINE_DURABLE_SUBSCRIBER_TIMEOUT);
            broker.setOfflineDurableSubscriberTaskSchedule(OFFLINE_DURABLE_SUBSCRIBER_TASK_SCHEDULE);
            PListStore pListStore = new PListStoreImpl();
            pListStore.setDirectory(new File(LOCATION_TEMP_STORE));
            broker.setTempDataStore(pListStore);
            broker.addConnector(CONNECTOR_URL);

            broker.start();
            if (sharedStore) {
                awaitSharedStore(broker);
            }

            return broker;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to init ActiveMQ broker", ex);
        }
    }

    /**
     * Points the broker at its message store, and decides whether it may take it on the thread that
     * refreshes the Spring context.
     *
     * A store shared through the SystemDB admits one broker at a time, and taking it means waiting for
     * whoever holds it to let go - which during a make-before-break rollout is until the orchestrator
     * stops the previous instance, and it will not stop it until this one reports ready. Starting on
     * the context thread therefore closes a circle: readiness waits for the store, the store waits for
     * readiness. ActiveMQ's asynchronous start breaks it: the context refreshes, the instance comes up
     * and serves, and the broker attaches by itself once the lease frees. A local KahaDB store has no
     * such contention and keeps starting synchronously.
     *
     * @param broker the broker
     * @param dataSource the SystemDB data source
     * @return true, if the store is the shared one
     * @throws IOException if the store cannot be configured
     */
    boolean configureStore(BrokerService broker, DataSource dataSource) throws IOException {
        boolean sharedStore = DirigibleConfig.MESSAGING_USE_DEFAULT_DATABASE.getBooleanValue();
        if (sharedStore) {
            broker.setPersistenceAdapter(createSharedStore(dataSource));
            broker.setStartAsync(true);
        }
        return sharedStore;
    }

    /**
     * The persistence adapter that keeps the messages in the platform's own SystemDB, so that they
     * outlive the process that queued them. The store is shared by every instance of the deployment and
     * only its master may run a broker against it, which is what the lease below arbitrates.
     *
     * @param dataSource the SystemDB data source
     * @return the persistence adapter
     * @throws IOException if the adapter rejects the locker
     */
    JDBCPersistenceAdapter createSharedStore(DataSource dataSource) throws IOException {
        JDBCPersistenceAdapter adapter = new JDBCPersistenceAdapter(dataSource, new OpenWireFormat());
        alignSchemaCaseWithDatabase(adapter, dataSource);
        adapter.setLockKeepAlivePeriod(SHARED_STORE_LEASE_KEEP_ALIVE);
        adapter.setLocker(createStoreLocker());
        return adapter;
    }

    /**
     * A lease on the shared store rather than ActiveMQ's default exclusive row lock. The row lock is
     * held for as long as the holder's database connection lives, so an instance that is killed keeps
     * the store until the database gives up on its connection - and every retry of the replacement
     * inherits the same wait. A lease simply expires.
     *
     * @return the locker
     */
    private Locker createStoreLocker() {
        LeaseDatabaseLocker locker = new LeaseDatabaseLocker();
        locker.setLeaseHolderId(leaseHolderId());
        locker.setLockAcquireSleepInterval(SHARED_STORE_LEASE_DURATION);
        locker.setMaxAllowableDiffFromDBTime(SHARED_STORE_MAX_CLOCK_DRIFT_MILLIS);
        locker.setQueryTimeout(SHARED_STORE_LOCK_QUERY_TIMEOUT_SECONDS);
        return locker;
    }

    /**
     * Identifies this instance in the lease row. It has to be unique per running instance, because the
     * lease is renewed and released by holder id - two instances sharing one id would renew and release
     * each other's claim on the store. The broker name cannot serve: every instance names its broker
     * {@code localhost}, which is what the vm:// connector URL is built from.
     *
     * @return the lease holder id
     */
    private static String leaseHolderId() {
        return hostName() + ":" + ProcessHandle.current()
                                               .pid();
    }

    /**
     * Gets the host name.
     *
     * @return the host name, or a placeholder when it cannot be resolved - the process id alone still
     *         identifies the instance
     */
    private static String hostName() {
        try {
            return InetAddress.getLocalHost()
                              .getHostName();
        } catch (UnknownHostException ex) {
            LOGGER.debug("Cannot resolve the local host name for the message store lease holder id", ex);
            return "unknown-host";
        }
    }

    /**
     * Points the adapter at the table names the database actually holds. ActiveMQ decides whether a
     * failed CREATE is worth a warning by probing the metadata for its own, upper case table name - a
     * probe that never matches on a database which folds unquoted identifiers to lower case. Every boot
     * after the first then logs the whole schema DDL failing, with stack traces, which is both alarming
     * and, for an instance that goes on to wait for the lease, the last thing it says. Naming the
     * tables in the case the database reports makes that probe find them. The DDL still runs, so an
     * ActiveMQ upgrade that alters the schema is unaffected.
     *
     * @param adapter the persistence adapter
     * @param dataSource the SystemDB data source
     */
    private void alignSchemaCaseWithDatabase(JDBCPersistenceAdapter adapter, DataSource dataSource) {
        Statements statements = adapter.getStatements();
        try (java.sql.Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            storedName(metaData, statements.getMessageTableName()).ifPresent(statements::setMessageTableName);
            storedName(metaData, statements.getDurableSubAcksTableName()).ifPresent(statements::setDurableSubAcksTableName);
            storedName(metaData, statements.getLockTableName()).ifPresent(statements::setLockTableName);
        } catch (SQLException ex) {
            LOGGER.warn("Failed to read the metadata of the message store schema - keeping the default table names", ex);
        }
    }

    /**
     * The name under which the database stores a table, when that differs from the name ActiveMQ uses
     * for it.
     *
     * @param metaData the database metadata
     * @param table the table name as ActiveMQ spells it
     * @return the stored name, or empty when the table is absent - the first boot creates it - or is
     *         already stored under the name given
     * @throws SQLException if the metadata cannot be read
     */
    private Optional<String> storedName(DatabaseMetaData metaData, String table) throws SQLException {
        if (exists(metaData, table)) {
            return Optional.empty();
        }
        String folded = table.toLowerCase(Locale.ROOT);
        return exists(metaData, folded) ? Optional.of(folded) : Optional.empty();
    }

    /**
     * Checks whether a table of the given name exists.
     *
     * @param metaData the database metadata
     * @param table the table name
     * @return true, if it exists
     * @throws SQLException if the metadata cannot be read
     */
    private boolean exists(DatabaseMetaData metaData, String table) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, table, TABLE_TYPE)) {
            return tables.next();
        }
    }

    /**
     * Gives an asynchronously starting broker a moment to take the shared store, so that an uncontended
     * instance comes up with messaging already attached, and one that is genuinely waiting for another
     * instance to let go says so once instead of going silent.
     *
     * @param broker the broker
     */
    private void awaitSharedStore(BrokerService broker) {
        if (broker.waitUntilStarted(SHARED_STORE_START_GRACE)) {
            return;
        }
        Throwable startFailure = broker.getStartException();
        if (null != startFailure) {
            throw new IllegalStateException("Failed to start the ActiveMQ broker against the shared message store", startFailure);
        }
        LOGGER.warn(
                "The ActiveMQ broker has not taken the shared message store within {} ms - another instance of this deployment"
                        + " still holds the lease on it. Startup continues; messaging attaches on its own once the lease frees.",
                SHARED_STORE_START_GRACE);
    }

    /**
     * Creates the session.
     *
     * @param connection the connection
     * @return the session
     */
    @Bean("ActiveMQSession")
    @Lazy
    Session createSession(@Qualifier("ActiveMQConnection") Connection connection) {
        try {
            return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException ex) {
            throw new IllegalStateException("Failed to create session to ActiveMQ", ex);
        }
    }

    /**
     * Creates the connection. Lazily, because there is nothing to connect to until the broker holds the
     * shared message store, and against a shared store this instance may still be waiting for it when
     * the context finishes refreshing. Attaching on first use lets the instance come up and serve
     * meanwhile; a send or a listener that runs before the broker is master fails and is retried, which
     * beats never having started at all.
     *
     * @param connectionArtifactsFactory the connection artifacts factory
     * @param loggingExceptionListener the logging exception listener
     * @return the connection
     */
    @Bean("ActiveMQConnection")
    @Lazy
    @DependsOn("ActiveMQBroker")
    Connection createConnection(ActiveMQConnectionArtifactsFactory connectionArtifactsFactory,
            LoggingExceptionListener loggingExceptionListener) {
        return connectionArtifactsFactory.createConnection(loggingExceptionListener);
    }
}
