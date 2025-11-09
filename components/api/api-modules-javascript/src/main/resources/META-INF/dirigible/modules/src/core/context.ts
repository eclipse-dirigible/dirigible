/**
 * API Context
 * * Provides a static interface for accessing and manipulating key-value pairs in a global, application-wide context.
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