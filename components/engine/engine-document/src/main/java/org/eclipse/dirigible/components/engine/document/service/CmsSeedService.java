/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.engine.document.domain.CmsSeed;
import org.eclipse.dirigible.components.engine.document.repository.CmsSeedRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The artefact service for {@link CmsSeed} entities.
 */
@Service
@Transactional
public class CmsSeedService extends BaseArtefactService<CmsSeed, Long> {

    /**
     * Instantiates a new CMS seed service.
     *
     * @param cmsSeedRepository the CMS seed repository
     */
    public CmsSeedService(CmsSeedRepository cmsSeedRepository) {
        super(cmsSeedRepository);
    }
}
