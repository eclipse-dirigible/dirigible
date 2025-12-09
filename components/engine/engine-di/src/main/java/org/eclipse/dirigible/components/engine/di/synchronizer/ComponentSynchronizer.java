/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.di.synchronizer;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.di.parser.ComponentMetadata;
import org.eclipse.dirigible.components.engine.di.parser.ComponentParser;
import org.eclipse.dirigible.components.engine.di.parser.ComponentRegister;
import org.eclipse.dirigible.components.engine.di.service.ComponentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class ComponentSynchronizer.
 */
@Component
@Order(SynchronizersOrder.COMPONENT)
public class ComponentSynchronizer extends BaseSynchronizer<org.eclipse.dirigible.components.engine.di.domain.Component, Long> {

    /** The Constant FILE_EXTENSION_COMPONENT. */
    public static final String FILE_EXTENSION_COMPONENT = "Component.ts";
    public static final String[] FILE_EXTENSIONS_COMPONENT = new String[] {"Component.ts", "Repository.ts", "Service.ts"};
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ComponentSynchronizer.class);
    /** The component service. */
    private final ComponentService componentService;
    private final ComponentParser componentParser = new ComponentParser();
    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new component synchronizer.
     *
     * @param componentService the component service
     */
    @Autowired
    public ComponentSynchronizer(ComponentService componentService) {
        this.componentService = componentService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return org.eclipse.dirigible.components.engine.di.domain.Component.ARTEFACT_TYPE.equals(type);
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
    protected List<org.eclipse.dirigible.components.engine.di.domain.Component> parseImpl(String location, byte[] content)
            throws ParseException {
        String source = new String(content, StandardCharsets.UTF_8);
        ComponentMetadata metadata = componentParser.parse(location, source);
        if (metadata.getComponentName() == null) {
            return new ArrayList<org.eclipse.dirigible.components.engine.di.domain.Component>();
        }

        org.eclipse.dirigible.components.engine.di.domain.Component component =
                new org.eclipse.dirigible.components.engine.di.domain.Component();
        component.setLocation(location);
        component.setName(Paths.get(location)
                               .getFileName()
                               .toString());
        component.setType(org.eclipse.dirigible.components.engine.di.domain.Component.ARTEFACT_TYPE);
        component.updateKey();
        component.setContent(source);
        try {
            org.eclipse.dirigible.components.engine.di.domain.Component maybe = getService().findByKey(component.getKey());
            if (maybe != null) {
                component.setId(maybe.getId());
            }
            component = getService().save(component);
            component.setContent(new String(content));
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("component: {}", component);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(component);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<org.eclipse.dirigible.components.engine.di.domain.Component, Long> getService() {
        return componentService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<org.eclipse.dirigible.components.engine.di.domain.Component> retrieve(String location) {
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
    public void setStatus(org.eclipse.dirigible.components.engine.di.domain.Component artefact, ArtefactLifecycle lifecycle, String error) {
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
    protected boolean completeImpl(TopologyWrapper<org.eclipse.dirigible.components.engine.di.domain.Component> wrapper,
            ArtefactPhase flow) {
        org.eclipse.dirigible.components.engine.di.domain.Component component = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (component.getLifecycle()
                             .equals(ArtefactLifecycle.NEW)
                        || component.getLifecycle()
                                    .equals(ArtefactLifecycle.FAILED)) {
                    ComponentRegister.addComponent(component.getLocation());
                    component.setRunning(true);
                    callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (component.getLifecycle()
                             .equals(ArtefactLifecycle.MODIFIED)
                        || component.getLifecycle()
                                    .equals(ArtefactLifecycle.FAILED)) {
                    ComponentRegister.addComponent(component.getLocation());
                    component.setRunning(true);
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                }
                break;
            case DELETE:
                if (component.getLifecycle()
                             .equals(ArtefactLifecycle.CREATED)
                        || component.getLifecycle()
                                    .equals(ArtefactLifecycle.UPDATED)
                        || component.getLifecycle()
                                    .equals(ArtefactLifecycle.FAILED)) {
                    ComponentRegister.removeComponent(component.getLocation());
                    component.setRunning(false);
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                }
                break;
            case START:
                if (ArtefactLifecycle.CREATED.equals(component.getLifecycle())
                        || ArtefactLifecycle.UPDATED.equals(component.getLifecycle())) {
                    if (component.getRunning() == null || !component.getRunning()) {
                        try {
                            ComponentRegister.addComponent(component.getLocation());
                            component.setRunning(true);
                        } catch (Exception e) {
                            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
                        }
                    }
                }
                break;
            case STOP:
        }

        return true;
    }

    /**
     * Prepare content.
     *
     * @param component the component
     * @return the string
     */
    public String prepareContent(org.eclipse.dirigible.components.engine.di.domain.Component component) {
        return component.getContent();
    }

    /**
     * Cleanup.
     *
     * @param component the component
     */
    @Override
    public void cleanupImpl(org.eclipse.dirigible.components.engine.di.domain.Component component) {
        try {
            ComponentRegister.removeComponent(component.getLocation());
            getService().delete(component);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, component, ArtefactLifecycle.DELETED, e);
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
     * Gets the file component.
     *
     * @return the file component
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_COMPONENT;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return org.eclipse.dirigible.components.engine.di.domain.Component.ARTEFACT_TYPE;
    }

    /**
     * Checks if is accepted.
     *
     * @param file the file
     * @param attrs the attrs
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(Path file, BasicFileAttributes attrs) {
        for (String extension : FILE_EXTENSIONS_COMPONENT) {
            if (file.toString()
                    .endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

}
