/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.domain.DataSourceProperty;
import org.eclipse.dirigible.components.data.sources.manager.DataSourceInitializer;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.manager.TenantDataSourceNameManager;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.tenant.TenantFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The tenant's data source is a clone of the application's own with the tenant's credentials in it
 * - and, above all, a re-registration must leave the running application using the credentials that
 * were just sent, not the ones the pool was built with.
 */
class TenantDataSourceRegistrationServiceTest {

    private static final String DEFAULT_DS = "DefaultDB";
    private static final String TENANT_DS = "acme_DefaultDB";

    private final DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
    private final DataSourceService dataSourceService = mock(DataSourceService.class);
    private final DataSourceInitializer dataSourceInitializer = mock(DataSourceInitializer.class);
    private final TenantDataSourceNameManager nameManager = mock(TenantDataSourceNameManager.class);

    private final TenantDataSourceRegistrationService service = new TenantDataSourceRegistrationService(dataSourcesManager,
            dataSourceService, dataSourceInitializer, nameManager, new TenantFactory(), DEFAULT_DS);

    private final Tenant tenant = tenant();

    @BeforeEach
    void wireDefaults() throws SQLException {
        when(nameManager.createName(any(), any())).thenReturn(TENANT_DS);
        when(dataSourcesManager.getDataSourceDefinition(DEFAULT_DS)).thenReturn(defaultDataSource());
        when(dataSourceService.findOptionalByName(TENANT_DS)).thenReturn(Optional.empty());
        // built before the stubbing starts: the helper stubs mocks of its own, and Mockito cannot
        // nest that inside an unfinished when(...)
        DirigibleDataSource working = dataSourceReportingConnection(true);
        when(dataSourceInitializer.initialize(any())).thenReturn(working);
    }

    @Test
    void aNewDataSourceClonesTheDefaultOneWithTheSuppliedCredentials() {
        assertTrue(service.register(tenant, parameter()));

        DataSource saved = savedDataSource();
        assertEquals(TENANT_DS, saved.getName());
        assertEquals("TENANT_DEFAULT", saved.getLocation());
        assertEquals("TENANT_PROVISIONING_API", saved.getCreatedBy());
        assertEquals("u_acme", saved.getUsername());
        assertEquals("s3cret", saved.getPassword());
        assertEquals("ACME", saved.getSchema());
        assertEquals("jdbc:h2:mem:default", saved.getUrl(), "the URL is inherited from the default data source");
        assertEquals("org.h2.Driver", saved.getDriver(), "the driver is inherited from the default data source");
    }

    /**
     * The URL, the driver and the connection properties are never a caller's to choose.
     *
     * <p>
     * They are executable surface - an H2 URL can carry {@code INIT=RUNSCRIPT FROM '<url>'}, a
     * PostgreSQL connection property can name a {@code socketFactory} class - so a request that could
     * set them would turn "may register a tenant's credentials" into "may run code on the application
     * server". They are not fields of the parameter at all; this pins that the definition takes them
     * from the application's own data source and from nowhere else.
     */
    @Test
    void theUrlDriverAndPropertiesComeOnlyFromTheApplicationsOwnDataSource() {
        service.register(tenant, parameter());

        DataSource saved = savedDataSource();
        assertEquals("jdbc:h2:mem:default", saved.getUrl());
        assertEquals("org.h2.Driver", saved.getDriver());
        assertEquals(1, saved.getProperties()
                             .size());
        assertEquals("ssl", saved.getProperties()
                                 .get(0)
                                 .getName());
        assertEquals("false", saved.getProperties()
                                   .get(0)
                                   .getValue());
    }

    /** Re-registration replaces the definition in place - two rows would be two data sources. */
    @Test
    void reRegistrationUpdatesTheExistingDefinition() {
        DataSource existing = existingTenantDataSource();
        when(dataSourceService.findOptionalByName(TENANT_DS)).thenReturn(Optional.of(existing));

        assertFalse(service.register(tenant, parameter()), "an existing data source is updated, not created");

        assertSame(existing, savedDataSource());
        assertEquals("s3cret", existing.getPassword());
    }

    /**
     * The reason the eviction is unconditional: an initialized pool keeps the password it was built
     * with, and only removing it closes it. Without this, a rotated password would take effect at the
     * next restart.
     */
    @Test
    void theLivePoolIsAlwaysEvicted() {
        service.register(tenant, parameter());

        verify(dataSourceInitializer).removeInitializedDataSource(TENANT_DS);
    }

