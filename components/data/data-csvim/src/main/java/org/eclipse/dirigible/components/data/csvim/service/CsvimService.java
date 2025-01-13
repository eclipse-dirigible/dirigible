/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.data.csvim.domain.Csvim;
import org.eclipse.dirigible.components.data.csvim.repository.CsvimRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ExtensionsCoreService.
 */
@Service
@Transactional
public class CsvimService extends BaseArtefactService<Csvim, Long> {

    /**
     * Instantiates a new csvim service.
     *
     * @param repository the repository
     */
    public CsvimService(CsvimRepository repository) {
        super(repository);
    }

}
