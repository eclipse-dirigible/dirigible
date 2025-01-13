/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.tableType;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractDropSqlBuilder;

/**
 * The Class DropTableTypeBuilder.
 */
public class DropTableTypeBuilder extends AbstractDropSqlBuilder {

    /**
     * Instantiates a new drop table type builder.
     *
     * @param dialect the dialect
     * @param tableType the tableType
     */
    public DropTableTypeBuilder(ISqlDialect dialect, String tableType) {
        super(dialect);
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        throw new IllegalStateException("Table type is not supported for this dialect");
    }
}
