/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.endpoint;

import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.domain.StartMode;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;

/**
 * Read-only projection of a {@link NativeApp} returned by the management endpoint. Credentials are
 * intentionally omitted so resolved secret values never leave the platform.
 */
class NativeAppDto {

    private Long id;
    private String name;
    private String basePath;
    private NativeAppKind kind;
    private StartMode startMode;
    private String remoteUrl;
    private Integer defaultPort;
    private boolean running;
    private Integer runningPort;

    public static NativeAppDto from(NativeApp app, NativeAppProcessManager processManager) {
        NativeAppDto dto = new NativeAppDto();
        dto.id = app.getId();
        dto.name = app.getName();
        dto.basePath = app.getBasePath();
        dto.kind = app.getKind();
        dto.startMode = app.getStartMode();
        dto.remoteUrl = app.getRemoteUrl();
        dto.defaultPort = app.getDefaultPort();
        boolean alive = processManager.isAlive(app);
        dto.running = alive;
        dto.runningPort = alive ? processManager.getState(app)
                                                .map(s -> s.getPort())
                                                .orElse(null)
                : null;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBasePath() {
        return basePath;
    }

    public NativeAppKind getKind() {
        return kind;
    }

    public StartMode getStartMode() {
        return startMode;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public Integer getDefaultPort() {
        return defaultPort;
    }

    public boolean isRunning() {
        return running;
    }

    public Integer getRunningPort() {
        return runningPort;
    }
}
