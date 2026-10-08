/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.table;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.dirigible.database.sql.DataType;

/**
 * The {@code DEFAULT} clause of a column definition, built from the default the model carries.
 *
 * <p>
 * A model carries a column's default as the text the author wrote: {@code web}, {@code 0},
 * {@code CURRENT_DATE}. The generated repository and the generated pages read that text as a value
 * of the column's type (see {@code JavaLiterals} / {@code JsLiterals}), so the DDL has to do the
 * same: on a character column the text is a string literal and must be quoted, on every other
 * column it is a SQL expression and goes in as written. Pasting {@code web} after {@code DEFAULT}
 * on a VARCHAR column is a column reference, which H2 lets through and PostgreSQL refuses - and a
 * refused CREATE TABLE there leaves the connection broken for every table that follows it in the
 * same pass (dirigible #7765).
 *
 * <p>
 * An author who already wrote a SQL literal ({@code 'draft'}) or a SQL expression on a character
 * column ({@code NULL}, {@code CURRENT_USER}, {@code gen_random_uuid()}) keeps it verbatim.
 */
public final class ColumnDefaultClause {

    /** The column types whose default is a string literal rather than a SQL expression. */
    private static final Set<DataType> CHARACTER_TYPES = EnumSet.of(DataType.VARCHAR, DataType.CHAR, DataType.NVARCHAR,
            DataType.CHARACTER_VARYING, DataType.CHARACTER, DataType.NCHAR, DataType.BPCHAR, DataType.TEXT, DataType.SHORTTEXT,
            DataType.ALPHANUM, DataType.CLOB, DataType.NCLOB, DataType.CHARACTER_LARGE_OBJECT);

    /**
     * A default that is a SQL expression even on a character column: the null literal, the standard
     * session functions that take no parentheses, or a function call.
     */
    private static final Pattern SQL_EXPRESSION =
            Pattern.compile("(?i)^(NULL|USER|CURRENT_USER|SESSION_USER|SYSTEM_USER|CURRENT_ROLE|CURRENT_SCHEMA|CURRENT_DATE|CURRENT_TIME"
                    + "|CURRENT_TIMESTAMP|LOCALTIME|LOCALTIMESTAMP|[A-Z_][A-Z0-9_.]*\\(.*\\))$", Pattern.DOTALL);

    private ColumnDefaultClause() {}

    /**
     * The {@code DEFAULT} clause for a column, padded with a space on either side so it appends to the
     * column's type arguments, or an empty string when the column declares no default.
     *
     * <p>
     * An empty default is kept as the empty string on a character column and dropped on any other,
     * where an empty expression is not a value.
     *
     * @param type the column's type
     * @param defaultValue the default as the model carries it, or null
     * @return the clause, or an empty string
     */
    public static String of(DataType type, String defaultValue) {
        if (defaultValue == null) {
            return "";
        }
        boolean character = CHARACTER_TYPES.contains(type);
        if (defaultValue.isEmpty()) {
            return character ? " DEFAULT '' " : "";
        }
        return " DEFAULT " + (character ? literal(defaultValue) : defaultValue) + " ";
    }

    /**
     * The authored text of a character column's default as the SQL it stands for: a string literal,
     * unless the author already wrote one or wrote a SQL expression.
     *
     * @param defaultValue the default as the model carries it
     * @return the literal or expression
     */
    static String literal(String defaultValue) {
        if (isQuoted(defaultValue) || SQL_EXPRESSION.matcher(defaultValue.trim())
                                                    .matches()) {
            return defaultValue;
        }
        return "'" + defaultValue.replace("'", "''") + "'";
    }

    private static boolean isQuoted(String value) {
        return value.length() > 1 && value.startsWith("'") && value.endsWith("'");
    }
}
