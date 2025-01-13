/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.repository.AccessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class AccessService.
 */
@Service
@Transactional
public class AccessService extends BaseArtefactService<Access, Long> {

    /**
     * Instantiates a new access service.
     *
     * @param repository the repository
     */
    public AccessService(AccessRepository repository) {
        super(repository);
    }

}
