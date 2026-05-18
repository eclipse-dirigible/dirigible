/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.synchronizer;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.engine.java.domain.JavaFile;
import org.eclipse.dirigible.engine.java.runtime.JavaLoader;
import org.eclipse.dirigible.engine.java.runtime.JavaSourceParser;
import org.eclipse.dirigible.engine.java.service.JavaFileService;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Discovers client {@code .java} sources in the registry and reconciles them into runtime state.
 *
 * <p>
 * Two-phase processing:
 * <ol>
 * <li>{@link #parseImpl}, called per file, parses the package + primary class name and persists a
 * {@link JavaFile} artefact. FQN uniqueness across all projects is enforced here — a second
 * artefact claiming the same FQN is rejected with a {@link ParseException}.</li>
 * <li>{@link #completeImpl}, called per artefact per phase, only flips lifecycle and sets a
 * {@code dirty} flag. No compilation happens here.</li>
 * <li>{@link #finishing()}, called once per synchronization cycle after every artefact has been
 * visited, performs a <b>single</b> batch compile + reload across the whole client codebase via
 * {@link JavaLoader#rebuild(List)}. Per-FQN successes and failures are then folded back into the
 * matching artefact's lifecycle ({@code CREATED} or {@code FAILED}).</li>
 * </ol>
 * The deferred-batch model exists because every client class is loaded under a single shared
 * {@link org.eclipse.dirigible.engine.java.runtime.ClientClassLoader ClientClassLoader} so client
 * code can reference any other client class (entity classes from handlers, shared utilities, …).
 * A batch compile lets {@code javac} resolve those cross-file references in a single pass.
 *
 * <p>
 * Ordering: this synchronizer runs after {@code JOB} (50) / {@code LISTENER} (60) but before
 * {@code EXPOSE} (70) — chosen so URL routing artefacts and other late-binding components can
 * observe registered classes if needed. The endpoint resolves classes at request time, so
 * ordering is not load-bearing; this just keeps the boot timeline tidy.
 */
@Component
@Order(JavaSynchronizer.SYNCHRONIZER_ORDER)
public class JavaSynchronizer extends BaseSynchronizer<JavaFile, Long> {

    public static final int SYNCHRONIZER_ORDER = 65;
    public static final String FILE_EXTENSION = ".java";

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSynchronizer.class);

    private final JavaFileService javaFileService;
    private final JavaLoader javaLoader;

    /** Set by any per-artefact lifecycle change in the current cycle; consumed by {@link #finishing}. */
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    private SynchronizerCallback callback;

    @Autowired
    public JavaSynchronizer(JavaFileService javaFileService, JavaLoader javaLoader) {
        this.javaFileService = javaFileService;
        this.javaLoader = javaLoader;
    }

    @Override
    public ArtefactService<JavaFile, Long> getService() {
        return javaFileService;
    }

    @Override
    public boolean isAccepted(String type) {
        return JavaFile.ARTEFACT_TYPE.equals(type);
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION;
    }

    @Override
    public String getArtefactType() {
        return JavaFile.ARTEFACT_TYPE;
    }

    @Override
    protected List<JavaFile> parseImpl(String location, byte[] content) throws ParseException {
        RepositoryPath repositoryPath = new RepositoryPath(location);
        if (repositoryPath.getSegments().length < 2) {
            throw new ParseException("Java source location must include at least project and file: " + location, 0);
        }
        String project = repositoryPath.getSegments()[0];
        String source = new String(content, StandardCharsets.UTF_8);

        JavaSourceParser.ParsedSource parsed;
        try {
            parsed = JavaSourceParser.parse(source);
        } catch (JavaSourceParser.JavaSourceParseException e) {
            throw new ParseException("Cannot parse Java source at [" + location + "]: " + e.getMessage(), 0);
        }

        // Enforce global FQN uniqueness. The single shared classloader cannot host two definitions
        // of the same fully-qualified class — surfacing the collision here gives a clear error per
        // duplicate rather than a confusing "produces no class file" downstream.
        JavaFile existingByFqn = findByFqn(parsed.fqn());
        if (existingByFqn != null && !existingByFqn.getLocation()
                                                    .equals(location)) {
            throw new ParseException("Java class [" + parsed.fqn() + "] is already declared at [" + existingByFqn.getLocation()
                    + "]; refusing to register a duplicate at [" + location + "]", 0);
        }

        JavaFile javaFile = new JavaFile(location, parsed.simpleName(), project, parsed.fqn());
        try {
            JavaFile existing = javaFileService.findByKey(javaFile.getKey());
            if (existing != null) {
                javaFile.setId(existing.getId());
            }
            javaFile = javaFileService.save(javaFile);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to persist Java artefact [{}]: {}", location, e.getMessage(), e);
            throw new ParseException("Failed to persist Java artefact [" + location + "]: " + e.getMessage(), 0);
        }
        return List.of(javaFile);
    }

    private JavaFile findByFqn(String fqn) {
        for (JavaFile candidate : javaFileService.getAll()) {
            if (fqn.equals(candidate.getClassFqn())) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public List<JavaFile> retrieve(String location) {
        return javaFileService.findByLocation(location);
    }

    @Override
    public void setStatus(JavaFile artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        javaFileService.save(artefact);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<JavaFile> wrapper, ArtefactPhase flow) {
        JavaFile artefact = wrapper.getArtefact();
        ArtefactLifecycle lifecycle = artefact.getLifecycle();

        switch (flow) {
            case CREATE:
                if (lifecycle == ArtefactLifecycle.NEW || lifecycle == ArtefactLifecycle.FAILED) {
                    dirty.set(true);
                    // The lifecycle is finalised in finishing() once we know whether the batch
                    // compile succeeded for this FQN.
                }
                break;
            case UPDATE:
                if (lifecycle == ArtefactLifecycle.MODIFIED || lifecycle == ArtefactLifecycle.FAILED) {
                    dirty.set(true);
                }
                break;
            case DELETE:
                if (lifecycle != ArtefactLifecycle.DELETED) {
                    dirty.set(true);
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, "");
                }
                break;
            case PREPARE:
            case START:
            case STOP:
                // No-op.
                break;
        }
        return true;
    }

    @Override
    public void finishing() {
        if (!dirty.compareAndSet(true, false)) {
            return;
        }
        rebuildAll();
    }

    private void rebuildAll() {
        List<JavaFile> all = javaFileService.getAll();
        List<JavaLoader.ClientSource> sources = new ArrayList<>(all.size());
        for (JavaFile file : all) {
            if (!RegistrySourceLoader.exists(file.getLocation())) {
                // The artefact's source disappeared between cycles; let the orchestrator's cleanup
                // loop delete the artefact row. We must not include a stale source here.
                continue;
            }
            byte[] bytes;
            try {
                bytes = RegistrySourceLoader.read(file.getLocation());
            } catch (RuntimeException e) {
                LOGGER.error("Failed to read Java source at [{}]: {}", file.getLocation(), e.getMessage(), e);
                file.setLifecycle(ArtefactLifecycle.FAILED);
                file.setError("Read failure: " + e.getMessage());
                javaFileService.save(file);
                continue;
            }
            sources.add(new JavaLoader.ClientSource(file.getProject(), file.getClassFqn(), new String(bytes, StandardCharsets.UTF_8)));
        }

        JavaLoader.RebuildResult result = javaLoader.rebuild(sources);

        for (JavaFile file : all) {
            String fqn = file.getClassFqn();
            if (result.failures()
                      .containsKey(fqn)) {
                String message = result.failures()
                                        .get(fqn);
                file.setLifecycle(ArtefactLifecycle.FAILED);
                file.setError(message);
                javaFileService.save(file);
            } else if (result.succeededFqns()
                              .contains(fqn)) {
                file.setLifecycle(ArtefactLifecycle.CREATED);
                file.setError(null);
                javaFileService.save(file);
            }
        }
    }

    @Override
    protected void cleanupImpl(JavaFile artefact) {
        try {
            javaFileService.delete(artefact);
            // Mark the cycle dirty so finishing() rebuilds the classloader without the removed
            // source; the consumer's onClassUnloaded fires from the rebuild's diff.
            dirty.set(true);
        } catch (RuntimeException e) {
            LOGGER.error("Cleanup failed for [{}]: {}", artefact.getLocation(), e.getMessage(), e);
            callback.addError(e.getMessage());
            callback.registerState(this, artefact, ArtefactLifecycle.DELETED, e.getMessage());
        }
    }

    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

}
