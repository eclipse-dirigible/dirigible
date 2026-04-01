/**
 * @module utils/alphanumeric
 * @package @aerokit/sdk/utils
 * @name Alphanumeric
 * @overview
 * 
 * The Alphanumeric class provides a collection of static utility methods for generating and validating alphanumeric strings. It includes methods for transforming strings to alphanumeric format, generating random alphanumeric sequences, and validating whether a given string is numeric or alphanumeric. These utilities are essential for scenarios where input sanitization, random identifier generation, or format validation is required, making it easier for developers to handle common string manipulation tasks in a consistent manner.
 * 
 * ### Key Features:
 * - **String Transformation**: The `toAlphanumeric` method transforms a given string into an alphanumeric sequence by removing non-conformant characters.
 * - **Random String Generation**: Methods like `randomString`, `alphanumeric`, and `alpha` allow for generating random strings of specified lengths and character sets.
 * - **Validation Methods**: The `isNumeric` and `isAlphanumeric` methods provide a way to validate whether a string is purely numeric or alphanumeric, respectively.
 * 
 * ### Use Cases:
 * - **Input Sanitization**: The `toAlphanumeric` method can be used to sanitize user input by stripping out unwanted characters, ensuring that only valid alphanumeric characters are retained.
 * - **Identifier Generation**: The random string generation methods are useful for creating unique identifiers, such as user IDs, session tokens, or any other scenario where a random string is needed.
 * - **Format Validation**: The validation methods can be used to check if user input or data conforms to expected formats, such as ensuring that a string is numeric before processing it as a number.
 * 
 * ### Example Usage:
 * ```ts
 * import { Alphanumeric } from "@aerokit/sdk/utils";
 * 
 * // Transform a string to alphanumeric format
 * const sanitized = Alphanumeric.toAlphanumeric("Hello, World! 123");
 * console.log(sanitized); // Output: "HelloWorld123"
 * 
 * // Generate a random alphanumeric string of length 8
 * const randomId = Alphanumeric.alphanumeric(8, true);
 * console.log(randomId); // Output: e.g., "a1b2c3d4"
 * 
 * // Validate if a string is numeric
 * const isNumeric = Alphanumeric.isNumeric("12345");
 * console.log(isNumeric); // Output: true
 * 
 * // Validate if a string is alphanumeric * const isAlphanumeric = Alphanumeric.isAlphanumeric("abc123");
 * console.log(isAlphanumeric); // Output: true
 * ```
 */

const LOWERCASEASCII = "abcdefghijklmnopqrstuvwxyz";
const UPPERCASEASCII = LOWERCASEASCII.toUpperCase();
const NUMBERS = "1234567890";

export class Alphanumeric {

	public static toAlphanumeric(string: string): string {
		return string.replace(/[^A-Za-z0-9_]/g, '');
	}

	/**
	 * Generates a random alphanumeric sequence with the specified length
	 * @param length {Integer} Defaults to 4
	 */
	public static randomString(length: number, charset: string): string {
		let text = "";
		if (length < 1)
			length = 4;
		for (var i = 0; i < length; i++)
			text += charset.charAt(Math.floor(Math.random() * charset.length));
		return text;
	}

	/**
	 * Generates a random alphanumeric sequence with the specified length
	 * @param length {Integer} Defaults to 4
	 * @param lowercase {Boolean} Defaults to true
	 */
	public static alphanumeric(length: number, lowercase: boolean): string {
		let charset = LOWERCASEASCII + NUMBERS;
		if (!lowercase) {
			charset += UPPERCASEASCII;
		}
		return Alphanumeric.randomString(length, charset);
	}

	/**
	 * Generates a random ASCII sequence with the specified length
	 * @param length {Integer} Defaults to 4
	 * @param lowercase {Boolean} Defaults to true
	 */
	public static alpha(length: number, lowercase: boolean): string {
		let charset = LOWERCASEASCII;
		if (!lowercase) {
			charset += UPPERCASEASCII;
		}
		return Alphanumeric.randomString(length, charset);
	}

	/**
	 * Generates a random numeric value
	 * @param length {Integer} Defaults to 4
	 */
	public static numeric(length: number): string {
		return Alphanumeric.randomString(length, NUMBERS);
	}

	/**
	 * Tests is the provided `str` argument is a valid numeric sequence.
	 * @param str {String} the string to test
	 */
	public static isNumeric(str: string): boolean {
		// a faster alternative to checking with  /[^a-zA-Z0-9]/.test(str)
		// copy from public domain at: https://stackoverflow.com/a/25352300
		let code, i, len;
		for (i = 0, len = str.length; i < len; i++) {
			code = str.charCodeAt(i);
			if (!(code > 47 && code < 58)) { // numeric (0-9)
				return false;
			}
		}
		return true;
	}

	/**
	 * Tests is the provided `str` argument is a valid alphanumeric sequence.
	 * @param str {String} the string to test
	 */
	public static isAlphanumeric(str: string): boolean {
		// a faster alternative to checking with  /[^a-zA-Z0-9]/.test(str)
		// copy from public domain at: https://stackoverflow.com/a/25352300
		let code, i, len;
		for (i = 0, len = str.length; i < len; i++) {
			code = str.charCodeAt(i);
			if (!(code > 47 && code < 58) && // numeric (0-9)
				!(code > 64 && code < 91) && // upper alpha (A-Z)
				!(code > 96 && code < 123)) { // lower alpha (a-z)
				return false;
			}
		}
		return true;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Alphanumeric;
}
