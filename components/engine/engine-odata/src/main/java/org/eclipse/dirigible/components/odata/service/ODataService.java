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
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.components.odata.repository.ODataRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ODataService.
 */
@Service
@Transactional
public class ODataService extends BaseArtefactService<OData, Long> implements InitializingBean {

    /**
     * Instantiates a new o data service.
     *
     * @param odataRepository the odata repository
     */
    public ODataService(ODataRepository odataRepository) {
        super(odataRepository);
    }

    /** The instance. */
    private static ODataService INSTANCE;

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
     * @return the o data service
     */
    public static ODataService get() {
        return INSTANCE;
    }

}
