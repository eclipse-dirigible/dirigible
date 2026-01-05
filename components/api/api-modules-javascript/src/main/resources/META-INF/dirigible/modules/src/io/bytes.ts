/**
 * Provides utilities for converting and manipulating byte arrays,
 * facilitating conversions between JavaScript arrays, Java arrays, text, and integers.
 */

const JString = Java.type("java.lang.String");
const JByte = Java.type("java.lang.Byte");
const JArray = Java.type("java.lang.reflect.Array");
const BytesFacade = Java.type("org.eclipse.dirigible.components.api.io.BytesFacade");

/**
 * The Bytes class provides static methods for byte array operations, primarily
 * used to bridge data types between the JavaScript environment and native Java components.
 */
export class Bytes {

	/**
	 * Converts a native JavaScript byte array (an array of numbers) to a Java byte array.
	 * This is used internally by the API layer to pass data to Java methods.
	 *
	 * @param bytes The JavaScript array of bytes (e.g., [104, 101, 108, 108, 111]).
	 * @returns A native Java byte array (internal representation).
	 */
	public static toJavaBytes(bytes: any[]): any[] {
		const internalBytes = JArray.newInstance(JByte.TYPE, bytes.length);
		for (let i = 0; i < bytes.length; i++) {
			internalBytes[i] = bytes[i];
		}
		return internalBytes;
	}

	/**
	 * Converts a native Java byte array back to a JavaScript array of numbers.
	 * This is used internally by the API layer to retrieve data from Java methods.
	 *
	 * @param internalBytes The native Java byte array.
	 * @returns A JavaScript array containing the byte values (numbers).
	 */
	public static toJavaScriptBytes(internalBytes: any[]): any[] {
		const bytes = [];
		for (let i = 0; i < internalBytes.length; i++) {
			bytes.push(internalBytes[i]);
		}
		return bytes;
	}

	/**
	 * Converts a standard text string into a byte array using the default platform encoding.
	 *
	 * @param text The input text string.
	 * @returns A JavaScript array representing the bytes of the text.
	 */
	public static textToByteArray(text: string): any[] {
		// JavaString is unused in the final implementation, but kept from original structure
		const javaString = new JString(text);
		const native = BytesFacade.textToByteArray(text);
		return Bytes.toJavaScriptBytes(native);
	}

	/**
	 * Converts a byte array back into a text string.
	 *
	 * @param data The JavaScript array of bytes.
	 * @returns The reconstructed text string.
	 */
	public static byteArrayToText(data: any[]): string {
		const native = Bytes.toJavaBytes(data);
		return String.fromCharCode.apply(String, Bytes.toJavaScriptBytes(native));
	}

	/**
	 * Converts a 32-bit integer value into a byte array, respecting the specified byte order.
	 *
	 * @param value The integer value to convert.
	 * @param byteOrder Specifies the byte ordering: "BIG_ENDIAN" (most significant byte first) or "LITTLE_ENDIAN" (least significant byte first).
	 * @returns A JavaScript array representing the 4-byte integer.
	 */
	public static intToByteArray(value: number, byteOrder:  "BIG_ENDIAN" | "LITTLE_ENDIAN"): any[] {
		return BytesFacade.intToByteArray(value, byteOrder);
	}

	/**
	 * Converts a 4-byte array back into a 32-bit integer value, respecting the specified byte order.
	 *
	 * @param data The 4-byte array (JavaScript array of numbers).
	 * @param byteOrder Specifies the byte ordering used during conversion.
	 * @returns The reconstructed integer value.
	 */
	public static byteArrayToInt(data: any[], byteOrder: "BIG_ENDIAN" | "LITTLE_ENDIAN"): number {
		return BytesFacade.byteArrayToInt(data, byteOrder);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Bytes;
}