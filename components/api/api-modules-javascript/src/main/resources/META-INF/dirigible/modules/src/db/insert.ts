/**
 * @module db/insert
 * @package @aerokit/sdk/db
 * @name Insert
 * @overview
 * 
 * The `Insert` class provides static methods for executing parameterized SQL INSERT statements against a database. It supports both single and batch insert operations, allowing developers to efficiently insert data into their databases while abstracting away the underlying database interaction details.
 * 
 * ### Key Features
 * - **Parameterized Queries**: Supports parameterized SQL queries to prevent SQL injection and ensure safe database interactions.
 * - **Batch Operations**: Allows for executing multiple insert statements in a single batch operation, improving performance when inserting large datasets.
 * - **Flexible Data Types**: Accepts a variety of data types for parameters, including strings, numbers, booleans, dates, and complex objects wrapped in `InsertParameter`.
 * - **Datasource Selection**: Provides the option to specify a datasource name for executing the insert operation against a specific database connection.
 * 
 * ### Use Cases
 * - Inserting new records into a database table in a secure and efficient manner.
 * - Performing bulk insert operations when dealing with large datasets to optimize performance.
 * - Abstracting database interactions to allow for easier maintenance and potential database engine changes in the future.
 * 
 * ### Example Usage
 * ```ts
 * import { Insert } from "@aerokit/sdk/db";
 * 
 * // Single insert example
 * const newUser = { name: "Alice", email: "alice@example.com" };
 * const result = Insert.execute("INSERT INTO Users (name, email) VALUES (?, ?)", [newUser.name, newUser.email]);
 * console.log(result); // Output: [{ id: 1, name: "Alice", email: "
 * 
 * // Batch insert example
 * const users = [
 *   { name: "Bob", email: "bob@example.com" },
 *   { name: "Charlie", email: "charlie@example.com" }
 * ];
 * const result = Insert.executeMany("INSERT INTO Users (name, email) VALUES (?, ?)", users.map(u => [u.name, u.email]));
 * console.log(result); // Output: [{ id: 2, name: "Bob", email: "bob@example.com" }, { id: 3, name: "Charlie", email: "charlie@example.com" }]
 * ```
 */
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