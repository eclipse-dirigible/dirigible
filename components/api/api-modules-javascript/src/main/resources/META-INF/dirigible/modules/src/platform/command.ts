/**
 * Provides a wrapper for executing system commands via the platform's CommandEngine.
 */
const CommandFacade = Java.type("org.eclipse.dirigible.components.api.platform.CommandFacade");
const ProcessExecutionOptions = Java.type("org.eclipse.dirigible.commons.process.execution.ProcessExecutionOptions");

/**
 * Defines the configuration options for the command execution process.
 */
interface CommandOptions {
	/** The directory in which the command will be executed. */
	workingDirectory: string
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