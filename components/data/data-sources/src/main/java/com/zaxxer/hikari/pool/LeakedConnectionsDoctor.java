/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.util.ConcurrentBag;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class LeakedConnectionsDoctor {

    public static final int INITIAL_DELAY = 30;
    private static final Logger LOGGER = LoggerFactory.getLogger(LeakedConnectionsDoctor.class);
    private static final long MAX_IN_USE_MILLIS =
            TimeUnit.SECONDS.toMillis(DirigibleConfig.LEAKED_CONNECTIONS_MAX_IN_USE_SECONDS.getIntValue());
    // registered from every thread that borrows a connection while the check thread iterates - the
    // weakly consistent iteration of a concurrent set never throws ConcurrentModificationException
    private static final Set<InUseConnectionEntry> IN_USE_CONNECTIONS = ConcurrentHashMap.newKeySet();

    public static void init() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        LOGGER.info("Scheduling check for leaked connection with initial delay of [{}] seconds and interval [{}] seconds.", INITIAL_DELAY,
                DirigibleConfig.LEAKED_CONNECTIONS_CHECK_INTERVAL_SECONDS.getIntValue());
        executor.scheduleAtFixedRate(LeakedConnectionsDoctor::checkForLeakedConnections, INITIAL_DELAY,
                DirigibleConfig.LEAKED_CONNECTIONS_CHECK_INTERVAL_SECONDS.getIntValue(), TimeUnit.SECONDS);

        Runtime.getRuntime()
               .addShutdownHook(new Thread(() -> {
                   executor.shutdown();
                   try {
                       executor.awaitTermination(5, TimeUnit.SECONDS);
                   } catch (InterruptedException ex) {
                       LOGGER.warn("Failed to await for termination", ex);
                   } finally {
                       if (!executor.isTerminated()) {
                           executor.shutdownNow();
                       }
                   }
               }));
    }

    private static void checkForLeakedConnections() {
        try {
            closeLeakedConnections();
        } catch (Throwable ex) {
            // a scheduled task that throws is never executed again and the executor reports nothing,
            // so every connection registered from then on would be retained until the heap is exhausted
            LOGGER.error("Failed to check for leaked connections. Will retry on the next scheduled execution.", ex);
        }
    }

    static void closeLeakedConnections() {
        LOGGER.debug("Checking [{}] registered connections for leaks...", IN_USE_CONNECTIONS.size());
        long executionStartedAt = System.currentTimeMillis();

        for (InUseConnectionEntry entry : IN_USE_CONNECTIONS) {
            if (isClosed(entry.getConnection())) {
                LOGGER.debug("Connection [{}] borrowed at [{}] is closed. Will be removed from the list.", entry.getConnection(),
                        entry.getBorrowedAt());
                IN_USE_CONNECTIONS.remove(entry);
                continue;
            }

            if (entry.getConnection() instanceof HikariProxyConnection hikariProxyConnection
                    && isInUse(hikariProxyConnection.getPoolEntry())
                    && (isNotBorrowedSinceRegistered(hikariProxyConnection.getPoolEntry(), entry)
                            && isNotAccessedSinceRegistered(hikariProxyConnection.getPoolEntry(), entry))) {

                boolean maxInUsePassed = executionStartedAt > (entry.getBorrowedAt() + MAX_IN_USE_MILLIS);
                if (maxInUsePassed) {
                    closeLeakedEntry(entry, hikariProxyConnection);
                    IN_USE_CONNECTIONS.remove(entry);
                } else {
                    LOGGER.debug(
                            "Connection [{}] borrowed at [{}] didn't reached the configured max in use time of [{}] millis. Will check it on the next execution.",
                            entry.getConnection(), entry.getBorrowedAt(), MAX_IN_USE_MILLIS);
                }
            } else {
                IN_USE_CONNECTIONS.remove(entry);
            }
        }
    }

    private static void closeLeakedEntry(InUseConnectionEntry entry, HikariProxyConnection hikariProxyConnection) {
        try {
            LOGGER.warn("Found leaked connection [{}] borrowed at [{}] and remained in state IN_USE. Will be closed.",
                    entry.getConnection(), entry.getBorrowedAt());
            entry.getConnection()
                 .close();
            hikariProxyConnection.getPoolEntry()
                                 .evict("The connection is leaked since it is in state IN_USE for more than [" + MAX_IN_USE_MILLIS
                                         + "] millis.");
            LOGGER.debug("Leaked connection [{}] borrowed at [{}] was closed", entry.getConnection(), entry.getBorrowedAt());
        } catch (RuntimeException | SQLException ex) {
            LOGGER.warn("Failed to close connection [{}] which was borrowed at [{}]", entry.getConnection(), entry.getBorrowedAt(), ex);
        }
    }

    private static boolean isNotAccessedSinceRegistered(PoolEntry poolEntry, InUseConnectionEntry entry) {
        return poolEntry.lastAccessed <= entry.getBorrowedAt();

    }

    private static boolean isInUse(PoolEntry poolEntry) {
        return poolEntry.getState() == ConcurrentBag.IConcurrentBagEntry.STATE_IN_USE;
    }

    private static boolean isNotBorrowedSinceRegistered(PoolEntry poolEntry, InUseConnectionEntry entry) {
        return poolEntry.lastBorrowed <= entry.getBorrowedAt();
    }

    private static boolean isClosed(Connection connection) {
        try {
            return connection.isClosed();
        } catch (SQLException e) {
            LOGGER.warn("Connection [{}] cannot be checked for isClosed. Will consider it is closed.", connection, e);
            return true;
        }
    }

    public static void registerConnection(Connection connection) {
        IN_USE_CONNECTIONS.add(new InUseConnectionEntry(connection));
    }

    /**
     * Forgets a connection that was returned to the pool, so that it is not retained until the next
     * check.
     *
     * @param connection the connection registered by {@link #registerConnection(Connection)}
     */
    public static void unregisterConnection(Connection connection) {
        IN_USE_CONNECTIONS.remove(new InUseConnectionEntry(connection));
    }

    static boolean isRegistered(Connection connection) {
        return IN_USE_CONNECTIONS.contains(new InUseConnectionEntry(connection));
    }
}
