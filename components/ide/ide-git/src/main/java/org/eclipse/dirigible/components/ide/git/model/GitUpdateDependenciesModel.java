/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.model;


/**
 * The Git Update Dependencies Model.
 */
public class GitUpdateDependenciesModel extends BaseGitProjectModel {

    /** The publish. */
    private boolean publish;

    /**
     * Checks if is publish.
     *
     * @return true, if is publish
     */
    public boolean isPublish() {
        return publish;
    }

    /**
     * Sets the publish.
     *
     * @param publish the new publish
     */
    public void setPublish(boolean publish) {
        this.publish = publish;
    }

}
