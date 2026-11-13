/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.params;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import org.eclipse.dirigible.components.database.ParameterizedByIndex;
import org.eclipse.dirigible.components.database.ParameterizedByName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonPrimitive;

/**
 * A JSON boolean bound by name was refused as a "wrong value" while the same value bound by index
 * was accepted, so every report filter on a boolean column (sent as {@code {"value": true}} and
 * bound as a named parameter) failed with a 500 (#7390).
 */
class BooleanParamSetterTest {

    private final BooleanParamSetter setter = new BooleanParamSetter();

    @Test
    void binds_a_json_boolean_by_name() throws SQLException {
        ParameterizedByName statement = mock(ParameterizedByName.class);

        setter.setParam(new JsonPrimitive(true), "paid", statement);
        setter.setParam(new JsonPrimitive(false), "open", statement);

        verify(statement).setBoolean("paid", true);
        verify(statement).setBoolean("open", false);
    }

    @Test
    void binds_a_json_boolean_by_index() throws SQLException {
        ParameterizedByIndex statement = mock(ParameterizedByIndex.class);

        setter.setParam(new JsonPrimitive(true), 1, statement);

        verify(statement).setBoolean(1, true);
    }

    @Test
    void binds_a_boolean_string_by_name() throws SQLException {
        ParameterizedByName statement = mock(ParameterizedByName.class);

        setter.setParam(new JsonPrimitive("true"), "paid", statement);

        verify(statement).setBoolean("paid", true);
    }
}
