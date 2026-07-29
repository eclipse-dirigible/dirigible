/**
 * @module platform/command
 * @package @aerokit/sdk/platform
 * @name Command
 * @overview
 * 
 * The Command module provides a static utility class for executing system commands through the platform's CommandEngine. It allows developers to run shell commands with configurable options, including working directory, execution timeout and environment variable management. The module abstracts the complexities of process execution, providing a simple interface for integrating command execution into applications.
 *
 * ### Key Features:
 * - **Command Execution**: The `execute` method allows for running system commands with specified options and environment variable configurations.
 * - **Structured Output**: The method returns a structured output containing the exit code, standard output, and error output of the executed command.
 * - **Timeouts**: A `timeoutMillis` option destroys a command that outlives it, so a hung process cannot block the caller indefinitely.
 * 
 * ### Use Cases:
 * - **System Integration**: This module is ideal for applications that need to interact with the underlying operating system or execute external processes as part of their functionality.
 * - **Automation and Scripting**: Developers can use this module to automate tasks by executing shell commands from within their applications.
 * 
 * ### Example Usage:
 * ```ts
 * import { Command } from "@aerokit/sdk/platform";
 * 
 * const result = Command.execute("ls -l", { workingDirectory: "/home/user" }, { "ENV_VAR": "value" }, ["OLD_ENV_VAR"]);
 * console.log("Exit Code:", result.exitCode);
 * console.log("Standard Output:", result.standardOutput);
 * console.log("Error Output:", result.errorOutput);
 *
 * // Give up on a command that takes longer than five seconds
 * Command.execute("./long-running.sh", { timeoutMillis: 5000 });
 * ```
 */

const CommandFacade = Java.type("org.eclipse.dirigible.components.api.platform.CommandFacade");
const ProcessExecutionOptions = Java.type("org.eclipse.dirigible.commons.process.execution.ProcessExecutionOptions");

/**
 * Defines the configuration options for the command execution process.
 */
interface CommandOptions {
	/** The directory in which the command will be executed. */
	workingDirectory?: string
	/**
	 * The maximum time in milliseconds the command is allowed to run. A command still running when the
	 * timeout elapses is destroyed and the execution throws instead of returning an exit code.
	 * Omit to let the command run for as long as it needs.
	 */
	timeoutMillis?: number
}

/**
 * Defines key-value pairs for environment variables to add during execution.
 */
interface EnvironmentVariables {
	[key: string]: string
}

/**
 * Defines the structured output returned after command execution.
 */
interface CommandOutput {
	/** The exit code returned by the executed process (0 usually means success). */
	exitCode: number;
	/** The standard output stream content. */
	standardOutput: string;
	/** The standard error stream content. */
	errorOutput: string;
}

/**
 * @class Command
 * @description Static utility class for executing system commands.
 */
export class Command {

	/**
	 * Executes a system command with specified configuration, environment variables, and exclusions.
	 *
	 * @param {string} command The command string to execute (e.g., "ls -l").
	 * @param {CommandOptions} [options] Optional configuration for the execution environment.
	 * @param {EnvironmentVariables} [add] Optional environment variables to add to the process.
	 * @param {string[]} [remove] Optional list of environment variable keys to remove from the process.
	 * @returns {CommandOutput} A structured object containing the exit code and output streams.
	 */
	public static execute(command: string, options?: CommandOptions, add?: EnvironmentVariables, remove?: string[]): CommandOutput {
		// Instantiate the native Java ProcessExecutionOptions object
		const processExecutionOptions = new ProcessExecutionOptions();

		if (options?.workingDirectory) {
			processExecutionOptions.setWorkingDirectory(options.workingDirectory);
		}

		if (options?.timeoutMillis) {
			processExecutionOptions.setTimeoutMillis(options.timeoutMillis);
		}

		// The facade returns a JSON string, which we parse into the CommandOutput interface
		const resultJson = CommandFacade.execute(command, add, remove, processExecutionOptions);
		return JSON.parse(resultJson) as CommandOutput;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Command;
}