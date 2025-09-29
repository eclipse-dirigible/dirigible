/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.proxy.domain;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.artefact.Artefact;

/**
 * The Class DataSource.
 */
@Entity
@Table(name = "DIRIGIBLE_PROXY", uniqueConstraints = @UniqueConstraint(columnNames = "ARTEFACT_NAME"))
public class Proxy extends Artefact {

    public static final String ARTEFACT_TYPE = "proxy";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROXY_ID", nullable = false)
    private Long id;

    @Column(name = "PROXY_URL", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String url;

    public Proxy() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Proxy{" + "id=" + id + ", name='" + name + '\'' + ", url='" + url + '\'' + '}';
    }
}
