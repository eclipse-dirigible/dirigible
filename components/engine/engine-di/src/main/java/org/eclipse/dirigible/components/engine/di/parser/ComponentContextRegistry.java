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

public class ComponentContextRegistry {

    private static final Map<String, ComponentContext> CONTEXTS = new ConcurrentHashMap<>();

    public static ComponentContext getContext(String contextId) {
        return CONTEXTS.computeIfAbsent(contextId, ComponentContext::new);
    }

    public static void removeContext(String contextId) {
        ComponentContext context = CONTEXTS.remove(contextId);
        if (context != null) {
            context.clear();
        }
    }

    public static void clearAll() {
        CONTEXTS.values()
                .forEach(ComponentContext::clear);
        CONTEXTS.clear();
    }
}
