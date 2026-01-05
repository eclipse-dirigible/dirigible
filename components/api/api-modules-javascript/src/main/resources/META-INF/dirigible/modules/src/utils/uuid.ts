const UuidFacade = Java.type("org.eclipse.dirigible.components.api.utils.UuidFacade");

/**
 * Utility class for generating and validating Universally Unique Identifiers (UUIDs).
 * It typically provides access to Type 4 (randomly generated) UUIDs.
 */
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