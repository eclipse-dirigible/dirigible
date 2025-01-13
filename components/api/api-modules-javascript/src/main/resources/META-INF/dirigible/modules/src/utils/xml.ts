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

const Xml2JsonFacade = Java.type("org.eclipse.dirigible.components.api.utils.Xml2JsonFacade");

export class XML {

	public static fromJson(input: string | any): string {
		let data = input;
		if (typeof data !== "string") {
			data = JSON.stringify(input);
		}
		return Xml2JsonFacade.fromJson(data);
	}

	public static toJson(input: string | any) {
		let data = input;
		if (typeof data !== "string") {
			data = JSON.stringify(input);
		}
		return Xml2JsonFacade.toJson(data);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = XML;
}
