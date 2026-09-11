/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.synchronizer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.listeners.domain.Listener;
import org.eclipse.dirigible.components.listeners.domain.ListenerKind;
import org.eclipse.dirigible.components.listeners.parser.ListenerMetadata;
import org.eclipse.dirigible.components.listeners.parser.ListenerParser;
import org.eclipse.dirigible.components.listeners.service.ListenerService;
import org.eclipse.dirigible.components.listeners.service.ListenersManager;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class ListenerSynchronizer.
 */
@Component
@Order(SynchronizersOrder.LISTENER)
public class ListenerSynchronizer extends MultitenantBaseSynchronizer<Listener, Long> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ListenerSynchronizer.class);

    /** The Constant FILE_EXTENSION_LISTENER. */
    private static final String FILE_EXTENSION_LISTENER = ".listener";

    public static final String[] FILE_EXTENSIONS_LISTENER = new String[] {".listener", "listener.ts"};

    /** The callback. */
    private SynchronizerCallback callback;

    /** The listener service. */
    @Autowired
    private ListenerService listenerService;

    /** The Scheduler manager. */
    @Autowired
    private ListenersManager listenersManager;

    private final ListenerParser listenerParser = new ListenerParser();

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Listener.ARTEFACT_TYPE.equals(type);
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
    protected List<Listener> parseImpl(String location, byte[] content) throws ParseException {
        Listener listener = null;
        String source = new String(content, StandardCharsets.UTF_8);
        if (location.endsWith(FILE_EXTENSION_LISTENER)) {
            listener = JsonHelper.fromJson(source, Listener.class);
        } else {
            ListenerMetadata metadata = listenerParser.parse(location, source);
            if (metadata.getName() == null) {
                return new ArrayList<Listener>();
            }
            listener = new Listener();
            listener.setName(metadata.getName());
            listener.setKind(resolveListenerKind(metadata.getKind()));
            String handler = location;
            if (handler.startsWith(IRepositoryStructure.SEPARATOR)) {
                handler = handler.substring(1);
            }
            listener.setHandler(handler);
        }
        Configuration.configureObject(listener);
        listener.setLocation(location);
        listener.setType(Listener.ARTEFACT_TYPE);
        listener.updateKey();
        try {
            Listener maybe = getService().findByKey(listener.getKey());
            if (maybe != null) {
                listener.setId(maybe.getId());
            }
            listener = getService().save(listener);
        } catch (Exception e) {
            logger.error("Failed to save listener [{}], content:\n{}", listener, new String(content), e);
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(listener);
    }

    private ListenerKind resolveListenerKind(String kind) {
        if (kind == null) {
            return ListenerKind.QUEUE;
        }
        if (kind.toLowerCase()
                .equals("q")
                || kind.toLowerCase()
                       .equals("queue")) {
            return ListenerKind.QUEUE;
        }
        if (kind.toLowerCase()
                .equals("t")
                || kind.toLowerCase()
                       .equals("topic")) {
            return ListenerKind.TOPIC;
        }
        return ListenerKind.QUEUE;
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Listener, Long> getService() {
        return listenerService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Listener> retrieve(String location) {
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
    public void setStatus(Listener artefact, ArtefactLifecycle lifecycle, String error) {
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
    protected boolean completeImpl(TopologyWrapper<Listener> wrapper, ArtefactPhase flow) {
        Listener listener = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(listener.getLifecycle())) {
                    try {
                        listenersManager.startListener(listener);
                        listener.setRunning(true);
                        getService().save(listener);
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    } catch (Exception e) {
                        // FAILED, not CREATED: nothing started, and only FAILED keeps the listener
                        // eligible for the START phase of this pass and of every later one (#7248).
                        // Returning false hands it to the in-pass topological retry as well.
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
                        return false;
                    }
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(listener.getLifecycle())) {
                    try {
                        listenersManager.stopListener(listener);
                        listener.setRunning(false);
                        getService().save(listener);
                        listenersManager.startListener(listener);
                        listener.setRunning(true);
                        getService().save(listener);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, e);
                    }
                }
                if (ArtefactLifecycle.FAILED.equals(listener.getLifecycle())) {
                    return false;
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(listener.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(listener.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(listener.getLifecycle())) {
                    try {
                        listenersManager.stopListener(listener);
                        listener.setRunning(false);
                        getService().delete(listener);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, e);
                    }
                }
                break;
            case START:
                // A FAILED listener is retried, never promoted to FATAL (#7248). A subscription fails
                // for reasons that heal later - the embedded broker still taking its store lease at
                // boot, the handler published on a later pass - and FATAL stripped the artefact from
                // every later pass, so a topic subscription lost to a boot race stayed lost, with
                // every message to that destination discarded, until the file's bytes changed.
                // Gated on FAILED as well as on running: the artefact completes once per tenant on one
                // shared object, and only the lifecycle is reset between tenants - so the first tenant
                // to subscribe flips running and would otherwise skip every tenant after it. The
                // manager is idempotent per tenant, so a tenant already subscribed is a no-op.
                if (ArtefactLifecycle.FAILED.equals(listener.getLifecycle()) || listener.getRunning() == null || !listener.getRunning()) {
                    try {
                        listenersManager.startListener(listener);
                        listener.setRunning(true);
                        getService().save(listener);
                        if (ArtefactLifecycle.FAILED.equals(listener.getLifecycle())) {
                            callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                        }
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
                        return false;
                    }
                }
                break;
            case STOP:
                if (listener.getRunning()) {
                    try {
                        listenersManager.stopListener(listener);
                        listener.setRunning(false);
                        getService().save(listener);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
                    }
                }
                break;
        }

        return true;
    }

    /**
     * Cleanup.
     *
     * @param listener the listener
     */
    @Override
    public void cleanupImpl(Listener listener) {
        try {
            listenersManager.stopListener(listener);
            getService().delete(listener);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, listener, ArtefactLifecycle.DELETED, e);
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
        return FILE_EXTENSION_LISTENER;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Listener.ARTEFACT_TYPE;
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
        for (String extension : FILE_EXTENSIONS_LISTENER) {
            if (file.toString()
                    .toLowerCase()
                    .endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

}
