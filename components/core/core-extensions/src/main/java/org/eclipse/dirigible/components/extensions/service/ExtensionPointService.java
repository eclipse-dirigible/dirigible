/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.extensions.domain.ExtensionPoint;
import org.eclipse.dirigible.components.extensions.repository.ExtensionPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processing the Extension Points Service incoming requests.
 */
@Service
@Transactional
public class ExtensionPointService extends BaseArtefactService<ExtensionPoint, Long> {

    /**
     * Instantiates a new extension point service.
     *
     * @param repository the repository
     */
    public ExtensionPointService(ExtensionPointRepository repository) {
        super(repository);
    }

}
