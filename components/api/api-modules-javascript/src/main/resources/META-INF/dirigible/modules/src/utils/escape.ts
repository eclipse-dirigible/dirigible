/**
 * @module utils/escape
 * @package @aerokit/sdk/utils
 * @name Escape
 * @overview
 * 
 * The Escape class provides static utility methods for performing context-aware string escaping and unescaping operations. These methods are essential for ensuring that strings are safely embedded in various contexts (like HTML, JavaScript, JSON, etc.) without introducing security vulnerabilities such as injection attacks. The class delegates the actual escaping logic to a native Java EscapeFacade, providing a simple interface for JavaScript developers to utilize these functionalities.
 * 
 * ### Key Features:
 * - **Context-Aware Escaping**: Methods to escape strings for specific contexts such as CSV, JavaScript, HTML (3.2 and 4.0), Java, JSON, and XML.
 * - **Unescaping Support**: Corresponding unescape methods to reverse the escaping process for each context.
 * - **Security**: Helps prevent injection attacks by properly escaping special characters in strings before embedding them in different formats.
 * 
 * ### Use Cases:
 * - **Data Serialization**: Safely serialize data into formats like JSON or XML by escaping special characters.
 * - **Web Development**: Escape user-generated content before embedding it in HTML or JavaScript to prevent XSS attacks.
 * - **CSV Handling**: Escape values that contain commas, quotes, or newlines when generating CSV files.
 * 
 * ### Example Usage:
 * ```ts
 * import { Escape } from "@aerokit/sdk/utils";
 * // Escape a string for use in HTML
 * const htmlSafe = Escape.escapeHtml4("<script>alert('XSS');</script>");
 * console.log(htmlSafe); // Output: &lt;script&gt;alert(&#39;XSS&#39;);&lt;/script&gt;
 * 
 * // Unescape the previously escaped HTML string
 * const original = Escape.unescapeHtml4(htmlSafe);
 * console.log(original); // Output: <script>alert('XSS');</script>
 * ```
 */

const EscapeFacade = Java.type("org.eclipse.dirigible.components.api.utils.EscapeFacade");

export class Escape {

	/**
	 * Escapes special characters in a string to make it safe for use as a value within a CSV file.
	 * Typically handles double quotes, commas, and newlines.
	 *
	 * @param input The string to be escaped.
	 * @returns The CSV-safe escaped string.
	 */
	public static escapeCsv(input: string): string {
		return EscapeFacade.escapeCsv(input);
	}

	/**
	 * Escapes characters in a string to create a valid JavaScript string literal.
	 * This makes it safe for embedding string values within JavaScript code blocks.
	 *
	 * @param input The string to be escaped.
	 * @returns The JavaScript-safe escaped string.
	 */
	public static escapeJavascript(input: string): string {
		return EscapeFacade.escapeJavascript(input);
	}

	/**
	 * Escapes characters in a string using HTML 3.2 entity references.
	 *
	 * @param input The string to be escaped.
	 * @returns The HTML 3.2 escaped string.
	 */
	public static escapeHtml3(input: string): string {
		return EscapeFacade.escapeHtml3(input);
	}

	/**
	 * Escapes characters in a string using HTML 4.0 entity references.
	 * This is the common standard for escaping characters like <, >, &, and ".
	 *
	 * @param input The string to be escaped.
	 * @returns The HTML 4.0 escaped string.
	 */
	public static escapeHtml4(input: string): string {
		return EscapeFacade.escapeHtml4(input);
	}

	/**
	 * Escapes characters in a string to create a valid Java string literal.
	 *
	 * @param input The string to be escaped.
	 * @returns The Java-safe escaped string.
	 */
	public static escapeJava(input: string): string {
		return EscapeFacade.escapeJava(input);
	}

	/**
	 * Escapes characters (like quotes, backslashes, and control characters) in a string
	 * to make it safe for embedding as a value within a JSON document.
	 *
	 * @param input The string to be escaped.
	 * @returns The JSON-safe escaped string.
	 */
	public static escapeJson(input: string): string {
		return EscapeFacade.escapeJson(input);
	}

	/**
	 * Escapes characters in a string to make it valid for use within an XML document.
	 * Typically handles characters like <, >, &, ", and '.
	 *
	 * @param input The string to be escaped.
	 * @returns The XML-safe escaped string.
	 */
	public static escapeXml(input: string): string {
		return EscapeFacade.escapeXml(input);
	}

	/**
	 * The inverse of `escapeCsv`: unescapes CSV-specific escape sequences back to their original form.
	 *
	 * @param input The CSV-escaped string.
	 * @returns The unescaped string.
	 */
	public static unescapeCsv(input: string): string {
		return EscapeFacade.unescapeCsv(input);
	}

	/**
	 * The inverse of `escapeJavascript`: unescapes JavaScript string literals.
	 *
	 * @param input The JavaScript-escaped string.
	 * @returns The unescaped string.
	 */
	public static unescapeJavascript(input: string): string {
		return EscapeFacade.unescapeJavascript(input);
	}

	/**
	 * The inverse of `escapeHtml3`: unescapes HTML 3.2 entity references.
	 *
	 * @param input The HTML 3.2 escaped string.
	 * @returns The unescaped string.
	 */
	public static unescapeHtml3(input: string): string {
		return EscapeFacade.unescapeHtml3(input);
	}

	/**
	 * The inverse of `escapeHtml4`: unescapes HTML 4.0 entity references.
	 *
	 * @param input The HTML 4.0 escaped string.
	 * @returns The unescaped string.
	 */
	public static unescapeHtml4(input: string): string {
		return EscapeFacade.unescapeHtml4(input);
	}

	/**
	 * The inverse of `escapeJava`: unescapes Java string literals.
	 *
	 * @param input The Java-escaped string.
	 * @returns The unescaped string.
	 */
	public static unescapeJava(input: string): string {
		return EscapeFacade.unescapeJava(input);
	}

	/**
	 * The inverse of `escapeJson`: unescapes JSON string escape sequences.
	 *
	 * @param input The JSON-escaped string.
	 * @returns The unescaped string.
	 */
	public static unescapeJson(input: string): string {
		return EscapeFacade.unescapeJson(input);
	}

	/**
	 * The inverse of `escapeXml`: unescapes XML entity references.
	 *
	 * @param input The XML-escaped string.
	 * @returns The unescaped string.
	 */
	public static unescapeXml(input: string): string {
		return EscapeFacade.unescapeXml(input);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Escape;
}