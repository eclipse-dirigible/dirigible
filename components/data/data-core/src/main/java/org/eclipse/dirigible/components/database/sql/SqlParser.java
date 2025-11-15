/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.sql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;

public class SqlParser {

    /**
     * Parse insert SQL
     *
     * @param sql sql
     * @return parsed insert
     * @throws SqlParseException in case of invalid SQL or statement which is not insert
     */
    public static Insert parseInsert(String sql) throws SqlParseException {

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);

            if (stmt instanceof Insert insert) {
                return insert;
            } else {
                String message =
                        "SQL [" + sql + "] is not an insert statement. Statement is parsed to type: " + stmt.getClass() + ": " + stmt;
                throw new SqlParseException(message);
            }
        } catch (JSQLParserException ex) {
            throw new SqlParseException("Failed to parse sql: " + sql, ex);
        }

    }
}
