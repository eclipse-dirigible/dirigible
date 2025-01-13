/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.snowflake;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.sequence.LastValueIdentityBuilder;

/**
 * The Snowflake Next Value Sequence Builder.
 */
public class SnowflakeLastValueIdentityBuilder extends LastValueIdentityBuilder {

    /**
     * Instantiates a new Snowflake last value identity builder.
     *
     * @param dialect the dialect
     */
    public SnowflakeLastValueIdentityBuilder(ISqlDialect dialect) {
        super(dialect);
        throw new IllegalArgumentException("Snowflake does not support current identity value");
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        return null;
    }
}
