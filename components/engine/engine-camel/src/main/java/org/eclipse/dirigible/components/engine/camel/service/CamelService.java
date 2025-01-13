/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.engine.camel.domain.Camel;
import org.eclipse.dirigible.components.engine.camel.repository.CamelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class CamelService.
 */
@Service
@Transactional
public class CamelService extends BaseArtefactService<Camel, Long> {

    /**
     * Instantiates a new camel service.
     *
     * @param repository the repository
     */
    public CamelService(CamelRepository repository) {
        super(repository);
    }
}
