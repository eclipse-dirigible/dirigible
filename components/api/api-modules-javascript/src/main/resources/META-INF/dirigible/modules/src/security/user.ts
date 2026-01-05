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