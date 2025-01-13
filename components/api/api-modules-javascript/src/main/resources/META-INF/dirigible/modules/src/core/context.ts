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
 * API Context
 */

const ContextFacade = Java.type("org.eclipse.dirigible.components.api.core.ContextFacade");

export class Context {

	public static get(name: string): any | undefined {
		const value = ContextFacade.get(name)
		return value ?? undefined;
	}

	public static set(name: string, value: any): void {
		ContextFacade.set(name, value);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Context;
}