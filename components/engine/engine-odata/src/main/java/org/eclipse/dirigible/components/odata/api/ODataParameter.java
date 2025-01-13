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
 * The Class ODataParameter.
 */
public class ODataParameter {

    /** The name. */
    @Expose
    private String name;

    /** The nullable. */
    @Expose
    private boolean nullable;

    /** The type. */
    @Expose
    private String type;

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name
     * @return the o data parameter
     */
    public ODataParameter setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Checks if is nullable.
     *
     * @return true, if is nullable
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Sets the nullable.
     *
     * @param nullable the nullable
     * @return the o data parameter
     */
    public ODataParameter setNullable(boolean nullable) {
        this.nullable = nullable;
        return this;
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
     * @param type the type
     * @return the o data parameter
     */
    public ODataParameter setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "ODataParameter [name=" + name + ", nullable=" + nullable + ", type=" + type + "]";
    }

}
