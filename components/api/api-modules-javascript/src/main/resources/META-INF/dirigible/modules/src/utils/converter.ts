/**
 * @module utils/converter
 * @package @aerokit/sdk/utils
 * @name Converter
 * @overview
 * 
 * The Converter class provides static utility methods for converting and normalizing common data types such as Date and Boolean within an object structure. It is designed to facilitate the preparation of data for persistence or API consumption by ensuring that date values are consistently represented as timestamps or ISO strings, and that boolean values are strictly coerced to true or false. This class abstracts common data transformation patterns, allowing developers to easily convert and normalize their data without having to implement repetitive conversion logic throughout their codebase.
 * 
 * ### Key Features:
 * - **Date Conversion**: Methods to convert date properties into Unix timestamps or ISO 8601 strings, with handling for local timezone offsets.
 * - **Boolean Conversion**: A method to coerce any truthy or falsy value into a strict boolean type.
 * 
 * ### Use Cases:
 * - **Data Preparation**: This class is useful for preparing data objects before saving them to a database or sending them in API requests, ensuring that date and boolean fields are in the expected format.
 * - **Consistency**: By using these conversion methods, developers can maintain consistency in how dates and booleans are represented across their application, reducing bugs related to data formatting.
 * 
 * ### Example Usage:
 * ```ts
 * import { Converter } from "@aerokit/sdk/utils";
 * 
 * const obj = {
 *   dateCreated: "2024-01-01T10:00:00Z",
 *   birthday: "1990-05-15",
 *   isActive: 1
 * };
 * 
 * Converter.setDate(obj, 'dateCreated');
 * Converter.setLocalDate(obj, 'birthday');
 * Converter.setBoolean(obj, 'isActive');
 * 
 * console.log(obj);
 * // Output:
 * // {
 * //   dateCreated: 1704096000000,
 * //   birthday: "1990-05-15T00:00:00.000Z",
 * //   isActive: true
 * // }
 * ```
 */

export class Converter {

	/**
	 * Converts a date property value within an object into a Unix timestamp (milliseconds since epoch).
	 *
	 * @param obj The object containing the property to be converted.
	 * @param property The string name of the date property (e.g., 'dateCreated').
	 * @example
	 * // Before: { date: "2024-01-01T10:00:00Z" }
	 * Converter.setDate(obj, 'date');
	 * // After: { date: 1704096000000 }
	 */
	public static setDate(obj: any, property: string): void {
		if (obj && obj[property]) {
			// Converts the date string/object to a Date instance and gets the timestamp in milliseconds.
			obj[property] = new Date(obj[property]).getTime();
		}
	}

	/**
	 * Converts a date property value into an ISO 8601 string, adjusted to represent
	 * the start of that day (local midnight) to handle timezone offsets consistently.
	 * This is typically used for fields that should represent a date *only*, without time of day ambiguity.
	 *
	 * @param obj The object containing the property to be converted.
	 * @param property The string name of the date property (e.g., 'birthday').
	 * @example
	 * // If local timezone is EST (UTC-5):
	 * // Before: { date: "2024-01-01" }
	 * Converter.setLocalDate(obj, 'date');
	 * // After: { date: "2024-01-01T05:00:00.000Z" } (start of day UTC)
	 */
	public static setLocalDate(obj: any, property: string): void {
		if (obj && obj[property]) {
			// Calculate the offset to force the date to local midnight before converting to ISOString.
			const date = new Date(obj[property]);
			const offsetHours = -(new Date().getTimezoneOffset() / 60);
			date.setHours(offsetHours, 0, 0, 0);

			obj[property] = date.toISOString();
		}
	}

	/**
	 * Explicitly coerces a property value to a strict boolean type (`true` or `false`).
	 * This handles truthy/falsy values like `1`, `0`, `null`, and empty strings.
	 *
	 * @param obj The object containing the property to be converted.
	 * @param property The string name of the boolean property (e.g., 'isActive').
	 * @example
	 * // Before: { flag: 1, other: null }
	 * Converter.setBoolean(obj, 'flag');
	 * Converter.setBoolean(obj, 'other');
	 * // After: { flag: true, other: false }
	 */
	public static setBoolean(obj: any, property: string): void {
		if (obj && obj[property] !== undefined) {
			obj[property] = obj[property] ? true : false;
		}
	}
}