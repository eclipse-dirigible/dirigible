/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.service.TableService;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.persistence.utils.DatabaseMetadataUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Which tables a tenant export takes: the application's, never the platform's.
 *
 * <p>
 * A tenant's schema is not only the application's - the platform keeps tables of its own beside
 * them. So the set is not a schema listing: it is the tables the published applications declare
 * (the deployed {@link Table} artefacts) that exist in the tenant's schema. The tables those depend
 * on through foreign keys are added by the schema export itself.
 */
@Component
@Conditional(TenantExportEnabledCondition.class)
class TenantExportTables {

    /** The prefix of the platform's own tables. */
    private static final String PLATFORM_PREFIX = "DIRIGIBLE_";

    private final DataSourcesManager dataSourcesManager;
    private final TableService tableService;

    TenantExportTables(DataSourcesManager dataSourcesManager, TableService tableService) {
        this.dataSourcesManager = dataSourcesManager;
        this.tableService = tableService;
    }

    /**
     * What the current tenant's export reads from.
     *
     * @param dataSource the name of the default data source, which resolves to the current tenant's
     * @param schema the schema the tenant's tables are in
     * @param tables the application's tables in it
     */
    record Source(String dataSource, String schema, Set<String> tables) {
    }

    /**
     * Resolves the current tenant's data source, its schema and the application's tables in it.
     *
     * @return the source
     * @throws SQLException when the tenant's database cannot be read
     */
    Source resolve() throws SQLException {
        String dataSourceName = DirigibleConfig.DEFAULT_DATA_SOURCE_NAME.getStringValue();
        DirigibleDataSource dataSource = dataSourcesManager.getDataSource(dataSourceName);
        String schema;
        try (Connection connection = dataSource.getConnection()) {
            schema = connection.getSchema();
        }
        List<String> schemaTables = DatabaseMetadataUtil.getTablesInSchema(dataSource, schema);
        List<String> declared = tableService.getAll()
                                            .stream()
                                            .map(Table::getName)
                                            .toList();
        return new Source(dataSourceName, schema, select(schemaTables, declared));
    }

    /**
     * The tables of a schema that the applications declare, by name ignoring case, without the
     * platform's own.
     *
     * @param schemaTables the tables in the tenant's schema, or null when it has none
     * @param declaredTables the names of the deployed table artefacts
     * @return the tables to export, as the schema names them
     */
    static Set<String> select(Collection<String> schemaTables, Collection<String> declaredTables) {
        if (schemaTables == null) {
            return Set.of();
        }
        Set<String> declared = declaredTables.stream()
                                             .filter(name -> name != null)
                                             .map(TenantExportTables::normalize)
                                             .collect(Collectors.toSet());
        return schemaTables.stream()
                           .filter(table -> !normalize(table).startsWith(PLATFORM_PREFIX))
                           .filter(table -> declared.contains(normalize(table)))
                           .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String normalize(String table) {
        return table.toUpperCase(Locale.ROOT);
    }
}
