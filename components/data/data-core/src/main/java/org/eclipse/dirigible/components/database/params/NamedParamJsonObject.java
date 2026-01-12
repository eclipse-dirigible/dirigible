/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.params;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class NamedParamJsonObject {

    private final String name;
    private final String type;
    private final JsonElement valueElement;

    NamedParamJsonObject(String name, String type, JsonElement valueElement) {
        this.name = name;
        this.type = type;
        this.valueElement = valueElement;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public JsonElement getValueElement() {
        return valueElement;
    }

    static NamedParamJsonObject fromJsonElement(JsonElement parameterElement) throws IllegalArgumentException {
        JsonObject jsonObject = parameterElement.getAsJsonObject();
        String name = getName(jsonObject);
        String type = getType(jsonObject);

        JsonElement valueElement = jsonObject.get("value");

        return new NamedParamJsonObject(name, type, valueElement);
    }

    private static String getName(JsonObject jsonObject) {
        JsonElement nameElement = jsonObject.get("name");
        if (null == nameElement) {
            throw new IllegalArgumentException("Missing name member in " + jsonObject);
        }
        if (!nameElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Invalid name member in " + jsonObject);
        }
        return nameElement.getAsJsonPrimitive()
                          .getAsString();
    }

    private static String getType(JsonObject jsonObject) {
        JsonElement typeElement = jsonObject.get("type");
        if (!typeElement.isJsonPrimitive() || !typeElement.getAsJsonPrimitive()
                                                          .isString()) {
            throw new IllegalArgumentException("Parameter 'type' must be a string representing the database type name");
        }
        return typeElement.getAsJsonPrimitive()
                          .getAsString();
    }
}
