/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.domain;

import org.eclipse.dirigible.repository.api.ICollection;

/**
 * The Workspace's Project.
 */
public class Project extends Folder {

    /**
     * Instantiates a new project.
     *
     * @param projectCollection the project collection
     */
    public Project(ICollection projectCollection) {
        super(projectCollection);
    }

}
