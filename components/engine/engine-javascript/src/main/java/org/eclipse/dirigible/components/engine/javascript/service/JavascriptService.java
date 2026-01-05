/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.javascript.service;

import java.util.Map;

import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Class JavascriptHandler.
 */
@Service
public class JavascriptService implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(JavascriptService.class);

    /** The repository. */
    private IRepository repository;

    /** The handler. */
    private JavascriptHandler handler;

    /** The instance. */
    private static JavascriptService INSTANCE;

    /**
     * Instantiates a new javascript service.
     *
     * @param repository the repository
     */
    @Autowired
    public JavascriptService(IRepository repository) {
        this.repository = repository;
        this.handler = new JavascriptHandler(getRepository());
    }

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
     * @return the javascript service
     */
    public static JavascriptService get() {
        return INSTANCE;
    }

    /**
     * Gets the reposiotry.
     *
     * @return the reposiotry
     */
    public IRepository getRepository() {
        return repository;
    }

    /**
     * Handle request.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param projectFilePathParam the project file path param
     * @param parameters the parameters
     * @param debug the debug
     * @param keep the keep context open
     * @return the object
     */
    public Object handleRequest(String projectName, String projectFilePath, String projectFilePathParam, Map<Object, Object> parameters,
            boolean debug, boolean keep) {
        return handler.handleRequest(projectName, projectFilePath, projectFilePathParam, parameters, debug, keep);
    }

    /**
     * Handle request.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param projectFilePathParam the project file path param
     * @param parameters the parameters
     * @param debug the debug
     * @return the object
     */
    public Object handleRequest(String projectName, String projectFilePath, String projectFilePathParam, Map<Object, Object> parameters,
            boolean debug) {
        return handleRequest(projectName, projectFilePath, projectFilePathParam, parameters, debug, false);
    }

    /**
     * Handle callback.
     *
     * @param filePath the file path
     * @param parameters the parameters
     * @return the object
     */
    public Object handleCallback(String filePath, Map<Object, Object> parameters) {
        return handler.handleCallback(filePath, parameters);
    }

}
