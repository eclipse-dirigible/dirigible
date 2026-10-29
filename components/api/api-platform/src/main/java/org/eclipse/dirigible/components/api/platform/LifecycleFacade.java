/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import org.eclipse.dirigible.components.workspace.service.ProjectPublisher;
import org.eclipse.dirigible.components.workspace.service.WorkspaceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * The Class LifecycleFacade.
 */
@Component
public class LifecycleFacade implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleFacade.class);

    /** The instance. */
    private static LifecycleFacade INSTANCE;

    /** The publisher of the IDE, absent where the IDE is not deployed. */
    private final ObjectProvider<ProjectPublisher> publisher;

    /**
     * Instantiates a new lifecycle facade.
     *
     * @param publisher the publisher of the IDE, absent where the IDE is not deployed
     */
    private LifecycleFacade(ObjectProvider<ProjectPublisher> publisher) {
        this.publisher = publisher;
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
    public static LifecycleFacade get() {
        return INSTANCE;
    }

    private ProjectPublisher publisher(String operation) {
        ProjectPublisher available = publisher.getIfAvailable();
        if (available == null) {
            throw new WorkspaceUnavailableException(operation);
        }
        return available;
    }

    /**
     * Publish.
     *
     * @param user the user
     * @param workspace the workspace
     * @param project the project
     * @return true, if successful
     */
    public static boolean publish(String user, String workspace, String project) {
        boolean isSuccessfulPublishRequest = false;
        try {
            LifecycleFacade.get()
                           .publisher("publish")
                           .publish(user, workspace, project, "");
            isSuccessfulPublishRequest = true;
        } catch (Exception e) {
            LOGGER.warn("Publishing project [{}] of workspace [{}] failed", project, workspace, e);
        }
        return isSuccessfulPublishRequest;
    }

    /**
     * Unpublish.
     *
     * @param project the project
     * @return true, if successful
     */
    public static boolean unpublish(String project) {
        boolean isSuccessfulUnpublishRequest = false;
        try {
            LifecycleFacade.get()
                           .publisher("unpublish")
                           .unpublish(project);
            isSuccessfulUnpublishRequest = true;
        } catch (Exception e) {
            LOGGER.warn("Unpublishing [{}] failed", project, e);
        }
        return isSuccessfulUnpublishRequest;
    }

}
