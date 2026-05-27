/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.proxy;

import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.springframework.web.servlet.function.ServerRequest;

final class NativeAppProxyAttributes {

    static final String NATIVE_APP = "dirigible.nativeApp";

    private NativeAppProxyAttributes() {}

    static NativeApp requireNativeApp(ServerRequest request) {
        Object value = request.attribute(NATIVE_APP)
                              .orElse(null);
        if (!(value instanceof NativeApp)) {
            throw new IllegalStateException("Missing required request attribute [" + NATIVE_APP + "].");
        }
        return (NativeApp) value;
    }
}
