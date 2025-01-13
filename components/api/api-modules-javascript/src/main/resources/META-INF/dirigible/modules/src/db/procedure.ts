/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * API Procedure
 *
 */
import { Update } from "./update";
import { Database } from "./database";

export interface ProcedureParameter {
	readonly type: string;
	readonly value: any;
}

export class Procedure {

    public static create(sql: string, datasourceName?: string): void {
        Update.execute(sql, [], datasourceName);
    }

    public static execute(sql: string, parameters: (string | number | ProcedureParameter)[] = [], datasourceName?: string): any[] {
        const result = [];

        let connection = null;
        let callableStatement = null;
        let resultSet = null;

        try {
            let hasMoreResults = false;

            connection = Database.getConnection(datasourceName);
            callableStatement = connection.prepareCall(sql);
            let mappedParameters = parameters.map((parameter) => {
                let mappedParameter: ProcedureParameter = {
                    value: parameter,
                    type: ""
                };
                let parameterType = typeof parameter;
                if (parameterType === "object") {
                    mappedParameter = parameter as ProcedureParameter;
                } else if (parameterType === "string") {
                    // @ts-ignore
                    mappedParameter.type = "string";
                } else if (parameterType === "number") {
                    // @ts-ignore
                    mappedParameter.type = parameter % 1 === 0 ? "int" : "double";
                } else {
                    throw new Error(`Procedure Call - Unsupported parameter type [${parameterType}]`);
                }
                return mappedParameter;
            });
            for (let i = 0; i < mappedParameters.length; i++) {
                switch (mappedParameters[i].type) {
                    case "string":
                        callableStatement.setString(i + 1, mappedParameters[i].value);
                        break;
                    case "int":
                    case "integer":
                    case "number":
                        callableStatement.setInt(i + 1, mappedParameters[i].value);
                        break;
                    case "float":
                        callableStatement.setFloat(i + 1, mappedParameters[i].value);
                        break;
                    case "double":
                        callableStatement.setDouble(i + 1, mappedParameters[i].value);
                        break;
                }
            }
            resultSet = callableStatement.executeQuery();

            do {
                result.push(JSON.parse(resultSet.toJson()));
                hasMoreResults = callableStatement.getMoreResults();
                if (hasMoreResults) {
                    resultSet.close();
                    resultSet = callableStatement.getResultSet();
                }
            } while (hasMoreResults)

            callableStatement.close();
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (callableStatement != null) {
                callableStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return result;
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Procedure;
}