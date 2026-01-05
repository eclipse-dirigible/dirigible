/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.synchronizer;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.web.domain.Expose;
import org.eclipse.dirigible.components.engine.web.exposure.ExposeManager;
import org.eclipse.dirigible.components.engine.web.service.ExposeService;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

/**
 * The Class ExposesSynchronizer.
 */
@Component
@Order(SynchronizersOrder.EXPOSE)
public class ExposesSynchronizer extends BaseSynchronizer<Expose, Long> {

    /** The Constant FILE_NAME. */
    public static final String FILE_NAME = "web.json";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ExposesSynchronizer.class);
    /** The expose service. */
    private final ExposeService exposeService;

    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /** The GSON instance. */
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                                                      .setPrettyPrinting()
                                                      .setStrictness(Strictness.LENIENT)
                                                      .create();

    /**
     * Instantiates a new exposes synchronizer.
     *
     * @param exposeService the expose service
     */
    @Autowired
    public ExposesSynchronizer(ExposeService exposeService) {
        this.exposeService = exposeService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Expose.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Load.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    @Override
    public List<Expose> parseImpl(String location, byte[] content) throws ParseException {

        RepositoryPath repositoryPath = new RepositoryPath(location);
        if (repositoryPath.getSegments().length <= 1) {
            throw new ParseException("Invalid location provided for web.json artefact: " + location, 0);
        }
        String project = repositoryPath.getSegments()[0];
        Expose expose = GSON.fromJson(new String(content, StandardCharsets.UTF_8), Expose.class);
        expose.setProject(project);
        Configuration.configureObject(expose);
        expose.setLocation(location);
        expose.setName(expose.getProject());
        expose.setType(Expose.ARTEFACT_TYPE);
        expose.updateKey();
        try {
            Expose maybe = getService().findByKey(expose.getKey());
            if (maybe != null) {
                expose.setId(maybe.getId());
            }
            expose = getService().save(expose);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("expose: {}", expose);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(expose);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Expose, Long> getService() {
        return exposeService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Expose> retrieve(String location) {
        return getService().findByLocation(location);
    }

    /**
     * Sets the status.
     *
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param error the error
     */
    @Override
    public void setStatus(Expose artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<Expose> wrapper, ArtefactPhase flow) {
        Expose expose = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (expose.getLifecycle()
                          .equals(ArtefactLifecycle.NEW)) {
                    if (expose.getExposes() != null) {
                        ExposeManager.registerExposableProject(expose.getName(), expose.getExposes());
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace(expose.getName() + " skipped due to lack of exposures");
                        }
                    }
                    callback.registerState(this, wrapper, ArtefactLifecycle.CREATED, "");
                }
                break;
            case UPDATE:
                if (expose.getLifecycle()
                          .equals(ArtefactLifecycle.MODIFIED)) {
                    ExposeManager.unregisterProject(expose.getName());
                    if (expose.getExposes() != null) {
                        ExposeManager.registerExposableProject(expose.getName(), expose.getExposes());
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace(expose.getName() + " skipped due to lack of exposures");
                        }
                    }
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED, "");
                }
                if (expose.getLifecycle()
                          .equals(ArtefactLifecycle.FAILED)) {
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED, "");
                    return false;
                }
                break;
            case DELETE:
                if (expose.getLifecycle()
                          .equals(ArtefactLifecycle.CREATED)
                        || expose.getLifecycle()
                                 .equals(ArtefactLifecycle.UPDATED)
                        || expose.getLifecycle()
                                 .equals(ArtefactLifecycle.FAILED)) {
                    ExposeManager.unregisterProject(expose.getName());
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, "");
                }
                break;
            case START:
            case STOP:
        }

        return true;
    }

    /**
     * Cleanup.
     *
     * @param expose the expose
     */
    @Override
    public void cleanupImpl(Expose expose) {
        try {
            ExposeManager.unregisterProject(expose.getName());
            getService().delete(expose);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            callback.addError(e.getMessage());
            callback.registerState(this, expose, ArtefactLifecycle.DELETED, e.getMessage());
        }
    }

    /**
     * Sets the callback.
     *
     * @param callback the new callback
     */
    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return FILE_NAME;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Expose.ARTEFACT_TYPE;
    }

}
