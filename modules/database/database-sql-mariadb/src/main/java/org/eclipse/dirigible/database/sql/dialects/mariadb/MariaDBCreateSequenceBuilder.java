/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible
 * contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mariadb;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.sequence.CreateSequenceBuilder;

/**
 * The MariaDB Create Sequence Builder.
 */
public class MariaDBCreateSequenceBuilder extends CreateSequenceBuilder {

    /**
     * Instantiates a new MariaDB create sequence builder.
     *
     * @param dialect the dialect
     * @param sequence the sequence
     */
    public MariaDBCreateSequenceBuilder(ISqlDialect dialect, String sequence) {
        super(dialect, sequence);
    }

    /**
     * Generate start.
     *
     * @param sql the sql
     */
    @Override
    protected void generateStart(StringBuilder sql) {
        throw new IllegalStateException("MariaDB does not support Sequences");
    }

}
