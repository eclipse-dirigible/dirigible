/**
 * Commmon function for initializng the callback functions in the ResourceMethod instances.
 *
 * @param thiz The ResourceMethod instance to which the function is bound (or Resource instance in the case of redirect).
 * @param configuration The configuration object where the handler will be attached.
 * @param sHandlerFuncName The name of the function that will be attached to the resource mappings configuration (e.g., 'serve', 'redirect').
 * @param fHandler The handler function or value that will be attached to the resource mappings configuration.
 * @returns The instance passed in as 'thiz' for method chaining.
 * @private
 */
export function handlerFunction(thiz: any, configuration: any, sHandlerFuncName: string, fHandler: Function | string): any {
    if (fHandler !== undefined) {
        // If fHandler is a string (only used for 'redirect'), it's valid.
        // If fHandler is a function, it's valid.
        if (typeof fHandler !== 'function' && typeof fHandler !== 'string') {
            throw new Error(`Invalid argument: ${sHandlerFuncName} method argument must be a valid javascript function or string, but instead is ${typeof fHandler}`);
        }
        configuration[sHandlerFuncName] = fHandler;
    }

    // Return the chaining object (Resource or ResourceMethod)
    return thiz;
}