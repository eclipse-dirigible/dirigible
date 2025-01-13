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
 * The Class GitProjectLocalBranches.
 */
public class GitProjectLocalBranches {

    /** The local. */
    private List<GitBranch> local;

    /**
     * Getter for the local branches.
     *
     * @return the local
     */
    public List<GitBranch> getLocal() {
        return local;
    }

    /**
     * Setter for the local branches.
     *
     * @param local the local to set
     */
    public void setLocal(List<GitBranch> local) {
        this.local = local;
    }

}
