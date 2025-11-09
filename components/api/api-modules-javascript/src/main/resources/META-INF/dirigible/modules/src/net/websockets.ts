/**
 * Provides a high-level API for managing WebSocket clients and handling
 * lifecycle events within the application context. It wraps the internal Java
 * WebsocketsFacade.
 */
const WebsocketsFacade = Java.type("org.eclipse.dirigible.components.api.websockets.WebsocketsFacade");

/**
 * @class Websockets
 * @description Static utility class for accessing and managing WebSocket functionality.
 */
export class Websockets {

	/**
	 * Creates a new WebSocket client connection to a specified URI, managed by a handler script.
	 *
	 * @param uri The target WebSocket URI (e.g., 'ws://example.com/socket').
	 * @param handler The identifier or path of the script handling the WebSocket events.
	 * @returns A wrapper object for the new WebSocket session.
	 */
	public static createWebsocket(uri: string, handler: string): WebsocketClient {
		const session = WebsocketsFacade.createWebsocket(uri, handler);
		return new WebsocketClient(session, uri, handler);
	}

	/**
	 * Retrieves a list of all active WebSocket clients.
	 *
	 * @returns An array of objects detailing the URI and handler of each client.
	 */
	public static getClients(): { uri: string, handler: string }[] {
		return JSON.parse(WebsocketsFacade.getClientsAsJson());
	}

	/**
	 * Retrieves a specific WebSocket client wrapper by its session ID.
	 *
	 * @param id The session ID of the client.
	 * @returns The client wrapper or undefined if not found.
	 */
	public static getClient(id: string): WebsocketClient | undefined {
		const native = WebsocketsFacade.getClient(id);
		return native ? new WebsocketClient(native.getSession(), native.getSession().getRequestURI(), native.getHandler()) : undefined;
	}

	/**
	 * Retrieves a specific WebSocket client wrapper by its handler identifier.
	 *
	 * @param handler The handler identifier associated with the client.
	 * @returns The client wrapper or undefined if not found.
	 */
	public static getClientByHandler(handler: string): WebsocketClient | undefined {
		const native = WebsocketsFacade.getClientByHandler(handler);
		return native ? new WebsocketClient(native.getSession(), native.getSession().getRequestURI(), native.getHandler()) : undefined;
	}

	/**
	 * Retrieves the message payload from the current context, typically used inside an 'onmessage' handler.
	 *
	 * @returns The message content.
	 */
	public static getMessage(): any {
		return __context.get('message');
	}

	/**
	 * Retrieves error details from the current context, typically used inside an 'onerror' handler.
	 *
	 * @returns The error object or string.
	 */
	public static getError(): any {
		return __context.get('error');
	}

	/**
	 * Retrieves the event method name that triggered the current script execution (e.g., "onopen", "onmessage").
	 *
	 * @returns The name of the event method.
	 */
	public static getMethod(): string {
		// Assumes __context is a global map provided by the execution environment
		return __context.get('method');
	}

	/**
	 * Checks if the current event context is 'onopen'.
	 * @returns True if the method is 'onopen'.
	 */
	public static isOnOpen(): boolean {
		return this.getMethod() === "onopen";
	}

	/**
	 * Checks if the current event context is 'onmessage'.
	 * @returns True if the method is 'onmessage'.
	 */
	public static isOnMessage(): boolean {
		return this.getMethod() === "onmessage";
	}

	/**
	 * Checks if the current event context is 'onerror'.
	 * @returns True if the method is 'onerror'.
	 */
	public static isOnError(): boolean {
		return this.getMethod() === "onerror";
	}

	/**
	 * Checks if the current event context is 'onclose'.
	 * @returns True if the method is 'onclose'.
	 */
	public static isOnClose(): boolean {
		return this.getMethod() === "onclose";
	}
}

/**
 * @class WebsocketClient
 * @description Wrapper for a native WebSocket session, providing methods to send and close the connection.
 */
class WebsocketClient {
	// Renamed to _session for better encapsulation of the native Java object
	private readonly _session: null | any;
	private readonly uri: string;
	private readonly handler: string;

	/**
	 * @param session The native Java session object.
	 * @param uri The connected URI.
	 * @param handler The handler identifier.
	 */
	constructor(session: null | any, uri: string, handler: string) {
		this._session = session;
		this.uri = uri;
		this.handler = handler;
	}

	/**
	 * Sends a text message over the WebSocket connection.
	 * @param text The message to send.
	 */
	public send(text: string): void {
		if (!this._session || this._session === null) {
			console.error("Websocket Session is null. Message not sent.");
			return;
		}
		this._session.send(this.uri, text);
	};

	/**
	 * Closes the WebSocket connection.
	 */
	public close(): void {
		if (this._session) {
			this._session.close();
		}
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Websockets;
}