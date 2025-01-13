/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.model;

import java.util.List;

import org.eclipse.dirigible.components.ide.git.domain.GitBranch;

/**
 * The Class GitProjectRemoteBranches.
 */
public class GitProjectRemoteBranches {

    /** The remote. */
    private List<GitBranch> remote;

    /**
     * Getter for the remote branches.
     *
     * @return the remote
     */
    public List<GitBranch> getRemote() {
        return remote;
    }

    /**
     * Setter for the remote branches.
     *
     * @param remote the remote to set
     */
    public void setRemote(List<GitBranch> remote) {
        this.remote = remote;
    }

}
