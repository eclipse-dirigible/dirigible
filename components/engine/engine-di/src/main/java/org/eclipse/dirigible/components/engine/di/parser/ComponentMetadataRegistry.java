/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.di.parser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.Value;

public class ComponentMetadataRegistry {

    private static final Map<String, Value> injectionsByComponentName = new ConcurrentHashMap<>();

    public static void register(String name, Value injectionsMap) {
        injectionsByComponentName.put(name, injectionsMap);
    }

    public static Value getInjections(String name) {
        return injectionsByComponentName.get(name);
    }

    public static void clear() {
        injectionsByComponentName.clear();
    }
}
