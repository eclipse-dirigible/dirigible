/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.workspace.domain.Project;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The platform's own on-save hooks (#8024) run on every save through the workspace service - a
 * created file and an updated one alike - with the project and the path the save named, and need no
 * script engine. A failing hook neither fails the save nor keeps the next hook from running.
 */
class WorkspaceOnSaveHandlerTest {

    private static final String PROJECT_PATH = "/users/guest/workspace/orders";

    private static final byte[] CONTENT = "<model/>".getBytes(StandardCharsets.UTF_8);

    private final IRepository repository = mock(IRepository.class);

    private final List<String> saves = new ArrayList<>();

    private final WorkspaceOnSaveHandler recording = (project, path) -> saves.add(project.getPath() + " " + path);

    @BeforeEach
    void projectInTheRepository() {
        ICollection workspace = mock(ICollection.class);
        ICollection project = mock(ICollection.class);
        when(repository.getCollection(anyString())).thenReturn(workspace);
        when(workspace.getCollection("orders")).thenReturn(project);
        when(project.getPath()).thenReturn(PROJECT_PATH);
        when(project.getRepository()).thenReturn(repository);
        when(repository.createResource(anyString(), any(byte[].class), anyBoolean(), any())).thenReturn(mock(IResource.class));
        when(repository.getResource(anyString())).thenReturn(mock(IResource.class));
    }

    @Test
    void aCreatedFileRunsTheHooks() {
        service(recording).createFile("workspace", "orders", "/model/orders.edm", CONTENT, "application/xml");

        assertThat(saves).containsExactly(PROJECT_PATH + " /model/orders.edm");
    }

    @Test
    void anUpdatedFileRunsTheHooks() {
        service(recording).updateFile("workspace", "orders", "/model/orders.edm", CONTENT);

        assertThat(saves).containsExactly(PROJECT_PATH + " /model/orders.edm");
    }

    @Test
    void aFailingHookFailsNeitherTheSaveNorTheNextHook() {
        WorkspaceOnSaveHandler failing = mock(WorkspaceOnSaveHandler.class);
        doThrow(new IllegalArgumentException("not a model")).when(failing)
                                                            .onSave(any(Project.class), anyString());

        service(failing, recording).updateFile("workspace", "orders", "/model/orders.edm", CONTENT);

        assertThat(saves).containsExactly(PROJECT_PATH + " /model/orders.edm");
    }

    @SuppressWarnings("unchecked")
    private WorkspaceService service(WorkspaceOnSaveHandler... handlers) {
        return new WorkspaceService(repository, mock(ObjectProvider.class), List.of(handlers));
    }

}
