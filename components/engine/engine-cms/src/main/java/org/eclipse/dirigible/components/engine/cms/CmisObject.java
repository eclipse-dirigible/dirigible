/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

import java.io.IOException;

/**
 * The Interface CmisObject.
 */
public interface CmisObject {

    /**
     * Sanitize.
     *
     * @param path the path
     * @return the string
     */
    String sanitize(String path);

    /**
     * Returns the ID of this CmisObject.
     *
     * @return the Id
     */
    String getId();

    /**
     * Returns the Name of this CmisObject.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the Type of this CmisObject.
     *
     * @return the object type
     */
    ObjectType getType();

    /**
     * Delete this CmisObject.
     *
     * @throws IOException IO Exception
     */
    void delete() throws IOException;

    /**
     * Delete this CmisObject.
     *
     * @param allVersions whether to delete all versions
     * @throws IOException IO Exception
     */
    void delete(boolean allVersions) throws IOException;

    /**
     * Rename this CmisObject.
     *
     * @param newName the new name
     * @throws IOException IO Exception
     */
    void rename(String newName) throws IOException;
}
