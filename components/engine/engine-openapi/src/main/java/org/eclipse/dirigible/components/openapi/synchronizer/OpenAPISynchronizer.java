/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.openapi.synchronizer;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.openapi.domain.OpenAPI;
import org.eclipse.dirigible.components.openapi.generator.OpenApiGenerator;
import org.eclipse.dirigible.components.openapi.service.OpenAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

/**
 * The Class OpenAPISynchronizer.
 */
@Component
@Order(SynchronizersOrder.OPENAPI)
public class OpenAPISynchronizer extends BaseSynchronizer<OpenAPI, Long> {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenAPISynchronizer.class);

    /**
     * The Constant FILE_EXTENSION_OPENAPI.
     */
    private static final String FILE_EXTENSION_OPENAPI = "Controller.ts";

    /**
     * The openAPI service.
     */

    private final OpenAPIService openAPIService;

    /**
     * The synchronization callback.
     */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new openAPI synchronizer.
     *
     * @param openAPIService the openAPI service
     */
    @Autowired
    public OpenAPISynchronizer(OpenAPIService openAPIService) {
        this.openAPIService = openAPIService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return OpenAPI.ARTEFACT_TYPE.equals(type);
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
    protected List<OpenAPI> parseImpl(String location, byte[] content) throws ParseException {
        OpenAPI openAPI = new OpenAPI();
        // JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), OpenAPI.class);
        Configuration.configureObject(openAPI);
        openAPI.setLocation(location);
        openAPI.setName(FilenameUtils.getBaseName(location));
        openAPI.setType(OpenAPI.ARTEFACT_TYPE);
        String openApiContent = OpenApiGenerator.generate(location, new String(content));
        openAPI.setContent(openApiContent);
        openAPI.updateKey();
        try {
            OpenAPI maybe = getService().findByKey(openAPI.getKey());
            if (maybe != null) {
                openAPI.setId(maybe.getId());
            }
            openAPI = getService().save(openAPI);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("openAPI: {}", openAPI);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(openAPI);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<OpenAPI, Long> getService() {
        return openAPIService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<OpenAPI> retrieve(String location) {
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
    public void setStatus(OpenAPI artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Cleanup.
     *
     * @param openAPI the openAPI
     */
    @Override
    public void cleanupImpl(OpenAPI openAPI) {
        try {
            getService().delete(openAPI);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, openAPI, ArtefactLifecycle.DELETED, e);
        }
    }

    /**
     * Complete.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<OpenAPI> wrapper, ArtefactPhase flow) {
        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
        return true;
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
        return FILE_EXTENSION_OPENAPI;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return OpenAPI.ARTEFACT_TYPE;
    }
}
