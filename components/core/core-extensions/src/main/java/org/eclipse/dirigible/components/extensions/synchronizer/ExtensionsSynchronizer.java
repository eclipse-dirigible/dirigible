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

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.extensions.domain.Extension;
import org.eclipse.dirigible.components.extensions.service.ExtensionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

/**
 * The Class ExtensionsSynchronizer.
 */
@Component
@Order(SynchronizersOrder.EXTENSION)
public class ExtensionsSynchronizer extends BaseSynchronizer<Extension, Long> {

    /** The Constant FILE_EXTENSION_EXTENSION. */
    public static final String FILE_EXTENSION_EXTENSION = ".extension";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ExtensionsSynchronizer.class);
    /** The extension service. */
    private final ExtensionService extensionService;

    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new extensions synchronizer.
     *
     * @param extensionService the extension service
     */
    @Autowired
    public ExtensionsSynchronizer(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Extension.ARTEFACT_TYPE.equals(type);
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
    protected List<Extension> parseImpl(String location, byte[] content) throws ParseException {
        Extension extension = JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), Extension.class);
        Configuration.configureObject(extension);
        extension.setLocation(location);
        extension.setName(FilenameUtils.getBaseName(location));
        extension.setType(Extension.ARTEFACT_TYPE);
        extension.updateKey();
        try {
            Extension maybe = getService().findByKey(extension.getKey());
            if (maybe != null) {
                extension.setId(maybe.getId());
            }
            extension = getService().save(extension);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("extension: {}", extension);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(extension);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Extension, Long> getService() {
        return extensionService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Extension> retrieve(String location) {
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
    public void setStatus(Extension artefact, ArtefactLifecycle lifecycle, String error) {
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
    protected boolean completeImpl(TopologyWrapper<Extension> wrapper, ArtefactPhase flow) {
        Extension extension = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(extension.getLifecycle())) {
                    callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(extension.getLifecycle())) {
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                }
                if (ArtefactLifecycle.FAILED.equals(extension.getLifecycle())) {
                    return false;
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(extension.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(extension.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(extension.getLifecycle())) {
                    try {
                        getService().delete(extension);
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
     * @param extension the extension
     */
    @Override
    public void cleanupImpl(Extension extension) {
        try {
            getService().delete(extension);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, extension, ArtefactLifecycle.DELETED, e.getMessage(), e);
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
        return FILE_EXTENSION_EXTENSION;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Extension.ARTEFACT_TYPE;
    }

}
