/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
/*
 *
 */
package org.eclipse.dirigible.components.ide.workspace.handlers;

import org.eclipse.dirigible.components.api.extensions.ExtensionsFacade;
import org.eclipse.dirigible.components.base.publisher.PublisherHandler;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class ExtensionsPublisherHandler.
 */

@Order(40)
@Component
public class ExtensionsPublisherHandler implements PublisherHandler {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ExtensionsPublisherHandler.class);

    /** The Constant EXTENSION_POINT_IDE_WORKSPACE_BEFORE_PUBLISH. */
    private static final String EXTENSION_POINT_IDE_WORKSPACE_BEFORE_PUBLISH = "ide-workspace-before-publish";

    /** The Constant EXTENSION_POINT_IDE_WORKSPACE_AFTER_PUBLISH. */
    private static final String EXTENSION_POINT_IDE_WORKSPACE_AFTER_PUBLISH = "ide-workspace-after-publish";

    /** The Constant EXTENSION_POINT_IDE_WORKSPACE_BEFORE_UNPUBLISH. */
    private static final String EXTENSION_POINT_IDE_WORKSPACE_BEFORE_UNPUBLISH = "ide-workspace-before-unpublish";

    /** The Constant EXTENSION_POINT_IDE_WORKSPACE_AFTER_UNPUBLISH. */
    private static final String EXTENSION_POINT_IDE_WORKSPACE_AFTER_UNPUBLISH = "ide-workspace-after-unpublish";

    /** The Constant EXTENSION_PARAMETER_PATH. */
    private static final String EXTENSION_PARAMETER_PATH = "path";

    /** The javascript service. */
    @Autowired
    private JavascriptService javascriptService;

    /**
     * Before publish.
     *
     * @param location the location
     */
    @Override
    public void beforePublish(String location) {
        triggerExtensions(location, EXTENSION_POINT_IDE_WORKSPACE_BEFORE_PUBLISH, "Before Publish");
    }

    /**
     * Trigger extensions.
     *
     * @param location the location
     * @param extensionPoint the extension point
     * @param state the state
     */
    private void triggerExtensions(String location, String extensionPoint, String state) {
        try {
            String[] modules = ExtensionsFacade.getExtensions(extensionPoint);
            for (String module : modules) {
                try {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Workspace {} Extension: {} triggered...", state, module);
                    }
                    Map<Object, Object> internal = new HashMap<>();
                    internal.put(EXTENSION_PARAMETER_PATH, location);
                    internal.put("handler", module);
                    RepositoryPath path = new RepositoryPath(module);
                    javascriptService.handleRequest(path.getSegments()[0], path.constructPathFrom(1), null, internal, false);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Workspace {} Extension: {} finshed.", state, module);
                    }
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * After publish.
     *
     * @param workspaceLocation the workspace location
     * @param registryLocation the registry location
     * @param metadata the metadata
     */
    @Override
    public void afterPublish(String workspaceLocation, String registryLocation, AfterPublishMetadata metadata) {
        triggerExtensions(workspaceLocation, EXTENSION_POINT_IDE_WORKSPACE_AFTER_PUBLISH, "After Publish");
    }

    /**
     * Before unpublish.
     *
     * @param location the location
     */
    @Override
    public void beforeUnpublish(String location) {
        triggerExtensions(location, EXTENSION_POINT_IDE_WORKSPACE_BEFORE_UNPUBLISH, "Before Unpublish");
    }

    /**
     * After unpublish.
     *
     * @param location the location
     */
    @Override
    public void afterUnpublish(String location) {
        triggerExtensions(location, EXTENSION_POINT_IDE_WORKSPACE_AFTER_UNPUBLISH, "After Unpublish");
    }

}
