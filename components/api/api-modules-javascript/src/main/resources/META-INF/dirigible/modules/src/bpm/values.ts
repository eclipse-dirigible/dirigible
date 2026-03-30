/**
 * @module bpm/values
 * @overview
 * 
 * The `Values` class provides utility methods for serializing and deserializing complex variable values (such as objects and arrays) to and from JSON strings. This is particularly useful for handling process variables in BPMN processes, where variables may need to be stored or transferred across API boundaries in a consistent format.
 * 
 * ### Key Features
 * - **parseValue**: Safely attempts to parse a string value as JSON, returning the original value if parsing fails.
 * - **parseValuesMap**: Applies JSON parsing to all values in a Map, allowing for bulk deserialization of process variables.
 * - **stringifyValue**: Converts objects and arrays into their JSON string representations, while leaving primitive types unchanged. Arrays are also converted into Java Lists for compatibility with Java APIs.
 * - **stringifyValuesMap**: Applies JSON stringification to all values in a Map, enabling bulk serialization of process variables before API calls.
 * 
 * ### Use Cases
 * - Managing complex process variables in BPMN processes, including objects and arrays.
 * - Ensuring consistent serialization and deserialization of variables when interacting with APIs that expect string values.
 * - Facilitating the transfer of structured data across API boundaries in a format that can be easily parsed and utilized.
 */

export class Values {

	/**
	 * Attempts to parse a value as a JSON string.
	 * If the value is a valid JSON string (representing an object or array), it is parsed and returned as an object.
	 * If parsing fails (e.g., the value is a primitive or an invalid JSON string), the original value is returned.
	 * @param value The value to parse, typically a string read from the API.
	 * @returns The parsed object, or the original value if parsing fails.
	 */
	public static parseValue(value: any): any {
		try {
			return JSON.parse(value);
		} catch (e) {
			// Do nothing
		}
		return value;
	}

	/**
	 * Iterates over the values of a Map and applies {@link #parseValue(any)} to each value.
	 * This is typically used to deserialize all variables returned from an API call.
	 * @param variables The Map of variable names to their values (which may be JSON strings).
	 * @returns The Map with all values deserialized where possible.
	 */
	public static parseValuesMap(variables: Map<string, any>): Map<string, any> {
		for (const [key, value] of variables) {
			variables.set(key, Values.parseValue(value));
		}
		return variables;
	}

	/**
	 * Serializes a value for persistence or API transfer.
	 * Arrays and objects are converted into their respective JSON string representations.
	 * Note: Arrays are additionally converted into a `java.util.List` of stringified elements for Java API compatibility.
	 * Primitive types are returned as is.
	 * @param value The value to serialize.
	 * @returns The JSON string representation, a Java List (for arrays), or the original primitive value.
	 */
	public static stringifyValue(value: any): any {
		if (Array.isArray(value)) {
			// @ts-ignore
			return java.util.Arrays.asList(value.map(e => JSON.stringify(e)));
		} else if (typeof value === 'object') {
			return JSON.stringify(value);
		}
		return value;
	}

	/**
	 * Iterates over the values of a Map and applies {@link #stringifyValue(any)} to each value.
	 * This is typically used to serialize a map of variables before sending them to an API call.
	 * @param variables The Map of variable names to their values.
	 * @returns The Map with all values serialized.
	 */
	public static stringifyValuesMap(variables: Map<string, any>): Map<string, any> {
		for (const [key, value] of variables) {
			variables.set(key, Values.stringifyValue(value));
		}
		return variables;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Values;
}