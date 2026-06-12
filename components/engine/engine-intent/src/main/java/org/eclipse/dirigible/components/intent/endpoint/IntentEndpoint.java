/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.endpoint;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationService;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST surface for the intent editor. The intent is an authoring artifact (like the {@code .edm});
 * both operations work on the current user's workspace, never on the registry:
 * <ul>
 * <li>{@code POST /services/ide/intent/parse} - body is the raw intent YAML; returns the parsed
 * {@link IntentModel} (drives the editor's live diagram), or {@code 422} with the full list of
 * structural issues.</li>
 * <li>{@code POST /services/ide/intent/generate} - generates every derived model file into the
 * given workspace project (next to the intent file) and scrubs stale output; returns the written
 * and scrubbed file names.</li>
 * </ul>
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "intent")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER"})
public class IntentEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentEndpoint.class);

    private final IntentGenerationService generationService;
    private final WorkspaceService workspaceService;

    public IntentEndpoint(IntentGenerationService generationService, WorkspaceService workspaceService) {
        this.generationService = generationService;
        this.workspaceService = workspaceService;
    }

    /**
     * Parse and validate an intent document. The editor calls this on every (debounced) change to
     * refresh the diagram and surface validation issues without saving.
     *
     * @param yaml the raw intent YAML
     * @return the parsed model, or {@code 422} with {@code {"issues": [...]}} when validation fails
     */
    @PostMapping(value = "/parse", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> parse(@RequestBody(required = false) String yaml) {
        try {
            IntentModel model = IntentParser.parse(yaml);
            return ResponseEntity.ok(model);
        } catch (IntentValidationException e) {
            return ResponseEntity.unprocessableEntity()
                                 .body(Map.of("issues", e.getIssues()));
        }
    }

    /**
     * Generate the derived model files for the given intent file into its workspace project.
     *
     * @param workspace the workspace name, e.g. {@code workspace}
     * @param project the project name
     * @param path the intent file path within the project, e.g. {@code app.intent}
     * @return the written and scrubbed file names, or {@code 422} with the validation issues
     */
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generate(@RequestParam("workspace") String workspace, @RequestParam("project") String project,
            @RequestParam("path") String path) {
        Workspace workspaceObject = workspaceService.getWorkspace(workspace);
        if (!workspaceObject.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace [" + workspace + "] does not exist");
        }
        Project projectObject = workspaceObject.getProject(project);
        if (!projectObject.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project [" + project + "] does not exist");
        }
        File intentFile = projectObject.getFile(path);
        if (!intentFile.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Intent file [" + path + "] does not exist in project [" + project + "]");
        }
        String yaml = new String(intentFile.getContent(), StandardCharsets.UTF_8);
        try {
            IntentGenerationService.GenerationResult result =
                    generationService.generate(yaml, projectObject.getPath(), project, baseName(path));
            return ResponseEntity.ok(
                    Map.of("workspace", workspace, "project", project, "written", result.written(), "scrubbed", result.scrubbed()));
        } catch (IntentValidationException e) {
            return ResponseEntity.unprocessableEntity()
                                 .body(Map.of("issues", e.getIssues()));
        } catch (RuntimeException e) {
            LOGGER.error("Intent generation failed for [{}/{}/{}]", workspace, project, path, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * The file's base name without path and the {@code .intent} extension - the fallback identity when
     * the YAML declares no {@code name:}.
     */
    private static String baseName(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
