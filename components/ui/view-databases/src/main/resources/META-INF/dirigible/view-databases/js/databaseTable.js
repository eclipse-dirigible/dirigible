/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { Controller, Get, Post, Put, Delete, response } from "@aerokit/sdk/http";
import { Query, Database, SQLBuilder, update } from "@aerokit/sdk/db";
import { Base64 } from '@aerokit/sdk/utils/base64';

@Controller
class CRUDService {

    @Get("/:datasourceName/:schemaName/:tableName")
    getAllRows(_, ctx) {
        const { datasourceName, schemaName, tableName } = ctx.pathParameters;
        try {
            const decodedTableName = String.fromCharCode.apply(null, Base64.decode(tableName));
            const escapeSymbol = this.getEscapeSymbol(datasourceName);
            const sqlQuery = `SELECT * 
                FROM ${escapeSymbol}${schemaName}${escapeSymbol}.${escapeSymbol}${decodedTableName}${escapeSymbol};
            `;
            return Query.execute(sqlQuery, undefined, datasourceName);
        } catch (error) {
            response.setStatus(400);
            return { error: "Failed to fetch rows", details: `${error}` };
        }
    }

    @Post("/:datasourceName/:schemaName/:tableName")
    createRow(data, ctx) {
        const { datasourceName, schemaName, tableName } = ctx.pathParameters;

        const columns = Object.keys(data);
        const values = Object.values(data);
        const placeholders = columns.map(() => "?");

        try {
            const decodedTableName = String.fromCharCode.apply(null, Base64.decode(tableName));
            const escapeSymbol = this.getEscapeSymbol(datasourceName);
            const sqlQuery = `INSERT INTO
                ${escapeSymbol}${schemaName}${escapeSymbol}.${escapeSymbol}${decodedTableName}${escapeSymbol} 
                (${columns.join(", ")})
                VALUES (${placeholders.join(", ")});
            `;

            update.execute(sqlQuery, values, datasourceName);
            return { success: true };
        } catch (error) {
            response.setStatus(400);
            return { error: "Failed to create row", details: `${error}` };
        }
    }

    @Put("/:datasourceName/:schemaName/:tableName")
    updateRow(record, ctx) {
        const { datasourceName, schemaName, tableName } = ctx.pathParameters;
        const { data, primaryKey } = record;

        if (!data || !primaryKey) {
            response.setStatus(400);
            return { error: "Required 'schemaName', 'tableName', 'data' and 'primaryKey'" };
        }

        const setClauses = [];
        const whereClauses = [];
        const keyValues = [];
        const columnValues = [];

        try {
            const decodedTableName = String.fromCharCode.apply(null, Base64.decode(tableName));
            const escapeSymbol = this.getEscapeSymbol(datasourceName);
            Object.keys(data).forEach((key) => {
                if (!primaryKey.includes(key)) {
                    setClauses.push(`${escapeSymbol}${key}${escapeSymbol} = ?`);
                    columnValues.push(data[key]);
                } else {
                    whereClauses.push(`${escapeSymbol}${key}${escapeSymbol} = ?`);
                    keyValues.push(data[key]);
                }
            });

            const parameters = columnValues.concat(keyValues);

            const sqlQuery = `UPDATE
                ${escapeSymbol}${schemaName}${escapeSymbol}.${escapeSymbol}${decodedTableName}${escapeSymbol}
                SET ${setClauses.join(", ")}
                WHERE ${whereClauses.join(" AND ")};
            `;

            update.execute(sqlQuery, parameters, datasourceName);
            return { success: true };
        } catch (error) {
            response.setStatus(400);
            return { error: "Failed to update row", details: `${error}` };
        }
    }

    @Delete("/:datasourceName/:schemaName/:tableName")
    deleteRow(keys, ctx) {
        const { datasourceName, schemaName, tableName } = ctx.pathParameters;
        const { data, primaryKey } = keys;

        if (!data || !primaryKey) {
            response.setStatus(400);
            return { error: "Required 'data', and 'primaryKey'" };
        }

        const whereClauses = [];
        const parameters = [];

        try {
            const decodedTableName = String.fromCharCode.apply(null, Base64.decode(tableName));
            const escapeSymbol = this.getEscapeSymbol(datasourceName);
            primaryKey.forEach((key) => {
                whereClauses.push(`${escapeSymbol}${key}${escapeSymbol} = ?`);
                parameters.push(data[key]);
            });

            const sqlQuery = `DELETE
                FROM ${escapeSymbol}${schemaName}${escapeSymbol}.${escapeSymbol}${decodedTableName}${escapeSymbol}
                WHERE ${whereClauses.join(" AND ")};
            `;

            update.execute(sqlQuery, parameters, datasourceName);
            return { success: true };
        } catch (error) {
            response.setStatus(400);
            return { error: "Failed to delete row", details: `${error}` };
        }
    }

    getEscapeSymbol(datasourceName) {
        let connection;
        try {
            connection = Database.getConnection(datasourceName);
            return SQLBuilder.getDialect(connection).select().native.getEscapeSymbol();
        } finally {
            if (connection) {
                connection.close();
            }
        }
    }
}
