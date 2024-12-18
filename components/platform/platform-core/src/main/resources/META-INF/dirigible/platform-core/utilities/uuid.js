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
function UUIDGenerate() {
    function _p8(s) {
        const p = (Math.random().toString(16) + '000000000').substring(2, 10);
        return s ? `-${p.substring(0, 4)}-${p.substring(4, 8)}` : p;
    }
    return _p8() + _p8(true) + _p8(true) + _p8();
}