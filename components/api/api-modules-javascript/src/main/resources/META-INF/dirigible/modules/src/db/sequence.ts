/**
 * API Sequence
 *
 * Provides static methods for managing and accessing database sequences.
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