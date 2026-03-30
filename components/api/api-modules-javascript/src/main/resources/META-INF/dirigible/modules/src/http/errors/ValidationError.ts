/**
 * @module http/errors/ValidationError
 * @overview
 * 
 * This module defines the `ValidationError` class, a custom error type that represents failures due to invalid input or data that violates domain-specific validation rules. The `ValidationError` class extends the built-in `Error` class and provides a default error message, while also allowing for custom messages to be specified when instantiated. This error is typically thrown when user input fails to meet certain criteria or when data does not conform to expected formats, helping developers to handle validation issues in a consistent manner across their applications.
 */

export class ValidationError extends Error {
    /**
     * The name of the error, set to "ValidationError".
     */
    readonly name = "ValidationError";
    /**
     * Captures the stack trace when the error is instantiated.
     */
    readonly stack = (new Error()).stack;

    /**
     * Creates an instance of ValidationError.
     *
     * @param message The detailed message describing the validation failure.
     */
    constructor(message: string) {
        super(message);
    }
}