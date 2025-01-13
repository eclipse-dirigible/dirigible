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
 * API Configurations
 */

const Configuration = Java.type("org.eclipse.dirigible.commons.config.Configuration");

export class Configurations {

	public static get(key: string, defaultValue?: string): string | undefined {
		const value = Configuration.get(key, defaultValue);
		return value ?? undefined;
	}

	public static set(key: string, value: string): void {
		Configuration.set(key, value);
	}

	public static remove(key: string): void {
		Configuration.remove(key);
	}

	public static getKeys(): string[] {
		return Configuration.getKeys();
	}

	public static load(path: string): void {
		Configuration.load(path);
	}

	public static update(): void {
		Configuration.update();
	}

	public static getOS(): string {
		return Configuration.getOS();
	}

	public static isOSWindows(): boolean {
		return Configuration.isOSWindows();
	}

	public static isOSMac(): boolean {
		return Configuration.isOSMac();
	}

	public static isOSUNIX(): boolean {
		return Configuration.isOSUNIX();
	}

	public static isOSSolaris(): boolean {
		return Configuration.isOSSolaris();
	}

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Configurations;
}