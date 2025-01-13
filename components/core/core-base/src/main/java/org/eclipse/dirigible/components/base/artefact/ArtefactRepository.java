/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * The Interface ArtefactRepository.
 *
 */
@NoRepositoryBean
public interface ArtefactRepository<A extends Artefact, ID> extends JpaRepository<A, ID> {

    /**
     * Find by location.
     *
     * @param location the location
     * @return the list
     */
    List<A> findByLocation(String location);

    /**
     * Find by name.
     *
     * @param name the name
     * @return the optional
     */
    Optional<A> findByName(String name);

    /**
     * Find by key.
     *
     * @param key the key
     * @return the optional
     */
    Optional<A> findByKey(String key);

    /**
     * Sets the running to all.
     *
     * @param running the new running to all
     */
    void setRunningToAll(boolean running);

}
