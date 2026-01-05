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

class IndexedParamJsonObject {

    private final JsonElement valueElement;

    IndexedParamJsonObject(JsonElement valueElement) {
        this.valueElement = valueElement;
    }

    public JsonElement getValueElement() {
        return valueElement;
    }

    static IndexedParamJsonObject fromJsonElement(JsonElement parameterElement) throws IllegalArgumentException {
        JsonObject jsonObject = parameterElement.getAsJsonObject();

        JsonElement valueElement = jsonObject.get("value");

        return new IndexedParamJsonObject(valueElement);
    }

}
