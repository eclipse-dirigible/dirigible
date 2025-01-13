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

const UTF8Facade = Java.type("org.eclipse.dirigible.components.api.utils.UTF8Facade");

export class UTF8 {

	/**
	 * Encode the input (text or byte array) as text
	 */
	public static encode(input: string | any[]): string {
		return UTF8Facade.encode(input);
	}

	/**
	 * Decode the input (text or byte array) as text
	 */
	public static decode(input: string | any[]): string {
		return UTF8Facade.decode(input);
	}

	/**
	 * Decode the input byte array as text
	 */
	public static bytesToString(bytes: any[], offset: number, length: number) {
		return UTF8Facade.bytesToString(bytes, offset, length);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = UTF8;
}
