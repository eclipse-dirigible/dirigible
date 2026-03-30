/**
 * @module core/context
 * @overview
 * 
 * The Context API provides a simple, static interface for storing and retrieving key-value pairs in a global application context. It allows components and modules to share data across the entire application without needing to pass values through function parameters or component props. The Context API is designed to be lightweight and easy to use, making it ideal for managing global state, configuration settings, or any other data that needs to be accessible throughout the application.
 * 
 * ### Key Features
 * - **Global Accessibility**: Values stored in the context can be accessed from anywhere in the application, facilitating data sharing across components and modules.
 * - **Simple API**: The Context API provides straightforward `get` and `set` methods for managing context values, making it easy to use without complex setup.
 * - **Dynamic Storage**: The context can store any type of value, including primitives, objects, arrays, and even functions, allowing for flexible data management.
 * - **Overwriting Values**: Setting a value with an existing name will overwrite the previous value, enabling dynamic updates to the context as needed.
 * 
 * ### Use Cases
 * - Managing global configuration settings that need to be accessed by multiple components or modules.
 * - Sharing state or data across different parts of the application without prop drilling or complex state management solutions.
 * - Storing references to services, utilities, or other shared resources that need to be accessible throughout the application.
 * - Facilitating communication between components that do not have a direct parent-child relationship by using the context as a shared data store.
 * 
 * ### Example Usage
 * ```ts
 * import { Context } from "@aerokit/sdk/core";
 * 
 * // Set a value in the context
 * Context.set("apiEndpoint", "https://api.example.com");
 * 
 * // Get a value from the context
 * const apiEndpoint = Context.get("apiEndpoint");
 * console.log(apiEndpoint); // Output: "https://api.example.com"
 * 
 * // Overwrite an existing value in the context
 * Context.set("apiEndpoint", "https://api.newexample.com");
 * const newApiEndpoint = Context.get("apiEndpoint");
 * console.log(newApiEndpoint); // Output: "https://api.newexample.com"
 * ```
 */

const ContextFacade = Java.type("org.eclipse.dirigible.components.api.core.ContextFacade");

export class Context {

	/**
	 * Retrieves the value associated with the specified name from the global context.
	 * @param name The name of the context variable.
	 * @returns The context value, or `undefined` if the name is not found or the value is null.
	 */
	public static get(name: string): any | undefined {
		const value = ContextFacade.get(name)
		return value ?? undefined;
	}

	/**
	 * Stores a value in the global context under the specified name.
	 * If the name already exists, its value is overwritten.
	 * @param name The name of the context variable.
	 * @param value The value to store.
	 */
	public static set(name: string, value: any): void {
		ContextFacade.set(name, value);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Context;
}