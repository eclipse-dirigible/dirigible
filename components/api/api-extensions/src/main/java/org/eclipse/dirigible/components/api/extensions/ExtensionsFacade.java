/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.extensions;

import java.util.List;

import org.eclipse.dirigible.components.extensions.domain.Extension;
import org.eclipse.dirigible.components.extensions.domain.ExtensionPoint;
import org.eclipse.dirigible.components.extensions.service.ExtensionPointService;
import org.eclipse.dirigible.components.extensions.service.ExtensionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The ExtensionsFacade expose the information about the current extension points and extensions.
 */
@Component
public class ExtensionsFacade implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ExtensionsFacade.class);

    /** The database facade. */
    private static ExtensionsFacade INSTANCE;

    /** The extension point service. */
    private final ExtensionPointService extensionPointService;

    /** The extension service. */
    private final ExtensionService extensionService;

    /**
     * Instantiates a new extensions facade.
     *
     * @param extensionPointService the extension point service
     * @param extensionService the extension service
     */
    @Autowired
    public ExtensionsFacade(ExtensionPointService extensionPointService, ExtensionService extensionService) {
        this.extensionPointService = extensionPointService;
        this.extensionService = extensionService;
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
     * @return the extensions facade
     */
    public static ExtensionsFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the extension point service.
     *
     * @return the extension point service
     */
    public ExtensionPointService getExtensionPointService() {
        return extensionPointService;
    }

    /**
     * Gets the extension service.
     *
     * @return the extension service
     */
    public ExtensionService getExtensionService() {
        return extensionService;
    }

    /**
     * Gets the extensions per extension point.
     *
     * @param extensionPointName the extension point name
     * @return the extensions
     * @throws Exception the exception
     */
    public static final String[] getExtensions(String extensionPointName) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("API - ExtensionsServiceFacade.getExtensions() -> begin");
        }
        List<Extension> extensionDefinitions = ExtensionsFacade.get().extensionService.findByExtensionPoint(extensionPointName);
        String[] extensions = new String[extensionDefinitions.size()];
        int i = 0;
        for (Extension extensionDefinition : extensionDefinitions) {
            extensions[i++] = extensionDefinition.getModule();
        }
        if (logger.isTraceEnabled()) {
            logger.trace("API - ExtensionsServiceFacade.getExtensions() -> end");
        }
        return extensions;
    }

    /**
     * Gets all the extension points.
     *
     * @return the extension points
     * @throws Exception the exception
     */
    public static final String[] getExtensionPoints() throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("API - ExtensionsServiceFacade.getExtensionPoints() -> begin");
        }
        List<ExtensionPoint> extensionPointDefinitions = ExtensionsFacade.get()
                                                                         .getExtensionPointService()
                                                                         .getAll();
        String[] extensionPoints = new String[extensionPointDefinitions.size()];
        int i = 0;
        for (ExtensionPoint extensionPointDefinition : extensionPointDefinitions) {
            extensionPoints[i++] = extensionPointDefinition.getName();
        }
        if (logger.isTraceEnabled()) {
            logger.trace("API - ExtensionsServiceFacade.getExtensionPoints() -> end");
        }
        return extensionPoints;
    }

}
