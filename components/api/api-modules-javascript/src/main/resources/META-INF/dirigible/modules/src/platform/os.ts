/**
 * Utility class for retrieving operating system information and checking OS types.
 * It leverages the platform's access to Java's SystemUtils for system properties.
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