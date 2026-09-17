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

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.Locker;
import org.apache.activemq.store.jdbc.JDBCPersistenceAdapter;
import org.apache.activemq.store.jdbc.LeaseDatabaseLocker;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/**
 * Covers how the embedded broker takes the message store it shares with the other instances of the
 * deployment. Both properties asserted here are what keeps a rolling deployment from deadlocking:
 * the claim on the store expires by itself, and waiting for it never runs on the thread that
 * refreshes the Spring context.
 */
class EmbeddedBrokerSharedStoreTest {

    /** The configuration under test. */
    private final EmbeddedBrokerMessagingConfig config = new EmbeddedBrokerMessagingConfig();

    /**
     * ActiveMQ's default locker holds the store with an exclusive row lock, which lives as long as the
     * holder's database connection does - so an instance that is killed keeps the store until the
     * database gives up on a peer it cannot see, and every retry of its replacement inherits the same
     * wait. A lease expires on a clock instead.
     *
     * @throws Exception the exception
     */
    @Test
    void testTheSharedStoreIsClaimedWithAnExpiringLease() throws Exception {
        JDBCPersistenceAdapter adapter = config.createSharedStore(dataSource());

        Locker locker = adapter.getLocker();

        assertThat(locker).isInstanceOf(LeaseDatabaseLocker.class);
        LeaseDatabaseLocker leaseLocker = (LeaseDatabaseLocker) locker;
        assertThat(leaseLocker.getLockAcquireSleepInterval()).isPositive();
        assertThat(adapter.getLockKeepAlivePeriod()).isPositive()
                                                    .isLessThan(leaseLocker.getLockAcquireSleepInterval());
        assertThat(leaseLocker.getQueryTimeout()).isPositive();
    }

    /**
     * The whole point of the exercise: the wait for the shared store must not run on the thread that
     * refreshes the Spring context, or an instance rolled out beside a running one never reaches
     * readiness - and the orchestrator never stops the instance it is waiting for.
     *
     * @throws Exception the exception
     */
    @Test
    void testTakingTheSharedStoreDoesNotRunOnTheContextThread() throws Exception {
        BrokerService broker = new BrokerService();

        assertThat(config.configureStore(broker, dataSource())).isTrue();

        assertThat(broker.isStartAsync()).isTrue();
    }

    /**
     * The lease is renewed and released by holder id, so an id shared by two instances would have them
     * renewing and releasing each other's claim. ActiveMQ's default - the broker name - is exactly
     * that: every instance names its broker {@code localhost}.
     *
     * @throws Exception the exception
     */
    @Test
    void testTheLeaseHolderIdentifiesTheInstanceRatherThanTheBroker() throws Exception {
        LeaseDatabaseLocker locker = (LeaseDatabaseLocker) config.createSharedStore(dataSource())
                                                                 .getLocker();

        assertThat(locker.getLeaseHolderId()).isNotBlank()
                                             .isNotEqualTo("localhost")
                                             .contains(String.valueOf(ProcessHandle.current()
                                                                                   .pid()));
    }

    /**
     * A lease is an absolute instant, so every instance has to read it against the same clock. Left to
     * measure with its own, an instance running ahead declares a living master expired and joins it on
     * the store.
     *
     * @throws Exception the exception
     */
    @Test
    void testTheLeaseIsMeasuredAgainstTheDatabaseClock() throws Exception {
        LeaseDatabaseLocker locker = (LeaseDatabaseLocker) config.createSharedStore(dataSource())
                                                                 .getLocker();

        assertThat(locker.getMaxAllowableDiffFromDBTime()).isPositive();
    }

    /**
     * Every boot after the first re-runs the schema DDL, and ActiveMQ decides whether the resulting
     * failures are worth a warning by probing the metadata for its own, upper case table name. On a
     * database that folds unquoted identifiers the probe never matches, so the whole DDL is logged as
     * failing, with stack traces - the last thing an instance says before it goes quiet waiting for the
     * lease. Naming the tables as the database stores them makes the probe find them.
     *
     * @throws Exception the exception
     */
    @Test
    void testTheTableNamesFollowTheCaseTheDatabaseStoresThemIn() throws Exception {
        JdbcDataSource dataSource = dataSource();
        execute(dataSource, "CREATE TABLE \"activemq_msgs\"(ID BIGINT)", "CREATE TABLE \"activemq_acks\"(ID BIGINT)",
                "CREATE TABLE \"activemq_lock\"(ID BIGINT)");

        JDBCPersistenceAdapter adapter = config.createSharedStore(dataSource);

        assertThat(adapter.getStatements()
                          .getMessageTableName()).isEqualTo("activemq_msgs");
        assertThat(adapter.getStatements()
                          .getDurableSubAcksTableName()).isEqualTo("activemq_acks");
        assertThat(adapter.getStatements()
                          .getLockTableName()).isEqualTo("activemq_lock");
    }

    /**
     * An empty database is the first boot - the tables are about to be created under the names ActiveMQ
     * ships with, so nothing is renamed.
     *
     * @throws Exception the exception
     */
    @Test
    void testAnEmptyDatabaseKeepsTheDefaultTableNames() throws Exception {
        JDBCPersistenceAdapter adapter = config.createSharedStore(dataSource());

        assertThat(adapter.getStatements()
                          .getMessageTableName()).isEqualTo("ACTIVEMQ_MSGS");
    }

    /**
     * A fresh in-memory database, kept alive for as long as the test holds a connection to it.
     *
     * @return the data source
     */
    private JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        return dataSource;
    }

    /**
     * Execute.
     *
     * @param dataSource the data source
     * @param sqls the statements to execute
     * @throws SQLException the SQL exception
     */
    private void execute(JdbcDataSource dataSource, String... sqls) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : sqls) {
                statement.execute(sql);
            }
        }
    }
}
