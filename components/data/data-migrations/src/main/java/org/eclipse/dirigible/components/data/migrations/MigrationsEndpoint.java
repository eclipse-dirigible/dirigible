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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.migrations.MigrationLedger.Entry;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.security.RolesAllowed;

/**
 * The migrations ledger, read-only: what a deployment's smoke test asserts after a release
 * ("version N shipped three migrations and each is applied once"). It lists the ledger of the
 * caller's tenant; a caller in the default tenant - the operator's - also sees the
 * {@code tenant: system} migrations, which no other tenant's administrator has any business
 * reading.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "migrations")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
class MigrationsEndpoint extends BaseEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationsEndpoint.class);

    private final MigrationLedger ledger;
    private final DataSourcesManager dataSourcesManager;
    private final TenantContext tenantContext;

    MigrationsEndpoint(MigrationLedger ledger, DataSourcesManager dataSourcesManager, TenantContext tenantContext) {
        this.ledger = ledger;
        this.dataSourcesManager = dataSourcesManager;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    List<Entry> list() {
        try {
            List<Entry> entries = new ArrayList<>(ledger.findAll(dataSourcesManager.getDefaultDataSource()));
            if (tenantContext.getCurrentTenant()
                             .isDefault()) {
                entries.addAll(ledger.findAll(dataSourcesManager.getSystemDataSource()));
            }
            return entries;
        } catch (SQLException ex) {
            LOGGER.error("Failed to read the migrations ledger", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the migrations ledger: " + ex.getMessage(),
                    ex);
        }
    }
}
