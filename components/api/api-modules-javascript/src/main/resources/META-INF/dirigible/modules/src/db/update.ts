/**
 * @module db/update
 * @overview
 * 
 * This module provides the `Update` class, which serves as a facade for executing SQL UPDATE, INSERT, and DELETE statements in the Dirigible environment. The `execute` method allows for parameterized queries using either primitive values or structured parameter objects that specify both type and value. This flexibility enables developers to work with various data types and database-specific requirements when performing update operations.
 * 
 * ### Key Features
 * - **Parameterized Queries**: Supports both primitive parameters and structured parameter objects for flexible query execution.
 * - **Named Parameters**: Allows for named parameters in the form of `:paramName` placeholders, enhancing readability and maintainability of SQL statements.
 * - **Type Specification**: Structured parameters can include type information, which is useful for databases that require explicit type definitions for certain operations.
 * - **Datasource Support**: The method accepts an optional datasource name, enabling execution against specific database connections in a multi-database environment.
 * 
 * ### Use Cases
 * - Performing update operations on database records with dynamic values.
 * - Executing complex SQL statements that require specific data types or named parameters.
 * - Integrating with other modules that generate parameters in JSON format, allowing for seamless data flow within the application.
 * 
 * ### Example Usage
 * ```ts
 * import { Update } from "@aerokit/sdk/db";
 * 
 * // Example with primitive parameters
 * const rowsAffected1 = Update.execute("UPDATE Users SET name = ? WHERE id = ?", ["Alice", 1]);
 * console.log(rowsAffected1); // Output: number of rows updated
 * 
 * // Example with structured parameters
 * const rowsAffected2 = Update.execute(
 *   "INSERT INTO Orders (order_number, total) VALUES (:order_number, :total)",
 *   [
 *     { name: "order_number", type: "CHAR", value: "ORD12345" },
 *     { name: "total", type: "DECIMAL", value: 99.99 }
 *   ]
 * );
 * console.log(rowsAffected2); // Output: number of rows inserted
 * ```
 */

const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");

/**
 * Interface used for complex parameter types if needed, otherwise primitive types are used directly.
 */
export type TypedUpdateParameter = {
  readonly type: string;
  readonly value: unknown;
};

/**
 * Interface defining a parameter for a named update query (using placeholders like :paramName).
 */
export interface NamedUpdateParameter {
  readonly name: string;
  readonly type: string;
  readonly value: any;
}


/**
 * Facade class for executing SQL UPDATE, INSERT, and DELETE statements.
 * Parameters array supports primitives e.g. `[1, 'John', 34.56]` or objects in format either `{'type':'[DATA_TYPE]', 'value':[VALUE]}` or `{'name':'[string]', 'type':'[DATA_TYPE]', 'value':[VALUE]}` e.g. `[{'type':'CHAR', 'value':'ISBN19202323322'}]` or `[{'name': 'order_number', 'type':'CHAR', 'value':'ISBN19202323322'}]`
 */
export class Update {

	/**
	 * Executes a parameterized SQL update statement (INSERT, UPDATE, or DELETE).
	 *
	 * @param sql The SQL query to execute.
	 * @param parameters An optional array of values (primitives, TypedQueryParameter or NamedQueryParameter objects) to replace '?' or :paramName placeholders.
	 * @param datasourceName The name of the database connection to use (optional).
	 * @returns The number of rows affected by the statement.
	 */
	public static execute(sql: string, parameters?: (string | number | boolean | Date | TypedUpdateParameter | NamedUpdateParameter)[], datasourceName?: string): number {
		let arr: any[] = [];

	    if (parameters == null) {
	      arr = [];
	    } else if (typeof parameters === "string") {
	      try {
	        const parsed = JSON.parse(parameters);
	        if (!Array.isArray(parsed)) {
	          throw new Error("Input parameter string must represent a JSON array");
	        }
	        arr = parsed;
	      } catch (e) {
	        throw new Error("Invalid JSON parameters: " + e);
	      }
	    } else if (Array.isArray(parameters)) {
	      arr = parameters;
	    } else {
	      throw new Error("Parameters must be either an array or a JSON string");
	    }

	    if (arr.length === 0) {
		  const result = DatabaseFacade.update(sql, null, datasourceName);
	      return result;
	    }

	    const first = arr[0];

	    // NamedUpdateParameter (has name + type)
	    if (first && typeof first === "object" && "name" in first && "type" in first) {
		  const result = DatabaseFacade.updateNamed(sql, JSON.stringify(arr), datasourceName);
		  return result;
	    }

	    // TypedUpdateParameter (has type, no name)
	    if (first && typeof first === "object" && "type" in first && !("name" in first)) {
		  const result = DatabaseFacade.update(sql, JSON.stringify(arr), datasourceName);
	      return result;
	    }

	    // Primitive array
	    if (
	      arr.every(
	        (v) =>
	          typeof v === "string" ||
	          typeof v === "number" ||
	          typeof v === "boolean" ||
	          v instanceof Date			  ||
			  	  Array.isArray(v)
	      )
	    ) {
		  const result = DatabaseFacade.update(sql, JSON.stringify(arr), datasourceName);
		  return result;
	    }

	    throw new Error("Unsupported parameter format: " + JSON.stringify(parameters));
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Update;
}