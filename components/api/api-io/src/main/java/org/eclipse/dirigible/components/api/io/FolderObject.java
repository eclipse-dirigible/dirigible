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

import java.util.ArrayList;
import java.util.List;

/**
 * The Class FolderObject.
 */
public class FolderObject extends FileObject {

    /**
     * Instantiates a new folder object.
     *
     * @param name the name
     * @param path the path
     * @param type the type
     */
    public FolderObject(String name, String path, String type) {
        super(name, path, type);
    }

    /** The files. */
    private List<FileObject> files = new ArrayList<>();

    /** The folders. */
    private List<FolderObject> folders = new ArrayList<>();

    /**
     * Gets the files.
     *
     * @return the files
     */
    public List<FileObject> getFiles() {
        return files;
    }

    /**
     * Gets the folders.
     *
     * @return the folders
     */
    public List<FolderObject> getFolders() {
        return folders;
    }

}
