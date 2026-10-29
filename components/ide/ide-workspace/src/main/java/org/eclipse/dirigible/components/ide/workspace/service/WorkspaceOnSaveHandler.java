/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import org.eclipse.dirigible.components.workspace.domain.Project;

/**
 * A platform hook that runs after a file of a workspace project is saved - created or updated
 * through {@link WorkspaceService}. It is the Java side of the {@code ide-workspace-on-save}
 * extension point: every Spring bean implementing it is called for every saved file, before the
 * scripts registered on the extension point, and needs no script engine.
 *
 * <p>
 * A handler decides for itself which files concern it. One that fails is logged and neither fails
 * the save nor keeps the other handlers from running.
 */
public interface WorkspaceOnSaveHandler {

    /**
     * Called after a file was saved.
     *
     * @param project the project the file belongs to
     * @param path the path of the file inside the project, as the save named it
     */
    void onSave(Project project, String path);

}
