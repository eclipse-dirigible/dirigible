/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.domain;

import com.google.gson.annotations.Expose;

import java.util.Collections;
import java.util.List;

public class LifecycleStop {

    @Expose
    private List<Command> commands;

    public List<Command> getCommands() {
        return commands == null ? Collections.emptyList() : commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }
}
