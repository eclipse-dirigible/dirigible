/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

export class Assert {

    static assertTrue(condition, message) {
        if (!condition) {
            throw new Error(message || "Assertion 'assertTrue' failed");
        }
    }

    static assertFalse(condition, message) {
        if (condition) {
            throw new Error(message || "Assertion 'assertFalse' failed");
        }
    }

    static assertNull(object, message) {
        if (object !== undefined && object !== null) {
            throw new Error(message || "Assertion 'assertNull' failed");
        }
    }

    static assertNotNull(object, message) {
        if (object === undefined || object === null) {
            throw new Error(message || "Assertion 'assertNotNull' failed");
        }
    }

    static assertEquals(actual, expected, message) {
        if (expected !== actual) {
            throw new Error(message || "Assertion 'assertEquals' failed - expected: '" + expected + "', but found: '" + actual + "'");
        }
    }
}

if (typeof module !== 'undefined') {
    module.exports = Assert;
}
