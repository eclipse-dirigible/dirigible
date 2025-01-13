/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.utils;

import org.eclipse.jgit.api.RemoteSetUrlCommand;
import org.eclipse.jgit.lib.Repository;

/**
 * The Class RemoteUrl.
 */
public class RemoteUrl extends RemoteSetUrlCommand {

    /**
     * Instantiates a new remote url.
     *
     * @param repo the repo
     */
    public RemoteUrl(Repository repo) {
        super(repo);
    }
}
