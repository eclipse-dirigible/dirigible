/**
 * @module platform/engines
 * @package @aerokit/sdk/platform
 * @name Engines
 * @overview
 * 
 * The Engines module provides a class for interacting with the platform's execution engines, such as JavaScript or Groovy. It allows developers to execute scripts or processes within the context of a project, passing parameters and optionally enabling debug mode. The module abstracts the complexities of engine execution, providing a simple interface for running code in various supported languages.
 * 
 * ### Key Features:
 * - **Engine Type Management**: Retrieve the list of available engine types supported by the platform.
 * - **Script Execution**: Execute project scripts or processes using a specified engine type, with support for passing parameters and enabling debug mode.
 * 
 * ### Use Cases:
 * - **Multi-Language Support**: This module is ideal for applications that need to execute code in different languages supported by the platform, such as JavaScript or Groovy.
 * - **Project-Based Execution**: Developers can use this module to run scripts within the context of a project, allowing for better organization and management of code.
 * 
 * ### Example Usage:
 * ```ts
 * import { Engine } from "@aerokit/sdk/platform";
 * 
 * // Get available engine types
 * const engineTypes = Engine.getTypes();
 * console.log("Available Engines:", engineTypes);
 * 
 * // Create an instance of the Engine class for JavaScript
 * const jsEngine = new Engine("javascript");
 * 
 * // Execute a script within a project context
 * const result = jsEngine.execute(
 *     "MyProject",
 *     "lib/script.js",
 *     "",
 *     { param1: "value1", param2: 42 },
 *     false
 * );
 * console.log("Execution Result:", result);
 * ```
 */

const EnginesFacade = Java.type("org.eclipse.dirigible.components.api.platform.EnginesFacade");
const HashMap = Java.type("java.util.HashMap");

/**
 * Interface defining the execution parameters expected by the Engine class.
 */
export interface ExecutionParameters {
	[key: string]: any
}

/**
 * @class Engine
 * @description Represents a specific execution engine type (e.g., JavaScript, Groovy)
 * and provides methods to interact with the platform's execution facade.
 */
export class Engine {
	private type: string;

	/**
	 * Creates an instance of Engine.
	 * @param {string} type The type of the execution engine (e.g., "javascript").
	 */
	constructor(type: string) {
		this.type = type;
	}

	/**
	 * Retrieves the list of available engine types from the platform.
	 * @returns {string[]} An array of supported engine type names.
	 */
	public static getTypes(): string[] {
		// The facade method returns a JSON array string, which we parse.
		return JSON.parse(EnginesFacade.getEngineTypes());
	}

	/**
	 * Executes a project script or process using the configured engine type.
	 *
	 * @param {string} projectName The name of the project.
	 * @param {string} projectFilePath The relative path to the main file to execute within the project (e.g., "lib/script.js").
	 * @param {string} projectFilePathParam A secondary file path parameter (often unused or context-specific).
	 * @param {ExecutionParameters} parameters An object containing key/value parameters to pass to the script context.
	 * @param {boolean} [debug=false] Whether to execute in debug mode.
	 * @returns {any} The result returned by the executed script.
	 */
	public execute(
		projectName: string,
		projectFilePath: string,
		projectFilePathParam: string,
		parameters: ExecutionParameters,
		debug: boolean = false
	): any {
		// Convert the TypeScript/JavaScript parameter object into a Java HashMap
		// which is required by the EnginesFacade API.
		const mapInstance = new HashMap();
		for (const property in parameters) {
			// CRITICAL FIX: Ensure we use the 'parameters' object, not the global 'context'
			if (parameters.hasOwnProperty(property)) {
				mapInstance.put(property, parameters[property]);
			}
		}

		return EnginesFacade.execute(
			this.type,
			projectName,
			projectFilePath,
			projectFilePathParam,
			mapInstance,
			debug
		);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Engine;
}