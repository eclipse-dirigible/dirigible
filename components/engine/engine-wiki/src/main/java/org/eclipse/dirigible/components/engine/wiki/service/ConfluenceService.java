/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.wiki.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.engine.wiki.domain.Confluence;
import org.eclipse.dirigible.components.engine.wiki.repository.ConfluenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ConfluenceService.
 */

@Service
@Transactional
public class ConfluenceService extends BaseArtefactService<Confluence, Long> {

    /**
     * Instantiates a new confluence service.
     *
     * @param repository the repository
     */
    public ConfluenceService(ConfluenceRepository repository) {
        super(repository);
    }
}
