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

import org.eclipse.dirigible.components.base.artefact.ArtefactRepository;
import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateSeed;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Spring Data repository for {@link PrintTemplateSeed} artefacts.
 */
@Repository("printTemplateSeedRepository")
public interface PrintTemplateSeedRepository extends ArtefactRepository<PrintTemplateSeed, Long> {

    /**
     * Sets the running flag to all shipped print templates.
     *
     * @param running the new running flag
     */
    @Override
    @Modifying
    @Transactional
    @Query(value = "UPDATE PrintTemplateSeed SET running = :running")
    void setRunningToAll(@Param("running") boolean running);

    /**
     * The template names and versions shipped now for a document type and language - without the
     * content, which a print never needs.
     *
     * @param entityName the document type
     * @param language the language code
     * @return the shipped versions
     */
    @Query("SELECT s.templateName AS templateName, s.version AS version FROM PrintTemplateSeed s"
            + " WHERE s.entityName = :entityName AND s.language = :language")
    List<ShippedVersion> findShippedVersions(@Param("entityName") String entityName, @Param("language") String language);

    /** A template name and the version it ships as. */
    interface ShippedVersion {

        /**
         * Gets the template name.
         *
         * @return the template name
         */
        String getTemplateName();

        /**
         * Gets the version.
         *
         * @return the version
         */
        String getVersion();
    }
}
