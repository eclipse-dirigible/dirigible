/**
 * Provides a static façade (`Writer` class) for adding new documents
 * or content to the native indexing service.
 */

const IndexingFacade = Java.type("org.eclipse.dirigible.components.api.indexing.IndexingFacade");

/**
 * The Writer class provides a static method for indexing documents, allowing
 * for full-text content, a last modification timestamp, and optional metadata.
 */
export class Writer {

	/**
	 * Adds a new document entry to the specified index.
	 *
	 * @param index The name or identifier of the index (e.g., 'documents', 'users').
	 * @param location A unique identifier or path for the indexed document (e.g., a file path or URL).
	 * @param contents The full-text content of the document to be indexed and made searchable.
	 * @param lastModified The Date object representing the last modification time of the document. Defaults to the current date/time if omitted.
	 * @param parameters Optional key-value map of additional metadata to associate with the document.
	 */
	public static add(index: string, location: string, contents: string, lastModified: Date = new Date(), parameters?: { [key: string]: string }) {
		let map = "{}";
		if (parameters) {
			// If parameters are provided, serialize the object into a JSON string
			map = JSON.stringify(parameters);
		}

		// The native facade requires the lastModified date as a string representing milliseconds since epoch.
		IndexingFacade.add(index, location, contents, '' + lastModified.getTime(), map);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Writer;
}