/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.domain;

/**
 * Changed File representation POJO.
 */
public class GitChangedFile {

    /** The path. */
    private String path;

    /** The type. */
    private int type;

    /**
     * The constructor.
     *
     * @param path the full path
     * @param type the change type
     */
    public GitChangedFile(String path, int type) {
        super();
        this.path = path;
        this.type = type;
    }

    /**
     * Getter for path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Setter for path.
     *
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Getter for type.
     *
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * Setter for type.
     *
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }



}
