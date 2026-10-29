/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.workspace.service.ProjectPublisher;
import org.eclipse.dirigible.repository.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The Class PublisherService.
 */
@Service
public class PublisherService implements ProjectPublisher {

    /** The Constant DIRIGIBLE_PUBLISH_DISABLED. */
    public static final String DIRIGIBLE_PUBLISH_DISABLED = "DIRIGIBLE_PUBLISH_DISABLED";

    /** The folder a publish never copies, see {@code LocalRepositoryDao.copyFolder}. */
    private static final String GIT_FOLDER = ".git";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PublisherService.class);

    /** The publisher handlers. */
    private final List<PublisherHandler> publisherHandlers;

    /** The repository. */
    private final IRepository repository;

    /**
     * Marks the window in which the registry is half-written, so a synchronization pass that lands in
     * it defers its cleanup instead of deleting artefacts whose sources are about to reappear.
     */
    private final RegistryMutationTracker registryMutationTracker;

    /**
     * The files each project's publishes copied, so the next publish can unpublish the deleted ones.
     */
    private final PublishedFiles publishedFiles;

    /**
     * Instantiates a new publisher service.
     *
     * @param repository the repository
     * @param publisherHandlers the publisher handlers
     * @param registryMutationTracker the registry mutation tracker
     */
    @Autowired
    public PublisherService(IRepository repository, List<PublisherHandler> publisherHandlers,
            RegistryMutationTracker registryMutationTracker) {
        this.repository = repository;
        this.publisherHandlers = publisherHandlers;
        this.registryMutationTracker = registryMutationTracker;
        this.publishedFiles = new PublishedFiles(repository);
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
    @Override
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
            publishResource(workspacePath.toString(), sourceLocation, targetLocation,
                    new PublisherHandler.AfterPublishMetadata(workspace, project, path, true));
        } else {
            IResource resource = getRepository().getResource(sourceLocation);
            if (resource.exists()) {
                String targetLocation = new RepositoryPath(IRepositoryStructure.PATH_REGISTRY_PUBLIC, project, path).toString();
                publishResource(workspacePath.toString(), sourceLocation, targetLocation,
                        new PublisherHandler.AfterPublishMetadata(workspace, project, path, false));
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
     * @param workspaceLocation the location of the workspace the source belongs to
     * @param sourceLocation the source location
     * @param targetLocation the target location
     * @param afterPublishMetadata the after publish metadata
     */
    private void publishResource(String workspaceLocation, String sourceLocation, String targetLocation,
            PublisherHandler.AfterPublishMetadata afterPublishMetadata) {
        registryMutationTracker.enter();
        try {
            doPublishResource(workspaceLocation, sourceLocation, targetLocation, afterPublishMetadata);
        } finally {
            registryMutationTracker.exit();
        }
    }

    private void doPublishResource(String workspaceLocation, String sourceLocation, String targetLocation,
            PublisherHandler.AfterPublishMetadata afterPublishMetadata) {
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

        unpublishRemovedFiles(workspaceLocation, sourceLocation);

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
     * Unpublishes the files an earlier publish copied from the published location that the workspace no
     * longer has, and records the files this publish copied. Copying the workspace over the registry
     * alone leaves a deleted file published - and its artefact running - until the whole project is
     * unpublished.
     *
     * @param workspaceLocation the location of the workspace
     * @param sourceLocation the published location, the workspace itself or a project, folder or file
     *        in it
     */
    private synchronized void unpublishRemovedFiles(String workspaceLocation, String sourceLocation) {
        String scope = sourceLocation.substring(workspaceLocation.length())
                                     .replaceFirst("^/+", "");
        if (scope.isEmpty()) {
            for (String project : getRepository().getCollection(sourceLocation)
                                                 .getCollectionsNames()) {
                unpublishRemovedFiles(workspaceLocation, project, "");
            }
            return;
        }
        int separator = scope.indexOf(IRepositoryStructure.SEPARATOR);
        if (separator < 0) {
            unpublishRemovedFiles(workspaceLocation, scope, "");
        } else {
            unpublishRemovedFiles(workspaceLocation, scope.substring(0, separator), scope.substring(separator + 1));
        }
    }

    private void unpublishRemovedFiles(String workspaceLocation, String project, String projectScope) {
        String projectLocation = new RepositoryPath(workspaceLocation, project).toString();
        Set<String> published = new TreeSet<>();
        collectFiles(projectLocation, new RepositoryPath(projectLocation, projectScope).toString(), published);

        Set<String> recorded = publishedFiles.read(project);
        Set<String> retained = new TreeSet<>();
        for (String file : recorded) {
            if (!isWithin(file, projectScope)) {
                retained.add(file);
            } else if (!published.contains(file)) {
                String targetLocation = new RepositoryPath(IRepositoryStructure.PATH_REGISTRY_PUBLIC, project, file).toString();
                logger.info("Unpublishing [{}]: it was deleted from the workspace since the last publish of project [{}]", targetLocation,
                        project);
                doUnpublishResource(targetLocation);
            }
        }
        retained.addAll(published);
        publishedFiles.write(project, retained);
    }

    private void collectFiles(String projectLocation, String location, Set<String> files) {
        ICollection collection = getRepository().getCollection(location);
        if (collection.exists()) {
            for (IResource resource : collection.getResources()) {
                files.add(resource.getPath()
                                  .substring(projectLocation.length() + 1));
            }
            for (ICollection child : collection.getCollections()) {
                if (!GIT_FOLDER.equals(child.getName())) {
                    collectFiles(projectLocation, child.getPath(), files);
                }
            }
        } else if (getRepository().getResource(location)
                                  .exists()) {
            files.add(location.substring(projectLocation.length() + 1));
        }
    }

    private static boolean isWithin(String file, String folder) {
        return folder.isEmpty() || file.equals(folder) || file.startsWith(folder + IRepositoryStructure.SEPARATOR);
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
    @Override
    public void unpublish(String path) {
        if (Boolean.parseBoolean(Configuration.get(DIRIGIBLE_PUBLISH_DISABLED, Boolean.FALSE.toString()))) {
            return;
        }
        String targetLocation = new RepositoryPath(IRepositoryStructure.PATH_REGISTRY_PUBLIC, path).toString();
        unpublishResource(targetLocation);
        forgetPublishedFiles(new RepositoryPath(path));
    }

    /**
     * Drops the unpublished files from their project's record, so a later publish does not unpublish
     * them a second time.
     *
     * @param path the unpublished path, relative to the registry
     */
    private synchronized void forgetPublishedFiles(RepositoryPath path) {
        String[] segments = path.getSegments();
        if (segments.length == 0) {
            return;
        }
        String project = segments[0];
        String projectScope = String.join(IRepositoryStructure.SEPARATOR, Arrays.copyOfRange(segments, 1, segments.length));
        Set<String> retained = new TreeSet<>();
        for (String file : publishedFiles.read(project)) {
            if (!isWithin(file, projectScope)) {
                retained.add(file);
            }
        }
        publishedFiles.write(project, retained);
    }

    /**
     * Publish resource.
     *
     * @param targetLocation the targetLocation
     */
    private void unpublishResource(String targetLocation) {
        registryMutationTracker.enter();
        try {
            doUnpublishResource(targetLocation);
        } finally {
            registryMutationTracker.exit();
        }
    }

    private void doUnpublishResource(String targetLocation) {

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
