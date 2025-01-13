/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.api;

import java.sql.SQLException;

import org.eclipse.dirigible.components.data.structures.domain.Table;

/**
 * The Interface ITableMetadataProvider.
 */
public interface TableMetadataProvider {

    /**
     * Gets the persistence table model.
     *
     * @param oDataEntityDefinition the o data entity definition
     * @return the persistence table model
     * @throws SQLException the SQL exception
     */
    Table getTableMetadata(ODataEntity oDataEntityDefinition) throws SQLException;
}
