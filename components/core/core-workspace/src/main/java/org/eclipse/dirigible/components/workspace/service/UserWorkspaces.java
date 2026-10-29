/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.workspace.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.dirigible.components.workspace.domain.Workspace;

/**
 * The workspaces of the current user - the folders under {@code /users/<user>} of the repository
 * where the IDE keeps the projects a developer edits.
 *
 * <p>
 * The runtime features that reach a workspace - the platform and Git API facades, the BPM modeler's
 * model service, the export of a database to a project - depend on this interface rather than on
 * the IDE, so they start where the IDE is not deployed. The IDE's workspace service provides the
 * implementation; without it, a consumer refuses with a {@link WorkspaceUnavailableException}.
 */
public interface UserWorkspaces {

    /**
     * Gets a workspace of the current user, whether it exists or not.
     *
     * @param name the workspace name
     * @return the workspace
     */
    Workspace getWorkspace(String name);

    /**
     * Creates a workspace of the current user.
     *
     * @param name the workspace name
     * @return the created workspace
     */
    Workspace createWorkspace(String name);

    /**
     * Gets every workspace of the current user.
     *
     * @return the workspaces
     */
    List<Workspace> getWorkspaces();

    /**
     * Deletes a workspace of the current user.
     *
     * @param name the workspace name
     */
    void deleteWorkspace(String name);

    /**
     * Whether a workspace of the current user exists.
     *
     * @param name the workspace name
     * @return {@code true} when it exists
     */
    boolean existsWorkspace(String name);

    /**
     * Gets the URI under which the IDE serves a workspace resource.
     *
     * @param workspace the workspace name
     * @param project the project name, or {@code null}
     * @param path the path inside the project, or {@code null}
     * @return the URI
     * @throws URISyntaxException when the names do not form a valid URI
     */
    URI getURI(String workspace, String project, String path) throws URISyntaxException;
}
