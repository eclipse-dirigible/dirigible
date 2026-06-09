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

import org.eclipse.dirigible.components.api.platform.LifecycleFacade;

/**
 * Promotes a project from a user workspace into the public registry (and removes it again). Used by
 * build pipelines, sample loaders, and admin tools that need to push prepared content into the live
 * runtime without going through the IDE UI.
 * <p>
 * Publishing triggers the same synchronizer chain that hot-reload uses — any artefacts the project
 * declares ({@code .bpmn}, {@code .listener}, {@code .csvim}, {@code .access}, etc.) come into
 * effect immediately after the call returns.
 */
public final class Lifecycle {

    private Lifecycle() {}

    public static boolean publish(String user, String workspace, String project) {
        return LifecycleFacade.publish(user, workspace, project);
    }

    public static boolean unpublish(String project) {
        return LifecycleFacade.unpublish(project);
    }
}
