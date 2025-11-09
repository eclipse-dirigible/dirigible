/**
 * API Query
 *
 */

// Java.type imports are necessary for environments like Dirigible/Rhino
const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");

/**
 * Interface used to wrap complex or specific values for non-named queries.
 */
export interface QueryParameter {
	readonly value: any;
}

/**
 * Interface defining a parameter for a named query (using placeholders like :paramName).
 */
export interface NamedQueryParameter {
	readonly name: string;
	readonly type: string;
	readonly value: any;
}

/**
 * Interface to specify formatting options for the query result set.
 */
export interface ResultParameter {
	readonly dateFormat: string;
}

/**
 * Provides static methods for executing parameterized SQL SELECT statements.
 */
export class Query {

	/**
	 * Executes a standard SQL query with positional parameters ('?').
	 *
	 * @param sql The SQL query to execute.
	 * @param parameters An optional array of values (primitives or QueryParameter objects) to replace '?' placeholders.
	 * @param datasourceName The name of the database connection to use (optional).
	 * @param resultParameter Optional formatting parameters for the result set (e.g., date format).
	 * @returns An array of records representing the query results.
	 */
	public static execute(sql: string, parameters?: (string | number | boolean | Date | QueryParameter)[], datasourceName?: string,  resultParameter?: ResultParameter): any[] {
		// Serialize parameters and result parameters for the Java facade
		const paramsJson = parameters ? JSON.stringify(parameters) : undefined;
		const resultParamsJson = resultParameter ? JSON.stringify(resultParameter) : undefined;

		// The DatabaseFacade returns a JSON string representation of the result set
		const resultset = DatabaseFacade.query(sql, paramsJson, datasourceName, resultParamsJson);

		// Parse the JSON string back into a JavaScript array of objects
		return JSON.parse(resultset);
	}
	
	/**
	 * Executes a SQL query with named parameters (e.g., ":name", ":id").
	 *
	 * @param sql The SQL query to execute.
	 * @param parameters An optional array of NamedQueryParameter objects.
	 * @param datasourceName The name of the database connection to use (optional).
	 * @returns An array of records representing the query results.
	 */
	public static executeNamed(sql: string, parameters?: NamedQueryParameter[], datasourceName?: string): any[] {
		// Serialize the array of named parameters for the Java facade
		const paramsJson = parameters ? JSON.stringify(parameters) : undefined;

		// The DatabaseFacade returns a JSON string representation of the result set
		const resultset = DatabaseFacade.queryNamed(sql, paramsJson, datasourceName);

		// Parse the JSON string back into a JavaScript array of objects
		return JSON.parse(resultset);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Query;
}