/**
 * @module core/globals
 * @overview
 * 
 * The Globals API provides a static interface for accessing and manipulating global application variables, typically backed by a central configuration or registry. It allows components and modules to share global state across the entire application without needing to pass values through function parameters or component props. The Globals API is designed to be lightweight and easy to use, making it ideal for managing global state, configuration settings, or any other data that needs to be accessible throughout the application.
 * 
 * ### Key Features
 * - **Global Accessibility**: Values stored in the globals can be accessed from anywhere in the application, facilitating data sharing across components and modules.
 * - **Simple API**: The Globals API provides straightforward `get`, `set`, and `list` methods for managing global variables, making it easy to use without complex setup.
 * - **Dynamic Storage**: The globals can store string values, allowing for flexible data management.
 * - **Overwriting Values**: Setting a value with an existing name will overwrite the previous value, enabling dynamic updates to the globals as needed.
 * 
 * ### Use Cases
 * - Managing global configuration settings that need to be accessed by multiple components or modules.
 * - Sharing state or data across different parts of the application without prop drilling or complex state management solutions.
 * - Storing references to services, utilities, or other shared resources that need to be accessible throughout the application.
 * - Facilitating communication between components that do not have a direct parent-child relationship by using the globals as a shared data store.
 * 
 * ### Example Usage
 * ```ts
 * import { Globals } from "@aerokit/sdk/core";
 * 
 * // Set a value in the globals
 * Globals.set("apiEndpoint", "https://api.example.com");
 * 
 * // Get a value from the globals
 * const apiEndpoint = Globals.get("apiEndpoint");
 * console.log(apiEndpoint); // Output: "https://api.example.com"
 * 
 * // List all global variables
 * const globalVars = Globals.list();
 * console.log(globalVars); // Output: { apiEndpoint: "https://api.example.com", ... }
 * ```
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