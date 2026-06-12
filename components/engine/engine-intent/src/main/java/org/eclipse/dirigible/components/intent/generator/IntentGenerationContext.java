/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.dirigible.components.intent.domain.Intent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;

/**
 * Per-regeneration call context handed to every {@link IntentTargetGenerator}. Carries the parsed
 * intent, the originating artefact (for location / key metadata), the project paths inside the
 * Dirigible repository, and the single write entry point {@link #writeModelFile(String, String)}.
 *
 * <p>
 * Generators write model files directly at the <b>project root</b> (next to {@code app.intent}) -
 * the location every downstream consumer is proven to handle: the platform fixtures keep
 * hand-authored {@code .edm} / {@code .bpmn} / {@code .form} files there, and the model-to-code
 * templates resolve their output paths relative to it. NOT under {@code gen/}: the templates wipe
 * that folder wholesale on every model-to-code regeneration, so intent output placed there would
 * not survive. A dedicated subfolder (e.g. {@code models/}) is a possible future refinement once
 * the template engine's handling of model files in subfolders is verified. All writes go through
 * {@link #writeModelFile(String, String)}, which records the emitted file names so
 * {@link IntentRegenerationService} can scrub files that a previous regeneration wrote but the
 * current one no longer produces.
 */
public final class IntentGenerationContext {

    /**
     * Full repository path of the project root, e.g. {@code /registry/public/orders}. Artefact
     * locations are registry-relative, so the registry prefix is applied by
     * {@link IntentRegenerationService} before this context is built.
     */
    private final String projectRoot;

    /** Project name - the first path segment of the intent's registry-relative location. */
    private final String projectName;

    private final Intent intent;
    private final IntentModel model;
    private final IRepository repository;

    /** Bare file names written under {@link #projectRoot} during this regeneration pass. */
    private final Set<String> writtenFileNames = new LinkedHashSet<>();

    IntentGenerationContext(Intent intent, IntentModel model, String projectRoot, String projectName, IRepository repository) {
        this.intent = intent;
        this.model = model;
        this.projectRoot = projectRoot;
        this.projectName = projectName;
        this.repository = repository;
    }

    /**
     * Write (create or overwrite) a model file at the project root. This is the only write surface
     * generators may use; the emitted file name is recorded for the post-pass scrub of stale output.
     * Byte-identical content is not rewritten - the synchronizer re-parses every intent each cycle, so
     * an unconditional write would re-trigger the registry file watcher on every cycle and turn the
     * scheduled synchronization into a perpetual no-op loop.
     *
     * @param fileName bare file name including extension, e.g. {@code orders.edm}
     * @param content the full file content
     */
    public void writeModelFile(String fileName, String content) {
        String path = projectRoot + "/" + fileName;
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            if (!Arrays.equals(existing.getContent(), bytes)) {
                existing.setContent(bytes);
            }
        } else {
            repository.createResource(path, bytes);
        }
        writtenFileNames.add(fileName);
    }

    /**
     * The bare file names emitted through {@link #writeModelFile(String, String)} so far.
     *
     * @return an unmodifiable view of the written file names
     */
    public Set<String> getWrittenFileNames() {
        return Collections.unmodifiableSet(writtenFileNames);
    }

    public String getProjectName() {
        return projectName;
    }

    public Intent getIntent() {
        return intent;
    }

    public IntentModel getModel() {
        return model;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public IRepository getRepository() {
        return repository;
    }
}
