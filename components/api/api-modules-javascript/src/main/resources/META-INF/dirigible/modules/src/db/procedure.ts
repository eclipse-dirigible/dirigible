/**
 * @module db/procedure
 * @overview
 * 
 * This module provides a `Procedure` class for executing stored procedures in a database. It allows for both the creation of stored procedures using DDL statements and the execution of existing stored procedures with parameter support. The `execute` method can handle multiple result sets returned by a stored procedure and returns them as an array of JSON objects.
 * 
 * ### Key Features
 * - **Create Procedures**: Use the `create` method to execute DDL statements for creating or modifying stored procedures.
 * - **Execute Procedures**: The `execute` method allows you to call stored procedures with parameters and handles multiple result sets.
 * - **Parameter Mapping**: Supports both primitive parameters (string and number) and structured parameters using the `ProcedureParameter` interface for explicit type definitions.
 * - **Resource Management**: Ensures proper closing of database resources (connections, statements, result sets) to prevent leaks.
 * 
 * ### Use Cases
 * - Managing complex database operations encapsulated in stored procedures, allowing for cleaner application code and improved performance.
 * - Handling multiple result sets returned by stored procedures, which is common in scenarios like reporting or batch processing.
 * - Providing a flexible parameter mapping mechanism that can accommodate various data types and structures when calling stored procedures.
 * 
 * ### Example Usage
 * ```ts
 * import { Procedure } from "@aerokit/sdk/db";
 * 
 * // Create a stored procedure
 * const createSql = `
 *   CREATE PROCEDURE GetUserById(IN userId INT)
 *   BEGIN
 *     SELECT * FROM Users WHERE id = userId;
 *   END
 * `;
 * Procedure.create(createSql);
 * 
 * // Execute the stored procedure with a parameter
 * const result = Procedure.execute("{CALL GetUserById(?)}", [1]);
 * console.log(result); // Output: [{ id: 1, name: "Alice", email: "alice@example.com" }]
 * ```
 */

import { Update } from "./update";
import { Database } from "./database";

/**
 * @interface ProcedureParameter
 * @description Defines a structured parameter for procedure calls, allowing the type 
 * to be explicitly defined when the natural JavaScript type mapping is insufficient.
 */
export interface ProcedureParameter {
	type: string; // Removed readonly to allow assignment during parameter mapping
	value: any;   // Removed readonly
}

export class Procedure {

    /**
     * Executes a DDL/DML statement to create or modify a stored procedure without results.
     * * @param {string} sql The SQL statement (e.g., CREATE PROCEDURE).
     * @param {string} [datasourceName] Optional name of the data source to use.
     */
    public static create(sql: string, datasourceName?: string): void {
        Update.execute(sql, [], datasourceName);
    }

    /**
     * Executes a stored procedure call and returns the result set(s).
     * * @param {string} sql The callable statement (e.g., {CALL my_procedure(?, ?)}).
     * @param {(string | number | ProcedureParameter)[]} [parameters=[]] An array of parameters. Primitives (string/number) are automatically typed. Use ProcedureParameter for explicit types.
     * @param {string} [datasourceName] Optional name of the data source to use.
     * @returns {any[]} An array of JSON objects representing the result set(s).
     */
    public static execute(sql: string, parameters: (string | number | ProcedureParameter)[] = [], datasourceName?: string): any[] {
        const result = [];

        let connection = null;
        let callableStatement = null;
        let resultSet = null;

        try {
            let hasMoreResults = false;

            connection = Database.getConnection(datasourceName);
            callableStatement = connection.prepareCall(sql);
            
            const mappedParameters: ProcedureParameter[] = parameters.map((parameter) => {
                
                // 1. If the parameter is an object and looks like a ProcedureParameter, use it directly.
                if (parameter && typeof parameter === "object" && 'type' in parameter && 'value' in parameter) {
                    return parameter as ProcedureParameter;
                }

                // 2. Handle primitive types (string or number) by explicitly narrowing the type.
                let type: string;
                let value: string | number;

                if (typeof parameter === "string") {
                    type = "string";
                    value = parameter;
                } else if (typeof parameter === "number") {
                    // Type is correctly narrowed to 'number' here, resolving the TS2362 error.
                    type = parameter % 1 === 0 ? "int" : "double";
                    value = parameter;
                } else {
                    // Throw error if the parameter is not a recognized type.
                    throw new Error(`Procedure Call - Unsupported parameter type [${typeof parameter}]`);
                }

                return { value, type };
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