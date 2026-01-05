/**
 * Provides a static façade (`Searcher` class) for performing
 * term-based and time-based queries against a native indexing service.
 */

const IndexingFacade = Java.type("org.eclipse.dirigible.components.api.indexing.IndexingFacade");

/**
 * The Searcher class provides methods for querying a specific index
 * using keywords or date ranges.
 */
export class Searcher {

	/**
	 * Executes a keyword search against a specified index.
	 * @param index The name or identifier of the index to search (e.g., 'documents', 'products').
	 * @param term The keyword or search phrase to look for.
	 * @returns An array of result objects, parsed from the native JSON string output.
	 */
	public static search(index: string, term: string): { [key: string]: string }[] {
		const results = IndexingFacade.search(index, term);
		return JSON.parse(results);
	}

	/**
	 * Finds all entries in the index that were indexed before the specified date.
	 * @param index The name or identifier of the index.
	 * @param date The Date object representing the upper bound (exclusive) of the time range.
	 * @returns An array of result objects, parsed from the native JSON string output.
	 */
	public static before(index: string, date: Date): { [key: string]: string }[] {
		// Converts the Date object to milliseconds since epoch as a string.
		const results = IndexingFacade.before(index, '' + date.getTime());
		return JSON.parse(results);
	}

	/**
	 * Finds all entries in the index that were indexed after the specified date.
	 * @param index The name or identifier of the index.
	 * @param date The Date object representing the lower bound (exclusive) of the time range.
	 * @returns An array of result objects, parsed from the native JSON string output.
	 */
	public static after(index: string, date: Date): { [key: string]: string }[] {
		// Converts the Date object to milliseconds since epoch as a string.
		const results = IndexingFacade.after(index, '' + date.getTime());
		return JSON.parse(results);
	}

	/**
	 * Finds all entries in the index that were indexed within the specified date range.
	 * @param index The name or identifier of the index.
	 * @param lower The Date object for the lower bound (exclusive).
	 * @param upper The Date object for the upper bound (exclusive).
	 * @returns An array of result objects, parsed from the native JSON string output.
	 */
	public static between(index: string, lower: Date, upper: Date): { [key: string]: string }[] {
		// Converts both Date objects to milliseconds since epoch as strings.
		const results = IndexingFacade.between(index, '' + lower.getTime(), '' + upper.getTime());
		return JSON.parse(results);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Searcher;
}