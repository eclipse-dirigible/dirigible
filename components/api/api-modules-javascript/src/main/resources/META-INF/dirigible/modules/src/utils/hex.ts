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
