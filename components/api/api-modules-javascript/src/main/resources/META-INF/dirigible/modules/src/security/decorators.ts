const UserFacade = Java.type("org.eclipse.dirigible.components.api.security.UserFacade");

/**
 * @param {string[]} roles
 */
export function Roles(roles) {
    return function (target, _context) {
        const moduleName = target.name || "<unknown>";
        for (const methodName of Object.getOwnPropertyNames(target.prototype)) {
            if (methodName === "constructor") {
                continue;
            }
            const original = target.prototype[methodName];
            if (typeof original !== "function") {
                continue;
            }
            target.prototype[methodName] = function (...args) {
                const allowed = roles.some(role => UserFacade.isInRole(role));
                if (!allowed) {
                    throw new Error(
                        "User is not allowed to call this module: " + moduleName
                    );
                }
                return original.apply(this, args);
            };
        }

        return target;
    };
}