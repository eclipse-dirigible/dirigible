const UTF8Facade = Java.type("org.eclipse.dirigible.components.api.utils.UTF8Facade");

/**
 * Utility class for performing UTF-8 encoding and decoding operations.
 * It provides methods to handle conversions between standard JavaScript strings and
 * raw UTF-8 byte representations.
 */
export class UTF8 {

	/**
	 * Encodes the input (either a standard JavaScript string or a raw byte array)
	 * into a UTF-8 encoded string representation.
	 *
	 * @param input The text string to be encoded, or a byte array to convert to its string representation.
	 * @returns The resulting UTF-8 encoded string.
	 */
	public static encode(input: string | any[]): string {
		return UTF8Facade.encode(input);
	}

	/**
	 * Decodes the input (either a UTF-8 encoded string or a raw byte array)
	 * back into a standard JavaScript string.
	 *
	 * @param input The UTF-8 encoded string or byte array to be decoded.
	 * @returns The resulting standard decoded string.
	 */
	public static decode(input: string | any[]): string {
		return UTF8Facade.decode(input);
	}

	/**
	 * Decodes a specific segment of a raw byte array into a standard string
	 * using UTF-8 encoding.
	 *
	 * @param bytes The raw byte array containing the UTF-8 data.
	 * @param offset The starting index (inclusive) from which to begin decoding.
	 * @param length The number of bytes to decode starting from the offset.
	 * @returns The decoded string segment.
	 */
	public static bytesToString(bytes: any[], offset: number, length: number): string {
		return UTF8Facade.bytesToString(bytes, offset, length);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = UTF8;
}