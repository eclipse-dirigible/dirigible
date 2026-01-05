const UrlFacade = Java.type("org.eclipse.dirigible.components.api.utils.UrlFacade");

/**
 * Utility class for performing various forms of URL encoding and decoding.
 * It wraps native Java URL utility methods for handling query parameters,
 * path segments, and form data.
 */
export class URL {

	/**
	 * URL-encodes the input string, typically used for encoding query parameter values.
	 *
	 * @param input The string to be encoded.
	 * @param charset The character set (e.g., 'UTF-8', 'ISO-8859-1') to use for encoding. Defaults to the system's preferred encoding if omitted.
	 * @returns The URL-encoded string.
	 */
	public static encode(input: string, charset?: string): string {
		return UrlFacade.encode(input, charset);
	}

	/**
	 * URL-decodes the input string, typically used for decoding query parameter values.
	 *
	 * @param input The string to be decoded.
	 * @param charset The character set (e.g., 'UTF-8', 'ISO-8859-1') that was used for encoding. Defaults to the system's preferred encoding if omitted.
	 * @returns The URL-decoded string.
	 */
	public static decode(input: string, charset?: string): string {
		return UrlFacade.decode(input, charset);
	}

	/**
	 * Escapes the input string using general URL escaping rules.
	 * This is typically equivalent to `encodeURIComponent` and is suitable for
	 * encoding query parameter *values*.
	 *
	 * @param input The string to escape.
	 * @returns The escaped string.
	 */
	public static escape(input: string): string {
		return UrlFacade.escape(input);
	}

	/**
	 * Escapes the input string specifically for use as a **URL path segment**.
	 * It typically preserves path delimiters like `/` that might otherwise be escaped
	 * in standard URL encoding.
	 *
	 * @param input The path string to escape.
	 * @returns The escaped path string.
	 */
	public static escapePath(input: string): string {
		return UrlFacade.escapePath(input);
	}

	/**
	 * Escapes the input string according to the rules for **HTML Form Data**
	 * (application/x-www-form-urlencoded). This typically replaces spaces with `+`
	 * instead of `%20`.
	 *
	 * @param input The form data string to escape.
	 * @returns The escaped form data string.
	 */
	public static escapeForm(input: string): string {
		return UrlFacade.escapeForm(input);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = URL;
}