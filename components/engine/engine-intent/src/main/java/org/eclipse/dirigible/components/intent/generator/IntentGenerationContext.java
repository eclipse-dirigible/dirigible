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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;

/**
 * Per-generation call context handed to every {@link IntentTargetGenerator}. Carries the parsed
 * intent model, the target project paths inside the Dirigible repository, and the single write
 * entry point {@link #writeModelFile(String, String)}.
 *
 * <p>
 * Generation targets the <b>developer's workspace project</b> ({@code /users/<user>/<workspace>/
 * <project>}) - the intent is an authoring artifact like the {@code .edm}: the developer edits it
 * in its editor, clicks Generate, reviews the derived model files in the project, and publishes
 * everything together. Model files are written directly at the project root (next to the
 * {@code .intent} file) - the location every downstream consumer is proven to handle. NOT under
 * {@code gen/}: the model-to-code templates wipe that folder wholesale on every regeneration.
 *
 * <p>
 * All writes go through {@link #writeModelFile(String, String)}, which records the emitted file
 * names so {@link IntentGenerationService} can scrub files that a previous generation wrote but the
 * current one no longer produces.
 */
public final class IntentGenerationContext {

    /** Repository path of the target project root, e.g. {@code /users/admin/workspace/my-library}. */
    private final String projectRoot;

    /** The target project name. */
    private final String projectName;

    /**
     * The workspace the target project lives in (e.g. {@code workspace}); used to build cross-model
     * projection paths {@code /<workspace>/<project>/<model>.model}.
     */
    private final String workspaceName;

    /** Base-name fallback when the intent YAML declares no {@code name:} - the file's base name. */
    private final String fallbackName;

    private final IntentModel model;
    private final IRepository repository;

    /** The project's {@code .settings} (loaded or scaffolded by the service before generators run). */
    private IntentSettings settings;

    /** Bare file names written under {@link #projectRoot} during this generation pass. */
    private final Set<String> writtenFileNames = new LinkedHashSet<>();

    IntentGenerationContext(IntentModel model, String projectRoot, String projectName, String workspaceName, String fallbackName,
            IRepository repository) {
        this.model = model;
        this.projectRoot = projectRoot;
        this.projectName = projectName;
        this.workspaceName = workspaceName;
        this.fallbackName = fallbackName;
        this.repository = repository;
    }

    /**
     * Write (create or overwrite) a model file at the project root. This is the only write surface
     * generators may use; the emitted file name is recorded for the post-pass scrub of stale output.
     * Byte-identical content is not rewritten.
     *
     * @param fileName bare file name including extension, e.g. {@code library.edm}
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
     * Write a model file only if it does not already exist at the project root; an existing file (a
     * developer's customization) is left untouched. Either way the file name is recorded so the
     * post-pass scrub keeps it. Use this for a generate-once template whose content the developer is
     * expected to adapt and keep — the {@code .print} document template, mirroring the developer-owned
     * {@code .settings} file — where regenerating over an already-formatted artifact would destroy
     * those edits.
     *
     * @param fileName bare file name including extension, e.g. {@code SalesInvoice.print}
     * @param content the full file content to create when absent
     */
    public void writeModelFileIfAbsent(String fileName, String content) {
        String path = projectRoot + "/" + fileName;
        IResource existing = repository.getResource(path);
        if (!existing.exists()) {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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

    /** The workspace the target project lives in; may be null/blank in non-workspace callers. */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * Base-name fallback for single-file outputs when the YAML omits {@code name:}.
     *
     * @return the intent file's base name, never null
     */
    public String getFallbackName() {
        return fallbackName;
    }

    public IntentModel getModel() {
        return model;
    }

    /**
     * The project's settings (template recipes + per-artefact overrides); never null once generation
     * starts.
     */
    public IntentSettings getSettings() {
        return settings;
    }

    void setSettings(IntentSettings settings) {
        this.settings = settings;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public IRepository getRepository() {
        return repository;
    }
}
