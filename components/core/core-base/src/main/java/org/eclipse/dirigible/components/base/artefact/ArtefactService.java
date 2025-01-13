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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The Interface ArtefactService.
 *
 */
public interface ArtefactService<A extends Artefact, ID> {

    /**
     * Find all.
     *
     * @return the page
     */
    List<A> getAll();

    /**
     * Find all.
     *
     * @param pageable the pageable
     * @return the page
     */
    Page<A> getPages(Pageable pageable);

    /**
     * Find by id.
     *
     * @param id the id
     * @return the a
     */
    A findById(ID id);

    /**
     * Find by name.
     *
     * @param name the name
     * @return the a
     */
    A findByName(String name);

    /**
     * Find by location.
     *
     * @param location the location
     * @return the a
     */
    List<A> findByLocation(String location);

    /**
     * Find by key.
     *
     * @param key the key
     * @return the a
     */
    A findByKey(String key);

    /**
     * Save.
     *
     * @param a the a
     * @return the a
     */
    A save(A a);

    /**
     * Delete.
     *
     * @param a the a
     */
    void delete(A a);

    /**
     * Sets the running to all.
     *
     * @param running the new running to all
     */
    void setRunningToAll(boolean running);

}
