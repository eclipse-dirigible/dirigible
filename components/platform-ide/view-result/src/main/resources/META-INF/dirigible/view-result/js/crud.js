/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { Controller, Get, Post, Put, Delete, response } from "sdk/http";
import { query } from "sdk/db";
import { update } from "sdk/db";

@Controller
class CRUDService {

    @Get("/")
    async getAllRows(filter) {
        const { schemaName, tableName } = filter;

        const sqlQuery = `
        SELECT * FROM "${schemaName}"."${tableName}";
    `;

        try {
            const result = query.execute(sqlQuery);
            res.send(result);
        } catch (error) {
            res.status(500).send({ error: "Failed to fetch rows", details: error });
        }
    }

    @Post("/create")
    async createRow(record) {
        const { schemaName, tableName, data } = record;

        if (!schemaName || !tableName || !data) {
            return res.status(400).send({ error: "Required 'schemaName', 'tableName' and 'data'" });
        }

        const columns = Object.keys(data);
        const values = Object.values(data);
        const placeholders = columns.map(() => "?");

        const sqlQuery = `
            INSERT INTO "${schemaName}"."${tableName}" 
            (${columns.join(", ")})
            VALUES (${placeholders.join(", ")});
        `;

        try {
            update.execute(sqlQuery, values);
            res.send({ success: true });
        } catch (error) {
            res.status(500).send({ error: "Failed to create row", details: error });
        }
    }

    @Put("/update")
    async updateRow(record) {

        const { schemaName, tableName, data, primaryKey } = record;

        if (!schemaName || !tableName || !data || !primaryKey) {
            return res.status(400).send({ error: "Required 'schemaName', 'tableName', 'data' and 'primaryKey'" });
        }

        const setClauses = [];
        const whereClauses = [];
        const keyValues = [];
        const columnValues = [];

        Object.keys(data).forEach((key) => {
            if (!primaryKey.includes(key)) {
                setClauses.push(`"${key}" = ?`);
                columnValues.push(data[key]);
            } else {
                whereClauses.push(`"${key}" = ?`);
                keyValues.push(data[key]);
            }
        });

        const parameters = columnValues.concat(keyValues);

        const sqlQuery = `
            UPDATE "${schemaName}"."${tableName}"
            SET ${setClauses.join(", ")}
            WHERE ${whereClauses.join(" AND ")};
        `;

        try {
            update.execute(sqlQuery, parameters);
            res.send({ success: true });
        } catch (error) {
            res.status(500).send({ error: "Failed to update row", details: error });
        }
    }

    @Post("/delete")
    async deleteRow(keys) {

        const { schemaName, tableName, data, primaryKey } = keys;

        if (!schemaName || !tableName || !primaryKey) {
            return res.status(400).send({ error: "Required 'schemaName', 'tableName' and 'primaryKey'" });
        }

        const whereClauses = [];
        const parameters = [];

        primaryKey.forEach((key) => {
            whereClauses.push(`"${key}" = ?`);
            parameters.push(data[key]);
        });

        const sqlQuery = `
            DELETE FROM "${schemaName}"."${tableName}"
            WHERE ${whereClauses.join(" AND ")};
        `;

        try {
            update.execute(sqlQuery, parameters);
            res.send({ success: true });
        } catch (error) {
            res.status(500).send({ error: "Failed to delete row", details: error });
        }
    }
}
