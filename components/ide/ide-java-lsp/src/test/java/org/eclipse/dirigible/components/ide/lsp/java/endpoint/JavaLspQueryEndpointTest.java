/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.lsp.java.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;

import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsInstance;
import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsManager;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * The Problems view's diagnostics request starts the Java language server only for a workspace that
 * holds a Java source, and keeps reading the snapshot of one already running (#8080).
 */
@ExtendWith(MockitoExtension.class)
class JavaLspQueryEndpointTest {

    private static final String USERNAME = "admin";

    private static final String WORKSPACE = "workspace";

    private static final String WORKSPACE_PATH = "/users/" + USERNAME + "/" + WORKSPACE;

    private static final String REQUEST = "{\"workspace\": \"" + WORKSPACE + "\"}";

    private static final String SNAPSHOT = "[{\"uri\":\"file:///workspace/workspace/shop/Order.java\",\"diagnostics\":[]}]";

    private static final Principal PRINCIPAL = () -> USERNAME;

    @TempDir
    Path root;

    @Mock
    private JdtLsManager manager;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private JdtLsInstance instance;

    private IRepository repository;

    private JavaLspQueryEndpoint endpoint;

    @BeforeEach
    void createEndpoint() {
        repository = new LocalRepository(root.toString(), true);
        endpoint = new JavaLspQueryEndpoint(manager, workspaceService);
    }

    @Test
    void aWorkspaceWithoutJavaSourcesIsAnsweredEmptyWithoutStartingTheLanguageServer() throws Exception {
        write("/shop/app.intent", "name: shop");
        write("/shop/api/orders.ts", "export const orders = [];");
        write("/shop/node_modules/lib/Lib.java", "class Lib {}");
        write("/shop/.settings/Tool.java", "class Tool {}");
        givenTheWorkspace();

        String body = diagnostics();

        assertThat(body).isEqualTo("[]");
        verify(manager, never()).getOrStart(anyString(), anyString());
    }

    @Test
    void aWorkspaceNotCreatedYetIsAnsweredEmptyWithoutStartingTheLanguageServer() throws Exception {
        givenTheWorkspace();

        String body = diagnostics();

        assertThat(body).isEqualTo("[]");
        verify(manager, never()).getOrStart(anyString(), anyString());
    }

    @Test
    void aWorkspaceWithAJavaSourceStartsTheLanguageServer() throws Exception {
        write("/shop/gen/shop/data/Order.java", "package gen.shop.data; class Order {}");
        givenTheWorkspace();
        givenTheLanguageServerStarts();

        String body = diagnostics();

        assertThat(body).isEqualTo(SNAPSHOT);
        verify(manager).getOrStart(USERNAME, WORKSPACE);
    }

    @Test
    void aUnitTestSourceCountsAsAJavaSource() throws Exception {
        write("/shop/custom/test/OrderTest.java", "class OrderTest {}");
        givenTheWorkspace();
        givenTheLanguageServerStarts();

        String body = diagnostics();

        assertThat(body).isEqualTo(SNAPSHOT);
        verify(manager).getOrStart(USERNAME, WORKSPACE);
    }

    @Test
    void aRunningLanguageServerAnswersItsSnapshotWithoutAWorkspaceScan() throws Exception {
        when(manager.isRunning(USERNAME, WORKSPACE)).thenReturn(true);
        givenTheLanguageServerStarts();

        String body = diagnostics();

        assertThat(body).isEqualTo(SNAPSHOT);
        verifyNoInteractions(workspaceService);
    }

    private void write(String path, String content) {
        repository.createResource(WORKSPACE_PATH + path, content.getBytes(StandardCharsets.UTF_8));
    }

    private void givenTheWorkspace() {
        when(workspaceService.getWorkspace(WORKSPACE)).thenReturn(new Workspace(repository.getCollection(WORKSPACE_PATH)));
    }

    private void givenTheLanguageServerStarts() throws Exception {
        when(manager.getOrStart(USERNAME, WORKSPACE)).thenReturn(instance);
        when(instance.ensureInitialized()).thenReturn(CompletableFuture.completedFuture(null));
        when(instance.diagnosticsSnapshot()).thenReturn(new ObjectMapper().readTree(SNAPSHOT));
    }

    private String diagnostics() {
        return endpoint.diagnostics(REQUEST, PRINCIPAL)
                       .getBody();
    }
}
