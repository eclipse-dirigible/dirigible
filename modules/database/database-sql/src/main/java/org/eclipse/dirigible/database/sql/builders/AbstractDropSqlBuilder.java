/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders;

import org.eclipse.dirigible.database.sql.ISqlDialect;

/**
 * The Abstract Drop SQL Builder.
 */
public abstract class AbstractDropSqlBuilder extends AbstractSqlBuilder {

    /**
     * Instantiates a new abstract drop sql builder.
     *
     * @param dialect the dialect
     */
    protected AbstractDropSqlBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Generate drop.
     *
     * @param sql the sql
     */
    protected void generateDrop(StringBuilder sql) {
        sql.append(KEYWORD_DROP);
    }

}
