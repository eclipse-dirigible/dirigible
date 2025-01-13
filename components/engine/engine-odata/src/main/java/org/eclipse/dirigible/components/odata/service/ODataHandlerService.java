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

import java.util.List;
import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.odata.domain.ODataHandler;
import org.eclipse.dirigible.components.odata.repository.ODataHandlerRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ODataHandlerService.
 */
@Service
@Transactional
public class ODataHandlerService extends BaseArtefactService<ODataHandler, Long> implements InitializingBean {

    /**
     * Instantiates a new o data handler service.
     *
     * @param repository the repository
     */
    public ODataHandlerService(ODataHandlerRepository repository) {
        super(repository);
    }

    /** The instance. */
    private static ODataHandlerService INSTANCE;

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
     * @return the o data handler service
     */
    public static ODataHandlerService get() {
        return INSTANCE;
    }

    /**
     * Removes the handler.
     *
     * @param location the location
     */
    public void removeHandlers(String location) {
        ODataHandler filter = new ODataHandler();
        filter.setLocation(location);
        Example<ODataHandler> example = Example.of(filter);
        getRepo().deleteAll(getRepo().findAll(example));
    }

    /**
     * Gets the by namespace name method and kind.
     *
     * @param namespace the namespace
     * @param name the name
     * @param method the method
     * @param kind the kind
     * @return the by namespace name method and kind
     */
    public List<ODataHandler> getByNamespaceNameMethodAndKind(String namespace, String name, String method, String kind) {
        ODataHandler filter = new ODataHandler();
        filter.setNamespace(namespace);
        filter.setName(name);
        filter.setMethod(method);
        filter.setKind(kind);
        Example<ODataHandler> example = Example.of(filter);
        return getRepo().findAll(example);
    }

}
