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

import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.components.database.Parameterized;

import com.google.gson.JsonElement;

abstract class BaseParamSetter implements ParamSetter {

    protected void throwWrongValue(JsonElement sourceParam, String paramName, NamedParameterStatement preparedStatement)
            throws IllegalArgumentException {
        throw new IllegalArgumentException(
                "Wrong value [" + sourceParam + "] for parameter with name [" + paramName + "] for statement: " + preparedStatement);
    }

    protected void throwWrongValue(JsonElement sourceParam, int paramIndex, Parameterized preparedStatement)
            throws IllegalArgumentException {
        throw new IllegalArgumentException(
                "Wrong value [" + sourceParam + "] at index [" + paramIndex + "] for statement: " + preparedStatement);
    }

}


