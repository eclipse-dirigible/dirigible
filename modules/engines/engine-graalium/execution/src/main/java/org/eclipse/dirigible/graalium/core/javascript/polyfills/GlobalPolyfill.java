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

/**
 * The Class GlobalPolyfill.
 */
public class GlobalPolyfill implements JavascriptPolyfill {

    /** The Constant POLYFILL_PATH_IN_RESOURCES. */
    private static final String POLYFILL_PATH_IN_RESOURCES = "/polyfills/global.js";

    /**
     * Gets the source.
     *
     * @return the source
     */
    @Override
    public String getSource() {
        return getPolyfillFromResources(POLYFILL_PATH_IN_RESOURCES);
    }

    /**
     * Gets the file name.
     *
     * @return the file name
     */
    @Override
    public String getFileName() {
        return POLYFILL_PATH_IN_RESOURCES;
    }
}
