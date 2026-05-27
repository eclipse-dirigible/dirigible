/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.synchronizer;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.domain.StartMode;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.eclipse.dirigible.components.engine.nativeapps.registry.NativeAppRegistry;
import org.eclipse.dirigible.components.engine.nativeapps.service.NativeAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

@Component
@Order(SynchronizersOrder.NATIVE_APP)
public class NativeAppSynchronizer extends BaseSynchronizer<NativeApp, Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppSynchronizer.class);

    private static final String FILE_EXTENSION = "." + NativeApp.ARTEFACT_TYPE;

    private final NativeAppService service;
    private final NativeAppRegistry registry;
    private final NativeAppProcessManager processManager;

    private SynchronizerCallback callback;

    public NativeAppSynchronizer(NativeAppService service, NativeAppRegistry registry, NativeAppProcessManager processManager) {
        this.service = service;
        this.registry = registry;
        this.processManager = processManager;
    }

    @Override
    public boolean isAccepted(String type) {
        return NativeApp.ARTEFACT_TYPE.equals(type);
    }

    @Override
    protected List<NativeApp> parseImpl(String location, byte[] content) throws ParseException {
        String contentString = new String(content, StandardCharsets.UTF_8);
        try {
            NativeApp app = NativeAppParser.parse(contentString);
            app.setLocation(location);
            app.setType(NativeApp.ARTEFACT_TYPE);
            if (app.getName() == null || app.getName()
                                            .isBlank()) {
                app.setName(FilenameUtils.getBaseName(location));
            }
            app.updateKey();
            NativeApp persisted = upsert(app);
            return List.of(persisted);
        } catch (RuntimeException ex) {
            String message = "Failed to parse native-app file [" + location + "]: " + ex.getMessage();
            LOGGER.error(message, ex);
            throw new ParseException(message, 0);
        }
    }

    private NativeApp upsert(NativeApp app) {
        NativeApp existing = service.findByKey(app.getKey());
        if (existing != null) {
            app.setId(existing.getId());
        }
        return service.save(app);
    }

    @Override
    public ArtefactService<NativeApp, Long> getService() {
        return service;
    }

    @Override
    public List<NativeApp> retrieve(String location) {
        return service.getAll();
    }

    @Override
    public void setStatus(NativeApp artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        service.save(artefact);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<NativeApp> wrapper, ArtefactPhase flow) {
        NativeApp app = wrapper.getArtefact();
        // Ensure the transient typed config tree is available for the process manager.
        NativeAppParser.rehydrateConfig(app);
        try {
            switch (flow) {
                case CREATE:
                    if (ArtefactLifecycle.NEW.equals(app.getLifecycle())) {
                        registry.register(app);
                        kickOffStart(app);
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    }
                    break;
                case UPDATE:
                    if (ArtefactLifecycle.MODIFIED.equals(app.getLifecycle())) {
                        if (app.getKind() == NativeAppKind.LOCAL) {
                            processManager.stop(app);
                        }
                        registry.register(app);
                        kickOffStart(app);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    }
                    if (ArtefactLifecycle.FAILED.equals(app.getLifecycle())) {
                        return false;
                    }
                    break;
                case DELETE:
                    if (ArtefactLifecycle.CREATED.equals(app.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(app.getLifecycle())
                            || ArtefactLifecycle.FAILED.equals(app.getLifecycle())) {
                        if (app.getKind() == NativeAppKind.LOCAL) {
                            processManager.stop(app);
                        }
                        registry.unregister(app);
                        service.delete(app);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    }
                    break;
                case START:
                    // LAZY apps are started on first proxy request only; never by the synchronizer's
                    // periodic START phase. Only ALWAYS apps get pre-emptively started here.
                    if (app.getKind() == NativeAppKind.LOCAL && app.getStartMode() == StartMode.ALWAYS && !processManager.isAlive(app)) {
                        processManager.startAsync(app);
                    }
                    break;
                case STOP:
                    if (app.getKind() == NativeAppKind.LOCAL) {
                        processManager.stop(app);
                    }
                    break;
                case PREPARE:
                    // No-op. Native apps have no prepare-phase work — start happens on CREATE
                    // (ALWAYS mode) or on first proxy request (LAZY mode).
                    break;
            }
            return true;
        } catch (Exception ex) {
            LOGGER.error("Native app synchronization failed for [{}]: {}", app.getName(), ex.getMessage(), ex);
            callback.addError(ex.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, ex);
            return false;
        }
    }

    private void kickOffStart(NativeApp app) {
        if (app.getKind() != NativeAppKind.LOCAL) {
            return;
        }
        if (app.getStartMode() == StartMode.ALWAYS) {
            // Best effort; don't block the synchronizer cycle on slow process boots.
            processManager.startAsync(app);
        }
    }

    @Override
    public void cleanupImpl(NativeApp app) {
        try {
            if (app.getKind() == NativeAppKind.LOCAL) {
                processManager.stop(app);
            }
            registry.unregister(app);
            service.delete(app);
        } catch (Exception ex) {
            LOGGER.error("Native app cleanup failed for [{}]: {}", app.getName(), ex.getMessage(), ex);
            callback.addError(ex.getMessage());
            callback.registerState(this, app, ArtefactLifecycle.DELETED, ex);
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
        return NativeApp.ARTEFACT_TYPE;
    }
}
