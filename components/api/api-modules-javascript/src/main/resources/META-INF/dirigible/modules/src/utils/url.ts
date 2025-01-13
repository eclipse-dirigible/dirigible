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

const UrlFacade = Java.type("org.eclipse.dirigible.components.api.utils.UrlFacade");

export class URL {

	public static encode(input: string, charset?: string): string {
		return UrlFacade.encode(input, charset);
	}

	public static decode(input: string, charset?: string): string {
		return UrlFacade.decode(input, charset);
	}

	public static escape(input: string): string {
		return UrlFacade.escape(input);
	}

	public static escapePath(input: string): string {
		return UrlFacade.escapePath(input);
	}

	public static escapeForm(input: string): string {
		return UrlFacade.escapeForm(input);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = URL;
}
