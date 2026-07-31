/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import java.util.List;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The artefact service for {@code .numbers} series declarations.
 */
@Service
@Transactional
public class NumberSeriesDeclarationService extends BaseArtefactService<NumberSeriesDeclaration, Long> {

    private final NumberSeriesDeclarationRepository repository;

    NumberSeriesDeclarationService(NumberSeriesDeclarationRepository repository) {
        super(repository);
        this.repository = repository;
    }

    /**
     * Every declaration of one series, across all declaring modules - the synchronizer's
     * conflict-detection read.
     *
     * @param name the series identity
     * @return the declarations
     */
    @Transactional(readOnly = true)
    public List<NumberSeriesDeclaration> findAllByName(String name) {
        return repository.findAllByName(name);
    }
}
