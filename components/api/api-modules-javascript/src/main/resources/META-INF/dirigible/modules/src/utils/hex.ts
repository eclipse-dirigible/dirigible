/**
 * @module utils/hex
 * @package @aerokit/sdk/utils
 * @name Hex
 * @overview
 * 
 * The Hex class provides static utility methods for encoding and decoding data using hexadecimal representation. It supports both string and byte array inputs, allowing developers to easily convert data to and from hexadecimal format. The class abstracts the underlying Java implementation, providing a simple interface for performing hexadecimal operations in JavaScript.
 * 
 * ### Key Features:
 * - **Encoding**: Methods to encode strings or byte arrays into hexadecimal format, returning either a hexadecimal string or a byte array.
 * - **Decoding**: Methods to decode hexadecimal strings or byte arrays back into their original byte array form.
 * 
 * ### Use Cases:
 * - **Data Serialization**: Hexadecimal encoding is commonly used for representing binary data in a human-readable format, which can be useful for debugging or logging purposes.
 * - **Cryptographic Operations**: Hexadecimal encoding is often used in cryptographic contexts to represent hash digests, keys, or other binary data in a readable format.
 * - **Data Transmission**: Developers can use hexadecimal encoding to transmit binary data over text-based protocols where Base64 might not be suitable.
 * 
 * ### Example Usage:
 * ```ts
 * import { Hex } from "@aerokit/sdk/utils";
 * 
 * // Encoding a string to hexadecimal
 * const hexString = Hex.encode("Hello, World!");
 * console.log(hexString); // Output: 48656c6c6f2c20576f726c6421
 * 
 * // Decoding a hexadecimal string back to bytes
 * const decodedBytes = Hex.decode(hexString);
 * console.log(decodedBytes); // Output: [72, 101, 108, 108, 111, 44, 32, 87, 111, 114, 108, 100, 33]
 * ```
 */

import { Streams } from "@aerokit/sdk/io/streams";
import { Bytes } from "@aerokit/sdk/io/bytes";

const HexFacade = Java.type("org.eclipse.dirigible.components.api.utils.HexFacade");

/**
 * Utility class for performing **Hexadecimal encoding and decoding** of data.
 * It handles conversion between JavaScript strings, JavaScript byte arrays (any[]),
 * and the native Java byte arrays required by the underlying HexFacade.
 */
export class Hex {

	/**
	 * Hexadecimal encoding: Converts the input data (text or byte array) into a
	 * standard **hexadecimal string representation**.
	 *
	 * @param input The data to encode, either as a string or a JavaScript byte array (any[]).
	 * @returns The resulting hexadecimal string.
	 */
	public static encode(input: string | any[]): string {
		return Bytes.byteArrayToText(Hex.encodeAsNativeBytes(input));
	}

	/**
	 * Hexadecimal encoding: Converts the input data (text or byte array) into a
	 * **JavaScript byte array (any[])** containing the hexadecimal representation.
	 *
	 * @param input The data to encode, either as a string or a JavaScript byte array (any[]).
	 * @returns The resulting byte array containing the hexadecimal data.
	 */
	public static encodeAsBytes(input: string | any[]): any[] {
		return Bytes.toJavaScriptBytes(Hex.encodeAsNativeBytes(input));
	}

	/**
	 * Hexadecimal encoding: Converts the input data (text or byte array) into a
	 * **native Java byte array** containing the hexadecimal representation.
	 * This method is generally for internal use.
	 *
	 * @param input The data to encode, either as a string or a JavaScript byte array (any[]).
	 * @returns The resulting native Java byte array.
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

		return HexFacade.encodeNative(native);
	}

	/**
	 * Hexadecimal decoding: Converts a hexadecimal input (text or byte array) back into
	 * the original **raw byte array (JavaScript any[])**.
	 *
	 * @param input The hexadecimal data to decode, either as a string or a JavaScript byte array (any[]).
	 * @returns The decoded raw byte array (any[]). Returns null if decoding fails or input is null.
	 */
	public static decode(input: string | any[]): any[] {
		const output = Hex.decodeAsNativeBytes(input);
		if (output) {
			return Bytes.toJavaScriptBytes(output);
		}
		return output;
	}

	/**
	 * Hexadecimal decoding: Converts a hexadecimal input (text or byte array) back into
	 * the original **native Java raw byte array**. This method is generally for internal use.
	 *
	 * @param input The hexadecimal data to decode, either as a string or a JavaScript byte array (any[]).
	 * @returns The decoded native Java byte array.
	 */
	public static decodeAsNativeBytes(input: string | any[]) {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}
		return HexFacade.decodeNative(native);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Hex;
}
