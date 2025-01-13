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

import com.google.gson.annotations.Expose;

/**
 * The Class ODataHandler.
 */
public class ODataHandler {

    /** The method. */
    @Expose
    private String method;

    /** The type. */
    @Expose
    private String type;

    /** The handler. */
    @Expose
    private String handler;

    /**
     * Gets the method.
     *
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the method.
     *
     * @param method the new method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the handler.
     *
     * @return the handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     * Sets the handler.
     *
     * @param handler the new handler
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "ODataHandler [method=" + method + ", type=" + type + ", handler=" + handler + "]";
    }

}
