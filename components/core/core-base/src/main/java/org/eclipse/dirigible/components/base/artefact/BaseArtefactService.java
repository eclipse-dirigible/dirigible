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
 * The Class BaseArtefactService.
 *
 */
public abstract class BaseArtefactService<A extends Artefact, ID> implements ArtefactService<A, ID> {

    /** The repository. */
    private final ArtefactRepository<A, ID> repository;

    /**
     * Instantiates a new base artefact service.
     *
     * @param repository the repository
     */
    protected BaseArtefactService(ArtefactRepository<A, ID> repository) {
        this.repository = repository;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @Override
    public final List<A> getAll() {
        return getRepo().findAll();
    }

    /**
     * Gets the pages.
     *
     * @param pageable the pageable
     * @return the pages
     */
    @Override
    public final Page<A> getPages(Pageable pageable) {
        return getRepo().findAll(pageable);
    }

    /**
     * Find by id.
     *
     * @param id the id
     * @return the a
     */
    @Override
    public final A findById(ID id) {
        return getRepo().findById(id)
                        .orElseThrow(() -> new IllegalArgumentException(this.getClass() + ": missing artefact with [" + id + "]"));
    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the a
     */
    @Override
    public A findByName(String name) {
        return getRepo().findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException(this.getClass() + ": missing artefact with name: [" + name + "]"));
    }

    /**
     * Find by location.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public final List<A> findByLocation(String location) {
        return getRepo().findByLocation(location);
    }

    /**
     * Find by key.
     *
     * @param key the key
     * @return the a
     */
    @Override
    public final A findByKey(String key) {
        return getRepo().findByKey(key)
                        .orElse(null);
    }

    /**
     * Save.
     *
     * @param a the a
     * @return the a
     */
    @Override
    public A save(A a) {
        return getRepo().saveAndFlush(a);
    }

    /**
     * Delete.
     *
     * @param a the a
     */
    @Override
    public void delete(A a) {
        getRepo().delete(a);
    }

    /**
     * Sets the running to all.
     *
     * @param running the new running to all
     */
    @Override
    public void setRunningToAll(boolean running) {
        getRepo().setRunningToAll(running);
    }

    /**
     * Gets the repo.
     *
     * @return the repo
     */
    protected ArtefactRepository<A, ID> getRepo() {
        return repository;
    }

}
