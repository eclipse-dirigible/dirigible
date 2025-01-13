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

const EnginesFacade = Java.type("org.eclipse.dirigible.components.api.platform.EnginesFacade");
const HashMap = Java.type("java.util.HashMap");

/**
 * Engine
 */
export class Engine {
	private type: string;

	constructor(type: string) {
		this.type = type;
	}

	public static getTypes(): string[] {
		return JSON.parse(EnginesFacade.getEngineTypes());
	}

	public execute(projectName: string, projectFilePath: string, projectFilePathParam: string, parameters: { [key: string]: any }, debug: boolean = false): any {
		const mapInstance = new HashMap();
		for (const property in parameters) {
			if (context.hasOwnProperty(property)) {
				mapInstance.put(property, context[property]);
			}
		}
		return EnginesFacade.execute(this.type, projectName, projectFilePath, projectFilePathParam, mapInstance, debug);
	}

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Engine;
}
