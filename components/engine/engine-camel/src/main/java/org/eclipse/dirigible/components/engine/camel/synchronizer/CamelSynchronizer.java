/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.synchronizer;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.engine.camel.domain.Camel;
import org.eclipse.dirigible.components.engine.camel.processor.CamelProcessor;
import org.eclipse.dirigible.components.engine.camel.service.CamelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

/**
 * The Class CamelSynchronizer.
 */
@Component
@Order(250)
public class CamelSynchronizer extends BaseSynchronizer<Camel, Long> {

    /** The Constant FILE_EXTENSION_CAMEL. */
    public static final String FILE_EXTENSION_CAMEL = ".camel";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CamelSynchronizer.class);
    /** The camel service. */
    private final CamelService camelService;

    /** The camel processor. */
    private final CamelProcessor camelProcessor;
    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new camel synchronizer.
     *
     * @param camelService the camel service
     * @param camelProcessor the camel processor
     */
    @Autowired
    public CamelSynchronizer(CamelService camelService, CamelProcessor camelProcessor) {
        this.camelService = camelService;
        this.camelProcessor = camelProcessor;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Camel.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Parses the.
     *
     * @param location the location
     * @param content the content
     * @return the list
     */
    @Override
    protected List<Camel> parseImpl(String location, byte[] content) {
        Camel camel = new Camel();
        camel.setLocation(location);
        camel.setName(Paths.get(location)
                           .getFileName()
                           .toString());
        camel.setType(Camel.ARTEFACT_TYPE);
        camel.updateKey();
        camel.setContent(content);
        try {
            Camel maybe = getService().findByKey(camel.getKey());
            if (maybe != null) {
                camel.setId(maybe.getId());
            }
            getService().save(camel);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("camel: {}", camel);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
        }
        return List.of(camel);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Camel, Long> getService() {
        return camelService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Camel> retrieve(String location) {
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
    public void setStatus(Camel artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete impl.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<Camel> wrapper, ArtefactPhase flow) {
        try {
            Camel camel = wrapper.getArtefact();

            switch (flow) {
                case CREATE:
                    if (ArtefactLifecycle.NEW.equals(camel.getLifecycle())) {
                        addToProcessor(camel);
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    }
                    break;
                case UPDATE:
                    if (ArtefactLifecycle.MODIFIED.equals(camel.getLifecycle())) {
                        addToProcessor(camel);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    }
                    if (ArtefactLifecycle.FAILED.equals(camel.getLifecycle())) {
                        return false;
                    }
                    break;
                case DELETE:
                    if (ArtefactLifecycle.CREATED.equals(camel.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(camel.getLifecycle())
                            || ArtefactLifecycle.FAILED.equals(camel.getLifecycle())) {
                        removeFromProcessor(camel);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    }
                    break;
                case START: {
                    if (ArtefactLifecycle.FAILED.equals(camel.getLifecycle())) {
                        String message = "Cannot start a Route in a failing state: " + camel.getKey();
                        callback.addError(message);
                        callback.registerState(this, wrapper, ArtefactLifecycle.FATAL, message);
                        return true;
                    }
                    addToProcessor(camel);
                    callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                }
                    break;
                case STOP: {
                    removeFromProcessor(camel);
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                }
                    break;
            }
            return true;
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }
    }

    /**
     * Adds the to processor.
     *
     * @param camel the camel
     */
    private void addToProcessor(Camel camel) {
        // remove trace context to decouple async routes from the synchronizer job
        Context rootContext = Context.root();
        try (Scope scope = rootContext.makeCurrent()) {
            camelProcessor.onCreateOrUpdate(camel);
        }
    }

    /**
     * Removes the from processor.
     *
     * @param camel the camel
     */
    private void removeFromProcessor(Camel camel) {
        // remove trace context to decouple async routes from the synchronizer job
        Context rootContext = Context.root();
        try (Scope scope = rootContext.makeCurrent()) {
            getService().delete(camel);
            camelProcessor.onRemove(camel);
        }

    }

    /**
     * Cleanup.
     *
     * @param camel the camel
     */
    @Override
    public void cleanupImpl(Camel camel) {
        try {
            removeFromProcessor(camel);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, camel, ArtefactLifecycle.DELETED, e);
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
        return FILE_EXTENSION_CAMEL;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Camel.ARTEFACT_TYPE;
    }
}
