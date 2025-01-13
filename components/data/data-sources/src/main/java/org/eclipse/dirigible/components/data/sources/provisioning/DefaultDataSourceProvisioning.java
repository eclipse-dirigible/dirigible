/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.provisioning;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantProvisioningException;
import org.eclipse.dirigible.components.base.tenant.TenantProvisioningStep;
import org.eclipse.dirigible.components.data.sources.config.DefaultDataSourceName;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.manager.TenantDataSourceNameManager;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * The Class DefaultDataSourceProvisioning.
 */
@Component
class DefaultDataSourceProvisioning implements TenantProvisioningStep {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataSourceProvisioning.class);

    /** The data sources manager. */
    private final DataSourcesManager dataSourcesManager;

    /** The data source service. */
    private final DataSourceService dataSourceService;

    /** The tenant data source name manager. */
    private final TenantDataSourceNameManager tenantDataSourceNameManager;
    private final String defaultDataSourceName;

    DefaultDataSourceProvisioning(DataSourcesManager dataSourcesManager, DataSourceService dataSourceService,
            TenantDataSourceNameManager tenantDataSourceNameManager, @DefaultDataSourceName String defaultDataSourceName) {
        this.dataSourcesManager = dataSourcesManager;
        this.dataSourceService = dataSourceService;
        this.tenantDataSourceNameManager = tenantDataSourceNameManager;
        this.defaultDataSourceName = defaultDataSourceName;
    }

    /**
     * Execute.
     *
     * @param tenant the tenant
     * @throws TenantProvisioningException the tenant provisioning exception
     */
    @Override
    public void execute(Tenant tenant) throws TenantProvisioningException {
        LOGGER.info("Registering Default DataSource for tenant [{}]...", tenant);

        if (tenant.isDefault()) {
            LOGGER.info("Default DataSoruce for the default tenant [{}] doesn't need provisioning. It will be skipped.");
            return;
        }

        String userId = generateUserId();
        String password = PasswordGenerator.generateSecurePassword(20);
        createUser(tenant, userId, password);
        LOGGER.info("Created user with id [{}] for tenant [{}]", userId, tenant);

        String schema = createSchema(tenant, userId);
        LOGGER.info("Created schema [{}] for tenant [{}] and user [{}]", schema, tenant, userId);

        DataSource dataSource = registerDataSource(tenant, userId, password, schema);
        LOGGER.info("Registered data source [{}] for tenant [{}]", dataSource, tenant);

        LOGGER.info("Default DataSource for tenant [{}] has been registered.", tenant);

    }

    /**
     * Generate user id.
     *
     * @return the string
     */
    private String generateUserId() {
        return UUID.randomUUID()
                   .toString();
    }

    /**
     * Creates the user.
     *
     * @param tenant the tenant
     * @param userId the user id
     * @param password the password
     */
    private void createUser(Tenant tenant, String userId, String password) {
        javax.sql.DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String sql = SqlFactory.getNative(connection)
                                   .create()
                                   .user(userId, password)
                                   .build();

            try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                prepareStatement.execute();
            }
        } catch (SQLException ex) {
            throw new TenantProvisioningException(
                    "Failed to create user with id [" + userId + "] and pass [" + password + "] for tenant " + tenant, ex);
        }
    }

    /**
     * Creates the schema.
     *
     * @param tenant the tenant
     * @param userId the user id
     * @return the string
     * @throws TenantProvisioningException the tenant provisioning exception
     */
    private String createSchema(Tenant tenant, String userId) throws TenantProvisioningException {
        javax.sql.DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String schemaName = getSchemaName(tenant);
            String sql = SqlFactory.getNative(connection)
                                   .create()
                                   .schema(schemaName)
                                   .authorization(userId)
                                   .build();

            try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                prepareStatement.execute();
            }

            return schemaName;
        } catch (SQLException ex) {
            throw new TenantProvisioningException("Failed to create schema for tenant " + tenant, ex);
        }
    }

    /**
     * Register data source.
     *
     * @param tenant the tenant
     * @param userId the user id
     * @param password the password
     * @param schema the schema
     * @return the data source
     */
    private DataSource registerDataSource(Tenant tenant, String userId, String password, String schema) {
        DataSource defaultDS = dataSourcesManager.getDataSourceDefinition(defaultDataSourceName);

        DataSource datasource = new DataSource();
        datasource.setLocation("TENANT_n/a");
        datasource.setType(DataSource.ARTEFACT_TYPE);
        datasource.setCreatedBy("TENANT_PROVISIONING_JOB");
        datasource.setLifecycle(ArtefactLifecycle.CREATED);
        datasource.setPhase(ArtefactPhase.CREATE);

        String description = defaultDataSourceName + " for tenant " + tenant.getId();
        datasource.setDescription(description);

        datasource.setDriver(defaultDS.getDriver());
        datasource.setUsername(userId);
        datasource.setPassword(password);
        datasource.setUrl(defaultDS.getUrl());
        datasource.setSchema(schema);
        datasource.setProperties(defaultDS.getProperties());

        String name = tenantDataSourceNameManager.createName(tenant, defaultDS.getName());
        datasource.setName(name);

        datasource.updateKey();

        return dataSourceService.save(datasource);
    }

    /**
     * Gets the schema name.
     *
     * @param tenant the tenant
     * @return the schema name
     */
    private String getSchemaName(Tenant tenant) {
        return tenant.getId()
                     .toUpperCase();
    }

}
