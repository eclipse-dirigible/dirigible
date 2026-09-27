/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.dirigible.components.ide.template.service.model.GeneratedFile;
import org.eclipse.dirigible.components.ide.template.service.model.ModelGenerationService;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Every generated application shell carries the platform's Users section (#7498): it loads the
 * tenantUsers store, routes Settings whether or not the model declares a SETTING entity, and offers
 * the Users entry and its pane in Settings. Without a SETTING entity the Settings footer shows only
 * while the Users section is offered - before #7498 such a shell had no Settings at all.
 *
 * <p>
 * Rendered, not published: the rendered pages are what a regenerated application carries, and the
 * browser behaviour of the fragment itself is covered by {@code TenantUsersSettingsIT}.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GeneratedShellTenantUsersIT extends IntegrationTest {

    private static final String TEMPLATE = "template-application-ui-harmonia-java/template/template.js";
    private static final String PROJECT = "tenant-users-shell";
    private static final String GUARD = "x-show=\"$store.tenantUsers && $store.tenantUsers.visible\"";

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ModelGenerationService modelGenerationService;

    @Test
    void aModelWithoutSettingEntitiesStillOffersSettingsForTheUsersSection() throws IOException {
        List<GeneratedFile> rendered = render("tenant-users-no-settings", "ModelGenerationIT/sales-order.model", "sales-order.model");
        String index = page(rendered, "/index.html");
        String settings = page(rendered, "/_settings.html");

        assertTrue(index.contains("stores/tenantUsers.js"), "the shell must load the tenantUsers store");
        assertTrue(index.contains("x-route=\"/settings\""), "Settings must be routed without a SETTING entity");
        assertTrue(index.contains("<li x-h-sidebar-menu-item " + GUARD), "the footer entry must show only for the Users section");
        assertTrue(settings.contains("select('tenantUsers'"), "Settings must offer the Users entry");
        assertTrue(settings.contains("$store.tenantUsers.markup"), "Settings must render the Users fragment");
        assertFalse(index.contains("$dollar"), "no unrendered Velocity reference may reach the page");
    }

    @Test
    void aModelWithSettingEntitiesKeepsItsSettingsAndAddsUsers() throws IOException {
        List<GeneratedFile> rendered = render("tenant-users-settings", "DependsOnHarmoniaIT/edm.model", "edm.model");
        String index = page(rendered, "/index.html");
        String settings = page(rendered, "/_settings.html");

        assertTrue(index.contains("x-route=\"/settings\""), "Settings must stay routed");
        assertFalse(index.contains("<li x-h-sidebar-menu-item " + GUARD), "with SETTING entities the footer entry is always shown");
        assertTrue(settings.contains("select('tenantUsers'"), "Settings must offer the Users entry beside the setting entities");
        assertTrue(settings.contains("shell.settings.configuration"), "the setting entities must still be listed");
    }

    private List<GeneratedFile> render(String workspace, String fixturePath, String fileName) throws IOException {
        String fixture = readClasspath(fixturePath);
        assertNotNull(fixture, "missing fixture " + fixturePath);
        Workspace workspaceObject = workspaceService.existsWorkspace(workspace) ? workspaceService.getWorkspace(workspace)
                : workspaceService.createWorkspace(workspace);
        Project project = workspaceObject.getProject(PROJECT);
        if (project == null || !project.exists()) {
            project = workspaceObject.createProject(PROJECT);
        }
        project.createFile(fileName, fixture.getBytes(StandardCharsets.UTF_8));
        return modelGenerationService.render(workspace, PROJECT, fileName, TEMPLATE, new LinkedHashMap<>());
    }

    private static String page(List<GeneratedFile> rendered, String suffix) {
        return rendered.stream()
                       .filter(file -> file.path()
                                           .endsWith(suffix)
                               && file.path()
                                      .startsWith("gen/"))
                       .map(GeneratedFile::content)
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no rendered page ending in " + suffix + " among " + rendered.stream()
                                                                                                                          .map(GeneratedFile::path)
                                                                                                                          .toList()));
    }

    private static String readClasspath(String path) throws IOException {
        try (InputStream content = GeneratedShellTenantUsersIT.class.getClassLoader()
                                                                    .getResourceAsStream(path)) {
            return content == null ? null : new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
