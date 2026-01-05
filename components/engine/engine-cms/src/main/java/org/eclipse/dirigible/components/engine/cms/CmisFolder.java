/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

import org.apache.chemistry.opencmis.commons.enums.VersioningState;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The Interface CmisFolder.
 */
public interface CmisFolder extends CmisObject {
    /**
     * Returns true if this CmisInternalFolder is a root folder and false otherwise.
     *
     * @return whether it is a root folder
     */
    boolean isRootFolder();

    /**
     * Returns the Path of this CmisInternalFolder.
     *
     * @return the path
     */
    String getPath();

    /**
     * Creates a new document under this folder.
     *
     * @param properties the properties
     * @param contentStream the content stream
     * @param versioningState the version state
     * @return CmisDocument
     * @throws IOException IO Exception
     */
    CmisDocument createDocument(Map<String, String> properties, CmisContentStream contentStream, VersioningState versioningState)
            throws IOException;

    /**
     * Creates a new document under this folder.
     *
     * @param properties the properties
     * @param contentStream the content stream
     * @return CmisDocument
     * @throws IOException IO Exception
     */
    CmisDocument createDocument(Map<String, String> properties, CmisContentStream contentStream) throws IOException;

    /**
     * Creates a new folder under this folder.
     *
     * @param properties the properties
     * @return CmisFolder throws IOException
     * @throws IOException IO Exception
     */
    CmisFolder createFolder(Map<String, String> properties) throws IOException;

    /**
     * Gets the children.
     *
     * @return the children
     * @throws IOException Signals that an I/O exception has occurred.
     */
    List<? extends CmisObject> getChildren() throws IOException;

    /**
     * Returns the parent CmisFolder of this folder.
     *
     * @return CmisFolder
     * @throws IOException IO Exception
     */
    CmisFolder getFolderParent() throws IOException;
}
