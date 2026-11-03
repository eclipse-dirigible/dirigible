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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * A client source the batch compile or the bean container fails is an error of the pass (#8239):
 * its lifecycle is settled in {@code finishing()}, outside the phases whose errors the orchestrator
 * collects, so without the report the pass completed READY with the source FAILED.
 */
class JavaSynchronizerFailureReportTest {

    private static final String LOCATION = "/broken/qa/Broken.java";
    private static final String FQN = "qa.Broken";

    private final IResource resource = mock(IResource.class);
    private final JavaFileService javaFileService = mock(JavaFileService.class);
    private final JavaLoader javaLoader = mock(JavaLoader.class);
    private final SynchronizerCallback callback = mock(SynchronizerCallback.class);
    private final JavaFile file = new JavaFile(LOCATION, "Broken", "broken", FQN);

    private JavaSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        IRepository repository = mock(IRepository.class);
        when(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + LOCATION)).thenReturn(resource);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(IRepository.class)).thenReturn(repository);
        new BeanProvider().setApplicationContext(context);

        when(resource.exists()).thenReturn(true);
        when(resource.getContent()).thenReturn("package qa; public class Broken { Strin x; }".getBytes(StandardCharsets.UTF_8));
        when(javaFileService.getAll()).thenReturn(List.of(file));

        synchronizer = new JavaSynchronizer(javaFileService, javaLoader);
        synchronizer.setCallback(callback);
    }

    @Test
    void a_compile_failure_is_reported_to_the_pass() {
        when(javaLoader.rebuild(anyList())).thenReturn(result(Set.of(), Map.of(FQN, "cannot find symbol"), Map.of()));

        synchronizer.finishing();

        verify(callback).addError(argThat(error -> error.contains(LOCATION) && error.contains("cannot find symbol")));
    }

    @Test
    void a_wiring_failure_is_reported_to_the_pass() {
        when(javaLoader.rebuild(anyList())).thenReturn(result(Set.of(), Map.of(), Map.of(FQN, "unsatisfied dependency")));

        synchronizer.finishing();

        verify(callback).addError(argThat(error -> error.contains(LOCATION) && error.contains("unsatisfied dependency")));
    }

    @Test
    void a_failed_source_is_reported_again_by_a_later_pass_that_does_not_recompile_it() {
        when(javaLoader.rebuild(anyList())).thenReturn(result(Set.of(), Map.of(FQN, "cannot find symbol"), Map.of()));
        synchronizer.finishing();
        clearInvocations(callback);

        synchronizer.completeImpl(new TopologyWrapper<>(file, new HashMap<>(), synchronizer), ArtefactPhase.START);
        synchronizer.finishing();

        verify(callback).addError(argThat(error -> error.contains(LOCATION)));
    }

    @Test
    void a_compiled_source_is_not_reported() {
        when(javaLoader.rebuild(anyList())).thenReturn(result(Set.of(FQN), Map.of(), Map.of()));

        synchronizer.finishing();

        verify(callback, never()).addError(anyString());
    }

    private static JavaLoader.RebuildResult result(Set<String> succeeded, Map<String, String> failures, Map<String, String> wiringErrors) {
        return new JavaLoader.RebuildResult(succeeded, failures, Set.of(), Map.of(), wiringErrors, Map.of());
    }

}
