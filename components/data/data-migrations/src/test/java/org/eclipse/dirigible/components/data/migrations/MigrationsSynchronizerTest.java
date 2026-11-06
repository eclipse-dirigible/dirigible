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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantArtefactLedger;
import org.eclipse.dirigible.components.base.tenant.TenantResult;
import org.eclipse.dirigible.components.data.migrations.MigrationExecutor.Outcome;
import org.eclipse.dirigible.components.data.migrations.MigrationExecutor.Predecessor;
import org.eclipse.dirigible.components.data.migrations.MigrationExecutor.Status;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** How the per-database outcomes become the ONE state of the artefact. */
class MigrationsSynchronizerTest {

    private static final String V1 = "/orders/migrations/001__first.migration";
    private static final String V2 = "/orders/migrations/002__second.migration";
    private static final String V3 = "/orders/migrations/003__third.migration";

    private final MigrationService migrationService = mock(MigrationService.class);
    private final MigrationExecutor executor = mock(MigrationExecutor.class);
    private final DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
    private final TenantContext tenantContext = mock(TenantContext.class);
    private final IRepository repository = mock(IRepository.class);
    private final SynchronizerCallback callback = mock(SynchronizerCallback.class);
    private final TenantArtefactLedger tenantArtefactLedger = mock(TenantArtefactLedger.class);

    private final DirigibleDataSource tenantDataSource = mock(DirigibleDataSource.class);
    private final DirigibleDataSource systemDataSource = mock(DirigibleDataSource.class);

    private final Map<String, String> registry = new HashMap<>();

    private MigrationsSynchronizer synchronizer;

    @BeforeEach
    void setUp() throws Exception {
        synchronizer =
                new MigrationsSynchronizer(migrationService, executor, dataSourcesManager, tenantContext, repository, tenantArtefactLedger);
        synchronizer.setCallback(callback);
        when(dataSourcesManager.getDefaultDataSource()).thenReturn(tenantDataSource);
        when(dataSourcesManager.getSystemDataSource()).thenReturn(systemDataSource);
        when(repository.getResource(anyString())).thenAnswer(invocation -> resource(invocation.getArgument(0)));
        givenTenants("tenant-a", "tenant-b");
    }

    @Test
    void oneTenantsFailureIsNotHiddenByAnotherTenantsSuccess() throws Exception {
        Migration migration = published(V1, "UPDATE ORDERS SET STATUS = 'OPEN';", ArtefactLifecycle.NEW);
        givenOutcome("tenant-a", new Outcome("tenant-a", Status.APPLIED, null));
        givenOutcome("tenant-b", new Outcome("tenant-b", Status.FAILED, "boom in tenant-b"));

        boolean depleted = synchronizer.completeImpl(wrap(migration), ArtefactPhase.CREATE);

        assertThat(depleted).isTrue();
        verify(callback).registerState(eq(synchronizer), any(TopologyWrapper.class), eq(ArtefactLifecycle.FAILED),
                contains("boom in tenant-b"), any());
        verify(callback, never()).registerState(any(), any(TopologyWrapper.class), eq(ArtefactLifecycle.CREATED));
    }

    /**
     * Each tenant's outcome is its own, so a tenant's initialization status reports its own failure
     * only.
     */
    @Test
    void eachTenantsOutcomeIsRecordedForThatTenant() throws Exception {
        Migration migration = published(V1, "UPDATE ORDERS SET STATUS = 'OPEN';", ArtefactLifecycle.NEW);
        givenOutcome("tenant-a", new Outcome("tenant-a", Status.APPLIED, null));
        givenOutcome("tenant-b", new Outcome("tenant-b", Status.FAILED, "boom in tenant-b"));

        synchronizer.completeImpl(wrap(migration), ArtefactPhase.CREATE);

        verify(tenantArtefactLedger).record(migration, "tenant-a", ArtefactLifecycle.CREATED, null);
        verify(tenantArtefactLedger).record(migration, "tenant-b", ArtefactLifecycle.FAILED, "boom in tenant-b");
    }

