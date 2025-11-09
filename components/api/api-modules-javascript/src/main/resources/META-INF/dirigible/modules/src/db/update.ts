const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");

/**
 * Interface used for complex parameter types if needed, otherwise primitive types are used directly.
 */
export interface UpdateParameter {
	readonly value: any;
}

/**
 * Facade class for executing SQL UPDATE, INSERT, and DELETE statements.
 */
export class Update {

	/**
	 * Executes a parameterized SQL update statement (INSERT, UPDATE, or DELETE).
	 *
	 * @param sql The SQL statement to execute.
	 * @param parameters Optional array of parameters to bind to the SQL statement (replaces '?').
	 * These are serialized to JSON before being passed to the native API.
	 * @param datasourceName Optional name of the data source to use. Defaults to the primary data source.
	 * @returns The number of rows affected by the statement.
	 */
	public static execute(sql: string, parameters?: (string | number | boolean | Date | UpdateParameter)[], datasourceName?: string): number {
		// Serialize parameters to a JSON string if they exist, otherwise pass undefined.
		const parametersJson = parameters ? JSON.stringify(parameters) : undefined;
		
		const result = DatabaseFacade.update(sql, parametersJson, datasourceName);
		
		return result;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Update;
}