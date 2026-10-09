/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document.repository;

import java.util.List;

import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The Spring Data repository of the shipped print template versions.
 */
@Repository("printTemplateVersionRepository")
public interface PrintTemplateVersionRepository extends JpaRepository<PrintTemplateVersion, Long> {

    /**
     * The versions shipped for a document type and language, oldest first.
     *
     * @param entityName the document type
     * @param language the language code
     * @return the versions
     */
    List<PrintTemplateVersion> findByEntityNameAndLanguageOrderByIdAsc(String entityName, String language);
}
