/**
 * API Env
 * * Provides a static interface for accessing and listing environment variables exposed to the runtime.
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