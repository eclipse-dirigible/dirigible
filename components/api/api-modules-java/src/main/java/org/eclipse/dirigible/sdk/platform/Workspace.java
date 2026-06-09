/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.platform;

import org.eclipse.dirigible.components.api.platform.WorkspaceFacade;
import org.eclipse.dirigible.components.ide.workspace.domain.File;

/**
 * IDE workspace operations — list, create, and delete the per-user workspaces that the IDE Projects
 * perspective shows, and read or replace the content of files inside them. The returned
 * {@link org.eclipse.dirigible.components.ide.workspace.domain.Workspace} and {@link File} domain
 * types are the same objects the IDE renders, so the workspace tree stays consistent across UI and
 * programmatic edits.
 * <p>
 * Use this from build / migration scripts and from IDE extensions that need to prepare or fix up a
 * user's workspace. For published artefacts use {@link Registry} (read-only) or {@link Repository}
 * (mutable); for filesystem paths outside the registry use
 * {@link org.eclipse.dirigible.sdk.io.Files Files}.
 */
public final class Workspace {

    private Workspace() {}

    public static org.eclipse.dirigible.components.ide.workspace.domain.Workspace createWorkspace(String name) {
        return WorkspaceFacade.createWorkspace(name);
    }

    public static org.eclipse.dirigible.components.ide.workspace.domain.Workspace getWorkspace(String name) {
        return WorkspaceFacade.getWorkspace(name);
    }

    public static String listWorkspaces() {
        return WorkspaceFacade.getWorkspacesNames();
    }

    public static void deleteWorkspace(String name) {
        WorkspaceFacade.deleteWorkspace(name);
    }

    public static byte[] getContent(File file) {
        return WorkspaceFacade.getContent(file);
    }

    public static void setContent(File file, String input) {
        WorkspaceFacade.setContent(file, input);
    }

    public static void setContent(File file, byte[] input) {
        WorkspaceFacade.setContent(file, input);
    }
}
