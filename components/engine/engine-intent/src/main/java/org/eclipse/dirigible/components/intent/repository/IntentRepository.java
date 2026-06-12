/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.repository;

import org.eclipse.dirigible.components.base.artefact.ArtefactRepository;
import org.eclipse.dirigible.components.intent.domain.Intent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for {@link Intent} artefacts.
 *
 * <p>
 * The explicit {@code @Query} on {@link #setRunningToAll(boolean)} is mandatory: every concrete
 * artefact repository must override the parent's abstract declaration, otherwise Spring Data tries
 * to derive a query from the method name {@code setRunningToAll} and fails on context startup with
 * "No property 'setRunningToAll' found for type 'Intent'".
 */
@Repository("intentRepository")
public interface IntentRepository extends ArtefactRepository<Intent, Long> {

    @Override
    @Modifying
    @Transactional
    @Query(value = "UPDATE Intent SET running = :running")
    void setRunningToAll(@Param("running") boolean running);
}
