/**
 * @module indexing/searcher
 * @package @aerokit/sdk/indexing
 * @name Searcher
 * @overview
 * 
 * The Searcher class provides a static façade for performing term-based and time-based queries against a native indexing service. It allows developers to execute keyword searches and filter indexed entries based on their indexing timestamps, facilitating efficient retrieval of relevant data from the index.
 * 
 * ### Key Features:
 * - **Keyword Search**: The `search` method enables searching for entries in a specified index using keywords or search phrases.
 * - **Time-Based Queries**: The `before`, `after`, and `between` methods allow filtering indexed entries based on their indexing timestamps, enabling retrieval of entries indexed before, after, or within a specific date range.
 * 
 * ### Use Cases:
 * - **Data Retrieval**: Developers can use the Searcher class to retrieve relevant data from an index based on keywords or time criteria, which is essential for applications that rely on indexed data for search functionality.
 * - **Index Management**: The time-based query methods can be useful for managing and maintaining the index by identifying entries that may need to be updated or removed based on their age.
 * 
 * ### Example Usage:
 * ```ts
 * import { Searcher } from "@aerokit/sdk/indexing";
 * 
 * // Perform a keyword search in the 'documents' index
 * const results = Searcher.search('documents', 'example search term');
 * console.log(results);
 * 
 * // Find entries indexed before a specific date
 * const oldEntries = Searcher.before('documents', new Date('2023-01-01'));
 * console.log(oldEntries);
 * 
 * // Find entries indexed after a specific date
 * const recentEntries = Searcher.after('documents', new Date('2023-06-01'));
 * console.log(recentEntries);
 * 
 * // Find entries indexed between two dates
 * const entriesInRange = Searcher.between('documents', new Date('2023-01-01'), new Date('2023-06-01'));
 * console.log(entriesInRange);
 * ```
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