/**
 * @module extensions/extensions
 * @overview
 * 
 * This module provides the `Extensions` class, which offers functionality for discovering and loading extensions defined against the Dirigible extension model. The `Extensions` class allows developers to retrieve registered extensions for specific extension points and to load those extensions as modules, ensuring that they meet specified requirements (e.g., exporting certain functions).
 * 
 * ### Key Features
 * - Retrieve registered extension module paths for specific extension points.
 * - Load extension modules with support for both synchronous and asynchronous loading mechanisms.
 * - Validate loaded extensions against required function exports to ensure they meet expected interfaces.
 * - Configurable error handling to either log issues or throw exceptions based on the use case.
 * 
 * ### Use Cases
 * - Dynamically loading and integrating extensions at runtime based on the application's needs.
 * - Ensuring that loaded extensions conform to expected interfaces by checking for required functions.
 * - Managing extensions in a modular application design, allowing for third-party contributions without modifying core code.
 * 
 * ### Example Usage
 * ```ts
 * import { Extensions } from "@aerokit/sdk/extensions";
 * 
 * // Load extensions for a specific extension point, requiring certain functions to be exported
 * const loadedExtensions = await Extensions.load("my.extension.point", ["initialize", "execute"], true);
 * 
 * // Use the loaded extensions
 * loadedExtensions.forEach(extension => {
 *   extension.initialize();
 *   extension.execute();
 * });
 * ```
 */

const ExtensionsFacade = Java.type("org.eclipse.dirigible.components.api.extensions.ExtensionsFacade");

/**
 * Provides functionality for discovering and loading extensions defined
 * against the Dirigible extension model.
 */
export class Extensions {

	/**
	 * Retrieves the list of extension module paths registered for a specific extension point.
	 *
	 * @param extensionPoint The unique identifier of the extension point (e.g., "my.extension.point").
	 * @returns An array of string paths (modules) registered for the given extension point.
	 */
	public static getExtensions(extensionPoint: string): string[] {
		const extensions = ExtensionsFacade.getExtensions(extensionPoint);
		// The native facade returns a Java List, which is converted to JSON string 
		// and then parsed to a JavaScript array.
		return JSON.parse(JSON.stringify(extensions));
	}

	/**
	 * Retrieves all available extension point identifiers.
	 *
	 * @returns An array of strings representing all registered extension point IDs.
	 */
	public static getExtensionPoints(): string[] {
		const extensionPoints = ExtensionsFacade.getExtensionPoints();
		return JSON.parse(JSON.stringify(extensionPoints));
	}

	/**
	 * Loads extension modules registered for a specific extension point.
	 * It handles both synchronous (require) and asynchronous (import) loading.
	 *
	 * @param extensionPoint The unique identifier of the extension point.
	 * @param requiredFunctions An optional list of function names that the extension module must export to be included.
	 * @param throwError If true, throws an error on failure; otherwise, logs the error and continues.
	 * @returns A Promise that resolves to an array of successfully loaded and validated extension modules (exports).
	 */
	public static async loadExtensionModules(extensionPoint: string, requiredFunctions: string[] = [], throwError: boolean = false): Promise<any[]> {
		const extensionModules = [];
		const extensions: string[] = this.getExtensions(extensionPoint);

		for (let i = 0; i < extensions?.length; i++) {
			const module: string = extensions[i];
			try {
				let extensionModule: any;
				try {
					// Fallback to native require() (Dirigible-specific)
					// @ts-ignore
					extensionModule = dirigibleRequire(module);
				} catch (e) {
					// Fallback to dynamic import (for environments supporting ESM/Async)
					extensionModule = await import(`../../../../${module}`);
				}

				if (!extensionModule || Object.keys(extensionModule).length === 0) {
					const errorMessage = `Extension '${module}' for extension point '${extensionPoint}' doesn't provide any function(s) or was not properly loaded, consider publishing it.`;
					this.logError(throwError, errorMessage);
					continue;
				}

				let requiredFunctionsFound = true;
				requiredFunctions.forEach(f => {
					requiredFunctionsFound &&= typeof extensionModule[f] === "function";
				});

				if (!requiredFunctionsFound) {
					const errorMessage = `Extension '${module}' for extension point '${extensionPoint}', doesn't provide the following required function(s): [\n\t${requiredFunctions.join("(),\n\t")}()\n]`;
					this.logError(throwError, errorMessage);
					continue;
				}

				extensionModules.push(extensionModule);
			} catch (e) {
				const errorMessage = `Error occurred while importing extension '${module}' for extension point '${extensionPoint}'.`;
				this.logError(throwError, errorMessage, e);
			}
		}

		return extensionModules;
	}

	/**
	 * Logs an error to the console and optionally throws an exception.
	 * @param throwError If true, an exception is thrown.
	 * @param errorData The data/message to log and/or use for the error message.
	 */
	private static logError(throwError: boolean, ...errorData: any[]): void {
		console.error(errorData);
		if (throwError) {
			throw new Error(errorData[0]);
		}
	}
	
	/** Alias for loadExtensionModules */
	public static async load(extensionPoint: string, requiredFunctions: string[] = [], throwError: boolean = false): Promise<any[]> {
		return Extensions.loadExtensionModules(extensionPoint, requiredFunctions, throwError);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Extensions;
}