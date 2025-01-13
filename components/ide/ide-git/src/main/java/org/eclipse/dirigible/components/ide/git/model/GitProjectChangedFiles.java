/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.model;

import java.util.List;

import org.eclipse.dirigible.components.ide.git.domain.GitChangedFile;

/**
 * The Class GitProjectChangedFiles.
 */
public class GitProjectChangedFiles {

    /** The files. */
    private List<GitChangedFile> files;

    /**
     * Getter for the files.
     *
     * @return the files
     */
    public List<GitChangedFile> getFiles() {
        return files;
    }

    /**
     * Setter for the files.
     *
     * @param files the files to set
     */
    public void setFiles(List<GitChangedFile> files) {
        this.files = files;
    }

}
