/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.definition;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processing the Definitions Service incoming requests.
 */
@Service
@Transactional
public class DefinitionService {

    /** The definition repository. */
    @Autowired
    private DefinitionRepository definitionRepository;

    /**
     * Gets the all.
     *
     * @return the all
     */
    @Transactional(readOnly = true)
    public List<Definition> getAll() {
        return definitionRepository.findAll();
    }

    /**
     * Gets the pages.
     *
     * @param pageable the pageable
     * @return the pages
     */
    @Transactional(readOnly = true)
    public Page<Definition> getPages(Pageable pageable) {
        return definitionRepository.findAll(pageable);
    }

    /**
     * Find by id.
     *
     * @param id the id
     * @return the definition
     */
    @Transactional(readOnly = true)
    public Definition findById(Long id) {
        Optional<Definition> definition = definitionRepository.findById(id);
        if (definition.isPresent()) {
            return definition.get();
        }
        throw new IllegalArgumentException("Definition with id does not exist: " + id);
    }

    /**
     * Find by key.
     *
     * @param key the key
     * @return the definition
     */
    @Transactional(readOnly = true)
    public Definition findByKey(String key) {
        Definition filter = new Definition();
        filter.setKey(key);
        Example<Definition> example = Example.of(filter);
        Optional<Definition> definition = definitionRepository.findOne(example);
        if (definition.isPresent()) {
            return definition.get();
        }
        return null;
    }

    /**
     * Find by location.
     *
     * @param location the location
     * @return the definition
     */
    @Transactional(readOnly = true)
    public Definition findByLocation(String location) {
        Definition filter = new Definition();
        filter.setLocation(location);
        Example<Definition> example = Example.of(filter);
        Optional<Definition> definition = definitionRepository.findOne(example);
        if (definition.isPresent()) {
            return definition.get();
        }
        return null;
    }

    /**
     * Save.
     *
     * @param definition the definition
     * @return the definition
     */
    public Definition save(Definition definition) {
        return definitionRepository.saveAndFlush(definition);
    }

    /**
     * Delete.
     *
     * @param definition the definition
     */
    public void delete(Definition definition) {
        definitionRepository.delete(definition);
    }

    /**
     * Update checksums.
     *
     * @param checksum the checksum
     * @param types the types
     */
    public void updateChecksums(String checksum, Set<String> types) {
        definitionRepository.updateChecksums(checksum, types);
    }

}
