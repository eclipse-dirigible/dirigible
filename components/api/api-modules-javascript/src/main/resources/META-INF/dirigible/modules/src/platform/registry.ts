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
import { Bytes } from "sdk/io/bytes";
const RegistryFacade = Java.type("org.eclipse.dirigible.components.api.platform.RegistryFacade");

export class Registry {

	public static getContent(path: string): any[] {
		return Bytes.toJavaScriptBytes(RegistryFacade.getContent(path));
	}

	public static getContentNative(path: string): any[] {
		return RegistryFacade.getContent(path);
	}

	public static getText(path: string): string {
		return RegistryFacade.getText(path);
	}

	public static find(path: string, pattern: string): string[] {
		return JSON.parse(RegistryFacade.find(path, pattern));
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Registry;
}
