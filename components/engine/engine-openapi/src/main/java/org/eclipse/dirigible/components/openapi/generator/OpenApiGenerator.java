/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.openapi.generator;

import java.util.Map;

/**
 * Parses TypeScript Controller source files and generates an OpenAPI (Swagger) specification JSON.
 */
public class OpenApiGenerator {

    /**
     * Entry point for generating the OpenAPI specification.
     *
     * @param location The actual source location
     * @param source The TypeScript source code string of the controller.
     * @return The OpenAPI specification as a formatted JSON string.
     */
    public static String generate(String location, String source) {
        Map<String, Object> controllerMetadata = OpenApiParser.parse(location, source);
        return OpenApiSerializer.serialize(controllerMetadata);
    }

}
