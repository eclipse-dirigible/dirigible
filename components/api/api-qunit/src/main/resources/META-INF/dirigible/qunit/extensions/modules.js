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
exports.getContent = function () {
    return [
        {
            "name": "@aerokit/sdk/qunit",
            "description": "Dirigible QUnit module",
            "isPackageDescription": true,
            "dtsPath": "qunit/extensions/qunit.d.ts"
        },
        {
            "name": "qunit/qunit",
            "description": "QUnit API",
            "api": "qunit",
            "versionedPaths": [
                "qunit/qunit"
            ],
            "pathDefault": "qunit/qunit"
        },
        {
            "name": "qunit/runner",
            "description": "Qunit Runner API",
            "api": "runner",
            "versionedPaths": [
                "qunit/runner"
            ],
            "pathDefault": "qunit/runner"
        },
    ];
};
