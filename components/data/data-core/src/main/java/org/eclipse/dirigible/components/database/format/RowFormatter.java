/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.format;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * The Row Formatter.
 *
 */
public interface RowFormatter {

    /**
     * Write formatted row.
     *
     * @param columnDescriptors the column descriptors
     * @param resultSetMetaData the result set meta data
     * @param resultSet the result set
     * @return the result
     * @throws SQLException the SQL exception
     */
    String write(List<ColumnDescriptor> columnDescriptors, ResultSetMetaData resultSetMetaData, ResultSet resultSet) throws SQLException;
}
