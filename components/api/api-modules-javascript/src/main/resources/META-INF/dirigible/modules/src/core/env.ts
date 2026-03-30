/**
 * @module core/env
 * @description
 * 
 * The `Env` module provides a static interface for accessing environment variables exposed to the runtime. It allows you to retrieve individual environment variable values by name, as well as list all available environment variables in a structured format.
 * 
 * ### Key Features
 * - **Simple Access**: Retrieve environment variable values using a straightforward `get` method.
 * - **Comprehensive Listing**: Obtain a complete map of all environment variables using the `list` method, which returns an object with key-value pairs.
 * - **Type Safety**: The `get` method returns `undefined` if an environment variable is not set, allowing for safe handling of missing variables.
 * 
 * ### Use Cases
 * - Accessing configuration settings that are provided through environment variables, such as API keys, database connection strings, or feature flags.
 * - Listing all available environment variables for debugging purposes or to dynamically adjust application behavior based on the runtime environment.
 * - Integrating with external services that require environment variable configuration, ensuring that sensitive information is not hardcoded in the application code.
 * 
 * ### Example Usage
 * ```ts
 * import { Env } from "@aerokit/sdk/core";
 * 
 * // Get a specific environment variable
 * const apiKey = Env.get("API_KEY");
 * console.log(apiKey); // Output: "your-api-key" or undefined if not set
 * 
 * // List all environment variables
 * const envVars = Env.list();
 * console.log(envVars); // Output: { API_KEY: "your-api-key", NODE_ENV: "production", ... }
 * ```
 */

const EnvFacade = Java.type("org.eclipse.dirigible.components.api.core.EnvFacade");

/**
 * Interface representing a map of environment variable names to their string values.
 */
export interface EnvValues {
	[key: string]: string;
}

export class Env {

	/**
	 * Retrieves the value of the environment variable with the specified name.
	 * @param name The name of the environment variable.
	 * @returns The variable's value as a string, or `undefined` if the variable is not set.
	 */
	public static get(name: string): string | undefined {
		const value = EnvFacade.get(name);
		return value ?? undefined;
	}

	/**
	 * Retrieves a map of all environment variables currently exposed to the application.
	 * @returns An {@link EnvValues} object containing all environment variables as key-value pairs.
	 */
	public static list(): EnvValues {
		return JSON.parse(EnvFacade.list());
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Env;
}