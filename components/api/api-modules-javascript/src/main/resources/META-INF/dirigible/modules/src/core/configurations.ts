/**
 * @module core/configurations
 * @overview
 * 
 * The Configurations API provides a centralized, type-safe interface for managing application configuration properties and detecting the runtime operating system. It serves as the foundation for environment-specific settings and system-aware behavior in Aerokit applications.
 * 
 * ### Key Features
 * 
 * - **Static Interface**: Thread-safe, singleton-like access to configuration data
 * - **Type Safety**: Full TypeScript support with proper type definitions
 * - **File Loading**: Support for loading configurations from external files
 * - **OS Detection**: Comprehensive operating system identification
 * - **Memory Efficient**: In-memory storage with optional persistence
 * 
 * ### Use Cases
 * - Application configuration management
 * - Environment variable management
 * - Feature flag configuration
 * - Database connection settings
 * - API endpoint configuration
 * - Platform-specific behavior adaptation
 * 
 * ### Example Usage
 * ```ts
 * import { Configurations } from "@aerokit/sdk/core";
 * 
 * // Set a configuration property
 * Configurations.set("apiEndpoint", "https://api.example.com");
 * // Get a configuration property
 * const apiEndpoint = Configurations.get("apiEndpoint");
 * console.log(apiEndpoint); // Output: "https://api.example.com"
 * 
 * // Check the operating system
 * if (Configurations.isOSWindows()) {
 *   console.log("Running on Windows");
 * } else if (Configurations.isOSMac()) {
 *   console.log("Running on Mac");
 * } else if (Configurations.isOSUNIX()) {
 *   console.log("Running on UNIX");
 * }
 * ```
*/

const Configuration = Java.type("org.eclipse.dirigible.commons.config.Configuration");

export class Configurations {

	/**
	 * Retrieves the configuration value associated with the given key.
	 * @param key The configuration key.
	 * @param defaultValue The optional default value to return if the key is not found.
	 * @returns The configuration value as a string, or `undefined` if the key is not found and no default is provided.
	 */
	public static get(key: string, defaultValue?: string): string | undefined {
		const value = Configuration.get(key, defaultValue);
		return value ?? undefined;
	}

	/**
	 * Sets the configuration value for the given key.
	 * If the key already exists, its value is overwritten.
	 * @param key The configuration key.
	 * @param value The configuration value to set.
	 */
	public static set(key: string, value: string): void {
		Configuration.set(key, value);
	}

	/**
	 * Removes the configuration property associated with the given key.
	 * @param key The configuration key to remove.
	 */
	public static remove(key: string): void {
		Configuration.remove(key);
	}

	/**
	 * Retrieves a list of all current configuration keys.
	 * @returns An array of configuration keys (strings).
	 */
	public static getKeys(): string[] {
		return Configuration.getKeys();
	}

	/**
	 * Loads configuration properties from a file at the specified path, overriding existing ones.
	 * @param path The file path to load configurations from.
	 */
	public static load(path: string): void {
		Configuration.load(path);
	}

	/**
	 * Reloads or updates the current configuration settings from their source (e.g., persistence layer).
	 */
	public static update(): void {
		Configuration.update();
	}

	/**
	 * Retrieves the name of the current Operating System.
	 * @returns The OS name as a string (e.g., "Windows", "Linux", "Mac OS X").
	 */
	public static getOS(): string {
		return Configuration.getOS();
	}

	/**
	 * Checks if the current Operating System is Windows.
	 * @returns True if the OS is Windows, false otherwise.
	 */
	public static isOSWindows(): boolean {
		return Configuration.isOSWindows();
	}

	/**
	 * Checks if the current Operating System is Mac OS (or Mac OS X).
	 * @returns True if the OS is Mac, false otherwise.
	 */
	public static isOSMac(): boolean {
		return Configuration.isOSMac();
	}

	/**
	 * Checks if the current Operating System is a UNIX-like system (e.g., Linux, macOS, or others).
	 * @returns True if the OS is a UNIX variant, false otherwise.
	 */
	public static isOSUNIX(): boolean {
		return Configuration.isOSUNIX();
	}

	/**
	 * Checks if the current Operating System is Solaris.
	 * @returns True if the OS is Solaris, false otherwise.
	 */
	public static isOSSolaris(): boolean {
		return Configuration.isOSSolaris();
	}

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Configurations;
}