/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mysql;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.sequence.LastValueIdentityBuilder;

import static java.text.MessageFormat.format;

/**
 * The MySQL Next Value Sequence Builder.
 */
public class MySQLLastValueIdentityBuilder extends LastValueIdentityBuilder {

    /** The Constant PATTERN_SELECT_LAST_VALUE_IDENTITY. */
    private static final String PATTERN_SELECT_LAST_VALUE_IDENTITY = "SELECT LAST_INSERT_ID()";

    /**
     * Instantiates a new H2 last value identity builder.
     *
     * @param dialect the dialect
     */
    public MySQLLastValueIdentityBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        return format(PATTERN_SELECT_LAST_VALUE_IDENTITY);
    }
}
