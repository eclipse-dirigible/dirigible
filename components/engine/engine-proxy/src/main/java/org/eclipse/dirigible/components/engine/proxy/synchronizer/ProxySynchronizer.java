/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.proxy.synchronizer;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.proxy.ProxyRegistry;
import org.eclipse.dirigible.components.engine.proxy.domain.Proxy;
import org.eclipse.dirigible.components.engine.proxy.service.ProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

@Component
@Order(SynchronizersOrder.PROXY)
public class ProxySynchronizer extends BaseSynchronizer<Proxy, Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxySynchronizer.class);

    private static final String FILE_EXTENSION = "." + Proxy.ARTEFACT_TYPE;

    private final ProxyService proxyService;
    private final ProxyRegistry proxyRegistry;

    private SynchronizerCallback callback;

    @Autowired
    public ProxySynchronizer(ProxyService proxyService, ProxyRegistry proxyRegistry) {
        this.proxyService = proxyService;
        this.proxyRegistry = proxyRegistry;
    }

    @Override
    public boolean isAccepted(String type) {
        return Proxy.ARTEFACT_TYPE.equals(type);
    }

    @Override
    protected List<Proxy> parseImpl(String location, byte[] content) throws ParseException {
        String contentString = new String(content, StandardCharsets.UTF_8);
        try {
            Proxy proxy = JsonHelper.fromJson(contentString, Proxy.class);
            Configuration.configureObject(proxy);

            proxy.setLocation(location);
            proxy.setType(Proxy.ARTEFACT_TYPE);

            proxy.updateKey();

            Proxy upsertedProxy = upsertProxy(proxy);
            return List.of(upsertedProxy);

        } catch (RuntimeException ex) {
            String errorMessage = "Failed to parse file with location [" + location + "] with content: " + contentString;
            LOGGER.error(errorMessage, ex);
            throw new ParseException(errorMessage, 0);
        }
    }

    private Proxy upsertProxy(Proxy proxy) {
        Proxy maybe = getService().findByKey(proxy.getKey());
        if (maybe != null) {
            proxy.setId(maybe.getId());
        }
        return getService().save(proxy);
    }

    @Override
    public ArtefactService<Proxy, Long> getService() {
        return proxyService;
    }

    @Override
    public List<Proxy> retrieve(String location) {
        return getService().getAll();
    }

    @Override
    public void setStatus(Proxy artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<Proxy> wrapper, ArtefactPhase flow) {
        try {
            Proxy proxy = wrapper.getArtefact();

            switch (flow) {
                case CREATE:
                    if (proxy.getLifecycle()
                             .equals(ArtefactLifecycle.NEW)) {
                        proxyRegistry.register(proxy);
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    }
                    break;
                case UPDATE:
                    if (proxy.getLifecycle()
                             .equals(ArtefactLifecycle.MODIFIED)) {
                        proxyRegistry.register(proxy);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    }
                    if (proxy.getLifecycle()
                             .equals(ArtefactLifecycle.FAILED)) {
                        return false;
                    }
                    break;
                case DELETE:
                    if (proxy.getLifecycle()
                             .equals(ArtefactLifecycle.CREATED)
                            || proxy.getLifecycle()
                                    .equals(ArtefactLifecycle.UPDATED)
                            || proxy.getLifecycle()
                                    .equals(ArtefactLifecycle.FAILED)) {
                        proxyRegistry.unregister(proxy);
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

    @Override
    public void cleanupImpl(Proxy proxy) {
        try {
            getService().delete(proxy);
            proxyRegistry.unregister(proxy);
        } catch (Exception ex) {
            String errorMessage = "Failed to delete proxy [" + proxy + "]. Error: " + ex.getMessage();
            LOGGER.error(errorMessage, ex);

            callback.addError(errorMessage);
            callback.registerState(this, proxy, ArtefactLifecycle.DELETED, ex);
        }
    }

    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION;
    }

    @Override
    public String getArtefactType() {
        return Proxy.ARTEFACT_TYPE;
    }

}
