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

/**
 * Publishes a project from a user's workspace to the public registry, where the synchronizers
 * reconcile it into the running system, and removes it from there.
 *
 * <p>
 * The platform API's lifecycle facade depends on this interface rather than on the IDE, so it
 * starts where the IDE is not deployed. The IDE's publisher service provides the implementation;
 * without it, a consumer refuses with a {@link WorkspaceUnavailableException}.
 */
public interface ProjectPublisher {

    /**
     * Publishes a project, or a path inside it, from a workspace of the given user.
     *
     * @param user the user owning the workspace
     * @param workspace the workspace name
     * @param project the project name, {@code *} for every project of the workspace
     * @param path the path inside the project, empty for the whole project
     */
    void publish(String user, String workspace, String project, String path);

    /**
     * Removes a project, or a path inside it, from the public registry.
     *
     * @param path the registry path, its project first
     */
    void unpublish(String path);
}
