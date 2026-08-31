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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.data.sources.config.DefaultDataSourceName;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.domain.DataSourceProperty;
import org.eclipse.dirigible.components.data.sources.manager.DataSourceInitializer;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.manager.TenantDataSourceNameManager;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.tenant.TenantFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registers the data source of an externally provisioned tenant.
 *
 * <p>
 * The definition is a clone of the application's own default data source with the tenant's
 * credentials and schema put in its place - the same shape the built-in provisioner produces, so
 * everything downstream resolves it exactly as it resolves a tenant it created itself.
 *
 * <p>
 * Two things make the upsert honest. An already-initialized connection pool does not notice that
 * its definition changed - only removing it closes it - so a re-registration that carried a rotated
 * password would otherwise keep serving the old one until the next restart; the pool is therefore
 * always evicted. And the credentials are tried before they are stored, through the platform's own
 * initialization rather than a bare JDBC call, so the check exercises the very pool the application
 * will use - driver resolution, connection properties and the database's own validation query
 * included. A check that fails leaves nothing behind: the caller gets the database's own complaint
 * and can register again.
 */
@Service
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantDataSourceRegistrationService {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantDataSourceRegistrationService.class);

    /** The location the built-in provisioner uses as well, so both kinds of tenant look alike. */
    private static final String LOCATION = "TENANT_DEFAULT";

    /** Marks the data sources this API registered. */
    private static final String CREATED_BY = "TENANT_PROVISIONING_API";

    /** How long the driver may take to answer whether a connection is usable. Hikari's own default. */
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    /** The data sources manager. */
    private final DataSourcesManager dataSourcesManager;

    /** The data source service. */
    private final DataSourceService dataSourceService;

    /** The data source initializer. */
    private final DataSourceInitializer dataSourceInitializer;

    /** The tenant data source name manager. */
    private final TenantDataSourceNameManager tenantDataSourceNameManager;

    /** The tenant factory. */
    private final TenantFactory tenantFactory;

    /** The default data source name. */
    private final String defaultDataSourceName;

    /**
     * Instantiates a new tenant data source registration service.
     *
     * @param dataSourcesManager the data sources manager
     * @param dataSourceService the data source service
     * @param dataSourceInitializer the data source initializer
     * @param tenantDataSourceNameManager the tenant data source name manager
     * @param tenantFactory the tenant factory
     * @param defaultDataSourceName the default data source name
     */
    TenantDataSourceRegistrationService(DataSourcesManager dataSourcesManager, DataSourceService dataSourceService,
            DataSourceInitializer dataSourceInitializer, TenantDataSourceNameManager tenantDataSourceNameManager,
            TenantFactory tenantFactory, @DefaultDataSourceName String defaultDataSourceName) {
        this.dataSourcesManager = dataSourcesManager;
        this.dataSourceService = dataSourceService;
        this.dataSourceInitializer = dataSourceInitializer;
        this.tenantDataSourceNameManager = tenantDataSourceNameManager;
        this.tenantFactory = tenantFactory;
        this.defaultDataSourceName = defaultDataSourceName;
    }

    /**
     * Registers or updates the tenant's default data source.
     *
     * @param tenant the tenant
     * @param parameter the credentials
     * @return true when the data source was created rather than updated
     */
    boolean register(Tenant tenant, TenantDataSourceParameter parameter) {
        String name = tenantDataSourceName(tenant);
        Optional<DataSource> existing = dataSourceService.findOptionalByName(name);

        DataSource dataSource = existing.orElseGet(DataSource::new);
        applyDefinition(dataSource, name, tenant, parameter);

        // A live pool keeps the credentials it was built with, whatever the definition says.
        dataSourceInitializer.removeInitializedDataSource(name);

        verifyConnectivity(dataSource);
        // Saving initializes the pool as well, through a data source lifecycle listener, so a failure
        // here is the same kind of failure and has to reach the caller the same way - not as a 500.
        failAsBadGateway(dataSource, () -> dataSourceService.save(dataSource));

        LOGGER.info("Data source [{}] of tenant [{}] has been {}.", name, tenant.getId(), existing.isPresent() ? "updated" : "registered");
        return existing.isEmpty();
    }

    /**
     * Whether the tenant's data source is registered - the precondition of an activation.
     *
     * @param tenant the tenant
     * @return true, if the data source is registered
     */
    boolean isRegistered(Tenant tenant) {
        return dataSourceService.findOptionalByName(tenantDataSourceName(tenant))
                                .isPresent();
    }

    /**
     * The name of the tenant's default data source, by the platform's own convention.
     *
     * @param tenant the tenant
     * @return the name
     */
    String tenantDataSourceName(Tenant tenant) {
        return tenantDataSourceNameManager.createName(tenantFactory.createFromEntity(tenant), defaultDataSourceName);
    }

    /**
     * Fills the definition from the application's default data source and the supplied credentials.
     *
     * @param dataSource the definition to fill, existing or new
     * @param name the data source name
     * @param tenant the tenant
     * @param parameter the credentials
     */
    private void applyDefinition(DataSource dataSource, String name, Tenant tenant, TenantDataSourceParameter parameter) {
        DataSource defaults = dataSourcesManager.getDataSourceDefinition(defaultDataSourceName);

        dataSource.setName(name);
        dataSource.setLocation(LOCATION);
        dataSource.setType(DataSource.ARTEFACT_TYPE);
        dataSource.setDescription(defaultDataSourceName + " for tenant " + tenant.getId());
        dataSource.setCreatedBy(CREATED_BY);
        dataSource.setLifecycle(ArtefactLifecycle.CREATED);
        dataSource.setPhase(ArtefactPhase.CREATE);

        dataSource.setUsername(parameter.getUsername());
        dataSource.setPassword(parameter.getPassword());
        dataSource.setSchema(parameter.getSchema());

        // Never from the caller: a JDBC URL and driver properties are executable surface - an H2 URL
        // carrying INIT=RUNSCRIPT, a PostgreSQL socketFactory property - so taking them from a request
        // would let whoever may register a tenant's credentials run code on this server instead.
        dataSource.setUrl(defaults.getUrl());
        dataSource.setDriver(defaults.getDriver());
        dataSource.setProperties(copyProperties(dataSource, defaults));

        dataSource.updateKey();
    }

    /**
     * The connection properties of the application's own data source, copied onto the tenant's.
     *
     * @param dataSource the owning definition
     * @param defaults the application's default data source
     * @return the properties
     */
    private List<DataSourceProperty> copyProperties(DataSource dataSource, DataSource defaults) {
        List<DataSourceProperty> properties = new ArrayList<>();
        if (defaults.getProperties() != null) {
            defaults.getProperties()
                    .forEach(p -> properties.add(property(dataSource, p.getName(), p.getValue())));
        }
        return properties;
    }

    /**
     * Property.
     *
     * @param dataSource the data source
     * @param name the name
     * @param value the value
     * @return the data source property
     */
    private static DataSourceProperty property(DataSource dataSource, String name, String value) {
        DataSourceProperty property = new DataSourceProperty();
        property.setName(name);
        property.setValue(value);
        property.setDatasource(dataSource);
        return property;
    }

    /**
     * Opens a connection with the supplied credentials and asks the driver whether it is usable.
     *
     * <p>
     * Through the platform's own initializer rather than {@code DriverManager}, so what is proven is
     * that the pool the application will use can be built and can connect. Building it is already most
     * of the answer: the pool validates its first connection while it is being constructed, using
     * whichever mechanism is right for that database - the {@code connectionTestQuery} a
     * {@code DatabaseConfigurator} registered, or {@link Connection#isValid(int)} where none did.
     *
     * <p>
     * Deliberately <b>no SQL of our own</b>. A hand-written {@code SELECT 1} looks portable and is not:
     * SAP HANA needs {@code SELECT 1 FROM DUMMY} - which is exactly why
     * {@code HanaDatabaseConfigurator} sets that as its test query - and Derby needs a {@code FROM}
     * clause too. Issuing a statement here would walk past the per-database knowledge the platform
     * already has and refuse working credentials on those systems. {@link Connection#isValid(int)} is
     * the JDBC-standard question, asked of the driver rather than of the SQL dialect.
     *
     * <p>
     * The pool stays initialized afterwards - it is the correct one either way - and is dropped again
     * when the check fails.
     *
     * @param dataSource the definition to try
     */
    private void verifyConnectivity(DataSource dataSource) {
        failAsBadGateway(dataSource, () -> {
            DirigibleDataSource initialized = dataSourceInitializer.initialize(dataSource);
            try (Connection connection = initialized.getConnection()) {
                if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                    throw new SQLException("The driver does not consider the connection usable");
                }
            }
        });
    }

    /**
     * Runs work that touches the tenant's database and turns any failure into the answer this API owes
     * a caller: the status that says "your credentials did not work" and the database's own words.
     *
     * @param dataSource the definition being registered
     * @param work the work to run
     */
    private void failAsBadGateway(DataSource dataSource, SqlWork work) {
        try {
            work.run();
        } catch (SQLException | RuntimeException ex) {
            dataSourceInitializer.removeInitializedDataSource(dataSource.getName());
            LOGGER.warn("Failed to connect to the database of data source [{}] with the supplied credentials.", dataSource.getName(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not connect to the database of data source ["
                    + dataSource.getName() + "] with the supplied credentials: " + rootMessage(ex));
        }
    }

    /**
     * Work that may fail the way a database fails.
     */
    @FunctionalInterface
    private interface SqlWork {

        /**
         * Run.
         *
         * @throws SQLException if the database refuses
         */
        void run() throws SQLException;
    }

    /**
     * The database's own complaint is usually the innermost one; the wrappers around it say nothing a
     * caller can act on.
     *
     * @param ex the exception
     * @return the message of its root cause
     */
    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
