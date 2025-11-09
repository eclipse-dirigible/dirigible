/**
 * Values
 * * Utility class for serializing (stringify) and deserializing (parse) complex variable values (like objects and arrays)
 * to and from JSON strings for storage or transfer across the API boundary.
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