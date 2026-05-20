/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.service;

import java.util.List;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.engine.java.domain.JavaFile;
import org.eclipse.dirigible.engine.java.repository.JavaFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional service over {@link JavaFile} artefacts.
 */
@Service
@Transactional
public class JavaFileService extends BaseArtefactService<JavaFile, Long> {

    private final JavaFileRepository repository;

    public JavaFileService(JavaFileRepository repository) {
        super(repository);
        this.repository = repository;
    }

    public List<JavaFile> findByProject(String project) {
        return repository.findByProject(project);
    }

}
