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

import java.util.HashMap;
import java.util.Map;

public class ComponentMetadata {
    private String componentName;
    private String className;
    private Map<String, String> propertyTypes = new HashMap<>();
    private String key;

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void addPropertyType(String property, String type) {
        propertyTypes.put(property, type);
    }

    public Map<String, String> getPropertyTypes() {
        return propertyTypes;
    }

    // Generate the key dynamically
    public String getKey() {
        if (key == null) {
            key = className;
        }
        return key;
    }
}


