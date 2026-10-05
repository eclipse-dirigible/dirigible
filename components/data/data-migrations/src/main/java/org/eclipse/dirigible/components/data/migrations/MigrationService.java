/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.migrations;

import java.util.List;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The service of the migration artefacts. */
@Service
@Transactional
public class MigrationService extends BaseArtefactService<Migration, Long> {

    private final MigrationRepository repository;

    MigrationService(MigrationRepository repository) {
        super(repository);
        this.repository = repository;
    }

    /**
     * Finds the migrations of a project.
     *
     * @param project the project
     * @return the project's migrations
     */
    @Transactional(readOnly = true)
    public List<Migration> findAllByProject(String project) {
        return repository.findAllByProject(project);
    }
}
