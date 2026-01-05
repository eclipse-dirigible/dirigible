/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.component;

public class ComponentContextHolder {
    private static final ThreadLocal<String> current = new ThreadLocal<>();

    public static void set(String contextId) {
        current.set(contextId);
    }

    public static String get() {
        return current.get() != null ? current.get() : "default";
    }

    public static void clear() {
        current.remove();
    }
}
