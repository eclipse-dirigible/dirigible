/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.odata.domain.ODataSchema;
import org.eclipse.dirigible.components.odata.repository.ODataSchemaRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ODataSchemaService.
 */
@Service
@Transactional
public class ODataSchemaService extends BaseArtefactService<ODataSchema, Long> {

    /**
     * Instantiates a new o data schema service.
     *
     * @param repository the repository
     */
    ODataSchemaService(ODataSchemaRepository repository) {
        super(repository);
    }

    /**
     * Removes the schema.
     *
     * @param location the location
     */
    public void removeSchema(String location) {
        ODataSchema filter = new ODataSchema();
        filter.setLocation(location);
        Example<ODataSchema> example = Example.of(filter);
        getRepo().deleteAll(getRepo().findAll(example));
    }

}
