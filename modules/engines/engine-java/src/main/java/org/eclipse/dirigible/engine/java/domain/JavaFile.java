/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.domain;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persisted projection of a client-supplied {@code .java} source file discovered in the registry.
 *
 * <p>
 * Constructed and updated by {@code JavaSynchronizer}; consumed by the runtime registry to map
 * incoming REST requests to compiled, loaded handler classes.
 */
@Entity
@Table(name = "DIRIGIBLE_JAVA_FILES")
public class JavaFile extends Artefact {

    /** The artefact type discriminator used across synchronizer/service. */
    public static final String ARTEFACT_TYPE = "java";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JAVAFILE_ID", nullable = false)
    private Long id;

    /** Owning project name (first segment under {@code /registry/public/}). */
    @Column(name = "JAVAFILE_PROJECT", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String project;

    /** Fully-qualified class name parsed from the source ({@code package} + top-level {@code class}). */
    @Column(name = "JAVAFILE_CLASS_FQN", columnDefinition = "VARCHAR", nullable = false, length = 510)
    private String classFqn;

    public JavaFile() {
        super();
    }

    public JavaFile(String location, String name, String project, String classFqn) {
        super(location, name, ARTEFACT_TYPE, null, null);
        this.project = project;
        this.classFqn = classFqn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getClassFqn() {
        return classFqn;
    }

    public void setClassFqn(String classFqn) {
        this.classFqn = classFqn;
    }

    @Override
    public String toString() {
        return "JavaFile [id=" + id + ", project=" + project + ", classFqn=" + classFqn + ", location=" + location + ", name=" + name
                + ", key=" + key + ", lifecycle=" + lifecycle + "]";
    }

}
