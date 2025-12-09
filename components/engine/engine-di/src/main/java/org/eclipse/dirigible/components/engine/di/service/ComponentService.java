/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.di.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.engine.di.domain.Component;
import org.eclipse.dirigible.components.engine.di.repository.ComponentRepository;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class EntityService.
 */
@Service
@Transactional
public class ComponentService extends BaseArtefactService<Component, Long> {

    /** The javascript service. */
    private final JavascriptService javascriptService;
    /** The repository. */
    private final IRepository contentRepository;

    /**
     * Instantiates a new component service.
     *
     * @param componentRepository the componentRepository
     * @param javascriptService the javascriptService
     * @param contentRepository the contentRepository
     */
    @Autowired
    public ComponentService(ComponentRepository componentRepository, JavascriptService javascriptService, IRepository contentRepository) {
        super(componentRepository);
        this.javascriptService = javascriptService;
        this.contentRepository = contentRepository;
    }

    /**
     * Execute java script.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @return the response
     */
    public Value executeJavaScript(String projectName, String projectFilePath) {
        try {
            Object object = getJavascriptService().handleRequest(projectName, projectFilePath, null, null, false, true);
            if (object instanceof Value) {
                return (Value) object;
            }
            String errorMessage = "Invalid result of the Component file [" + projectFilePath + "] in project [" + projectName
                    + "]. The reference of the component class must be the last expression in the file. Returned object is: [" + object
                    + "] of type [" + (null == object ? object : object.getClass()) + "] but expected instance of " + Value.class;
            throw new IllegalArgumentException(errorMessage);
        } catch (RepositoryNotFoundException e) {
            String message = e.getMessage() + ". Try to publish the service before execution.";
            throw new RepositoryNotFoundException(message, e);
        }
    }

    /**
     * Gets the javascript handler.
     *
     * @return the javascript handler
     */
    protected JavascriptService getJavascriptService() {
        return javascriptService;
    }

}
