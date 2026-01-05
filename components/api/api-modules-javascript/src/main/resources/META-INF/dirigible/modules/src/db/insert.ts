const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");

/**
 * Interface used to wrap complex or other specific values for database insertion.
 */
export interface InsertParameter {
	readonly value: any;
}

/**
 * Type alias for a single allowed parameter value in an INSERT statement.
 */
type ParameterValue = string | number | boolean | Date | InsertParameter;

/**
 * Provides static methods for executing INSERT SQL statements.
 */
export class Insert {

	/**
	 * Executes a single parameterized INSERT statement.
	 * * @param sql The SQL query to execute, with '?' placeholders for parameters.
	 * @param parameters An optional array of values to replace the '?' placeholders.
	 * @param datasourceName The name of the database connection to use (optional).
	 * @returns An array of records representing the result of the insertion (e.g., generated keys).
	 */
	public static execute(sql: string, parameters?: ParameterValue[], datasourceName?: string): Array<Record<string, any>> {
        const params = parameters ? JSON.stringify(parameters) : undefined;
		return DatabaseFacade.insert(sql, params, datasourceName);
	}

	/**
	 * Executes multiple parameterized INSERT statements as a batch operation.
	 * * @param sql The SQL query to execute, with '?' placeholders for parameters.
	 * @param parameters An optional array of parameter arrays, where each inner array corresponds to one execution of the SQL statement.
	 * @param datasourceName The name of the database connection to use (optional).
	 * @returns An array of records representing the results of the batched insertions.
	 */
	public static executeMany(sql: string, parameters?: ParameterValue[][], datasourceName?: string): Array<Record<string, any>> {
		const params = parameters ? JSON.stringify(parameters) : undefined;
		return DatabaseFacade.insertMany(sql, params, datasourceName);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Insert;
}