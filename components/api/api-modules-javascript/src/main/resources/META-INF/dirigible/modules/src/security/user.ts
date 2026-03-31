/**
 * @module security/user
 * @package @aerokit/sdk/security
 * @name User
 * @overview
 * 
 * The User class provides static methods to access the security and session context of the currently authenticated user. It serves as a facade for the underlying UserFacade component, allowing developers to easily retrieve information about the user's identity, roles, session timeout, authentication type, security token, invocation count, and preferred language. This class is essential for implementing role-based access control and managing user sessions within applications built on the platform.
 * 
 * ### Key Features:
 * - **User Identity**: Retrieve the principal name (username or ID) of the currently authenticated user.
 * - **Role Checking**: Determine if the user is assigned to specific security roles, enabling role-based access control.
 * - **Session Information**: Access session-related information such as remaining timeout and security tokens.
 * - **Invocation Tracking**: Monitor the number of requests made by the user during their session.
 * - **Localization Support**: Retrieve the user's preferred language setting for localization purposes.
 * 
 * ### Use Cases:
 * - **Access Control**: Use the `isInRole` method to enforce role-based access control in your application, ensuring that only authorized users can access certain functionality.
 * - **Session Management**: Utilize session information to manage user sessions effectively, such as implementing session timeouts or tracking user activity.
 * - **Localization**: Leverage the user's preferred language to provide localized content and enhance the user experience.
 * 
 * ### Example Usage:
 * ```ts
 * import { User } from "@aerokit/sdk/security";
 * 
 * // Check if the current user is an administrator
 * if (User.isInRole("Administrator")) {
 *     console.log(`Welcome, ${User.getName()}! You have administrator access.`);
 * } else {
 *     console.log(`Hello, ${User.getName()}. You do not have administrator access.`);
 * }
 * 
 * // Get session information
 * const timeout = User.getTimeout();
 * const authType = User.getAuthType();
 * const securityToken = User.getSecurityToken();
 * const invocationCount = User.getInvocationCount();
 * const preferredLanguage = User.getLanguage();
 * ```
 */

const UserFacade = Java.type("org.eclipse.dirigible.components.api.security.UserFacade");

/**
 * Provides static access to the currently authenticated user's security and session context.
 * This class acts as a facade for the underlying UserFacade component.
 */
export class User {

    /**
     * Retrieves the principal name (username or ID) of the currently authenticated user.
     *
     * @returns The user's name or identifier as a string.
     */
    public static getName(): string {
        return UserFacade.getName();
    }

    /**
     * Checks if the currently authenticated user is assigned to a specific security role.
     *
     * @param role The name of the role to check (e.g., 'Administrator', 'User').
     * @returns True if the user is in the specified role, false otherwise.
     */
    public static isInRole(role: string): boolean {
        return UserFacade.isInRole(role);
    }

    /**
     * Retrieves the remaining session timeout for the current user session in seconds.
     *
     * @returns The session timeout duration in seconds.
     */
    public static getTimeout(): number {
        return UserFacade.getTimeout();
    }

    /**
     * Retrieves the authentication mechanism used for the current session (e.g., 'BASIC', 'FORM').
     *
     * @returns The type of authentication used.
     */
    public static getAuthType(): string {
        return UserFacade.getAuthType();
    }

    /**
     * Retrieves the security token associated with the current user session.
     * This might be a session ID or an access token.
     *
     * @returns The security token as a string.
     */
    public static getSecurityToken(): string {
        return UserFacade.getSecurityToken();
    }

    /**
     * Retrieves the number of requests (invocations) made by the current user
     * during the lifecycle of the current session.
     *
     * @returns The total invocation count.
     */
    public static getInvocationCount(): number {
        return UserFacade.getInvocationCount();
    }

    /**
     * Retrieves the preferred language setting (e.g., 'en', 'de', 'es') for the current user.
     *
     * @returns The user's preferred language code.
     */
    public static getLanguage(): string {
        return UserFacade.getLanguage();
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = User;
}