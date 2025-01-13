/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.api;

/**
 * The Enum ODataHandlerMethods.
 */
public enum ODataHandlerMethods {

    /** The create. */
    create,
    /** The update. */
    update,
    /** The delete. */
    delete;

    /**
     * Value.
     *
     * @return the string
     */
    public String value() {
        return name();
    }

    /**
     * From value.
     *
     * @param v the v
     * @return the o data handler methods
     */
    public static ODataHandlerMethods fromValue(String v) {
        return valueOf(v);
    }
}
