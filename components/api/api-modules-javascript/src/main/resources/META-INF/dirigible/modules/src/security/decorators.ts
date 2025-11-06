const UserFacade = Java.type("org.eclipse.dirigible.components.api.security.UserFacade");

/**
 * @param {string[]} roles
 */
export function RolesAllowed(roles) {
    return function (target) {

        const moduleName = target.name || "<anonymous>";

        Object.getOwnPropertyNames(target.prototype).forEach(methodName => {

            if (methodName === "constructor") {
                return;
            }

            const original = target.prototype[methodName];

            target.prototype[methodName] = function (...args) {

                const allowed = roles.some(role => UserFacade.isInRole(role));

                if (!allowed) {
                    throw new Error(
                        "User is not allowed to call this module: " + moduleName
                    );
                }

                return original.apply(this, args);
            };
        });

        return target;
    };
}