/**
 * Provides a wrapper for the underlying logging facility, allowing
 * for categorized and leveled logging messages with support for variable arguments,
 * including error objects.
 */
const LogFacade = Java.type("org.eclipse.dirigible.components.api.log.LogFacade");


/**
 * The main entry point for the logging API. Use this class to obtain a named
 * logger instance.
 */
export class Logging {

	/**
	 * Retrieves or creates a Logger instance associated with a specific name.
	 * The logger name is typically used to categorize log messages (e.g., 'com.app.service').
	 *
	 * @param loggerName The name of the logger.
	 * @returns A {@link Logger} instance.
	 */
	public static getLogger(loggerName: string): Logger {
		return new Logger(loggerName);
	}
}

/**
 * Represents a named logger instance used for emitting log messages at various levels.
 */
export class Logger {

	private loggerName: string;

	/**
	 * @param loggerName The name of the logger.
	 */
	constructor(loggerName: string) {
		this.loggerName = loggerName;
	}

	/**
	 * Sets the logging level for this specific logger instance.
	 * Messages below this threshold will be ignored.
	 *
	 * @param level The desired logging level (e.g., 'TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR').
	 * @returns The Logger instance for method chaining.
	 */
	public setLevel(level: string): Logger {
		LogFacade.setLevel(this.loggerName, level);
		return this;
	}

    /**
     * Checks if the DEBUG level is currently enabled for this logger.
     * @returns True if DEBUG logging is enabled, false otherwise.
     */
    public isDebugEnabled(): boolean {
        return LogFacade.isDebugEnabled(this.loggerName);
    }

    /**
     * Checks if the ERROR level is currently enabled for this logger.
     * @returns True if ERROR logging is enabled, false otherwise.
     */
    public isErrorEnabled(): boolean {
        return LogFacade.isErrorEnabled(this.loggerName);
    }

    /**
     * Checks if the WARN level is currently enabled for this logger.
     * @returns True if WARN logging is enabled, false otherwise.
     */
    public isWarnEnabled(): boolean {
        return LogFacade.isWarnEnabled(this.loggerName);
    }

    /**
     * Checks if the INFO level is currently enabled for this logger.
     * @returns True if INFO logging is enabled, false otherwise.
     */
    public isInfoEnabled(): boolean {
        return LogFacade.isInfoEnabled(this.loggerName);
    }

    /**
     * Checks if the TRACE level is currently enabled for this logger.
     * @returns True if TRACE logging is enabled, false otherwise.
     */
    public isTraceEnabled(): boolean {
        return LogFacade.isTraceEnabled(this.loggerName);
    }

	/**
	 * The core logging method. Logs a message at the specified level, optionally
	 * supporting parameters for message formatting and a final Error object for stack trace logging.
	 *
	 * @param msg The log message template (e.g., "User {0} failed to connect: {1}").
	 * @param level The logging level (e.g., 'DEBUG', 'ERROR').
	 * @param [args] Optional arguments for message formatting. The last argument can be an Error object.
	 */
	public log(msg: string, level: string): void {
		const args = Array.prototype.slice.call(arguments);
		let msgParameters = [];

		let rawError = null;
		if (args.length > 2) {
			if (args[args.length-1] instanceof Error) {
                rawError = args[args.length-1];

                if (rawError.stack) {
                    console.debug("Handling error with stack:\n" + rawError.stack);
                }
			}
			const endIndex = rawError ? (args.length - 1) : args.length;
			msgParameters = args.slice(2, endIndex).map(function (param) {
				return typeof param === 'object' ? JSON.stringify(param) : param;
			});
		}

		LogFacade.log(this.loggerName, level, msg, JSON.stringify(msgParameters), rawError);
	}

	/**
	 * Logs a message at the DEBUG level.
	 *
	 * @param msg The log message template.
	 * @param [args] Optional arguments for message formatting. The last argument can be an Error object.
	 */
	public debug(msg: string, ..._: any[]): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'DEBUG');//insert DEBUG on second position in arguments array
		this.log.apply(this, args);
	}

	/**
	 * Logs a message at the INFO level.
	 *
	 * @param msg The log message template.
	 * @param [args] Optional arguments for message formatting. The last argument can be an Error object.
	 */
	public info(msg: string, ..._: any[]): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'INFO');//insert INFO on second position in arguments array
		this.log.apply(this, args);
	}

	/**
	 * Logs a message at the TRACE level.
	 *
	 * @param msg The log message template.
	 * @param [args] Optional arguments for message formatting. The last argument can be an Error object.
	 */
	public trace(msg: string, ..._: any[]): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'TRACE');//insert TRACE on second position in arguments array
		this.log.apply(this, args);
	}

	/**
	 * Logs a message at the WARN level.
	 *
	 * @param msg The log message template.
	 * @param [args] Optional arguments for message formatting. The last argument can be an Error object.
	 */
	public warn(msg: string, ..._: any[]): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'WARN');//insert WARN on second position in arguments array
		this.log.apply(this, args);
	}

	/**
	 * Logs a message at the ERROR level.
	 *
	 * @param msg The log message template.
	 * @param [args] Optional arguments for message formatting. The last argument can be an Error object.
	 */
	public error(msg: string, ..._: any[]): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'ERROR');//insert ERROR on second position in arguments array
		this.log.apply(this, args);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Logging;
}