/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.transformers;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.odata.api.ODataEntity;
import org.eclipse.dirigible.components.odata.api.TableMetadataProvider;

import java.sql.SQLException;

/**
 * The Class DefaultTableMetadataProvider.
 */
public class DefaultTableMetadataProvider implements TableMetadataProvider {

    /** The db metadata util. */
    private final ODataDatabaseMetadataUtil odataDatabaseMetadataUtil;

    /**
     * Instantiates a new default table metadata provider.
     */
    public DefaultTableMetadataProvider() {
        this(new ODataDatabaseMetadataUtil());
    }

    /**
     * Instantiates a new default table metadata provider.
     *
     * @param odataDatabaseMetadataUtil the odata database metadata util
     */
    DefaultTableMetadataProvider(ODataDatabaseMetadataUtil odataDatabaseMetadataUtil) {
        this.odataDatabaseMetadataUtil = odataDatabaseMetadataUtil;
    }

    /**
     * Gets the persistence table model.
     *
     * @param odataEntityDefinition the o data entity definition
     * @return the persistence table model
     * @throws SQLException the SQL exception
     */
    @Override
    public Table getTableMetadata(ODataEntity odataEntityDefinition) throws SQLException {
        String table = odataEntityDefinition.getTable();
        String schema = null != odataEntityDefinition.getSchema() ? odataEntityDefinition.getSchema()
                : odataDatabaseMetadataUtil.getOdataArtifactTypeSchema(table);
        return odataDatabaseMetadataUtil.getTableMetadata(table, schema);
    }
}
