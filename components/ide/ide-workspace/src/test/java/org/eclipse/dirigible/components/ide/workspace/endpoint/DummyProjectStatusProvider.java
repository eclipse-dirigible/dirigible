/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.endpoint;

import java.io.IOException;

import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatus;
import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatusProvider;

/**
 * The Class DummyProjectStatusProvider.
 */
public class DummyProjectStatusProvider implements ProjectStatusProvider {

    /**
     * Gets the project status.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project status
     */
    @Override
    public ProjectStatus getProjectStatus(String workspace, String project) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the project git folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project git folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public String getProjectGitFolder(String workspace, String project) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
