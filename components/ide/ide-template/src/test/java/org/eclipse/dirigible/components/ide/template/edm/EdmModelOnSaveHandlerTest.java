/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.edm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.workspace.domain.File;
import org.eclipse.dirigible.components.workspace.domain.Project;
import org.junit.jupiter.api.Test;

/**
 * Saving an {@code .edm} writes the {@code .model} beside it; any other file is left alone, and an
 * {@code .edm} no {@code .model} can be derived from writes none.
 */
class EdmModelOnSaveHandlerTest {

    private static final String EDM = """
            <model>
             <entities>
              <entity name="Country" dataName="COUNTRY">
               <property name="Id" dataName="COUNTRY_ID"></property>
              </entity>
             </entities>
            </model>
            """;

    private final EdmModelOnSaveHandler handler = new EdmModelOnSaveHandler();

    @Test
    void anEdmWritesTheModelBesideIt() {
        Project project = projectWith("/data/countries.edm", EDM);

        handler.onSave(project, "/data/countries.edm");

        verify(project).createFile("/data/countries.model", EdmModelTransformation.transform(EDM)
                                                                                  .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void onlyTheExtensionIsReplaced() {
        Project project = projectWith("/data.edm/countries.edm", EDM);

        handler.onSave(project, "/data.edm/countries.edm");

        verify(project).createFile("/data.edm/countries.model", EdmModelTransformation.transform(EDM)
                                                                                      .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void anyOtherFileIsLeftAlone() {
        Project project = mock(Project.class);

        handler.onSave(project, "/data/countries.model");

        verifyNoInteractions(project);
    }

    @Test
    void anEdmThatIsNotXmlWritesNoModel() {
        Project project = projectWith("/data/countries.edm", "<model><entities></model>");

        assertThrows(IllegalArgumentException.class, () -> handler.onSave(project, "/data/countries.edm"));

        verify(project, never()).createFile(anyString(), any(byte[].class));
    }

    private static Project projectWith(String path, String content) {
        Project project = mock(Project.class);
        File file = mock(File.class);
        when(project.getFile(path)).thenReturn(file);
        when(file.getContent()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
        return project;
    }

}
