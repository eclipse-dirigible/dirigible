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
 * API Files
 */
const SystemUtils = Java.type("org.apache.commons.lang3.SystemUtils")

export class OS {

	public static readonly OS_NAME: string = SystemUtils.OS_NAME;

	public static isWindows(): boolean {
		return SystemUtils.IS_OS_WINDOWS;
	}

	public static isUnix(): boolean {
		return SystemUtils.IS_OS_UNIX;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = OS;
}
