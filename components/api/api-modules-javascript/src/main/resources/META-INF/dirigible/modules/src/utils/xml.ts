const Xml2JsonFacade = Java.type("org.eclipse.dirigible.components.api.utils.Xml2JsonFacade");

/**
 * Utility class for converting data between XML and JSON formats.
 * It automatically handles input serialization if an object is passed instead of a string.
 */
export class XML {

	/**
	 * Converts a JSON input (either a JSON string or a raw JavaScript object) into an XML string.
	 *
	 * Note: If a JavaScript object is passed, it is first stringified using JSON.stringify().
	 *
	 * @param input The JSON string or object to be converted to XML.
	 * @returns The resulting XML content as a string.
	 */
	public static fromJson(input: string | any): string {
		let data = input;
		if (typeof data !== "string") {
			data = JSON.stringify(input);
		}
		return Xml2JsonFacade.fromJson(data);
	}

	/**
	 * Converts an XML input (expected as an XML string) into a JSON formatted string.
	 *
	 * @param input The XML string to be converted to JSON.
	 * @returns The resulting JSON content as a string.
	 */
	public static toJson(input: string | any): string {
		let data = input;
		if (typeof data !== "string") {
			// This path is usually unexpected for XML input but kept for consistency
			data = JSON.stringify(input);
		}
		return Xml2JsonFacade.toJson(data);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = XML;
}