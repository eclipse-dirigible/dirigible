/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.domain;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * The Class Constraint.
 */
public class Constraint {

    /**
     * The scope.
     */
    @Expose
    private String scope;

    /**
     * The path.
     */
    @Expose
    private String path;

    /**
     * The method.
     */
    @Expose
    private String method;

    /**
     * The roles.
     */
    @Expose
    private List<String> roles = new ArrayList<String>();

    /**
     * Gets the scope.
     *
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope.
     *
     * @param scope the new scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path.
     *
     * @param path the new path
     */
    public void setPath(String path) {
        this.path = path;
    }

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
     * Gets the roles.
     *
     * @return the roles
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Constraint [scope=" + scope + ", path=" + path + ", method=" + method + ", roles=" + roles + "]";
    }

}
