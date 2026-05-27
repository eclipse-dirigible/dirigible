/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.synchronizer;

import com.google.gson.annotations.Expose;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppConfig;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;

/**
 * DTO that matches the {@code .native-app} JSON file 1:1. Decoupled from the JPA entity so we can
 * keep the JSON field {@code type} (LOCAL / REMOTE) without colliding with
 * {@link org.eclipse.dirigible.components.base.artefact.Artefact#type Artefact.type}, which is the
 * artefact-type string used internally.
 */
public class NativeAppFile {

    @Expose
    private String id;

    @Expose
    private String name;

    @Expose
    private String description;

    @Expose
    private String basePath;

    @Expose
    private NativeAppKind type;

    @Expose
    private NativeAppConfig config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public NativeAppKind getType() {
        return type;
    }

    public void setType(NativeAppKind type) {
        this.type = type;
    }

    public NativeAppConfig getConfig() {
        return config;
    }

    public void setConfig(NativeAppConfig config) {
        this.config = config;
    }
}
