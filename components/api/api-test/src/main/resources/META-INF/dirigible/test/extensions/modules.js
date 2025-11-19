/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
exports.getContent = function() {
	return [
		{
			"name": "@aerokit/sdk/test",
			"description": "Dirigible Test Module",
			"isPackageDescription": true,
			"dtsPath": "test/extensions/test.d.ts"
		},
		{
			"name": "test/assert",
			"description": "Assert API",
			"api": "assert",
			"versionedPaths": [
				"test/assert"
			],
			"pathDefault": "test/assert"
		},
		{
			"name": "test/runner",
			"description": "Test Runner API",
			"api": "runner",
			"versionedPaths": [
				"test/runner"
			],
			"pathDefault": "test/runner"
		}
	];
};
