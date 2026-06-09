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

public class ExposedPath {

    @Expose
    private String path;

    @Expose
    private List<String> scopes;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getScopes() {
        return scopes == null ? Collections.emptyList() : scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