    /**
     * Saving a definition initializes its pool too, through a data source lifecycle listener, so a
     * failure there is the same failure as a refused connection and has to reach the caller the same
     * way. Before this was handled it surfaced as an unhandled 500 with no message.
     */
    @Test
    void aPoolFailureRaisedBySavingIsAlsoAnsweredAsBadGateway() {
        when(dataSourceService.save(any())).thenThrow(new IllegalStateException("Failed to initialize pool: Wrong user name or password"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(tenant, parameter()));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        assertTrue(ex.getReason()
                     .contains("Wrong user name or password"),
                ex.getReason());
    }

    @Test
    void credentialsThatDoNotWorkAreRefusedAndNothingIsStored() throws SQLException {
        when(dataSourceInitializer.initialize(any())).thenThrow(new IllegalStateException("Wrong user name or password"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(tenant, parameter()));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        assertTrue(ex.getReason()
                     .contains("Wrong user name or password"),
                "the database's own complaint has to reach the caller, got: " + ex.getReason());
        verify(dataSourceService, never()).save(any());
        // twice: once before the attempt, once to drop the pool the failed attempt may have left
        verify(dataSourceInitializer, org.mockito.Mockito.times(2)).removeInitializedDataSource(TENANT_DS);
    }

    @Test
    void aRegisteredDataSourceIsRecognizedAsThePreconditionOfAnActivation() {
        when(dataSourceService.findOptionalByName(TENANT_DS)).thenReturn(Optional.of(existingTenantDataSource()));

        assertTrue(service.isRegistered(tenant));
    }

    @Test
    void anAbsentDataSourceIsRecognizedAsMissing() {
        assertFalse(service.isRegistered(tenant));
    }

    /**
     * The portability guarantee, stated as an assertion.
     *
     * <p>
     * A hand-written {@code SELECT 1} here would look portable and would refuse working credentials on
     * SAP HANA, which needs {@code SELECT 1 FROM DUMMY} - the reason {@code HanaDatabaseConfigurator}
     * registers exactly that as the pool's test query - and on Derby, which needs a {@code FROM} clause
     * of its own. The check must therefore ask the driver, never the SQL dialect, and this test fails
     * the moment somebody issues a statement here again.
     */
    @Test
    void connectivityIsCheckedWithoutIssuingAnySqlOfOurOwn() throws SQLException {
        DirigibleConnection connection = mock(DirigibleConnection.class);
        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(dataSourceInitializer.initialize(any())).thenReturn(dataSource);

        service.register(tenant, parameter());

        verify(connection).isValid(anyInt());
        verify(connection, never()).createStatement();
        verify(connection, never()).prepareStatement(any());
    }

    @Test
    void aConnectionTheDriverCallsUnusableIsRefusedAndNothingIsStored() throws SQLException {
        DirigibleDataSource unusable = dataSourceReportingConnection(false);
        when(dataSourceInitializer.initialize(any())).thenReturn(unusable);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(tenant, parameter()));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        verify(dataSourceService, never()).save(any());
    }

    private DataSource savedDataSource() {
        ArgumentCaptor<DataSource> captor = ArgumentCaptor.forClass(DataSource.class);
        verify(dataSourceService).save(captor.capture());
        return captor.getValue();
    }

    private static TenantDataSourceParameter parameter() {
        TenantDataSourceParameter parameter = new TenantDataSourceParameter();
        parameter.setUsername("u_acme");
        parameter.setPassword("s3cret");
        parameter.setSchema("ACME");
        return parameter;
    }

    private static DataSource defaultDataSource() {
        DataSource dataSource = new DataSource("-", DEFAULT_DS, "", "org.h2.Driver", "jdbc:h2:mem:default", "sa", "");
        DataSourceProperty property = new DataSourceProperty();
        property.setName("ssl");
        property.setValue("false");
        dataSource.setProperties(List.of(property));
        return dataSource;
    }

    private static DataSource existingTenantDataSource() {
        return new DataSource("TENANT_DEFAULT", TENANT_DS, "", "org.h2.Driver", "jdbc:h2:mem:default", "u_acme", "old-password");
    }

    /**
     * A data source whose connection the driver reports as usable.
     *
     * @param usable what {@code isValid} answers
     * @return the data source
     * @throws SQLException never - the mocks declare it
     */
    private static DirigibleDataSource dataSourceReportingConnection(boolean usable) throws SQLException {
        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        DirigibleConnection connection = mock(DirigibleConnection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(usable);
        return dataSource;
    }

    private static Tenant tenant() {
        Tenant tenant = new Tenant("-", "Acme Ltd", "", "acme", TenantStatus.PENDING_ACTIVATION);
        tenant.setId("acme");
        return tenant;
    }
}
