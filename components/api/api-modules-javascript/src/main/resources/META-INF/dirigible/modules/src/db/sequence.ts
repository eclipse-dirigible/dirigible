/**
 * @module db/sequence
 * @overview
 * 
 * This module provides a `Sequence` class for managing database sequences in the Dirigible environment. The `Sequence` class offers static methods to retrieve the next value from a sequence, create new sequences, and drop existing sequences. It abstracts the underlying database interactions, allowing developers to work with sequences in a consistent manner across different database systems supported by Dirigible.
 * 
 * ### Key Features
 * - Retrieve the next value from a specified database sequence.
 * - Create new sequences with optional starting values.
 * - Drop existing sequences when they are no longer needed.
 * - Support for specifying datasource names to target specific database connections.
 * 
 * ### Use Cases
 * - Generating unique identifiers for records in a database table using sequences.
 * - Managing database sequences as part of application setup or migration processes.
 * - Abstracting sequence management to allow for easier maintenance and potential database engine changes in the future.
 * 
 * ### Example Usage
 * ```ts
 * import { Sequence } from "@aerokit/sdk/db";
 * 
 * // Create a new sequence named "user_id_seq" starting at 1000
 * Sequence.create("user_id_seq", 1000);
 * 
 * // Get the next value from the "user_id_seq" sequence
 * const nextUserId = Sequence.nextval("user_id_seq");
 * console.log(nextUserId); // Output: 1000
 * 
 * // Drop the "user_id_seq" sequence when it's no longer needed
 * Sequence.drop("user_id_seq");
 * ```
 */

// Import the Java type used to bridge to the underlying database functionality.
const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");

/**
 * Utility class for interacting with database sequence objects.
 */
export class Sequence {

	/**
	 * Retrieves the next available value from a specified sequence.
	 *
	 * @param sequence The name of the database sequence.
	 * @param tableName Optional: The name of the table associated with the sequence (depends on database dialect/facade implementation).
	 * @param datasourceName Optional: The name of the database connection to use.
	 * @returns The next sequence value as a number.
	 */
	public static nextval(sequence: string, tableName?: string, datasourceName?: string): number {
		// Note: The original JavaScript order of arguments for DatabaseFacade.nextval is:
		// DatabaseFacade.nextval(sequence, datasourceName, tableName);
		return DatabaseFacade.nextval(sequence, datasourceName, tableName);
	}

	/**
	 * Creates a new database sequence.
	 *
	 * @param sequence The name of the sequence to create.
	 * @param start Optional: The starting value for the sequence (defaults to 1 if not provided).
	 * @param datasourceName Optional: The name of the database connection to use.
	 */
	public static create(sequence: string, start?: number, datasourceName?: string): void {
		DatabaseFacade.createSequence(sequence, start, datasourceName);
	}

	/**
	 * Drops (deletes) an existing database sequence.
	 *
	 * @param sequence The name of the sequence to drop.
	 * @param datasourceName Optional: The name of the database connection to use.
	 */
	public static drop(sequence: string, datasourceName?: string): void {
		DatabaseFacade.dropSequence(sequence, datasourceName);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Sequence;
}