/**
 * @module utils/base64
 * @package @aerokit/sdk/utils
 * @name Base64
 * @overview
 * 
 * The Base64 class provides static utility methods for encoding and decoding data using the Base64 encoding scheme. It supports both string and byte array inputs, allowing developers to easily convert data to and from Base64 format. The class abstracts the underlying Java implementation, providing a simple interface for performing Base64 operations in JavaScript.
 * 
 * ### Key Features:
 * - **Encoding**: Methods to encode strings or byte arrays into Base64 format, returning either a Base64 string or a byte array.
 * - **Decoding**: Methods to decode Base64 strings or byte arrays back into their original byte array form.
 * 
 * ### Use Cases:
 * - **Data Serialization**: Base64 encoding is commonly used for serializing binary data (like images or files) into a text format that can be easily transmitted over text-based protocols such as HTTP.
 * - **Authentication**: Base64 encoding is often used in authentication schemes (e.g., Basic Authentication) to encode credentials before transmission.
 * - **Data Storage**: Developers can use Base64 encoding to store binary data in databases that only support text formats.
 * 
 * ### Example Usage:
 * ```ts
 * import { Base64 } from "@aerokit/sdk/utils";
 * 
 * // Encoding a string to Base64
 * const encodedString = Base64.encode("Hello, World!");
 * console.log(encodedString); // Output: SGVsbG8sIFdvcmxkIQ==
 * 
 * // Decoding a Base64 string back to bytes
 * const decodedBytes = Base64.decode(encodedString);
 * console.log(decodedBytes); // Output: [72, 101, 108, 108, 111, 44, 32, 87, 111, 114, 108, 100, 33]
 * ```
 */

import { Streams } from "@aerokit/sdk/io/streams";
import { Bytes } from "@aerokit/sdk/io/bytes";

const Base64Facade = Java.type("org.eclipse.dirigible.components.api.utils.Base64Facade");

/**
 * Utility class for performing **Base64 encoding and decoding** of data.
 * It handles conversion between JavaScript strings, JavaScript byte arrays (any[]),
 * and the native Java byte arrays required by the underlying Base64Facade.
 */
export class Base64 {

	/**
	 * Base64 encoding: Converts the input data (text or byte array) into a
	 * standard **Base64 encoded string representation**.
	 *
	 * @param input The data to encode, either as a string or a JavaScript byte array (any[]).
	 * @returns The resulting Base64 encoded string.
	 */
	public static encode(input: string | any[]): string {
		return Bytes.byteArrayToText(Base64.encodeAsNativeBytes(input));
	}

	/**
	 * Base64 encoding: Converts the input data (text or byte array) into a
	 * **JavaScript byte array (any[])** containing the Base64 encoded representation.
	 *
	 * @param input The data to encode, either as a string or a JavaScript byte array (any[]).
	 * @returns The resulting byte array containing the Base64 encoded data.
	 */
	public static encodeAsBytes(input: string | any[]): any[] {
		return Bytes.toJavaScriptBytes(Base64.encodeAsNativeBytes(input));
	}

	/**
	 * Base64 encoding: Converts the input data (text or byte array) into a
	 * **native Java byte array** containing the Base64 encoded representation.
	 * This method is generally for internal use.
	 *
	 * @param input The data to encode, either as a string or a JavaScript byte array (any[]).
	 * @returns The resulting native Java byte array containing the Base64 data.
	 */
	public static encodeAsNativeBytes(input: string | any[]): any[] {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		return Base64Facade.encodeNative(native);
	}

	/**
	 * Base64 decoding: Converts a Base64 input (text or byte array) back into
	 * the original **raw byte array (JavaScript any[])**.
	 *
	 * @param input The Base64 data to decode, either as a string or a JavaScript byte array (any[]).
	 * @returns The decoded raw byte array (any[]). Returns null if decoding fails or input is null.
	 */
	public static decode(input: string | any[]): any[] {
		const output = Base64.decodeAsNativeBytes(input);
		if (output) {
			return Bytes.toJavaScriptBytes(output);
		}
		return output;
	}

	/**
	 * Base64 decoding: Converts a Base64 input (text or byte array) back into
	 * the original **native Java raw byte array**. This method is generally for internal use.
	 *
	 * @param input The Base64 data to decode, either as a string or a JavaScript byte array (any[]).
	 * @returns The decoded native Java byte array.
	 */
	public static decodeAsNativeBytes(input: string | any[]): any[] {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}
		return Base64Facade.decodeNative(native);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Base64;
}
