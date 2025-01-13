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
/**
 * HTTP API Session
 *
 */

const HttpSessionFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpSessionFacade");

export class Session {

	public static isValid(): boolean {
		return HttpSessionFacade.isValid();
	}

	public static getAttribute(name: string): string {
		return HttpSessionFacade.getAttribute(name);
	}

	public static getAttributeNames(): string[] {
		const attrNames = HttpSessionFacade.getAttributeNamesJson();
		return attrNames ? JSON.parse(attrNames) : [];
	}

	public static getCreationTime(): Date {
		return new Date(HttpSessionFacade.getCreationTime());
	}

	public static getId(): string {
		return HttpSessionFacade.getId();
	}

	public static getLastAccessedTime(): Date {
		return new Date(HttpSessionFacade.getLastAccessedTime());
	}

	public static getMaxInactiveInterval(): number {
		return HttpSessionFacade.getMaxInactiveInterval();
	}

	public static invalidate(): void {
		HttpSessionFacade.invalidate();
	}

	public static isNew(): boolean {
		return HttpSessionFacade.isNew();
	}

	public static setAttribute(name: string, value: any): void {
		HttpSessionFacade.setAttribute(name, value);
	}

	public static removeAttribute(name: string): void {
		HttpSessionFacade.removeAttribute(name);
	}

	public static setMaxInactiveInterval(interval: number): void {
		HttpSessionFacade.setMaxInactiveInterval(interval);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Session;
}
