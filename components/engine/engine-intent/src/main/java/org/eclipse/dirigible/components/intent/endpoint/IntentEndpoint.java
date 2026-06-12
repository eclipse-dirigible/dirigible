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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.security.RolesAllowed;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.intent.domain.Intent;
import org.eclipse.dirigible.components.intent.generator.IntentRegenerationService;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.service.IntentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST surface for the intent perspective. Reads the persisted {@link Intent} artefacts and the
 * derived {@link IntentModel} tree. Mutating endpoints are limited to manual regeneration; the
 * intent itself is authored through the IDE workspace (or a future Claude bridge) and reconciled
 * via {@code IntentSynchronizer}.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "intent")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER", "OPERATOR"})
public class IntentEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentEndpoint.class);

    private final IntentService intentService;
    private final IntentRegenerationService regenerationService;

    public IntentEndpoint(IntentService intentService, IntentRegenerationService regenerationService) {
        this.intentService = intentService;
        this.regenerationService = regenerationService;
    }

    /**
     * List every project that has at least one persisted {@link Intent}. Project name is the first path
     * segment of the artefact location.
     */
    @GetMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> projects() {
        Set<String> projects = new LinkedHashSet<>();
        for (Intent intent : intentService.getAll()) {
            String project = projectOf(intent.getLocation());
            if (project != null) {
                projects.add(project);
            }
        }
        return ResponseEntity.ok(projects.stream()
                                         .sorted()
                                         .collect(Collectors.toList()));
    }

    /**
     * Return the parsed intent model for the given project. 404 if the project has no {@code .intent}
     * on file.
     */
    @GetMapping(value = "/projects/{project}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IntentModel> intent(@PathVariable("project") String project) {
        Intent intent = findIntent(project);
        return ResponseEntity.ok(IntentParser.parse(intent.getContent()));
    }

    /**
     * Return the raw YAML source of the intent file. Suitable for read-only display in the perspective;
     * the workspace's normal editor is the authoring path.
     */
    @GetMapping(value = "/projects/{project}/source", produces = "text/yaml; charset=UTF-8")
    public ResponseEntity<String> source(@PathVariable("project") String project) {
        Intent intent = findIntent(project);
        return ResponseEntity.ok(intent.getContent() == null ? "" : intent.getContent());
    }

    /**
     * Force a regeneration pass for the given project's intent. Useful from the perspective's
     * Regenerate button: avoids waiting for the next synchronizer cycle.
     */
    @PostMapping(value = "/projects/{project}/regenerate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> regenerate(@PathVariable("project") String project) {
        Intent intent = findIntent(project);
        try {
            regenerationService.regenerate(intent);
            return ResponseEntity.ok(Map.of("project", project, "status", "regenerated"));
        } catch (RuntimeException e) {
            LOGGER.error("Regeneration failed for project [{}]", project, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Regeneration failed: " + e.getMessage(), e);
        }
    }

    private Intent findIntent(String project) {
        for (Intent intent : intentService.getAll()) {
            if (project.equals(projectOf(intent.getLocation()))) {
                return intent;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No intent for project [" + project + "]");
    }

    /**
     * First non-empty path segment of an artefact location.
     */
    private static String projectOf(String location) {
        if (location == null) {
            return null;
        }
        int start = location.startsWith("/") ? 1 : 0;
        int end = location.indexOf('/', start);
        if (end < 0) {
            return null;
        }
        return location.substring(start, end);
    }
}
