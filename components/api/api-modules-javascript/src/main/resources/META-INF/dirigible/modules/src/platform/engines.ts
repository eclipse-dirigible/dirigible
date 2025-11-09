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