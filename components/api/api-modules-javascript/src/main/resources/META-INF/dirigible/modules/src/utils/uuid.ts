/**
 * @module utils/uuid
 * @package @aerokit/sdk/utils
 * @name UUID
 * @overview
 * 
 * The UUID class provides static methods for generating and validating Universally Unique Identifiers (UUIDs). It typically focuses on Type 4 UUIDs, which are randomly generated. The class abstracts the underlying native Java UuidFacade, allowing developers to easily generate new UUIDs and validate existing ones in their JavaScript applications without needing to manage the complexities of UUID generation and validation directly.
 * 
 * ### Key Features:
 * - **UUID Generation**: Generate new random UUIDs (Type 4) in a standard string format.
 * - **UUID Validation**: Validate whether a given string conforms to the standard UUID format.
 * 
 * ### Use Cases:
 * - **Unique Identifiers**: Generate unique identifiers for database records, session tokens, or any other entities that require uniqueness.
 * - **Data Correlation**: Use UUIDs to correlate data across different systems or components without relying on sequential IDs.
 * - **Validation**: Ensure that input strings are valid UUIDs before processing them in applications that require UUIDs for identification.
 * 
 * ### Example Usage:
 * ```ts
 * import { UUID } from "@aerokit/sdk/utils";
 * 
 * // Generate a new random UUID
 * const newUuid = UUID.random();
 * console.log(newUuid); // Output: a randomly generated UUID string
 * 
 * // Validate a UUID
 * const isValid = UUID.validate(newUuid);
 * console.log(isValid); // Output: true
 * ```
 */

const UuidFacade = Java.type("org.eclipse.dirigible.components.api.utils.UuidFacade");

export class UUID {

	/**
	 * Generates a new random UUID (Type 4).
	 * The generated string is typically in the format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx.
	 *
	 * @returns A string representing the newly generated UUID.
	 */
	public static random(): string {
		return UuidFacade.random();
	}

	/**
	 * Validates if the provided string conforms to the standard UUID format
	 * (e.g., a valid 36-character string including hyphens).
	 *
	 * @param input The string to validate.
	 * @returns true if the input string is a valid UUID, false otherwise.
	 */
	public static validate(input: string): boolean {
		return UuidFacade.validate(input);
	}

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = UUID;
}