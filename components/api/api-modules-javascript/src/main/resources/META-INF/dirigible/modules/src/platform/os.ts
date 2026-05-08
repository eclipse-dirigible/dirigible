/**
 * @module platform/os
 * @package @aerokit/sdk/platform
 * @name OS
 * @overview
 * 
 * The OS module provides a static utility class for retrieving operating system information and checking OS types. It leverages the platform's access to Java's SystemUtils for system properties, allowing developers to easily determine the operating system their application is running on. This can be particularly useful for applications that need to perform OS-specific operations or optimizations.
 * 
 * ### Key Features:
 * - **OS Name Retrieval**: Access the full name of the operating system through a static constant.
 * - **OS Type Checking**: Static methods to check if the operating system is a variant of Windows or Unix (including Linux, macOS, and BSD).
 * 
 * ### Use Cases:
 * - **Cross-Platform Compatibility**: This module is ideal for applications that need to ensure compatibility across different operating systems by conditionally executing code based on the OS type.
 * - **System Information**: Developers can use this module to gather information about the operating system for logging, debugging, or analytics purposes.
 * 
 * ### Example Usage:
 * ```ts
 * import { OS } from "@aerokit/sdk/platform";
 * 
 * console.log("Operating System:", OS.OS_NAME);
 * if (OS.isWindows()) {
 *     console.log("Running on Windows");
 * } else if (OS.isUnix()) {
 *     console.log("Running on a Unix-like OS");
 * } else {
 *     console.log("Running on an unknown OS");
 * }
 * ```
 */

const SystemUtils = Java.type("org.apache.commons.lang3.SystemUtils")

/**
 * @class OS
 * @description Provides static methods and constants related to the operating system
 * the underlying Java platform is running on.
 */
export class OS {

	/**
	 * The full name of the operating system (e.g., "Windows 10", "Linux").
	 * This value is read directly from the Java system property 'os.name'.
	 * @type {string}
	 */
	public static readonly OS_NAME: string = SystemUtils.OS_NAME;

	/**
	 * Checks if the operating system is a variant of Windows.
	 * @returns {boolean} True if the OS is Windows, false otherwise.
	 */
	public static isWindows(): boolean {
		return SystemUtils.IS_OS_WINDOWS;
	}

	/**
	 * Checks if the operating system is a variant of Unix (including Linux, macOS, and BSD).
	 * @returns {boolean} True if the OS is Unix-like, false otherwise.
	 */
	public static isUnix(): boolean {
		return SystemUtils.IS_OS_UNIX;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = OS;
}