/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.db;

import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;

import org.eclipse.dirigible.components.database.sql.SqlParseException;
import org.eclipse.dirigible.components.database.sql.SqlParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlParserTest {

    @Test
    void testParseInsert_withInsertWithColumns() {
        String sql = "INSERT INTO \"TEST_TABLE\" (\"COL_01\", \"COL_03\", \"COL_03\") VALUES (?, ?, ?)";
        Insert insertStatement = SqlParser.parseInsert(sql);

        assertThat(insertStatement).isNotNull();

        Table table = insertStatement.getTable();
        assertThat(table).isNotNull();
        assertThat(table.getName()).isEqualTo("\"TEST_TABLE\"");

        assertThat(table.getSchemaName()).isNull();

        ExpressionList<Column> columns = insertStatement.getColumns();
        assertThat(columns.size()).isEqualTo(3);

        List<String> columnNames = columns.stream()
                                          .map(Column::getColumnName)
                                          .toList();

        assertThat(columnNames).containsExactly("\"COL_01\"", "\"COL_03\"", "\"COL_03\"");
    }

    @Test
    void testParseInsert_withInsertWithSchema() {
        String sql = "INSERT INTO \"PUBLIC\".\"TEST_TABLE\" VALUES (?, ?, ?)";
        Insert insertStatement = SqlParser.parseInsert(sql);

        assertThat(insertStatement).isNotNull();

        Table table = insertStatement.getTable();
        assertThat(table).isNotNull();
        assertThat(table.getName()).isEqualTo("\"TEST_TABLE\"");

        String schemaName = table.getSchemaName();
        assertThat(schemaName).isNotNull();
        assertThat(schemaName).isEqualTo("\"PUBLIC\"");
    }

    @Test
    void testParseInsert_withInvalidSql() {
        String sql = "INSERT INTO TEST_TABLE V_A_L_U_ES (?, ?, ?)";

        SqlParseException ex = assertThrows(SqlParseException.class, () -> {
            SqlParser.parseInsert(sql);
        });

        assertThat(ex.getMessage()).isEqualTo("Failed to parse sql: INSERT INTO TEST_TABLE V_A_L_U_ES (?, ?, ?)");
    }

    @Test
    void testParseInsert_withNotInsertSql() {
        String sql = "SELECT * FROM TEST_TABLE";

        SqlParseException ex = assertThrows(SqlParseException.class, () -> {
            SqlParser.parseInsert(sql);
        });

        assertThat(ex.getMessage()).isEqualTo(
                "SQL [SELECT * FROM TEST_TABLE] is not an insert statement. Statement is parsed to type: class net.sf.jsqlparser.statement.select.PlainSelect: SELECT * FROM TEST_TABLE");
    }

    @Test
    void testParseInsert_withInsertWithoutColumns() {
        String sql = "INSERT INTO \"TEST_TABLE\" VALUES (?, ?, ?)";
        Insert insertStatement = SqlParser.parseInsert(sql);

        assertThat(insertStatement).isNotNull();

        Table table = insertStatement.getTable();
        assertThat(table).isNotNull();
        assertThat(table.getName()).isEqualTo("\"TEST_TABLE\"");

        ExpressionList<Column> columns = insertStatement.getColumns();
        assertThat(columns).isNull();
    }
}
