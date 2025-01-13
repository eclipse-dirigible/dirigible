/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core;

import java.nio.file.Path;

/**
 * The Interface JavascriptSourceProvider.
 */
public interface JavascriptSourceProvider {

    /**
     * Gets the absolute source path.
     *
     * @param projectName the project name
     * @param projectFileName the project file name
     * @return the absolute source path
     */
    public Path getAbsoluteSourcePath(String projectName, String projectFileName);

    /**
     * Gets the absolute project path.
     *
     * @param projectName the project name
     * @return the absolute project path
     */
    public Path getAbsoluteProjectPath(String projectName);

    /**
     * Gets the source.
     *
     * @param sourceFilePath the project file path
     * @return the source
     */
    public String getSource(String sourceFilePath);

    /**
     * Unpacked to file system.
     *
     * @param pathToUnpack the path to unpack
     * @param pathToLookup the path to lookup
     * @return the path
     */
    public Path unpackedToFileSystem(Path pathToUnpack, Path pathToLookup);

}
