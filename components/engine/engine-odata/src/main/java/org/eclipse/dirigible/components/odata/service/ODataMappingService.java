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
import org.eclipse.dirigible.components.odata.domain.ODataMapping;
import org.eclipse.dirigible.components.odata.repository.ODataMappingRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ODataMappingService.
 */
@Service
@Transactional
public class ODataMappingService extends BaseArtefactService<ODataMapping, Long> implements InitializingBean {

    /**
     * Instantiates a new o data mapping service.
     *
     * @param repository the repository
     */
    public ODataMappingService(ODataMappingRepository repository) {
        super(repository);
    }

    /** The instance. */
    private static ODataMappingService INSTANCE;

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the.
     *
     * @return the o data mapping service
     */
    public static ODataMappingService get() {
        return INSTANCE;
    }

    /**
     * Removes the mapping.
     *
     * @param location the location
     */
    public void removeMappings(String location) {
        ODataMapping filter = new ODataMapping();
        filter.setLocation(location);
        Example<ODataMapping> example = Example.of(filter);
        getRepo().deleteAll(getRepo().findAll(example));
    }

}
