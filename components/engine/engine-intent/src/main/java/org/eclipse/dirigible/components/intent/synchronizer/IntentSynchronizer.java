/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.synchronizer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.intent.domain.Intent;
import org.eclipse.dirigible.components.intent.generator.IntentRegenerationService;
import org.eclipse.dirigible.components.intent.service.IntentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Synchronizer for {@code .intent} files. Sits at the top of {@link SynchronizersOrder} so its
 * regenerated {@code gen/} output participates in the next reconciliation cycle ahead of the
 * downstream entity / schema / BPMN / form synchronizers.
 *
 * <p>
 * The synchronizer itself owns no runtime state - it persists the intent's JSON payload and lets
 * {@link IntentRegenerationService} produce / refresh the gen/ files in {@link #finishing()}.
 * Lifecycle transitions are pure book-keeping; nothing to start, nothing to stop.
 */
@Component
@Order(SynchronizersOrder.INTENT)
public class IntentSynchronizer extends BaseSynchronizer<Intent, Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentSynchronizer.class);

    /** File extension recognized by this synchronizer. */
    public static final String FILE_EXTENSION_INTENT = ".intent";

    private final IntentService intentService;
    private final IntentRegenerationService regenerationService;
    private SynchronizerCallback callback;

    /**
     * Intents that changed in the current cycle and need their gen/ output refreshed in
     * {@link #finishing()}. Keyed by location to coalesce repeated parses of the same file.
     */
    private final Map<String, Intent> dirty = new LinkedHashMap<>();

    public IntentSynchronizer(IntentService intentService, IntentRegenerationService regenerationService) {
        this.intentService = intentService;
        this.regenerationService = regenerationService;
    }

    @Override
    public boolean isAccepted(String type) {
        return Intent.ARTEFACT_TYPE.equals(type);
    }

    @Override
    public boolean isAccepted(Path file, BasicFileAttributes attrs) {
        return file.toString()
                   .toLowerCase()
                   .endsWith(FILE_EXTENSION_INTENT);
    }

    @Override
    protected List<Intent> parseImpl(String location, byte[] content) throws ParseException {
        String yaml = new String(content, StandardCharsets.UTF_8);
        Intent intent = new Intent();
        intent.setLocation(location);
        intent.setType(Intent.ARTEFACT_TYPE);
        intent.setName(deriveName(location));
        intent.setContent(yaml);
        intent.updateKey();
        try {
            Intent existing = intentService.findByKey(intent.getKey());
            if (existing != null) {
                intent.setId(existing.getId());
            }
            intent = intentService.save(intent);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to save intent at [{}]", location, e);
            throw new ParseException(e.getMessage(), 0);
        }
        synchronized (dirty) {
            dirty.put(location, intent);
        }
        return List.of(intent);
    }

    @Override
    public ArtefactService<Intent, Long> getService() {
        return intentService;
    }

    @Override
    public List<Intent> retrieve(String location) {
        return intentService.findByLocation(location);
    }

    @Override
    public void setStatus(Intent artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        intentService.save(artefact);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<Intent> wrapper, ArtefactPhase flow) {
        Intent intent = wrapper.getArtefact();
        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(intent.getLifecycle())) {
                    callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(intent.getLifecycle())) {
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(intent.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(intent.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(intent.getLifecycle())) {
                    intentService.delete(intent);
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                }
                break;
            case START:
            case STOP:
            default:
                break;
        }
        return true;
    }

    @Override
    public void cleanupImpl(Intent intent) {
        try {
            intentService.delete(intent);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to delete intent [{}]", intent.getLocation(), e);
            callback.addError(e.getMessage());
            callback.registerState(this, intent, ArtefactLifecycle.DELETED, e);
        }
    }

    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_INTENT;
    }

    @Override
    public String getArtefactType() {
        return Intent.ARTEFACT_TYPE;
    }

    /**
     * Run the regeneration pass for every intent that changed in the current cycle. Files written under
     * {@code gen/} become visible to the downstream entity / schema / BPMN / form synchronizers in the
     * next reconciliation cycle (the orchestrator's file walk runs once at the start of each cycle, so
     * newly-written files inside the same cycle are missed by design).
     */
    @Override
    public void finishing() {
        List<Intent> snapshot;
        synchronized (dirty) {
            if (dirty.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(dirty.values());
            dirty.clear();
        }
        for (Intent intent : snapshot) {
            try {
                regenerationService.regenerate(intent);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to regenerate gen/ for intent [{}]", intent.getLocation(), e);
            }
        }
    }

    /**
     * Derive a logical name from the file location: strip the path and the {@code .intent} extension,
     * returning the base name. Used when the intent JSON itself omits the name field.
     */
    private static String deriveName(String location) {
        int lastSlash = location.lastIndexOf('/');
        String fileName = lastSlash < 0 ? location : location.substring(lastSlash + 1);
        if (fileName.toLowerCase()
                    .endsWith(FILE_EXTENSION_INTENT)) {
            fileName = fileName.substring(0, fileName.length() - FILE_EXTENSION_INTENT.length());
        }
        return fileName;
    }
}
