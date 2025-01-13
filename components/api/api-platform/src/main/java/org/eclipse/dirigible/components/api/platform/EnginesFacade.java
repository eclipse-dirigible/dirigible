/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import java.util.Map;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class EnginesFacade.
 */
@Component
public class EnginesFacade implements InitializingBean {

    /** The instance. */
    private static EnginesFacade INSTANCE;

    /** The engines. */
    private static String[] ENGINES = new String[] {"javascript"};

    /** The publisherService. */
    private JavascriptService javascriptService;

    /**
     * Instantiates a new engines facade.
     *
     * @param javascriptService the javascript service
     */
    @Autowired
    private EnginesFacade(JavascriptService javascriptService) {
        this.javascriptService = javascriptService;
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
     * Gets the instance.
     *
     * @return the database facade
     */
    public static EnginesFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the javascript service.
     *
     * @return the javascript service
     */
    public JavascriptService getJavascriptService() {
        return javascriptService;
    }


    /**
     * Execute.
     *
     * @param type the type
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param projectFilePathParam the project file path param
     * @param parameters the parameters
     * @param debug the debug
     * @return the object
     * @throws Exception the exception
     */
    public static Object execute(String type, String projectName, String projectFilePath, String projectFilePathParam,
            Map<Object, Object> parameters, boolean debug) throws Exception {
        if ("javascript".equals(type)) {
            return EnginesFacade.get()
                                .getJavascriptService()
                                .handleRequest(projectName, projectFilePath, projectFilePathParam, parameters, debug);
        } else {
            throw new Exception("Engine does not exist: " + type);
        }
    }

    /**
     * Gets the engine types.
     *
     * @return the engine types
     */
    public static String getEngineTypes() {
        return GsonHelper.toJson(ENGINES);
    }

}