    @Test
    void aMigrationEveryTenantTookIsCreated() throws Exception {
        Migration migration = published(V1, "UPDATE ORDERS SET STATUS = 'OPEN';", ArtefactLifecycle.NEW);
        givenOutcome("tenant-a", new Outcome("tenant-a", Status.APPLIED, null));
        givenOutcome("tenant-b", new Outcome("tenant-b", Status.ALREADY_APPLIED, null));

        assertThat(synchronizer.completeImpl(wrap(migration), ArtefactPhase.CREATE)).isTrue();

        verify(callback).registerState(eq(synchronizer), any(TopologyWrapper.class), eq(ArtefactLifecycle.CREATED));
    }

    @Test
    void aMigrationWaitingForAnEarlierVersionStaysUndepletedWithItsLifecycle() throws Exception {
        Migration migration = published(V2, "UPDATE ORDERS SET STATUS = 'OPEN';", ArtefactLifecycle.NEW);
        givenOutcome("tenant-a", new Outcome("tenant-a", Status.APPLIED, null));
        givenOutcome("tenant-b", new Outcome("tenant-b", Status.WAITING, "waits for version [1]"));

        boolean depleted = synchronizer.completeImpl(wrap(migration), ArtefactPhase.CREATE);

        assertThat(depleted).isFalse();
        assertThat(migration.getLifecycle()).isEqualTo(ArtefactLifecycle.NEW);
        assertThat(migration.getError()).contains("waits for version [1]");
        verifyNoInteractions(callback);
    }

    @Test
    void aSystemMigrationRunsOnceOnTheSystemDatabase() throws Exception {
        Migration migration = published(V1, "-- tenant: system\nUPDATE ORDERS SET STATUS = 'OPEN';", ArtefactLifecycle.NEW);
        when(executor.apply(any(), eq(V1), eq(MigrationsSynchronizer.SYSTEM_TENANT), eq(systemDataSource), anyList())).thenReturn(
                new Outcome(MigrationsSynchronizer.SYSTEM_TENANT, Status.APPLIED, null));

        assertThat(synchronizer.completeImpl(wrap(migration), ArtefactPhase.CREATE)).isTrue();

        verify(tenantContext, never()).executeForEachMaintainedTenant(any());
        verify(callback).registerState(eq(synchronizer), any(TopologyWrapper.class), eq(ArtefactLifecycle.CREATED));
    }

