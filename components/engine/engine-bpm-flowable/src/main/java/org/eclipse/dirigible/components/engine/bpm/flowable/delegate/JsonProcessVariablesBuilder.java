/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JsonProcessVariablesBuilder {

    private final Map<String, Object> variables;

    public JsonProcessVariablesBuilder() {
        this.variables = new HashMap<>();
    }

    public Map<String, Object> build() {
        return variables;
    }

    public JsonProcessVariablesBuilder addVariable(String variableName, Object value) {
        Object serializedValue = VariableValueSerializer.serializeValue(value);
        variables.put(variableName, serializedValue);
        return this;
    }
}
