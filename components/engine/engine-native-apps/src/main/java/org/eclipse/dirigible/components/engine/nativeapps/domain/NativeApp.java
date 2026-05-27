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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import org.eclipse.dirigible.components.base.artefact.Artefact;

@Entity
@Table(name = "DIRIGIBLE_NATIVE_APPS", uniqueConstraints = @UniqueConstraint(columnNames = "ARTEFACT_NAME"))
public class NativeApp extends Artefact {

    public static final String ARTEFACT_TYPE = "native-app";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NATIVE_APP_ID", nullable = false)
    private Long id;

    @Column(name = "NATIVE_APP_BASE_PATH", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String basePath;

    @Column(name = "NATIVE_APP_KIND", columnDefinition = "VARCHAR", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private NativeAppKind kind;

    @Column(name = "NATIVE_APP_START_MODE", columnDefinition = "VARCHAR", nullable = true, length = 16)
    @Enumerated(EnumType.STRING)
    private StartMode startMode;

    @Column(name = "NATIVE_APP_REMOTE_URL", columnDefinition = "VARCHAR", nullable = true, length = 500)
    private String remoteUrl;

    @Column(name = "NATIVE_APP_DEFAULT_PORT", nullable = true)
    private Integer defaultPort;

    /**
     * Raw post-placeholder-expansion JSON of the {@code config} block. The typed
     * {@link NativeAppConfig} tree is rebuilt on demand from this column and is never persisted
     * directly as columns.
     */
    @Column(name = "NATIVE_APP_CONFIG_JSON", columnDefinition = "TEXT", nullable = true)
    private String configJson;

    @Transient
    private NativeAppConfig config;

    public NativeApp() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public NativeAppKind getKind() {
        return kind;
    }

    public void setKind(NativeAppKind kind) {
        this.kind = kind;
    }

    public StartMode getStartMode() {
        return startMode;
    }

    public void setStartMode(StartMode startMode) {
        this.startMode = startMode;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public Integer getDefaultPort() {
        return defaultPort;
    }

    public void setDefaultPort(Integer defaultPort) {
        this.defaultPort = defaultPort;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public NativeAppConfig getConfig() {
        return config;
    }

    public void setConfig(NativeAppConfig config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "NativeApp{id=" + id + ", name='" + name + "', basePath='" + basePath + "', kind=" + kind + ", startMode=" + startMode + "}";
    }
}
