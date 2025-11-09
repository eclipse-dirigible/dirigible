/**
 * Custom error class representing a 403 Forbidden status, indicating
 * that the user does not have permission to access the requested resource.
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