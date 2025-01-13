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

import java.io.IOException;

/**
 * The Interface ProjectStatusProvider.
 */
public interface ProjectStatusProvider {

    /**
     * Gets the project status.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project status
     */
    ProjectStatus getProjectStatus(String workspace, String project);

    /**
     * Gets the project git folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project git folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    String getProjectGitFolder(String workspace, String project) throws IOException;

}
