/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.listeners.domain.Listener;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Class ListenersManager.
 */
@Component("ListenersManager")
public class ListenersManager {

    /** The listeners. */
    static final Map<ListenerDescriptor, ListenerManager> LISTENERS = Collections.synchronizedMap(new HashMap<>());

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenersManager.class);

    /** The repository. */
    private final IRepository repository;

    /** The message listener manager factory. */
    private final ListenerManagerFactory messageListenerManagerFactory;

    /** The listener creator. */
    private final ListenerCreator listenerCreator;

    /** The connection the artefacts share, closed once every listener is stopped. */
    private final SharedListenerConnection sharedConnection;

    /** The listener artefacts. */
    private final ListenerService listenerService;

    /** The tenant scope a tenant's listeners are started in. */
    private final TenantContext tenantContext;

    /**
     * Instantiates a new listeners manager.
     *
     * @param repository the repository
     * @param messageListenerManagerFactory the message listener manager factory
     * @param listenerCreator the listener creator
     * @param sharedConnection the connection the artefacts share in the shared mode
     * @param listenerService the listener artefacts
     * @param tenantContext the tenant scope a tenant's listeners are started in
     */
    @Autowired
    public ListenersManager(IRepository repository, ListenerManagerFactory messageListenerManagerFactory, ListenerCreator listenerCreator,
            SharedListenerConnection sharedConnection, ListenerService listenerService, TenantContext tenantContext) {
        this.repository = repository;
        this.messageListenerManagerFactory = messageListenerManagerFactory;
        this.listenerCreator = listenerCreator;
        this.sharedConnection = sharedConnection;
        this.listenerService = listenerService;
        this.tenantContext = tenantContext;
    }

    /**
     * Start listener.
     *
     * @param listenerEntity the listener
     */
    public void startListener(Listener listenerEntity) {
        ListenerDescriptor listenerDescriptor = listenerCreator.fromEntity(listenerEntity);
        if (LISTENERS.containsKey(listenerDescriptor)) {
            if (DestinationNameManager.isGlobal(listenerEntity.getName())) {
                // Every provisioned tenant reaches this artefact, and a global destination resolves to
                // one physical name for all of them - so the tenants after the first share the single
                // subscription that is already open. Expected, not a misconfiguration.
                LOGGER.debug("Listener [{}] is bound to a global destination and is already running for the whole deployment.",
                        listenerDescriptor);
            } else {
                LOGGER.warn("Message consumer for listener [{}] already running!", listenerDescriptor);
            }
            return;

        }
        start(listenerDescriptor);
    }

    /**
     * Registers a listener of a suspended tenant without starting it: it is refused exactly as
     * {@link #startListener} refuses it, so a listener whose handler is missing stays failed, and
     * nothing is subscribed. Resuming the tenant starts it ({@link #startTenantListeners}).
     *
     * @param listenerEntity the listener
     */
    public void registerListener(Listener listenerEntity) {
        ListenerDescriptor listenerDescriptor = listenerCreator.fromEntity(listenerEntity);
        refuseMissingHandler(listenerDescriptor);
        LOGGER.info("Listener [{}] registered and not started - its tenant is suspended.", listenerDescriptor);
    }

    /**
     * Starts the tenant's consumers of every listener artefact - the resume of a suspended tenant,
     * whose listeners {@link #stopTenantListeners} stopped. Messages that arrived meanwhile stayed on
     * the broker and are consumed now. Idempotent: a consumer already running is left alone. A listener
     * bound to a global destination is skipped - its one subscription serves the whole deployment and
     * was never stopped - and so is one the synchronizer holds failed: its retry starts it for every
     * tenant once it can be started.
     *
     * @param tenantId the tenant id
     * @return how many listeners were started
     * @throws IllegalStateException naming every listener that could not be started, once all the
     *         others are
     */
    public int startTenantListeners(String tenantId) {
        List<Listener> listeners = listenerService.getAll()
                                                  .stream()
                                                  .filter(listener -> !DestinationNameManager.isGlobal(listener.getName()))
                                                  .filter(listener -> ArtefactLifecycle.FAILED != listener.getLifecycle()
                                                          && ArtefactLifecycle.FATAL != listener.getLifecycle())
                                                  .toList();
        List<String> failures = new ArrayList<>();
        int started = tenantContext.execute(tenantId, () -> {
            int count = 0;
            for (Listener listener : listeners) {
                ListenerDescriptor listenerDescriptor = listenerCreator.fromEntity(listener);
                if (LISTENERS.containsKey(listenerDescriptor)) {
                    continue;
                }
                try {
                    start(listenerDescriptor);
                    count++;
                } catch (RuntimeException ex) {
                    LOGGER.warn("Could not start listener [{}] of tenant [{}]: {}", listenerDescriptor, LogSanitizer.sanitize(tenantId),
                            ex.getMessage(), ex);
                    failures.add(listener.getName() + ": " + ex.getMessage());
                }
            }
            return count;
        });
        if (!failures.isEmpty()) {
            throw new IllegalStateException("Could not start the listener(s) of tenant [" + tenantId + "]: " + String.join("; ", failures));
        }
        return started;
    }

    private void start(ListenerDescriptor listenerDescriptor) {
        refuseMissingHandler(listenerDescriptor);
        ListenerManager listenerManager = messageListenerManagerFactory.create(listenerDescriptor);
        listenerManager.startListener();

        LISTENERS.put(listenerDescriptor, listenerManager);
        LOGGER.info("Listener [{}] started.", listenerDescriptor);
    }

    private void refuseMissingHandler(ListenerDescriptor listenerDescriptor) {
        if (isMissingHandler(listenerDescriptor)) {
            // Thrown, not logged and swallowed: a normal return had the synchronizer record the
            // artefact as CREATED and running with nothing subscribed (#7248). The handler may
            // simply be published on a later pass, and FAILED is what keeps the listener retried.
            throw new IllegalStateException("Listener " + listenerDescriptor + " cannot be started, because the handler ["
                    + listenerDescriptor.getHandlerPath() + "] does not exist");
        }
    }

    /**
     * Checks if is missing handler.
     *
     * @param listenerDescriptor the listener
     * @return true, if is missing handler
     */
    private boolean isMissingHandler(ListenerDescriptor listenerDescriptor) {
        IResource resource = repository.getResource(
                IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepositoryStructure.SEPARATOR + listenerDescriptor.getHandlerPath());
        return !resource.exists();
    }

    /**
     * Stop listeners.
     */
    public synchronized void stopListeners() {
        LOGGER.info("Stopping all background listeners...");

        LISTENERS.forEach((l, m) -> {
            if (null != m) {
                m.stopListener();
            }
        });
        LISTENERS.clear();
        sharedConnection.close();
    }

    /**
     * Stop listener.
     *
     * @param listenerEntity the listener
     */
    /**
     * Stops every listener subscribed to one of the tenant's destinations ({@code <tenantId>###<name>})
     * - the tenant's deprovisioning or suspension. The destinations themselves stay on the broker.
     *
     * @param tenantId the tenant id
     * @return how many listeners were stopped
     */
    public int stopTenantListeners(String tenantId) {
        String prefix = tenantId + "###";
        List<ListenerManager> stopped = new ArrayList<>();
        synchronized (LISTENERS) {
            Iterator<Map.Entry<ListenerDescriptor, ListenerManager>> entries = LISTENERS.entrySet()
                                                                                        .iterator();
            while (entries.hasNext()) {
                Map.Entry<ListenerDescriptor, ListenerManager> entry = entries.next();
                if (entry.getKey()
                         .getDestination()
                         .startsWith(prefix)) {
                    stopped.add(entry.getValue());
                    entries.remove();
                }
            }
        }
        for (ListenerManager manager : stopped) {
            if (manager != null) {
                manager.stopListener();
            }
        }
        return stopped.size();
    }

    public void stopListener(Listener listenerEntity) {
        ListenerDescriptor listenerDescriptor = listenerCreator.fromEntity(listenerEntity);
        ListenerManager listenerManager = LISTENERS.get(listenerDescriptor);
        if (listenerManager != null) {
            listenerManager.stopListener();
            LISTENERS.remove(listenerDescriptor);
        } else {
            LOGGER.warn("There is NO configured listener for [{}]", listenerDescriptor);
        }
    }

}
