/**
 * @module utils/digest
 * @package @aerokit/sdk/utils
 * @name Digest
 * @overview
 * 
 * The Digest class provides static utility methods for calculating cryptographic hash digests (MD5, SHA1, SHA256, SHA384, SHA512) from input data. It supports both string and byte array inputs, allowing developers to easily compute digests in various formats. The class abstracts the underlying Java implementation, providing a simple interface for performing digest operations in JavaScript.
 * 
 * ### Key Features:
 * - **Multiple Digest Algorithms**: Supports MD5, SHA1, SHA256, SHA384, and SHA512 digest algorithms.
 * - **Flexible Input Types**: Accepts both string and byte array inputs for digest calculation.
 * - **Output Formats**: Provides methods to return digest results as byte arrays or hexadecimal strings.
 * 
 * ### Use Cases:
 * - **Data Integrity**: Compute digests to verify the integrity of data by comparing computed digests with expected values.
 * - **Password Hashing**: Use digest functions to hash passwords before storing them in a database (note: consider using a stronger hashing algorithm with salt for password storage).
 * - **Unique Identifiers**: Generate unique identifiers for data based on their content by computing their digests.
 * 
 * ### Example Usage:
 * ```ts
 * import { Digest } from "@aerokit/sdk/utils";
 * 
 * // Calculate MD5 digest of a string and get it as a hex string
 * const md5Hex = Digest.md5Hex("Hello, World!");
 * console.log(md5Hex); // Output: 65a8e27d8879283831b664bd8b7f0ad4
 * 
 * // Calculate SHA256 digest of a byte array and get it as a byte array
 * const sha256Bytes = Digest.sha256([72, 101, 108, 108, 111]); // "Hello" in bytes
 * console.log(sha256Bytes); // Output: [185, 105, 241, 149, 122, 223, 173, 190, ...]
 * ```
 */

import { Streams } from "@aerokit/sdk/io/streams";
import { Bytes } from "@aerokit/sdk/io/bytes";

const DigestFacade = Java.type("org.eclipse.dirigible.components.api.utils.DigestFacade");

export class Digest {

	/**
	 * Calculate MD5 digest from input (text or byte array) and return result as byte array
	 */
	public static md5(input: string | any[]): any[] {
		return Bytes.toJavaScriptBytes(Digest.md5AsNativeBytes(input));
	}

	/**
	 * Calculate MD5 digest from input (text or byte array) and return result as 16 elements java native byte array
	 */
	public static md5AsNativeBytes(input: string | any[]): any[] {
		const data = input;
		let native;
		if (typeof data === 'string') {
			var baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		return DigestFacade.md5(native);
	}

	/**
	 * Calculate MD5 digest from input (text or byte array) and return result as 32 character hex string
	 */
	public static md5Hex(input: string | any[]): string {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		return DigestFacade.md5Hex(native);
	}

	/**
	 * Calculate SHA1 digest from input (text or byte array) and return result as 20 elements byte array
	 */
	public static sha1(input: string | any[]): any[] {
		return Bytes.toJavaScriptBytes(Digest.sha1AsNativeBytes(input));
	}

	/**
	 * Calculate SHA1 digest from input (text or byte array) and return result as 20 elements java native byte array
	 */
	public static sha1AsNativeBytes(input: string | any[]): any[] {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		return DigestFacade.sha1(native);
	}

	/**
	 * Calculate SHA256 digest from input (text or byte array) and return result as 32 elements byte array
	 */
	public static sha256(input: string | any[]): any[] {
		return Bytes.toJavaScriptBytes(Digest.sha256AsNativeBytes(input));
	}

	/**
	 * Calculate SHA256 digest from input (text or byte array) and return result as 32 elements java native byte array
	 */
	public static sha256AsNativeBytes(input: string | any[]): any[] {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		return DigestFacade.sha256(native);
	}

	/**
	 * Calculate SHA384 digest from input (text or byte array) and return result as 48 elements byte array
	 */
	public static sha384(input: string | any[]): any[] {
		return Bytes.toJavaScriptBytes(Digest.sha384AsNativeBytes(input));
	}

	/**
	 * Calculate SHA384 digest from input (text or byte array) and return result as 48 elements java native byte array
	 */
	public static sha384AsNativeBytes(input: string | any[]): any[] {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		const output = DigestFacade.sha384(native);
		return output;
	}

	/**
	 * Calculate SHA512 digest from input (text or byte array) and return result as 64 elements byte array
	 */
	public static sha512(input: string | any[]): any[] {
		return Bytes.toJavaScriptBytes(Digest.sha512AsNativeBytes(input));
	}

	/**
	 * Calculate SHA512 digest from input (text or byte array) and return result as 64 elements java native byte array
	 */
	public static sha512AsNativeBytes(input: string | any[]) {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		return DigestFacade.sha512(native);
	}

	/**
	 * Calculate SHA1 digest from input (text or byte array) and return result as 40 character hex string
	 */
	public static sha1Hex(input: string | any[]): string {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		return DigestFacade.sha1Hex(native);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Digest;
}
