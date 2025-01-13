/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.javascript.polyfills;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The Interface JavascriptPolyfill.
 */
public interface JavascriptPolyfill {

    /**
     * Gets the source.
     *
     * @return the source
     */
    String getSource();

    /**
     * Gets the file name.
     *
     * @return the file name
     */
    String getFileName();

    /**
     * Gets the polyfill from resources.
     *
     * @param polyfillPathInResources the polyfill path in resources
     * @return the polyfill from resources
     */
    default String getPolyfillFromResources(String polyfillPathInResources) {
        try {
            InputStream polyfillCodeStream = JavascriptPolyfill.class.getResourceAsStream(polyfillPathInResources);
            if (polyfillCodeStream == null) {
                throw new RuntimeException("Polyfill '" + polyfillPathInResources + "' not found in resources!");
            }
            return new String(polyfillCodeStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
