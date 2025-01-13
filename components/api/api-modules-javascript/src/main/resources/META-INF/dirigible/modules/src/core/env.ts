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
 * API Env
 */

const EnvFacade = Java.type("org.eclipse.dirigible.components.api.core.EnvFacade");

export interface EnvValues {
	[key: string]: string;
}

export class Env {

	public static get(name: string): string | undefined {
		const value = EnvFacade.get(name);
		return value ?? undefined;
	}

	public static list(): EnvValues {
		return JSON.parse(EnvFacade.list());
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Env;
}