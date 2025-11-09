import { Streams } from "sdk/io/streams";
import { Bytes } from "sdk/io/bytes";

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