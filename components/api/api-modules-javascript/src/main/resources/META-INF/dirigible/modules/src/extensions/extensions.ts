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
 * API Extensions
 *
 */

const ExtensionsFacade = Java.type("org.eclipse.dirigible.components.api.extensions.ExtensionsFacade");

export class Extensions {

	public static getExtensions(extensionPoint: string): string[] {
		const extensions = ExtensionsFacade.getExtensions(extensionPoint);
		return JSON.parse(JSON.stringify(extensions));
	}

	public static getExtensionPoints(): string[] {
		const extensionPoints = ExtensionsFacade.getExtensionPoints();
		return JSON.parse(JSON.stringify(extensionPoints));
	}

	public static async loadExtensionModules(extensionPoint: string, requiredFunctions: string[] = [], throwError = false): Promise<any[]> {
		const extensionModules = [];
		const extensions = this.getExtensions(extensionPoint);

		for (let i = 0; i < extensions?.length; i++) {
			const module = extensions[i];
			try {
				let extensionModule;
				try {
					// Fallback to require()
					extensionModule = dirigibleRequire(module);
				} catch (e) {
					extensionModule = await import(`../../../../${module}`);
				}

				if (!extensionModule || Object.keys(extensionModule).length === 0) {
					const errorMessage = `Extension '${module}' for extension point '${extensionPoint}' doesn't provide any function(s) or was not properly loaded, consider publishing it.`;
					this.logError(throwError, errorMessage);
					continue;
				}

				let requiredFunctionsFound = true;
				requiredFunctions.forEach(f => {
					requiredFunctionsFound &&= typeof extensionModule[f] === "function";
				});

				if (!requiredFunctionsFound) {
					const errorMessage = `Extension '${module}' for extension point '${extensionPoint}', doesn't provide the following required function(s): [\n\t${requiredFunctions.join("(),\n\t")}()\n]`;
					this.logError(throwError, errorMessage);
					continue;
				}

				extensionModules.push(extensionModule);
			} catch (e) {
				const errorMessage = `Error occurred while importing extension '${module}' for extension point '${extensionPoint}'.`;
				this.logError(throwError, errorMessage, e);
			}
		}

		return extensionModules;
	}

	private static logError(throwError: boolean, ...errorData: any[]): void {
		console.error(errorData);
		if (throwError) {
			throw new Error(errorData[0]);
		}
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Extensions;
}
