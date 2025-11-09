/**
 * Custom error class used for representing failures due to invalid input
 * or data that violates domain-specific validation rules.
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