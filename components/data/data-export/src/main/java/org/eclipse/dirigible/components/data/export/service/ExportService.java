/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.service;

import java.util.List;

import org.eclipse.dirigible.components.data.export.domain.Export;
import org.eclipse.dirigible.components.data.export.repository.ExportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ExportService.
 */
@Service
@Transactional
public class ExportService {

    private final ExportRepository repository;

    @Autowired
    public ExportService(ExportRepository repository) {
        this.repository = repository;
    }

    public Export findById(Long id) {
        return this.repository.findById(id)
                              .orElse(null);
    }

    public Export findByName(String name) {
        return this.repository.findByName(name)
                              .orElse(null);
    }

    public List<Export> findAll() {
        return this.repository.findAll();
    }

    public Long save(Export export) {
        return this.repository.saveAndFlush(export)
                              .getId();
    }

    public void delete(Long id) {
        this.repository.deleteById(id);
    }

    public void deleteAll() {
        this.repository.deleteAll();
    }

}
