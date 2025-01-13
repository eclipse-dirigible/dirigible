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
import org.eclipse.dirigible.components.odata.domain.ODataContainer;
import org.eclipse.dirigible.components.odata.repository.ODataContainerRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ODataContainerService.
 */
@Service
@Transactional
public class ODataContainerService extends BaseArtefactService<ODataContainer, Long> {

    /**
     * Instantiates a new o data container service.
     *
     * @param odataContainerRepository the odata container repository
     */
    public ODataContainerService(ODataContainerRepository odataContainerRepository) {
        super(odataContainerRepository);
    }

    /**
     * Removes the container.
     *
     * @param location the location
     */
    public void removeContainer(String location) {
        ODataContainer filter = new ODataContainer();
        filter.setLocation(location);
        Example<ODataContainer> example = Example.of(filter);
        getRepo().deleteAll(getRepo().findAll(example));
    }

}
