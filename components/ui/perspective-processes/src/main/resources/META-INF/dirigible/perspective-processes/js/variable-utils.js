/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

function stringifyValue(value) {
    try {
        if (typeof value === 'object') {
            if (Array.isArray(value)) {
                value = value.map(e => JSON.parse(e));
            }
            value = JSON.stringify(value, null, 4);
        } else {
            value = JSON.parse(value);
            value = JSON.stringify(value, null, 4);
        }
    } catch (e) {
        // Not a JSON - do nothing
    }
    return value;
}