/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.polyfills;

import org.eclipse.dirigible.graalium.core.javascript.polyfills.JavascriptPolyfill;

/**
 * The Class RequirePolyfill.
 */
public class RequirePolyfill implements JavascriptPolyfill {

    /** The Constant POLYFILL_PATH_IN_RESOURCES. */
    private static final String POLYFILL_PATH_IN_RESOURCES = "/polyfills/require.js";

    /**
     * Instantiates a new require polyfill.
     */
    public RequirePolyfill() {}

    /**
     * Gets the source.
     *
     * @return the source
     */
    public String getSource() {
        return this.getPolyfillFromResources("/polyfills/require.js");
    }

    /**
     * Gets the file name.
     *
     * @return the file name
     */
    public String getFileName() {
        return "/polyfills/require.js";
    }
}

