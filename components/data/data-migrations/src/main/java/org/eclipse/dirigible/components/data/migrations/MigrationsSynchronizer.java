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

import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantResult;
import org.eclipse.dirigible.components.data.migrations.MigrationExecutor.Outcome;
import org.eclipse.dirigible.components.data.migrations.MigrationExecutor.Status;
import org.eclipse.dirigible.components.data.migrations.MigrationScript.Scope;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Synchronizes {@code .migration} artefacts (see {@link MigrationScript} for the file shape): each
 * one is applied exactly once to every tenant's schema, or once to the system database, and
 * recorded in that database's {@code DIRIGIBLE_MIGRATIONS} ledger in the same transaction.
 *
 * <p>
 * Applying is idempotent by construction - the ledger, not the artefact's lifecycle, says whether a
 * database has a migration - so every path that runs it again is safe: the re-parse that follows a
 * new tenant's provisioning (it migrates that tenant and finds the others done), a boot against a
 * fresh system database, the retry of a FAILED artefact. The artefact is FAILED when any database
 * did not take the migration: a failed statement (rolled back with its ledger row), an applied file
 * edited afterwards (an applied migration is not re-run unless it declares itself
 * {@code idempotent: true}), or an earlier version of its project that failed.
 *
 * <p>
 * The tenants are iterated here rather than by {@code BaseSynchronizer}, which completes a
 * multitenant artefact once per tenant and keeps the state the LAST tenant registered - one
 * tenant's success would hide another's failure. This synchronizer still reports
 * {@link #multitenantExecution()} so the post-provisioning re-trigger includes it.
 *
 * <p>
 * DELETE and cleanup remove the artefact only. A migration that ran is history: its data change and
 * its ledger row stay.
 */
@Component
@Order(SynchronizersOrder.MIGRATION)
class MigrationsSynchronizer extends BaseSynchronizer<Migration, Long> {

    /** The tenant column of a ledger row written by a {@code tenant: system} migration. */
    static final String SYSTEM_TENANT = "system";

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationsSynchronizer.class);

    private final MigrationService migrationService;
    private final MigrationExecutor executor;
    private final DataSourcesManager dataSourcesManager;
    private final TenantContext tenantContext;
    private final IRepository repository;

    private SynchronizerCallback callback;

    MigrationsSynchronizer(MigrationService migrationService, MigrationExecutor executor, DataSourcesManager dataSourcesManager,
            TenantContext tenantContext, IRepository repository) {
        this.migrationService = migrationService;
        this.executor = executor;
        this.dataSourcesManager = dataSourcesManager;
        this.tenantContext = tenantContext;
        this.repository = repository;
    }

    @Override
    public boolean isAccepted(String type) {
        return Migration.ARTEFACT_TYPE.equals(type);
    }

    @Override
    protected List<Migration> parseImpl(String location, byte[] content) throws ParseException {
        MigrationScript script = MigrationScript.parse(location, content);
        rejectDuplicateVersion(location, script);
        Migration migration = new Migration(location, location.substring(location.lastIndexOf('/') + 1), script);
        migration.updateKey();
        try {
            Migration existing = migrationService.findByKey(migration.getKey());
            if (existing != null) {
                migration.setId(existing.getId());
            }
            return List.of(migrationService.save(migration));
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to save migration [{}]", location, ex);
            throw new ParseException(ex.getMessage(), 0);
        }
    }

    /**
     * Two files may not claim the same version of a project: the ledger keys a migration by project and
     * version. A claim by a file that is no longer in the registry does not count - that is a rename,
     * whose old artefact the cleanup of this very pass removes.
     */
    private void rejectDuplicateVersion(String location, MigrationScript script) throws ParseException {
        for (Migration other : migrationService.findAllByProject(script.project())) {
            if (!other.getLocation()
                      .equals(location)
                    && other.getVersion()
                            .equals(script.version())
                    && repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + other.getLocation())
                                 .exists()) {
                throw new ParseException("Version [" + script.version() + "] of project [" + script.project() + "] is claimed by both ["
                        + location + "] and [" + other.getLocation() + "] - every migration of a project needs its own version", 0);
            }
        }
    }

    @Override
    public ArtefactService<Migration, Long> getService() {
        return migrationService;
    }

    @Override
    public List<Migration> retrieve(String location) {
        return migrationService.findByLocation(location);
    }

    @Override
    public void setStatus(Migration artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        migrationService.save(artefact);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<Migration> wrapper, ArtefactPhase flow) {
        Migration migration = wrapper.getArtefact();
        ArtefactLifecycle lifecycle = migration.getLifecycle();
        return switch (flow) {
            case CREATE -> !ArtefactLifecycle.NEW.equals(lifecycle) || apply(wrapper, ArtefactLifecycle.CREATED);
            case UPDATE -> !ArtefactLifecycle.MODIFIED.equals(lifecycle) || apply(wrapper, ArtefactLifecycle.UPDATED);
            // The retry of a FAILED migration - in this pass, and on an idle instance (#7248).
            case START -> !ArtefactLifecycle.FAILED.equals(lifecycle) || apply(wrapper, ArtefactLifecycle.CREATED);
            case DELETE -> {
                migrationService.delete(migration);
                callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                yield true;
            }
            case PREPARE, STOP -> true;
        };
    }

    /**
     * Applies the migration to every database its scope names and registers the one state that sums
     * them up.
     *
     * @return false only while the migration waits for an earlier version still being applied in this
     *         pass - the depleter then offers it again once that one has completed
     */
    private boolean apply(TopologyWrapper<Migration> wrapper, ArtefactLifecycle success) {
        Migration migration = wrapper.getArtefact();
        String location = migration.getLocation();
        MigrationScript script;
        try {
            script = read(location);
        } catch (ParseException ex) {
            fail(wrapper, ex.getMessage(), ex);
            return true;
        }

        List<Migration> predecessors = predecessors(script, location);
        List<Outcome> outcomes = script.scope() == Scope.SYSTEM
                ? List.of(executor.apply(script, location, SYSTEM_TENANT, dataSourcesManager.getSystemDataSource(), predecessors))
                : tenantContext.executeForEachTenant(() -> executor.apply(script, location, tenantContext.getCurrentTenant()
                                                                                                         .getId(),
                        dataSourcesManager.getDefaultDataSource(), predecessors))
                               .stream()
                               .map(TenantResult::getResult)
                               .toList();

        String failures = messages(outcomes, Status.FAILED);
        if (!failures.isEmpty()) {
            Throwable cause = outcomes.stream()
                                      .map(Outcome::cause)
                                      .filter(Objects::nonNull)
                                      .findFirst()
                                      .orElse(null);
            fail(wrapper, failures, cause);
            return true;
        }
        String waiting = messages(outcomes, Status.WAITING);
        if (!waiting.isEmpty()) {
            // No state is registered: the lifecycle the phase matched on must survive for the next
            // round. The message reaches the "undepleted" report should the wait never end.
            migration.setError(waiting);
            return false;
        }
        callback.registerState(this, wrapper, success);
        return true;
    }

    /** The file as it is in the registry now - the artefact keeps what it declares, not the SQL. */
    private MigrationScript read(String location) throws ParseException {
        IResource resource = repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + location);
        if (!resource.exists()) {
            throw new ParseException("The migration [" + location + "] is no longer in the registry", 0);
        }
        return MigrationScript.parse(location, resource.getContent());
    }

    private List<Migration> predecessors(MigrationScript script, String location) {
        return migrationService.findAllByProject(script.project())
                               .stream()
                               .filter(other -> !other.getLocation()
                                                      .equals(location))
                               .filter(other -> Objects.equals(other.getScope(), script.scope()
                                                                                       .name()))
                               .filter(other -> MigrationScript.compareVersions(other.getVersion(), script.version()) < 0)
                               .toList();
    }

    private static String messages(List<Outcome> outcomes, Status status) {
        return outcomes.stream()
                       .filter(outcome -> outcome.status() == status)
                       .map(Outcome::message)
                       .collect(Collectors.joining("; "));
    }

    private void fail(TopologyWrapper<Migration> wrapper, String message, Throwable cause) {
        callback.addError(message);
        callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, message, cause);
    }

    @Override
    public void cleanupImpl(Migration migration) {
        try {
            migrationService.delete(migration);
        } catch (RuntimeException ex) {
            callback.addError(ex.getMessage());
            callback.registerState(this, migration, ArtefactLifecycle.DELETED, ex);
        }
    }

    /**
     * Included in the post-provisioning re-trigger, so a new tenant is migrated. The tenants are then
     * iterated in {@link #apply}, not per artefact by the base class - see the class comment.
     */
    @Override
    public boolean multitenantExecution() {
        return true;
    }

    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getFileExtension() {
        return MigrationScript.FILE_EXTENSION;
    }

    @Override
    public String getArtefactType() {
        return Migration.ARTEFACT_TYPE;
    }
}
