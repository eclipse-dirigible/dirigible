export class ForbiddenError extends Error {
    readonly name = "ForbiddenError";
    readonly stack = (new Error()).stack;

    constructor(message: string = "You don't have permission to access this resource") {
        super(message);
    }
}