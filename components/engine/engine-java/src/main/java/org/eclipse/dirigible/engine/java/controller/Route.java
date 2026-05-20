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

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpMethod;

/**
 * A single HTTP route on a controller class: an HTTP method, a compiled path pattern, the reflected
 * {@link Method}, the per-parameter binding plan, the required roles for access, and metadata for
 * OpenAPI generation.
 *
 * @param httpMethod the HTTP verb this route serves
 * @param pathTemplate the original (uncompiled) path suffix declared on the method annotation, e.g.
 *        {@code "/{id}"}; kept for OpenAPI emission
 * @param pathPattern compiled regex with named groups for placeholders, anchored implicitly via
 *        {@code Matcher#matches()}
 * @param placeholders ordered list of placeholder names extracted from the path
 * @param method the reflected target method
 * @param paramBindings binding plan in the same order as the method's declared parameters
 * @param roles roles required to invoke the route (any-of); empty means no restriction
 */
public record Route(HttpMethod httpMethod, String pathTemplate, Pattern pathPattern, List<String> placeholders, Method method,
        List<ParamBinding> paramBindings, List<String> roles) {
}
