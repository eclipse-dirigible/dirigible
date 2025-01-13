/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.globals;

import org.eclipse.dirigible.graalium.core.graal.globals.GlobalObject;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class DirigibleContextGlobalObject.
 */
public class DirigibleContextGlobalObject implements GlobalObject {

    /** The dirigible context value. */
    private final Map<Object, Object> dirigibleContextValue;

    /**
     * Instantiates a new dirigible context global object.
     *
     * @param dirigibleContextValue the dirigible context value
     */
    public DirigibleContextGlobalObject(Map<Object, Object> dirigibleContextValue) {
        this.dirigibleContextValue = dirigibleContextValue != null ? dirigibleContextValue : new HashMap<>();
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return "__context";
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    @Override
    public Object getValue() {
        return dirigibleContextValue;
    }
}
