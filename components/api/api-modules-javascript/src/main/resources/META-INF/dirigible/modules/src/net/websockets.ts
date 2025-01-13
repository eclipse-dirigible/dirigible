/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const WebsocketsFacade = Java.type("org.eclipse.dirigible.components.api.websockets.WebsocketsFacade");

export class Websockets {

	public static createWebsocket(uri: string, handler: string): WebsocketClient {
		const session = WebsocketsFacade.createWebsocket(uri, handler);
		return new WebsocketClient(session, uri, handler);
	}

	public static getClients(): { uri: string, handler: string }[] {
		return JSON.parse(WebsocketsFacade.getClientsAsJson());
	}

	public static getClient(id: string): WebsocketClient | undefined {
		const native = WebsocketsFacade.getClient(id);
		return native ? new WebsocketClient(native.getSession(), native.getSession().getRequestURI(), native.getHandler()) : undefined;
	}

	public static getClientByHandler(handler: string): WebsocketClient | undefined {
		const native = WebsocketsFacade.getClientByHandler(handler);
		return native ? new WebsocketClient(native.getSession(), native.getSession().getRequestURI(), native.getHandler()) : undefined;
	}

	public static getMessage(): any {
		return __context.get('message');
	}

	public static getError(): any {
		return __context.get('error');
	}

	public static getMethod(): any {
		return __context.get('method');
	}

	public static isOnOpen(): boolean {
		return this.getMethod() === "onopen";
	}

	public static isOnMessage(): boolean {
		return this.getMethod() === "onmessage";
	}

	public static isOnError(): boolean {
		return this.getMethod() === "onerror";
	}

	public static isOnClose(): boolean {
		return this.getMethod() === "onclose";
	}
}

/**
 * WebsocketClient
 */
class WebsocketClient {
	private session: null | any;
	private uri: string;
	private handler: string;

	constructor(session: null | any, uri: string, handler: string) {
		this.session = session;
		this.uri = uri;
		this.handler = handler;
	}

	public send(text: string): void {
		if (!this.session || this.session === null) {
			console.error("Websocket Session is null");
		}
		this.session.send(this.uri, text);
	};

	public close(): void {
		this.session.close();
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Websockets;
}