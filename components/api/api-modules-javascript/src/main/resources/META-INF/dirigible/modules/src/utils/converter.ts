/**
 * Utility class for converting and normalizing common data types (Date, Boolean)
 * within an object structure, typically for persistence or API consumption.
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