/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.migrations;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.data.migrations.MigrationLedger.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * Applies one migration to one database: the statements and the ledger row that records them in one
 * transaction, so a migration is either applied and recorded or neither. What it decides, per
 * database:
 * <ul>
 * <li>recorded with the same checksum - already applied, nothing runs;</li>
 * <li>recorded with another checksum - the file was edited after it applied: a failure, unless the
 * migration declares itself idempotent, in which case it runs again and the row is updated;</li>
 * <li>not recorded, but an earlier version of the same project is not recorded either - it waits,
 * so a project's migrations apply in version order;</li>
 * <li>otherwise - it runs.</li>
 * </ul>
 */
@Component
class MigrationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationExecutor.class);

    /** What applying a migration to one database came to. */
    enum Status {
        /** It ran now. */
        APPLIED,
        /** The ledger already records it with the same content. */
        ALREADY_APPLIED,
        /** An earlier version of the project has not applied yet, but still may in this pass. */
        WAITING,
        /** It did not apply and will not by itself: a failed statement, an edit, a failed predecessor. */
        FAILED
    }

    /**
     * The outcome for one database.
     *
     * @param tenant the tenant whose database it is, {@code system} for the system database
     * @param status what happened
     * @param message why, for anything but a success
     * @param cause the exception behind a failure, if one was thrown
     */
    record Outcome(String tenant, Status status, String message, Throwable cause) {

        Outcome(String tenant, Status status, String message) {
            this(tenant, status, message, null);
        }
    }

    private final MigrationLedger ledger;

    MigrationExecutor(MigrationLedger ledger) {
        this.ledger = ledger;
    }

    /**
     * Applies a migration to a database unless its ledger says otherwise. Never throws: every failure
     * is an outcome, so one tenant's failure cannot keep the next tenant from being migrated.
     *
     * @param script the migration
     * @param location the registry-relative location of its file
     * @param tenant the tenant whose database it is, {@code system} for the system database
     * @param dataSource the database
     * @param predecessors the project's other migrations of the same scope with a lower version
     * @return the outcome
     */
    Outcome apply(MigrationScript script, String location, String tenant, DataSource dataSource, List<Migration> predecessors) {
        try {
            ledger.prepare(dataSource);
            try (Connection connection = dataSource.getConnection()) {
                Map<String, Entry> applied = ledger.findByProject(connection, script.project());
                Entry recorded = applied.get(script.version());
                if (recorded == null) {
                    Outcome waiting = waitForPredecessors(applied, script, tenant, predecessors);
                    return waiting != null ? waiting : run(connection, script, location, tenant, false);
                }
                if (recorded.checksum()
                            .equals(script.checksum())) {
                    return new Outcome(tenant, Status.ALREADY_APPLIED, null);
                }
                if (!script.idempotent()) {
                    return new Outcome(tenant, Status.FAILED, "Migration [" + location + "] was applied to tenant [" + tenant + "] on ["
                            + recorded.appliedAt() + "] with checksum [" + recorded.checksum() + "], but the file now has checksum ["
                            + script.checksum()
                            + "]. An applied migration is never re-run: restore the file and ship the change as a new version, or declare the migration [idempotent: true] if running it again is safe.");
                }
                return run(connection, script, location, tenant, true);
            }
        } catch (SQLException | RuntimeException ex) {
            // DEBUG: the synchronizer registers the failure with this cause, which logs it at ERROR the
            // first time and quietly when the retry of the FAILED artefact meets it again (#7248).
            LOGGER.debug("Failed to apply migration [{}] to tenant [{}]", location, tenant, ex);
            return new Outcome(tenant, Status.FAILED, "Migration [" + location + "] failed for tenant [" + tenant + "]: " + describe(ex),
                    ex);
        }
    }

    private static Outcome waitForPredecessors(Map<String, Entry> applied, MigrationScript script, String tenant,
            List<Migration> predecessors) {
        return predecessors.stream()
                           .filter(predecessor -> !applied.containsKey(predecessor.getVersion()))
                           .min(Comparator.comparing(Migration::getVersion, MigrationScript::compareVersions))
                           .map(pending -> isFailed(pending)
                                   ? new Outcome(tenant, Status.FAILED,
                                           "Migration version [" + script.version() + "] of project [" + script.project()
                                                   + "] waits for version [" + pending.getVersion() + "] (" + pending.getLocation()
                                                   + "), which failed for tenant [" + tenant + "]")
                                   : new Outcome(tenant, Status.WAITING,
                                           "Migration version [" + script.version() + "] of project [" + script.project()
                                                   + "] waits for version [" + pending.getVersion() + "] (" + pending.getLocation()
                                                   + ") to apply to tenant [" + tenant + "] first"))
                           .orElse(null);
    }

    private static boolean isFailed(Migration migration) {
        return ArtefactLifecycle.FAILED.equals(migration.getLifecycle()) || ArtefactLifecycle.FATAL.equals(migration.getLifecycle());
    }

    private Outcome run(Connection connection, MigrationScript script, String location, String tenant, boolean reapply)
            throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            long started = System.nanoTime();
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ByteArrayResource(script.sql()
                                                                                                     .getBytes(StandardCharsets.UTF_8),
                    location), StandardCharsets.UTF_8));
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            Entry entry = new Entry(script.project(), script.version(), location, script.checksum(), tenant, Instant.now(), durationMillis);
            if (reapply) {
                ledger.update(connection, entry);
            } else {
                ledger.insert(connection, entry);
            }
            connection.commit();
            LOGGER.info("{} migration [{}] to tenant [{}] in [{}] ms", reapply ? "Re-applied" : "Applied", location, tenant,
                    durationMillis);
            return new Outcome(tenant, Status.APPLIED, null);
        } catch (SQLException | ScriptException ex) {
            connection.rollback();
            // Another node may have applied it in the meantime - its ledger row is what made this
            // insert fail, and it is the proof that the migration is applied.
            Entry recorded = ledger.findByProject(connection, script.project())
                                   .get(script.version());
            if (recorded != null && recorded.checksum()
                                            .equals(script.checksum())) {
                LOGGER.info("Migration [{}] was applied to tenant [{}] by another node", location, tenant, ex);
                return new Outcome(tenant, Status.ALREADY_APPLIED, null);
            }
            throw ex;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static String describe(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root == ex ? String.valueOf(ex.getMessage()) : ex.getMessage() + " - " + root.getMessage();
    }
}
