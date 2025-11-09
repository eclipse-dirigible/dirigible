/**
 * API Globals
 * * Provides a static interface for accessing and manipulating global application variables, typically backed by a central configuration or registry.
 */

const GlobalsFacade = Java.type("org.eclipse.dirigible.components.api.core.GlobalsFacade");

/**
 * Interface representing a map of global variable names to their string values.
 */
export interface GlobalsValues {
	[key: string]: string;
}

export class Globals {

	/**
	 * Retrieves the value of the global variable with the specified name.
	 * @param name The name of the global variable.
	 * @returns The variable's value as a string, or `undefined` if the variable is not set or its value is null.
	 */
	public static get(name: string): string | undefined {
		const value = GlobalsFacade.get(name);
		return value ?? undefined;
	}

	/**
	 * Sets the value of a global variable.
	 * If the variable already exists, its value is overwritten.
	 * @param name The name of the global variable.
	 * @param value The value to set (must be a string).
	 */
	public static set(name: string, value: string): void {
		GlobalsFacade.set(name, value);
	}

	/**
	 * Retrieves a map of all global variables currently defined in the application.
	 * @returns A {@link GlobalsValues} object containing all global variables as key-value pairs.
	 */
	public static list(): GlobalsValues {
		return JSON.parse(GlobalsFacade.list());
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Globals;
}