    @Test
    void thePredecessorsAreTheLowerVersionsOfTheSameProjectAndScope() throws Exception {
        Migration first = published(V1, "UPDATE ORDERS SET STATUS = 'A';", ArtefactLifecycle.CREATED);
        Migration systemZero =
                published("/orders/migrations/000__system.migration", "-- tenant: system\nUPDATE X SET Y = 1;", ArtefactLifecycle.CREATED);
        Migration third = published(V3, "UPDATE ORDERS SET STATUS = 'C';", ArtefactLifecycle.NEW);
        Migration second = published(V2, "UPDATE ORDERS SET STATUS = 'B';", ArtefactLifecycle.NEW);
        when(migrationService.findAllByProject("orders")).thenReturn(List.of(first, systemZero, third, second));
        when(executor.apply(any(), any(), any(), any(), anyList())).thenAnswer(
                invocation -> new Outcome(invocation.getArgument(2), Status.APPLIED, null));

        synchronizer.completeImpl(wrap(second), ArtefactPhase.CREATE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Predecessor>> predecessors = ArgumentCaptor.forClass(List.class);
        verify(executor, org.mockito.Mockito.atLeastOnce()).apply(any(), eq(V2), any(), any(), predecessors.capture());
        assertThat(predecessors.getValue()).singleElement()
                                           .satisfies(predecessor -> {
                                               assertThat(predecessor.migration()).isSameAs(first);
                                               assertThat(predecessor.script()
                                                                     .sql()).contains("STATUS = 'A'");
                                           });
    }

    /**
     * A pass offers a project's migrations in no particular order - the order of a hash set, different
     * from boot to boot - and across phases: a new version in CREATE, an earlier one in START. Whatever
     * comes first, the versions apply lowest first, every offer completes, and nothing is reported as
     * failed (#8496). Real executor, real ledger, in-memory database.
     */
    @Test
    void aProjectsMigrationsOfferedInReverseApplyInVersionOrderWithoutAFailure() throws Exception {
        JdbcDataSource database = new JdbcDataSource();
        database.setURL("jdbc:h2:mem:migrations_synchronizer_order_test;DB_CLOSE_DELAY=-1");
        // Both tenants of setUp() share this one database: the second finds every version applied.
        when(tenantDataSource.getConnection()).thenAnswer(invocation -> h2(database.getConnection()));
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE ORDERS (ID INT PRIMARY KEY, STATUS VARCHAR(40))");
            statement.execute("INSERT INTO ORDERS VALUES (1, NULL)");
        }
        MigrationsSynchronizer ordered = new MigrationsSynchronizer(migrationService, new MigrationExecutor(new MigrationLedger()),
                dataSourcesManager, tenantContext, repository, tenantArtefactLedger);
        ordered.setCallback(callback);
        Migration first = published(V1, "UPDATE ORDERS SET STATUS = 'OPEN' WHERE STATUS IS NULL;", ArtefactLifecycle.NEW);
        Migration second = published(V2, "UPDATE ORDERS SET STATUS = 'ARCHIVED' WHERE STATUS = 'OPEN';", ArtefactLifecycle.NEW);
        Migration third = published(V3, "UPDATE ORDERS SET STATUS = STATUS || '-3' WHERE STATUS = 'ARCHIVED';", ArtefactLifecycle.NEW);
        // An earlier pass left the oldest one FAILED: this pass retries it in START, after CREATE.
        Migration zero = published("/orders/migrations/000__zero.migration", "UPDATE ORDERS SET STATUS = NULL;", ArtefactLifecycle.FAILED);
        when(migrationService.findAllByProject("orders")).thenReturn(List.of(zero, first, second, third));

        for (Migration migration : List.of(third, second, first)) {
            assertThat(ordered.completeImpl(wrap(migration, ordered), ArtefactPhase.CREATE)).as(migration.getLocation() + " completes")
                                                                                            .isTrue();
        }
        assertThat(ordered.completeImpl(wrap(zero, ordered), ArtefactPhase.START)).isTrue();

        try (Connection connection = database.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT STATUS FROM ORDERS WHERE ID = 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).as("0 cleared it, 1 opened it, 2 archived it, 3 marked the archived row")
                                              .isEqualTo("ARCHIVED-3");
        }
        assertThat(new MigrationLedger().findAll(database)).extracting(MigrationLedger.Entry::version)
                                                           .containsExactlyInAnyOrder("0", "1", "2", "3");
        verify(callback, never()).registerState(any(), any(TopologyWrapper.class), eq(ArtefactLifecycle.FAILED), anyString(), any());
        verify(callback, never()).addError(anyString());
        verify(callback, org.mockito.Mockito.times(4)).registerState(eq(ordered), any(TopologyWrapper.class),
                eq(ArtefactLifecycle.CREATED));
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
    }

    @Test
    void aMigrationOnlyItsPhaseMatchesIsTouched() throws Exception {
        Migration applied = published(V1, "UPDATE ORDERS SET STATUS = 'OPEN';", ArtefactLifecycle.CREATED);

        assertThat(synchronizer.completeImpl(wrap(applied), ArtefactPhase.CREATE)).isTrue();
        assertThat(synchronizer.completeImpl(wrap(applied), ArtefactPhase.UPDATE)).isTrue();
        assertThat(synchronizer.completeImpl(wrap(applied), ArtefactPhase.START)).isTrue();

        verifyNoInteractions(executor);
    }

    @Test
    void anApplyAfterTheFileWasRemovedFailsTheArtefact() throws Exception {
        Migration migration = published(V1, "UPDATE ORDERS SET STATUS = 'OPEN';", ArtefactLifecycle.FAILED);
        registry.remove(V1);

        assertThat(synchronizer.completeImpl(wrap(migration), ArtefactPhase.START)).isTrue();

        verify(callback).registerState(eq(synchronizer), any(TopologyWrapper.class), eq(ArtefactLifecycle.FAILED),
                contains("no longer in the registry"), any(ParseException.class));
        verifyNoInteractions(executor);
    }

    @Test
    void aVersionSpelledWithLeadingZerosIsClaimedByTheFileSpellingItWithout() throws Exception {
        Migration first = published(V1, "UPDATE ORDERS SET STATUS = 'A';", ArtefactLifecycle.CREATED);
        when(migrationService.findAllByProject("orders")).thenReturn(List.of(first));
        String other = "/orders/migrations/V1__other.migration";

        assertThatThrownBy(() -> synchronizer.parseImpl(other,
                "UPDATE ORDERS SET STATUS = 'B';".getBytes(StandardCharsets.UTF_8))).isInstanceOf(ParseException.class)
                                                                                    .hasMessageContaining("Version [1]")
                                                                                    .hasMessageContaining(V1)
                                                                                    .hasMessageContaining(other);
    }

    private Migration published(String location, String sql, ArtefactLifecycle lifecycle) throws ParseException {
        registry.put(location, sql);
        Migration migration = new Migration(location, location.substring(location.lastIndexOf('/') + 1),
                MigrationScript.parse(location, sql.getBytes(StandardCharsets.UTF_8)));
        migration.setLifecycle(lifecycle);
        return migration;
    }

    private TopologyWrapper<Migration> wrap(Migration migration) {
        return wrap(migration, synchronizer);
    }

    private static TopologyWrapper<Migration> wrap(Migration migration, MigrationsSynchronizer owner) {
        return new TopologyWrapper<>(migration, new HashMap<>(), owner);
    }

    /** The connection the platform hands out: a JDBC connection that knows its database system. */
    private static DirigibleConnection h2(Connection connection) {
        return mock(DirigibleConnection.class, call -> {
            switch (call.getMethod()
                        .getName()) {
                case "getDatabaseSystem":
                    return DatabaseSystem.H2;
                case "isOfType":
                    return DatabaseSystem.H2 == call.getArgument(0);
                default:
                    try {
                        return call.getMethod()
                                   .invoke(connection, call.getArguments());
                    } catch (InvocationTargetException ex) {
                        throw ex.getCause();
                    }
            }
        });
    }

    private IResource resource(String path) {
        String location = path.substring(IRepositoryStructure.PATH_REGISTRY_PUBLIC.length());
        IResource resource = mock(IResource.class);
        String content = registry.get(location);
        when(resource.exists()).thenReturn(content != null);
        when(resource.getContent()).thenReturn(content == null ? null : content.getBytes(StandardCharsets.UTF_8));
        return resource;
    }

    private void givenOutcome(String tenant, Outcome outcome) {
        when(executor.apply(any(), any(), eq(tenant), eq(tenantDataSource), anyList())).thenReturn(outcome);
    }

    /** Runs the callable once per tenant with that tenant current, as the platform's context does. */
    @SuppressWarnings("unchecked")
    private void givenTenants(String... ids) throws Exception {
        AtomicReference<Tenant> current = new AtomicReference<>();
        when(tenantContext.getCurrentTenant()).thenAnswer(invocation -> current.get());
        when(tenantContext.executeForEachMaintainedTenant(any())).thenAnswer(invocation -> {
            CallableResultAndException<Object, ?> callable = invocation.getArgument(0);
            List<TenantResult<Object>> results = new ArrayList<>();
            for (String id : ids) {
                Tenant tenant = mock(Tenant.class);
                when(tenant.getId()).thenReturn(id);
                current.set(tenant);
                Object result = callable.call();
                results.add(new TenantResult<>() {
                    @Override
                    public Tenant getTenant() {
                        return tenant;
                    }

                    @Override
                    public Object getResult() {
                        return result;
                    }
                });
            }
            current.set(null);
            return results;
        });
    }
}
