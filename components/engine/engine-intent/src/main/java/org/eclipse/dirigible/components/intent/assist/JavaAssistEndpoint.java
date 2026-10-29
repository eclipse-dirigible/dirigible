/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.components.intent.ai.AssistantNotConfiguredException;
import org.eclipse.dirigible.components.intent.ai.AssistantUpstreamException;
import org.eclipse.dirigible.components.intent.ai.ChatTurn;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST surface for the Workbench's Java assistant.
 *
 * <p>
 * {@code POST /services/ide/intent/assist} - body names a workspace file and carries the editor
 * buffer, the developer's message and the prior transcript; returns {@code {reply, proposedSource,
 * diagnostics}}. The assistant only proposes; the view diffs the proposal and the developer accepts
 * it, so nothing is written here. Returns {@code 412} when no API key is configured and {@code 502}
 * when the upstream model call fails - the same contract the intent agent endpoint answers, since
 * both go through one client.
 *
 * <p>
 * Scope is deliberately the project's hand-written Java. A path under {@code gen/} is refused: that
 * folder is the template engine's output and is wiped on the next generation, so a proposal there
 * could only be lost. A TypeScript source or a JavaScript module is ported: the proposal is the
 * Java file that replaces it, named by {@code target}.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "intent")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER"})
class JavaAssistEndpoint {

    private static final String GENERATED_FOLDER = "gen/";

    private static final String JAVA_EXTENSION = ".java";

    private final JavaAssistService assistService;
    private final WorkspaceJavaSources sources;
    private final WorkspaceService workspaceService;

    JavaAssistEndpoint(JavaAssistService assistService, WorkspaceJavaSources sources, WorkspaceService workspaceService) {
        this.assistService = assistService;
        this.sources = sources;
        this.workspaceService = workspaceService;
    }

    @PostMapping(value = "/assist", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AssistReply> assist(@RequestBody AssistRequest request) {
        String path = StringUtils.defaultString(request.path());
        AssistContext context = ScriptPorts.isPortable(path) ? portContext(request, path) : javaContext(request, path);
        try {
            return ResponseEntity.ok(assistService.chat(context, request.message(), history(request)));
        } catch (AssistantNotConfiguredException ex) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), ex);
        } catch (AssistantUpstreamException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage(), ex);
        }
    }

    /** A turn about a hand-written Java file. */
    private AssistContext javaContext(AssistRequest request, String path) {
        if (!path.endsWith(JAVA_EXTENSION)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The assistant works on Java files, and ports TypeScript to Java; [" + path + "] is neither");
        }
        if (path.startsWith(GENERATED_FOLDER)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "[" + path + "] is generated code - it is regenerated from the model, so it cannot be edited here");
        }
        Project project = resolveProject(request);

        String source = request.source() != null ? request.source() : sources.read(project, path);
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "File [" + path + "] does not exist in project [" + request.project() + "]");
        }
        return new AssistContext(request.project(), path, source, sources.intentYaml(project), sources.siblings(project, path, source));
    }

    /**
     * A turn porting a server-side script to Java (#8051): the proposal is the Java file that replaces
     * it, compiled like any other, and the script itself is left for the developer to delete once the
     * port is in. Generated TypeScript is not ported - it is regenerated with the Java templates.
     */
    private AssistContext portContext(AssistRequest request, String path) {
        if (path.startsWith(GENERATED_FOLDER)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "[" + path + "] is generated code - regenerate the project with the Java templates instead of porting it");
        }
        String target = StringUtils.isNotBlank(request.target()) ? request.target() : ScriptPorts.javaPathOf(path);
        if (!target.endsWith(JAVA_EXTENSION) || target.startsWith(GENERATED_FOLDER)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A port writes a hand-written Java file outside gen/; [" + target + "] is not one");
        }
        Project project = resolveProject(request);

        String script = request.source() != null ? request.source() : sources.read(project, path);
        if (script == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "File [" + path + "] does not exist in project [" + request.project() + "]");
        }
        String existing = StringUtils.defaultString(sources.read(project, target));
        return new AssistContext(request.project(), target, existing, sources.intentYaml(project),
                sources.siblings(project, target, script + "\n" + existing),
                new AssistContext.PortedScript(path, ScriptPorts.languageOf(path), script));
    }

    private Project resolveProject(AssistRequest request) {
        Workspace workspace = workspaceService.getWorkspace(request.workspace());
        if (!workspace.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace [" + request.workspace() + "] does not exist");
        }
        Project project = workspace.getProject(request.project());
        if (!project.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project [" + request.project() + "] does not exist");
        }
        return project;
    }

    private static List<ChatTurn> history(AssistRequest request) {
        return request.history() == null ? List.of() : request.history();
    }
}
