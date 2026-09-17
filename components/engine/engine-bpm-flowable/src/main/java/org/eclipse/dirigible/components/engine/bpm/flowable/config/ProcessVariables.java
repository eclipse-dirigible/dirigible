/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

/**
 * Reads a JSON document of process variables, keeping a whole number whole (issue #7373).
 * <p>
 * The default Gson deserialization of an untyped JSON number is a {@code Double}, so a record's
 * primary key handed to {@code Process.start} as {@code 33} landed in Flowable's variable store as
 * {@code 33.0} - and every reader that renders such a variable as text says so: an Inbox row's
 * subject locator read {@code "33.0"}, which the generated controller's integer path parameter then
 * refused, so the card could not load the very record the task is about. A JSON number with no
 * fractional part is an integer, so it is read as a {@code Long}; a genuine decimal stays a
 * {@code Double}.
 */
final class ProcessVariables {

    /** Variables are untyped, so the number strategy is what decides Long versus Double. */
    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                                                      .create();

    private static final TypeToken<Map<String, Object>> VARIABLES = new TypeToken<>() {};

    private ProcessVariables() {}

    /**
     * Parses a JSON document whose top-level fields are process variables.
     *
     * @param json the document, may be null
     * @return the variables, or null when there is no document
     */
    static Map<String, Object> fromJson(String json) {
        return GSON.fromJson(json, VARIABLES);
    }
}
