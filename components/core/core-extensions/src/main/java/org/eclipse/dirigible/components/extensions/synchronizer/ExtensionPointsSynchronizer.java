/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.synchronizer;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.extensions.domain.ExtensionPoint;
import org.eclipse.dirigible.components.extensions.service.ExtensionPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class ExtensionPointsSynchronizer.
 */
@Component
@Order(SynchronizersOrder.EXTENSIONPOINT)
public class ExtensionPointsSynchronizer extends BaseSynchronizer<ExtensionPoint, Long> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ExtensionPointsSynchronizer.class);

    /** The Constant FILE_EXTENSION_EXTENSIONPOINT. */
    private static final String FILE_EXTENSION_EXTENSIONPOINT = ".extensionpoint";

    /** The extension point service. */
    private final ExtensionPointService extensionPointService;

    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new extension points synchronizer.
     *
     * @param extensionPointService the extension point service
     */
    @Autowired
    public ExtensionPointsSynchronizer(ExtensionPointService extensionPointService) {
        this.extensionPointService = extensionPointService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return ExtensionPoint.ARTEFACT_TYPE.equals(type);
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
    protected List<ExtensionPoint> parseImpl(String location, byte[] content) throws ParseException {
        ExtensionPoint extensionPoint = JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), ExtensionPoint.class);
        Configuration.configureObject(extensionPoint);
        extensionPoint.setLocation(location);
        extensionPoint.setType(ExtensionPoint.ARTEFACT_TYPE);
        extensionPoint.updateKey();
        try {
            ExtensionPoint maybe = getService().findByKey(extensionPoint.getKey());
            if (maybe != null) {
                extensionPoint.setId(maybe.getId());
            }
            extensionPoint = getService().save(extensionPoint);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("extension point: {}", extensionPoint);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(extensionPoint);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<ExtensionPoint, Long> getService() {
        return extensionPointService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<ExtensionPoint> retrieve(String location) {
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
    public void setStatus(ExtensionPoint artefact, ArtefactLifecycle lifecycle, String error) {
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
    protected boolean completeImpl(TopologyWrapper<ExtensionPoint> wrapper, ArtefactPhase flow) {
        ExtensionPoint extensionPoint = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(extensionPoint.getLifecycle())) {
                    callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(extensionPoint.getLifecycle())) {
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                }
                if (ArtefactLifecycle.FAILED.equals(extensionPoint.getLifecycle())) {
                    return false;
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(extensionPoint.getLifecycle())
                        || ArtefactLifecycle.UPDATED.equals(extensionPoint.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(extensionPoint.getLifecycle())) {
                    try {
                        getService().delete(extensionPoint);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, e);
                    }
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
     * @param extensionPoint the extension point
     */
    @Override
    public void cleanupImpl(ExtensionPoint extensionPoint) {
        try {
            getService().delete(extensionPoint);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, extensionPoint, ArtefactLifecycle.DELETED, e.getMessage(), e);
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
        return FILE_EXTENSION_EXTENSIONPOINT;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return ExtensionPoint.ARTEFACT_TYPE;
    }

}
