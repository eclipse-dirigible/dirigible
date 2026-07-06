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

import org.eclipse.dirigible.components.base.artefact.ArtefactRepository;
import org.eclipse.dirigible.components.engine.document.domain.CmsSeed;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Spring Data repository for {@link CmsSeed} artefacts.
 */
@Repository("cmsSeedRepository")
public interface CmsSeedRepository extends ArtefactRepository<CmsSeed, Long> {

    /**
     * Sets the running flag to all CMS seeds.
     *
     * @param running the new running flag
     */
    @Override
    @Modifying
    @Transactional
    @Query(value = "UPDATE CmsSeed SET running = :running")
    void setRunningToAll(@Param("running") boolean running);
}
