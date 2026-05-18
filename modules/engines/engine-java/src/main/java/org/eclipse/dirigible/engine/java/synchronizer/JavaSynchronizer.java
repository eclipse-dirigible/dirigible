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
import java.util.List;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.engine.java.domain.JavaFile;
import org.eclipse.dirigible.engine.java.runtime.JavaCompilationException;
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
 * Flow:
 * <ol>
 * <li>{@link #parseImpl} parses the package + primary class name from the source, persists a
 * {@link JavaFile} artefact, and returns it for orchestration.</li>
 * <li>{@link #completeImpl} reacts to {@link ArtefactPhase#CREATE}, {@link ArtefactPhase#UPDATE},
 * and {@link ArtefactPhase#DELETE} by compiling, loading and registering — or unregistering —
 * the handler against the {@link org.eclipse.dirigible.engine.java.runtime.JavaClassRegistry
 * JavaClassRegistry} (via {@link JavaLoader}).</li>
 * <li>{@link #cleanupImpl} hard-deletes the artefact and removes any live registration —
 * called for sources that have vanished from the registry between scans.</li>
 * </ol>
 *
 * <p>
 * Ordering: this synchronizer runs after {@code JOB} (50) / {@code LISTENER} (60) but before
 * {@code EXPOSE} (70) — chosen so URL routing artefacts and other late-binding components can
 * observe the registered classes if needed. The endpoint resolves classes at request time, so
 * ordering is not load-bearing; this just keeps the boot timeline tidy.
 */
@Component
@Order(JavaSynchronizer.SYNCHRONIZER_ORDER)
public class JavaSynchronizer extends BaseSynchronizer<JavaFile, Long> {

    /** Synchronizer execution order — see class javadoc. */
    public static final int SYNCHRONIZER_ORDER = 65;

    /** File extension this synchronizer accepts. */
    public static final String FILE_EXTENSION = ".java";

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSynchronizer.class);

    private final JavaFileService javaFileService;
    private final JavaLoader javaLoader;

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
                    return loadHandlerSafely(wrapper);
                }
                break;
            case UPDATE:
                if (lifecycle == ArtefactLifecycle.MODIFIED || lifecycle == ArtefactLifecycle.FAILED) {
                    // Re-load: the registry overwrites the prior entry and drops its ClassLoader.
                    return loadHandlerSafely(wrapper);
                }
                break;
            case DELETE:
                if (lifecycle == ArtefactLifecycle.CREATED || lifecycle == ArtefactLifecycle.UPDATED
                        || lifecycle == ArtefactLifecycle.FAILED) {
                    javaLoader.unload(artefact.getProject(), artefact.getClassFqn());
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, "");
                }
                break;
            case PREPARE:
            case START:
            case STOP:
                // No-op: this engine has no separate prepare/start/stop semantics.
                break;
        }
        return true;
    }

    private boolean loadHandlerSafely(TopologyWrapper<JavaFile> wrapper) {
        JavaFile artefact = wrapper.getArtefact();
        try {
            byte[] content = readSourceContent(artefact);
            String source = new String(content, StandardCharsets.UTF_8);
            javaLoader.loadAndRegister(artefact.getProject(), artefact.getClassFqn(), source);
            callback.registerState(this, wrapper, ArtefactLifecycle.CREATED, "");
            return true;
        } catch (JavaCompilationException e) {
            LOGGER.error("Compilation failed for Java source [{}]: {}", artefact.getLocation(), e.getMessage());
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e.getMessage());
            return false;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to load Java source [{}]: {}", artefact.getLocation(), e.getMessage(), e);
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e.getMessage(), e);
            return false;
        }
    }

    private byte[] readSourceContent(JavaFile artefact) {
        // The orchestrator hands the source bytes to parseImpl() but not to completeImpl(); we
        // need to re-read from the registry here. Done via the IRepository bean rather than the
        // filesystem so multi-tenant registry overlays remain transparent.
        return RegistrySourceLoader.read(artefact.getLocation());
    }

    @Override
    protected void cleanupImpl(JavaFile artefact) {
        try {
            javaLoader.unload(artefact.getProject(), artefact.getClassFqn());
            javaFileService.delete(artefact);
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
