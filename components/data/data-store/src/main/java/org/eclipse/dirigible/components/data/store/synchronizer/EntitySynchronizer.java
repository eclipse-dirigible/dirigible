/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.synchronizer;

import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.data.store.DataStore;
import org.eclipse.dirigible.components.data.store.domain.Entity;
import org.eclipse.dirigible.components.data.store.parser.EntityParser;
import org.eclipse.dirigible.components.data.store.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class BpmnSynchronizer.
 */
@Component
@Order(SynchronizersOrder.ENTITY)
public class EntitySynchronizer extends BaseSynchronizer<Entity, Long> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(EntitySynchronizer.class);

    /** The Constant FILE_EXTENSION_BPMN. */
    public static final String FILE_EXTENSION_ENTITY = "Entity.ts";

    /** The entity service. */
    private final EntityService entityService;

    /** The object store. */
    private final DataStore dataStore;

    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new entity synchronizer.
     *
     * @param entityService the entity service
     * @param dataStore the data store
     */
    @Autowired
    public EntitySynchronizer(EntityService entityService, DataStore dataStore) {
        this.entityService = entityService;
        this.dataStore = dataStore;
    }

    /**
     * Gets the data store.
     *
     * @return the data store
     */
    public DataStore getDataStore() {
        return dataStore;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Entity.ARTEFACT_TYPE.equals(type);
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
    protected List<Entity> parseImpl(String location, byte[] content) throws ParseException {
        Entity entity = new Entity();
        entity.setLocation(location);
        entity.setName(Paths.get(location)
                            .getFileName()
                            .toString());
        entity.setType(Entity.ARTEFACT_TYPE);
        entity.updateKey();
        entity.setContent(new String(content));
        try {
            Entity maybe = getService().findByKey(entity.getKey());
            if (maybe != null) {
                entity.setId(maybe.getId());
            }
            entity = getService().save(entity);
            entity.setContent(new String(content));
            EntityParser parser = new EntityParser();
            parser.parse(location, new String(content)); // register the Entity metadata at this point
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("entity: {}", entity);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(entity);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Entity, Long> getService() {
        return entityService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Entity> retrieve(String location) {
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
    public void setStatus(Entity artefact, ArtefactLifecycle lifecycle, String error) {
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
    protected boolean completeImpl(TopologyWrapper<Entity> wrapper, ArtefactPhase flow) {
        Entity entity = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (entity.getLifecycle()
                          .equals(ArtefactLifecycle.NEW)) {
                    dataStore.addMapping(entity.getLocation(), prepareContent(entity));
                    entity.setRunning(true);
                    callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (entity.getLifecycle()
                          .equals(ArtefactLifecycle.MODIFIED)) {
                    dataStore.removeMapping(entity.getLocation());
                    dataStore.addMapping(entity.getLocation(), prepareContent(entity));
                    entity.setRunning(true);
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                }
                if (entity.getLifecycle()
                          .equals(ArtefactLifecycle.FAILED)) {
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    return false;
                }
                break;
            case DELETE:
                if (entity.getLifecycle()
                          .equals(ArtefactLifecycle.CREATED)
                        || entity.getLifecycle()
                                 .equals(ArtefactLifecycle.UPDATED)
                        || entity.getLifecycle()
                                 .equals(ArtefactLifecycle.FAILED)) {
                    dataStore.removeMapping(entity.getLocation());
                    entity.setRunning(false);
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                }
                break;
            case START:
                if (ArtefactLifecycle.CREATED.equals(entity.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(entity.getLifecycle())) {
                    if (entity.getRunning() == null || !entity.getRunning()) {
                        try {
                            dataStore.addMapping(entity.getLocation(), prepareContent(entity));
                            entity.setRunning(true);
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
     * @param entity the entity
     * @return the string
     */
    public String prepareContent(Entity entity) {
        return entity.getContent();
    }

    /**
     * Cleanup.
     *
     * @param entity the entity
     */
    @Override
    public void cleanupImpl(Entity entity) {
        try {
            dataStore.removeMapping(entity.getLocation());
            getService().delete(entity);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, entity, ArtefactLifecycle.DELETED, e);
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
     * Gets the file entity.
     *
     * @return the file entity
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_ENTITY;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Entity.ARTEFACT_TYPE;
    }

    /**
     * Finishing.
     */
    @Override
    public void finishing() {
        dataStore.recreate();
    }

}
