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

public class LifecycleStart {

    @Expose
    private StartMode mode;

    @Expose
    private List<Command> commands;

    public StartMode getMode() {
        return mode;
    }

    public void setMode(StartMode mode) {
        this.mode = mode;
    }

    public List<Command> getCommands() {
        return commands == null ? Collections.emptyList() : commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }
}
