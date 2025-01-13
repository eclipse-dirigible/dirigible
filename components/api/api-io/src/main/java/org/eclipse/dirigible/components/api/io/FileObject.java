/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.io;

/**
 * The Class FileObject.
 */
public class FileObject {

    /** The name. */
    private String name;

    /** The path. */
    private String path;

    /** The type. */
    private String type;

    /**
     * Instantiates a new file object.
     *
     * @param name the name
     * @param path the path
     * @param type the type
     */
    public FileObject(String name, String path, String type) {
        super();
        this.name = name;
        this.path = path;
        this.type = type;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
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
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

}
