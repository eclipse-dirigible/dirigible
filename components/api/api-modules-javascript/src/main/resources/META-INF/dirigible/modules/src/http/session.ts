/**
 * Provides a static façade (`Session` class) for accessing and manipulating
 * the HTTP session associated with the current request. This module is often used
 * to store user-specific data during their interaction with the application.
 */

const HttpSessionFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpSessionFacade");

/**
 * The static Session class provides methods to interact with the current user session
 * (e.g., storing attributes, checking status, managing lifetime).
 */
export class Session {

	/**
	 * Checks if a session is currently valid and active for the request context.
	 * @returns True if the session is valid, false otherwise (e.g., if it has been invalidated or timed out).
	 */
	public static isValid(): boolean {
		return HttpSessionFacade.isValid();
	}

	/**
	 * Retrieves the value of a named attribute stored in the session.
	 * Note: The underlying Java facade typically stores strings, but the value may represent
     * serialized data that should be parsed if complex.
	 * @param name The name of the attribute.
	 * @returns The attribute value as a string, or null/undefined if not found.
	 */
	public static getAttribute(name: string): string {
		return HttpSessionFacade.getAttribute(name);
	}

	/**
	 * Retrieves an array of all attribute names currently stored in the session.
	 * The names are retrieved as a JSON string from the facade and then parsed.
	 * @returns An array of attribute names (strings), or an empty array if no attributes are present.
	 */
	public static getAttributeNames(): string[] {
		const attrNames = HttpSessionFacade.getAttributeNamesJson();
		return attrNames ? JSON.parse(attrNames) : [];
	}

	/**
	 * Returns the time at which this session was created, converted to a JavaScript Date object.
	 * @returns A Date object representing the session's creation time.
	 */
	public static getCreationTime(): Date {
		// The facade returns time in milliseconds since the epoch, which Date() accepts.
		return new Date(HttpSessionFacade.getCreationTime());
	}

	/**
	 * Returns the unique identifier assigned to this session.
	 * @returns The session ID string.
	 */
	public static getId(): string {
		return HttpSessionFacade.getId();
	}

	/**
	 * Returns the last time the client accessed this session, converted to a JavaScript Date object.
	 * Access includes requests that retrieve or set session attributes.
	 * @returns A Date object representing the last access time.
	 */
	public static getLastAccessedTime(): Date {
		// The facade returns time in milliseconds since the epoch.
		return new Date(HttpSessionFacade.getLastAccessedTime());
	}

	/**
	 * Returns the maximum time interval, in seconds, that the server should keep this session open
	 * between client requests. After this interval, the session will be invalidated.
	 * @returns The maximum inactive interval in seconds.
	 */
	public static getMaxInactiveInterval(): number {
		return HttpSessionFacade.getMaxInactiveInterval();
	}

	/**
	 * Invalidates this session, unbinding any objects bound to it.
	 * After this call, the session is no longer valid.
	 */
	public static invalidate(): void {
		HttpSessionFacade.invalidate();
	}

	/**
	 * Checks if the client does not yet know about the session, typically meaning
	 * the server has not yet returned the session ID via a cookie or encoded URL.
	 * @returns True if the session is new (not yet used in a response), false otherwise.
	 */
	public static isNew(): boolean {
		return HttpSessionFacade.isNew();
	}

	/**
	 * Binds an object to this session, using the specified name.
	 * This is the primary way to store data in the user's session.
	 * @param name The name to bind the object under.
	 * @param value The value/object to store in the session.
	 */
	public static setAttribute(name: string, value: any): void {
		HttpSessionFacade.setAttribute(name, value);
	}

	/**
	 * Removes the attribute with the given name from the session.
	 * @param name The name of the attribute to remove.
	 */
	public static removeAttribute(name: string): void {
		HttpSessionFacade.removeAttribute(name);
	}

	/**
	 * Specifies the maximum time interval, in seconds, that the server should keep this session open
	 * between client requests before automatically invalidating it.
	 * @param interval The new interval in seconds.
	 */
	public static setMaxInactiveInterval(interval: number): void {
		HttpSessionFacade.setMaxInactiveInterval(interval);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Session;
}