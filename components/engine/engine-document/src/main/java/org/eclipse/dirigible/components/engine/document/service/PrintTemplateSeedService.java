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

import java.util.List;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateSeed;
import org.eclipse.dirigible.components.engine.document.repository.PrintTemplateSeedRepository;
import org.eclipse.dirigible.components.engine.document.repository.PrintTemplateSeedRepository.ShippedVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The artefact service for {@link PrintTemplateSeed} entities.
 */
@Service
@Transactional
public class PrintTemplateSeedService extends BaseArtefactService<PrintTemplateSeed, Long> {

    private final PrintTemplateSeedRepository printTemplateSeedRepository;

    /**
     * Instantiates a new shipped print template service.
     *
     * @param printTemplateSeedRepository the repository
     */
    public PrintTemplateSeedService(PrintTemplateSeedRepository printTemplateSeedRepository) {
        super(printTemplateSeedRepository);
        this.printTemplateSeedRepository = printTemplateSeedRepository;
    }

    /**
     * The template names and versions the registry ships now for a document type and language, without
     * their content.
     *
     * @param entityName the document type
     * @param language the language code
     * @return the shipped versions
     */
    @Transactional(readOnly = true)
    public List<ShippedVersion> findShippedVersions(String entityName, String language) {
        return printTemplateSeedRepository.findShippedVersions(entityName, language);
    }
}
