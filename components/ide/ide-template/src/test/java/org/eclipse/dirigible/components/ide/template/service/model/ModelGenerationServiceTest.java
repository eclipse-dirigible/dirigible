/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateMetadataSource;
import org.eclipse.dirigible.components.workspace.domain.File;
import org.eclipse.dirigible.components.workspace.domain.Project;
import org.junit.jupiter.api.Test;

/**
 * A source declaring {@code "overwrite": false} keeps the file the project already has - the
 * project descriptor a developer may have edited - and is rendered only where there is none, or a
 * blank one.
 */
class ModelGenerationServiceTest {

    private static final String PROJECT_TEMPLATE = "/template-application-dao-java/project.json.template";

    private static final String ENTITY_TEMPLATE = "/template-application-dao-java/data/Entity.java.template";

    private static final String RENDERED_PROJECT = "{\"guid\":\"orders\"}";

    @Test
    void aProjectFileTheProjectHasIsKept() {
        Project project = project("project.json", "{\"guid\":\"orders\",\"actions\":[]}");

        List<GeneratedFile> files = ModelGenerationService.keepExisting(project, rendered(), sources());

        assertEquals("{\"guid\":\"orders\",\"actions\":[]}", files.get(0)
                                                                  .content());
        assertEquals("class Order {}", files.get(1)
                                            .content());
    }

    @Test
    void aBlankProjectFileIsGenerated() {
        Project project = project("project.json", "  \n");

        List<GeneratedFile> files = ModelGenerationService.keepExisting(project, rendered(), sources());

        assertEquals(RENDERED_PROJECT, files.get(0)
                                            .content());
    }

    @Test
    void aMissingProjectFileIsGenerated() {
        Project project = project("project.json", null);

        List<GeneratedFile> files = ModelGenerationService.keepExisting(project, rendered(), sources());

        assertEquals(RENDERED_PROJECT, files.get(0)
                                            .content());
    }

    @Test
    void aSourceThatOverwritesReplacesTheFileTheProjectHas() {
        Project project = mock(Project.class);
        GenerationTemplateMetadataSource entity = source(ENTITY_TEMPLATE, null);

        List<GeneratedFile> files = ModelGenerationService.keepExisting(project, rendered(), List.of(entity));

        assertEquals(rendered(), files);
    }

    private static List<GeneratedFile> rendered() {
        return List.of(new GeneratedFile(PROJECT_TEMPLATE, "project.json", RENDERED_PROJECT),
                new GeneratedFile(ENTITY_TEMPLATE, "gen/orders/data/OrderEntity.java", "class Order {}"));
    }

    private static List<GenerationTemplateMetadataSource> sources() {
        return List.of(source(PROJECT_TEMPLATE, false), source(ENTITY_TEMPLATE, null));
    }

    private static GenerationTemplateMetadataSource source(String location, Boolean overwrite) {
        GenerationTemplateMetadataSource source = new GenerationTemplateMetadataSource();
        source.setLocation(location);
        source.setOverwrite(overwrite);
        return source;
    }

    private static Project project(String path, String content) {
        Project project = mock(Project.class);
        File file = mock(File.class);
        when(project.getFile(path)).thenReturn(file);
        when(file.exists()).thenReturn(content != null);
        if (content != null) {
            when(file.getContent()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
        }
        return project;
    }
}
