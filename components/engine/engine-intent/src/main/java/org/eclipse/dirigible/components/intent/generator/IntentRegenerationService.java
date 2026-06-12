/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.intent.domain.Intent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the regeneration pass for a single {@link Intent}. Hands every registered
 * {@link IntentTargetGenerator} the same {@link IntentGenerationContext} in {@code @Order} order
 * and isolates per-generator failures so one broken slice does not block the others.
 *
 * <p>
 * After the generators run, model-layer files at the project root that were written by a previous
 * pass but not re-emitted by this one are deleted. In an intent project the model files at the
 * project root are owned by the regeneration; the extension filter keeps the scrub away from
 * {@code app.intent} itself, code files, and the {@code gen/} / {@code custom/} subfolders (only
 * direct child resources are considered). Removing a process / form / report / seed from the intent
 * therefore removes its model file on the next regeneration instead of leaving a stale artefact
 * deployed.
 */
@Component
public class IntentRegenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentRegenerationService.class);

    /**
     * The model-layer extensions intent generators may emit. Files with one of these extensions at the
     * project root are owned (and scrubbed) by the regeneration pass.
     */
    private static final Set<String> INTENT_OWNED_EXTENSIONS = Set.of(".edm", ".model", ".bpmn", ".form", ".report", ".roles", ".access",
            ".dsm", ".schema", ".table", ".view", ".csvim", ".csv");

    private final List<IntentTargetGenerator> generators;
    private final IRepository repository;

    public IntentRegenerationService(List<IntentTargetGenerator> generators, IRepository repository) {
        this.generators = generators;
        this.repository = repository;
    }

    /**
     * Re-emit every model artefact for the given intent and scrub stale intent-owned files. The
     * intent's registry-relative {@code location} (e.g. {@code /orders/app.intent}) is resolved against
     * {@code /registry/public} to find the project root.
     *
     * @param intent the intent whose model output should be refreshed
     */
    public void regenerate(Intent intent) {
        IntentModel model = IntentParser.parse(intent.getContent());
        intent.setModel(model);
        String projectRoot = resolveProjectRoot(intent.getLocation());
        String projectName = resolveProjectName(intent.getLocation());
        IntentGenerationContext context = new IntentGenerationContext(intent, model, projectRoot, projectName, repository);
        LOGGER.info("Regenerating model files for intent [{}] under [{}] via {} generator(s)", intent.getName(), projectRoot,
                generators.size());
        for (IntentTargetGenerator generator : generators) {
            try {
                generator.generate(context);
            } catch (RuntimeException e) {
                LOGGER.error("Intent generator [{}] failed for intent [{}]", generator.name(), intent.getName(), e);
            }
        }
        scrubStaleModelFiles(context.getProjectRoot(), context.getWrittenFileNames());
    }

    /**
     * Delete every intent-owned model file at the given intent's project root. Called when the
     * {@code .intent} file itself is removed, so the model files do not survive their source of truth.
     *
     * @param intent the deleted intent
     */
    public void cleanup(Intent intent) {
        scrubStaleModelFiles(resolveProjectRoot(intent.getLocation()), Set.of());
    }

    /**
     * Remove intent-owned model files at the project root that are not part of the current output set.
     */
    private void scrubStaleModelFiles(String projectRoot, Set<String> keep) {
        ICollection project = repository.getCollection(projectRoot);
        if (!project.exists()) {
            return;
        }
        for (String fileName : project.getResourcesNames()) {
            if (keep.contains(fileName) || !isIntentOwned(fileName)) {
                continue;
            }
            try {
                repository.removeResource(projectRoot + "/" + fileName);
                LOGGER.info("Scrubbed stale intent output [{}/{}]", projectRoot, fileName);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to scrub stale intent output [{}/{}]", projectRoot, fileName, e);
            }
        }
    }

    private static boolean isIntentOwned(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && INTENT_OWNED_EXTENSIONS.contains(fileName.substring(dot));
    }

    /**
     * Full repository path of the project root: the registry prefix plus the location's directory.
     * Artefact locations are registry-relative ({@code /<project>/app.intent}), while
     * {@code IRepository} paths are repository-absolute, so the {@code /registry/public} prefix is
     * mandatory - without it the output lands outside the registry and no downstream synchronizer ever
     * sees it.
     */
    private static String resolveProjectRoot(String location) {
        if (location == null) {
            return IRepositoryStructure.PATH_REGISTRY_PUBLIC;
        }
        int lastSlash = location.lastIndexOf('/');
        String relativeRoot = lastSlash <= 0 ? "" : location.substring(0, lastSlash);
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + relativeRoot;
    }

    /**
     * First non-empty path segment of the registry-relative location.
     */
    private static String resolveProjectName(String location) {
        if (location == null) {
            return "";
        }
        int start = location.startsWith("/") ? 1 : 0;
        int end = location.indexOf('/', start);
        return end < 0 ? "" : location.substring(start, end);
    }
}
