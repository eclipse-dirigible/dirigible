/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import static java.text.MessageFormat.format;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.sequence.LastValueIdentityBuilder;

/**
 * The HANA Next Value Sequence Builder.
 */
public class HanaLastValueIdentityBuilder extends LastValueIdentityBuilder {

    /** The Constant PATTERN_SELECT_LAST_VALUE_IDENTITY. */
    private static final String PATTERN_SELECT_LAST_VALUE_IDENTITY = "SELECT CURRENT_IDENTITY_VALUE() FROM ";

    /** The args. */
    private String[] args = null;

    /**
     * Instantiates a new HANA last value identity builder.
     *
     * @param dialect the dialect
     * @param args the args
     */
    public HanaLastValueIdentityBuilder(ISqlDialect dialect, String... args) {
        super(dialect);
        if ((args == null) || (args.length < 1)) {
            throw new IllegalArgumentException("HANA does not support current identity value without a table specified");
        }
        this.args = args;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        String sql = format(PATTERN_SELECT_LAST_VALUE_IDENTITY + args[0]);
        return sql;
    }
}
