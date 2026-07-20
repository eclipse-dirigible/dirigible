/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single step in a {@link ProcessIntent}. {@link #kind} is one of {@code userTask},
 * {@code serviceTask}, {@code decision}, {@code script}, {@code wait}, {@code end}. {@link #args}
 * carries kind-specific configuration ({@code assignee}, {@code form}, {@code if}, {@code then},
 * {@code call}, a wait's {@code onCreate}/{@code onUpdate}/{@code via}, a user task's
 * {@code timeout}/{@code expire} boundary timers); the process generator validates per kind.
 */
public class StepIntent {

    private String name;
    private String kind;
    private Map<String, Object> args = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args == null ? new LinkedHashMap<>() : args;
    }
}
