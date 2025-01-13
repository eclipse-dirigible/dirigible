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

/**
 * The Interface CmisFolder.
 */
public interface CmisFolder {
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
}
