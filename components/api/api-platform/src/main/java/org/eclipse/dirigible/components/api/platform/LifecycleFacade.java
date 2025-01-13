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

import org.eclipse.dirigible.components.ide.workspace.service.PublisherService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class LifecycleFacade.
 */
@Component
public class LifecycleFacade implements InitializingBean {

    /** The instance. */
    private static LifecycleFacade INSTANCE;

    /** The publisherService. */
    private PublisherService publisherService;

    /**
     * Instantiates a new lifecycle facade.
     *
     * @param publisherService the publisher service
     */
    @Autowired
    private LifecycleFacade(PublisherService publisherService) {
        this.publisherService = publisherService;
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

    /**
     * Gets the publisher service.
     *
     * @return the publisher service
     */
    public PublisherService getPublisherService() {
        return publisherService;
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
                           .getPublisherService()
                           .publish(user, workspace, project, "");
            isSuccessfulPublishRequest = true;
        } catch (Exception e) {
            // Do nothing
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
                           .getPublisherService()
                           .unpublish(project);
            isSuccessfulUnpublishRequest = true;
        } catch (Exception e) {
            // Do nothing
        }
        return isSuccessfulUnpublishRequest;
    }

}
