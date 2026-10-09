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

import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateVersion;
import org.eclipse.dirigible.components.engine.document.repository.PrintTemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ledger of the print template versions this instance has shipped.
 */
@Service
@Transactional
public class PrintTemplateVersionService {

    private final PrintTemplateVersionRepository printTemplateVersionRepository;

    /**
     * Instantiates the service.
     *
     * @param printTemplateVersionRepository the repository
     */
    public PrintTemplateVersionService(PrintTemplateVersionRepository printTemplateVersionRepository) {
        this.printTemplateVersionRepository = printTemplateVersionRepository;
    }

    /**
     * The versions shipped for a document type and language, oldest first.
     *
     * @param entityName the document type
     * @param language the language code
     * @return the versions
     */
    @Transactional(readOnly = true)
    public List<PrintTemplateVersion> findShipped(String entityName, String language) {
        return printTemplateVersionRepository.findByEntityNameAndLanguageOrderByIdAsc(entityName, language);
    }

    /**
     * Records a version as shipped.
     *
     * @param version the version
     * @return the recorded version, with the id that orders it
     */
    public PrintTemplateVersion record(PrintTemplateVersion version) {
        return printTemplateVersionRepository.saveAndFlush(version);
    }
}
