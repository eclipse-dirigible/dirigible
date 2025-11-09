/**
 * API Procedure
 *
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