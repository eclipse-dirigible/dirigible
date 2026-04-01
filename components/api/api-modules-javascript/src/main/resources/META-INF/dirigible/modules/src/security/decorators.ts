/**
 * @module security/decorators
 * @package @aerokit/sdk/security
 * @name Decorators
 * @overview
 * 
 * This module provides security-related decorators for enforcing role-based access control on classes and methods. The primary decorator, `@Roles`, allows developers to specify which user roles are required to access certain functionality within their application. By applying the `@Roles` decorator to a class or method, developers can ensure that only users with the appropriate roles can execute the associated code, enhancing the security of their applications.
 * 
 * ### Key Features:
 * - **Role-Based Access Control**: The `@Roles` decorator enables developers to define access control rules based on user roles, ensuring that only authorized users can access specific functionality.
 * - **Class and Method Level Security**: The `@Roles` decorator can be applied at both the class and method levels, allowing for flexible security configurations.
 * 
 * ### Use Cases:
 * - **Securing API Endpoints**: Developers can use the `@Roles` decorator to protect API endpoints, ensuring that only users with the necessary roles can access certain routes or perform specific actions.
 * - **Enforcing Business Logic**: By applying the `@Roles` decorator to methods that contain critical business logic, developers can prevent unauthorized access and maintain the integrity of their applications.
 * 
 * ### Example Usage:
 * ```ts
 * import { Roles } from "@aerokit/sdk/security";
 * 
 * @Roles(["admin"])
 * class AdminController {
 *     @Roles(["admin", "manager"])
 *     deleteUser(userId: string) {
 *         // Logic to delete a user, accessible only to admin and manager roles
 *     }
 * 
 *     @Roles(["admin"])
 *     createUser(userData: any) {
 *         // Logic to create a new user, accessible only to admin role
 *     }
 * }
 * ```
 */

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
                    const errorMessage = `Current user [${UserFacade.getName()}] is not allowed to call module [${moduleName}]. Required some of roles [${roles}]`;
                    console.error(errorMessage);

                    throw new Error(errorMessage);
                }
                return original.apply(this, args);
            };
        }

        return target;
    };
}
