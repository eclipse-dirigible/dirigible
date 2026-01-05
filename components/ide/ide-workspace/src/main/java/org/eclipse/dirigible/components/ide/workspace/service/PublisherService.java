/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.publisher.PublisherHandler;
import org.eclipse.dirigible.repository.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The Class PublisherService.
 */
@Service
public class PublisherService {

    /** The Constant DIRIGIBLE_PUBLISH_DISABLED. */
    public static final String DIRIGIBLE_PUBLISH_DISABLED = "DIRIGIBLE_PUBLISH_DISABLED";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PublisherService.class);

    /** The publisher handlers. */
    private final List<PublisherHandler> publisherHandlers;

    /** The repository. */
    private final IRepository repository;

    /**
     * Instantiates a new publisher service.
     *
     * @param repository the repository
     * @param publisherHandlers the publisher handlers
     */
    @Autowired
    public PublisherService(IRepository repository, List<PublisherHandler> publisherHandlers) {
        this.repository = repository;
        this.publisherHandlers = publisherHandlers;
    }

    /**
     * Publish.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     */
    public void publish(String workspace, String project, String path) {
        String user = UserFacade.getName();
        publish(user, workspace, project, path);
    }

    /**
     * Publish.
     *
     * @param user the user
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     */
    public void publish(String user, String workspace, String project, String path) {
        if (Boolean.parseBoolean(Configuration.get(DIRIGIBLE_PUBLISH_DISABLED, Boolean.FALSE.toString()))) {
            return;
        }
        StringBuilder workspacePath = generateWorkspacePath(user, workspace, null, null);
        if ("*".equals(project)) {
            project = "";
        }

        String sourceLocation = new RepositoryPath(workspacePath.toString(), project, path).toString();
        ICollection collection = getRepository().getCollection(sourceLocation);
        if (collection.exists()) {
            String targetLocation = new RepositoryPath(IRepositoryStructure.PATH_REGISTRY_PUBLIC, project, path).toString();
            publishResource(sourceLocation, targetLocation, new PublisherHandler.AfterPublishMetadata(workspace, project, path, true));
        } else {
            IResource resource = getRepository().getResource(sourceLocation);
            if (resource.exists()) {
                String targetLocation = new RepositoryPath(IRepositoryStructure.PATH_REGISTRY_PUBLIC, project, path).toString();
                publishResource(sourceLocation, targetLocation, new PublisherHandler.AfterPublishMetadata(workspace, project, path, false));
            }
        }
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public IRepository getRepository() {
        return repository;
    }

    /**
     * Publish resource.
     *
     * @param sourceLocation the source location
     * @param targetLocation the target location
     * @param afterPublishMetadata the after publish metadata
     */
    private void publishResource(String sourceLocation, String targetLocation, PublisherHandler.AfterPublishMetadata afterPublishMetadata) {
        for (PublisherHandler next : publisherHandlers) {
            try {
                next.beforePublish(sourceLocation);
            } catch (Exception e) {
                logger.error("{} failed on before publish.", next.getClass()
                                                                 .getCanonicalName());
                logger.error(e.getMessage(), e);
            }
        }

        ICollection sourceCollection = getRepository().getCollection(sourceLocation);
        if (sourceCollection.exists()) {
            // publish collection
            ICollection targetCollection = getRepository().getCollection(targetLocation);
            logger.info("Publishing collection: [{}] to [{}]", sourceCollection.getPath(), targetCollection.getPath());
            long currentMillis = System.currentTimeMillis();
            sourceCollection.copyTo(targetCollection.getPath());
            logger.info("Published collection: [{}] to [{}]. Copy took [{}] millis", sourceCollection.getPath(), targetCollection.getPath(),
                    (System.currentTimeMillis() - currentMillis));
        } else {
            // publish a single resource
            IResource sourceResource = getRepository().getResource(sourceLocation);
            IResource targetResource = getRepository().getResource(targetLocation);
            if (targetResource.exists()) {
                targetResource.setContent(sourceResource.getContent());
            } else {
                getRepository().createResource(targetLocation, sourceResource.getContent());
            }
            logger.info("Published resource: {} -> {}", sourceResource.getPath(), targetResource.getPath());
        }

        for (PublisherHandler next : publisherHandlers) {
            try {
                next.afterPublish(sourceLocation, targetLocation, afterPublishMetadata);
            } catch (Exception e) {
                logger.error("{} failed on after publish.", next.getClass()
                                                                .getCanonicalName(),
                        e);
            }
        }
    }

    /**
     * Generate workspace path.
     *
     * @param user the user
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the string builder
     */
    private StringBuilder generateWorkspacePath(String user, String workspace, String project, String path) {
        StringBuilder relativePath = new StringBuilder(IRepositoryStructure.PATH_USERS).append(IRepositoryStructure.SEPARATOR)
                                                                                       .append(user)
                                                                                       .append(IRepositoryStructure.SEPARATOR)
                                                                                       .append(workspace);
        if (project != null) {
            relativePath.append(IRepositoryStructure.SEPARATOR)
                        .append(project);
        }
        if (path != null) {
            relativePath.append(IRepositoryStructure.SEPARATOR)
                        .append(path);
        }
        return relativePath;
    }

    /**
     * Unpublish.
     *
     * @param path the path
     */
    public void unpublish(String path) {
        if (Boolean.parseBoolean(Configuration.get(DIRIGIBLE_PUBLISH_DISABLED, Boolean.FALSE.toString()))) {
            return;
        }
        String targetLocation = new RepositoryPath(IRepositoryStructure.PATH_REGISTRY_PUBLIC, path).toString();
        unpublishResource(targetLocation);
    }

    /**
     * Publish resource.
     *
     * @param targetLocation the targetLocation
     */
    private void unpublishResource(String targetLocation) {

        for (PublisherHandler next : publisherHandlers) {
            try {
                next.beforeUnpublish(targetLocation);
            } catch (Exception e) {
                logger.error("{} failed on before unpublish.", next.getClass()
                                                                   .getCanonicalName());
                logger.error(e.getMessage(), e);
            }
        }

        ICollection targetCollection = getRepository().getCollection(targetLocation);
        if (targetCollection.exists()) {
            // unpublish collection
            targetCollection.delete();
            logger.info("Unpublished collection: {}", targetCollection.getPath());
        } else {
            // unpublish a single resource
            IResource targetResource = getRepository().getResource(targetLocation);
            if (targetResource.exists()) {
                targetResource.delete();
            }
            logger.info("Unpublished resource: {}", targetCollection.getPath());
        }

        for (PublisherHandler next : publisherHandlers) {
            try {
                next.afterUnpublish(targetLocation);
            } catch (Exception e) {
                logger.error("{} failed on after unpublish.", next.getClass()
                                                                  .getCanonicalName());
                logger.error(e.getMessage(), e);
            }
        }
    }

}
