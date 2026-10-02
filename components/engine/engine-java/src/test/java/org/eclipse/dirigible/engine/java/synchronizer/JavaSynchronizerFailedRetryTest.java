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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.engine.java.domain.JavaFile;
import org.eclipse.dirigible.engine.java.runtime.JavaLoader;
import org.eclipse.dirigible.engine.java.service.JavaFileService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * The FAILED retry (#7248) gives a FAILED artefact only a {@code START}. A Java file that failed
 * because the rebuild read it while a publish was still writing it must heal on that retry; a file
 * that is merely broken must not recompile the whole codebase on every retry interval.
 */
class JavaSynchronizerFailedRetryTest {

    private static final String LOCATION = "/posting/gen/Repository.java";
    private static final String FQN = "gen.Repository";

    private final IResource resource = mock(IResource.class);
    private final JavaFileService javaFileService = mock(JavaFileService.class);
    private final JavaLoader javaLoader = mock(JavaLoader.class);
    private final JavaFile file = new JavaFile(LOCATION, "Repository", "posting", FQN);

    private JavaSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        IRepository repository = mock(IRepository.class);
        when(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + LOCATION)).thenReturn(resource);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(IRepository.class)).thenReturn(repository);
        new BeanProvider().setApplicationContext(context);

        when(resource.exists()).thenReturn(true);
        when(javaFileService.getAll()).thenReturn(List.of(file));
        when(javaLoader.rebuild(anyList())).thenReturn(result(false));

        synchronizer = new JavaSynchronizer(javaFileService, javaLoader);
        synchronizer.setCallback(mock(SynchronizerCallback.class));
        // The startup rebuild reads the torn source - a publish is still writing it - and fails.
        source("package gen;");
        synchronizer.finishing();
        assertEquals(ArtefactLifecycle.FAILED, file.getLifecycle());
    }

    @Test
    void a_failed_file_whose_source_was_completed_since_rebuilds_on_the_retry() {
        source("package gen; public class Repository {}");
        when(javaLoader.rebuild(anyList())).thenReturn(result(true));

        retry();

        verify(javaLoader, times(2)).rebuild(anyList());
        assertEquals(ArtefactLifecycle.CREATED, file.getLifecycle());
    }

    @Test
    void a_failed_file_with_the_same_source_is_not_recompiled_on_every_retry() {
        retry();
        retry();

        verify(javaLoader, times(1)).rebuild(anyList());
        assertEquals(ArtefactLifecycle.FAILED, file.getLifecycle());
    }

    private void retry() {
        synchronizer.completeImpl(new TopologyWrapper<>(file, new HashMap<>(), synchronizer), ArtefactPhase.START);
        synchronizer.finishing();
    }

    private void source(String source) {
        when(resource.getContent()).thenReturn(source.getBytes(StandardCharsets.UTF_8));
    }

    private static JavaLoader.RebuildResult result(boolean compiles) {
        return new JavaLoader.RebuildResult(compiles ? Set.of(FQN) : Set.of(), compiles ? Map.of() : Map.of(FQN, "cannot find symbol"),
                Set.of(), Map.of(), Map.of(), Map.of());
    }

}
