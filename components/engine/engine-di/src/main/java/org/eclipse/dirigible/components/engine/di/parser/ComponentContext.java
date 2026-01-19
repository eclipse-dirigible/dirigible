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

public class ComponentContext {

    private final String contextId;

    private final Map<String, ComponentFileMetadata> metadata = new ConcurrentHashMap<>();

    public ComponentContext(String contextId) {
        this.contextId = contextId;
    }

    public String getContextId() {
        return contextId;
    }

    public void unregisterComponent(String name) {
        metadata.remove(name);
    }

    public void clear() {
        metadata.clear();
    }

    public void registerComponentFileMetadata(String name, String location, String projectName, String filePath, String contextId) {
        metadata.put(name, new ComponentFileMetadata(name, location, projectName, filePath, contextId));
    }

    public ComponentFileMetadata getComponentFileMetadata(String name) {
        return metadata.get(name);
    }
}
