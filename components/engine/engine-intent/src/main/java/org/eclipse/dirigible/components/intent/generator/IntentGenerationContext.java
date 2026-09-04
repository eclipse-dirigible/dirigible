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
     * The workspace the target project lives in (e.g. {@code workspace}) - generation-target identity.
     * No generator reads it any more: it used to be baked into the cross-model projection path, which
     * made a committed model file depend on whose IDE produced it (#6423).
     */
    private final String workspaceName;

    /** Base-name fallback when the intent YAML declares no {@code name:} - the file's base name. */
    private final String fallbackName;

    private final IntentModel model;
    private final IRepository repository;

    /**
     * A validation-only pass: generators run and report exactly as they would for a real Generate, but
     * nothing is written and nothing is scrubbed (dirigible #6956). Reads stay live - what already
     * exists still decides what a generator would do - so the issues collected are the ones a real pass
     * would raise.
     */
    private final boolean dryRun;

    /**
     * A BOOTSTRAP pass: a cross-model {@code generates} target whose owner model does not exist yet is
     * skipped (with a reported warning) instead of failing the whole Generate (dirigible #6539). It is
     * the declared escape from a MUTUAL cross-model cycle - model A mints a document into model B while
     * B holds a foreign key back to A - where neither project can be generated first. Nothing else is
     * relaxed: an owner model that IS there but declares no such entity still fails loudly, and a
     * cross-model RELATION never falls back to a guess (its table / key column / FK type cannot be
     * invented without emitting a broken schema).
     */
    private final boolean bootstrap;

    /** The project's {@code .settings} (loaded or scaffolded by the service before generators run). */
    private IntentSettings settings;

    /** Bare file names written under {@link #projectRoot} during this generation pass. */
    private final Set<String> writtenFileNames = new LinkedHashSet<>();

    /**
     * Non-fatal generation issues (e.g. a piece of glue that could not be emitted because a reference
     * did not resolve) collected during the pass. Surfaced in the generate response so the drop is not
     * silent at the API level (dirigible #6360) - the generation still succeeds.
     */
    private final java.util.List<String> issues = new java.util.ArrayList<>();

    /**
     * Non-fatal observations that no change to THIS document can address (e.g. a cross-model capacity
     * guard that belongs to the child's own model). Kept apart from {@link #issues} so the assistant's
     * repair loop is never asked to "fix" something that is not fixable here - a round spent on an
     * unfixable advisory is a round not spent on a real defect - while the generate response still
     * surfaces them to the developer.
     */
    private final java.util.List<String> advisories = new java.util.ArrayList<>();

    IntentGenerationContext(IntentModel model, String projectRoot, String projectName, String workspaceName, String fallbackName,
            IRepository repository) {
        this(model, projectRoot, projectName, workspaceName, fallbackName, repository, false);
    }

    IntentGenerationContext(IntentModel model, String projectRoot, String projectName, String workspaceName, String fallbackName,
            IRepository repository, boolean dryRun) {
        this(model, projectRoot, projectName, workspaceName, fallbackName, repository, dryRun, false);
    }

    IntentGenerationContext(IntentModel model, String projectRoot, String projectName, String workspaceName, String fallbackName,
            IRepository repository, boolean dryRun, boolean bootstrap) {
        this.bootstrap = bootstrap;
        this.model = model;
        this.projectRoot = projectRoot;
        this.projectName = projectName;
        this.workspaceName = workspaceName;
        this.fallbackName = fallbackName;
        this.repository = repository;
        this.dryRun = dryRun;
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
        if (dryRun) {
            // The name is still recorded - a dry run must report the same output set a real pass
            // would produce - but the repository is never touched.
            writtenFileNames.add(fileName);
            return;
        }
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
        if (dryRun) {
            writtenFileNames.add(fileName);
            return;
        }
        String path = projectRoot + "/" + fileName;
        IResource existing = repository.getResource(path);
        if (!existing.exists()) {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
        writtenFileNames.add(fileName);
    }

    /**
     * Claim an already-present, developer-owned model file: it is neither written nor scrubbed by this
     * pass. This is the write-once counterpart for a generator that cannot always produce content — it
     * lets the generator bail out early (before building output it would only discard) while still
     * keeping the existing file out of the post-pass scrub, which owns the extension.
     *
     * @param fileName bare file name including extension, e.g. {@code library.test}
     * @return {@code true} when the file exists and is now recorded as kept, {@code false} when it is
     *         absent and the generator should produce it
     */
    public boolean keepExistingModelFile(String fileName) {
        // No repository or no project to look into (a dry run over a proposal that belongs to no
        // project yet): nothing can exist, so the generator should produce - which a dry run then
        // discards, having exercised the build path.
        if (repository == null || projectRoot == null || !repository.getResource(projectRoot + "/" + fileName)
                                                                    .exists()) {
            return false;
        }
        writtenFileNames.add(fileName);
        return true;
    }

    /**
     * The bare file names emitted through {@link #writeModelFile(String, String)} so far.
     *
     * @return an unmodifiable view of the written file names
     */
    public Set<String> getWrittenFileNames() {
        return Collections.unmodifiableSet(writtenFileNames);
    }

    /**
     * Record a non-fatal generation issue (dropped glue). Surfaced in the generate response.
     *
     * @param issue a human-readable description of what was not generated and why
     */
    public void addIssue(String issue) {
        issues.add(issue);
    }

    /**
     * The non-fatal issues collected during this pass.
     *
     * @return an unmodifiable view of the issues
     */
    public java.util.List<String> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    /**
     * Record an observation no change to this document can address - surfaced to the developer, but
     * never handed to the assistant's repair loop as something to fix.
     *
     * @param advisory a human-readable observation
     */
    public void addAdvisory(String advisory) {
        advisories.add(advisory);
    }

    /**
     * The advisories collected during this pass.
     *
     * @return an unmodifiable view of the advisories
     */
    public java.util.List<String> getAdvisories() {
        return Collections.unmodifiableList(advisories);
    }

    /**
     * Whether this pass is validation-only (nothing is written or scrubbed).
     *
     * @return {@code true} for a dry run
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Whether this pass may skip a cross-model {@code generates} whose owner model does not exist yet
     * (dirigible #6539) - the declared bootstrap of a mutual cross-model cycle.
     *
     * @return {@code true} for a bootstrap pass
     */
    public boolean isBootstrap() {
        return bootstrap;
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
