/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.watcher;

public class PlatformAsset {

    public enum Type {
        CSS, SCRIPT, PRELOAD
    }

    private final Type type;
    private final String path;
    private final boolean module;
    private final boolean defer;

    public PlatformAsset(Type type, String path, boolean module, boolean defer) {

        this.type = type;
        this.path = path;
        this.module = module;
        this.defer = defer;
    }

    public Type getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public boolean isModule() {
        return module;
    }

    public boolean isDefer() {
        return defer;
    }

}
