/**
 * @module http/errors/ForbiddenError
 * @overview
 * 
 * This module defines the `ForbiddenError` class, a custom error type that represents a 403 Forbidden HTTP status. This error is typically thrown when a user attempts to access a resource or perform an action for which they do not have the necessary permissions. The `ForbiddenError` class extends the built-in `Error` class and provides a default error message, while also allowing for custom messages to be specified when instantiated.
 */

export class ForbiddenError extends Error {
    /**
     * The name of the error, set to "ForbiddenError".
     */
    readonly name = "ForbiddenError";
    /**
     * Captures the stack trace when the error is instantiated.
     */
    readonly stack = (new Error()).stack;

    /**
     * Creates an instance of ForbiddenError.
     *
     * @param message The error message. Defaults to "You don't have permission to access this resource".
     */
    constructor(message: string = "You don't have permission to access this resource") {
        super(message);
    }
}