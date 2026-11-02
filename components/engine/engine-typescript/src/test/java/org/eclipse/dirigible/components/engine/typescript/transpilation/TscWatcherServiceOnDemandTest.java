/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.typescript.transpilation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * Guards the on-demand start of the tsc watcher (#8004): a registry without TypeScript sources -
 * the platform's own typings and the prebuilt SDK are not sources tsc compiles - must not cost a
 * Node process, and the sources tsc does compile must be recognised wherever a project keeps them.
 */
class TscWatcherServiceOnDemandTest {

    @Test
    void staysStoppedWhenTheRegistryHoldsNoTypeScriptSources(@TempDir Path registry) throws IOException {
        write(registry, "platform-core/types/platform.d.ts");
        write(registry, "modules/src/http/response.ts");
        write(registry, "my-java-app/gen/api/CustomerController.java");
        TscWatcherService service = new TscWatcherService(repositoryAt(registry));

        service.onApplicationEvent(mock(ApplicationReadyEvent.class));
        service.monitorTscProcess();
        service.restart();

        assertFalse(service.isRunning(), "no TypeScript source, so no tsc process");
        assertFalse(Files.exists(registry.resolve("tsconfig.json")), "the registry must be left untouched");
    }

    @Test
    void anUnpublishResumesOnlyAWatcherItSuspended(@TempDir Path registry) throws IOException {
        write(registry, "app/service.ts");
        TscWatcherService service = new TscWatcherService(repositoryAt(registry));

        service.suspend();
        service.resume();

        assertFalse(service.isRunning(), "the watcher was not running before the unpublish");
        assertFalse(Files.exists(registry.resolve("tsconfig.json")), "the registry must be left untouched");
    }

    @Test
    void recognisesTheSourcesTscCompiles(@TempDir Path registry) throws IOException {
        assertTypeScriptFound(registry, "app/service.ts");
        assertTypeScriptFound(registry, "app/deep/folder/module.mts");
        assertTypeScriptFound(registry, "app/common.cts");
        assertTypeScriptFound(registry, "app/view.tsx");
        assertTypeScriptFound(registry, "app/modules/nested-is-not-excluded.ts");
    }

    @Test
    void ignoresWhatTscDoesNotCompile(@TempDir Path registry) throws IOException {
        write(registry, "app/types.d.ts");
        write(registry, "app/types.d.mts");
        write(registry, "app/types.d.cts");
        write(registry, "app/handler.js");
        write(registry, "app/node_modules/lib/index.ts");
        write(registry, "app/.hidden/draft.ts");
        write(registry, "modules/src/db/store.ts");
        write(registry, "modules-tests/src/db/store.ts");

        assertFalse(TscWatcherService.containsTypeScriptSources(registry));
    }

    @Test
    void reportsNoSourcesForAMissingRegistry(@TempDir Path root) {
        assertFalse(TscWatcherService.containsTypeScriptSources(root.resolve("registry/public")));
    }

    private static void assertTypeScriptFound(Path registry, String source) throws IOException {
        Path file = write(registry, source);
        assertTrue(TscWatcherService.containsTypeScriptSources(registry), source + " is a source tsc compiles");
        Files.delete(file);
    }

    private static Path write(Path registry, String relativePath) throws IOException {
        Path file = registry.resolve(relativePath);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, "export {};\n");
    }

    private static IRepository repositoryAt(Path registry) {
        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC)).thenReturn(registry.toString());
        return repository;
    }
}
