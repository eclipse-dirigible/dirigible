/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.controller;

import java.lang.reflect.Type;

/**
 * Describes how a single controller-method parameter should be supplied at invocation time.
 *
 * @param kind binding category (body, path placeholder, query parameter, or context object)
 * @param name placeholder/query-parameter name; {@code null} for {@link Kind#BODY},
 *        {@link Kind#CTX_REQUEST}, {@link Kind#CTX_RESPONSE} and {@link Kind#CTX_PARAMS}
 * @param targetType the parameter's declared {@code Class}, used for type coercion and Jackson
 *        deserialization
 * @param genericType the parameter's declared generic {@code Type}, needed for Jackson when the
 *        target is a generic container (e.g. {@code List<Country>})
 */
public record ParamBinding(Kind kind, String name, Class<?> targetType, Type genericType) {

    /** Binding category for a controller-method parameter. */
    public enum Kind {
        /** JSON request body deserialized into {@link #targetType()} via Jackson. */
        BODY,
        /** Value of a path placeholder declared in the route URL. */
        PATH,
        /** Value of an HTTP query-string parameter. */
        QUERY,
        /** Context object resolved by type ({@code HttpServletRequest}, {@code HttpServletResponse}). */
        CTX_REQUEST,
        /** Context object resolved by type ({@code HttpServletResponse}). */
        CTX_RESPONSE,
        /** Merged path + query parameters as a {@code Map<String, String>}. */
        CTX_PARAMS
    }
}
