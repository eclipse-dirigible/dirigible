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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs the generation pass for one intent document against a target project: hands every registered
 * {@link IntentTargetGenerator} the same {@link IntentGenerationContext} in {@code @Order} order
 * and isolates per-generator failures so one broken slice does not block the others.
 *
 * <p>
 * After the generators run, model-layer files at the project root that were written by a previous
 * pass but not re-emitted by this one are deleted. In an intent project the model files at the
 * project root are owned by generation; the extension filter keeps the scrub away from the
 * {@code .intent} file itself, code files, and the {@code gen/} / {@code custom/} subfolders (only
 * direct child resources are considered). Removing a process / form / report / seed from the intent
 * therefore removes its model file on the next Generate instead of leaving a stale artefact around.
 */
@Component
public class IntentGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentGenerationService.class);

    /**
     * The model-layer extensions intent generators may emit. Files with one of these extensions at the
     * project root are owned (and scrubbed) by the generation pass.
     */
    private static final Set<String> INTENT_OWNED_EXTENSIONS = Set.of(".edm", ".model", ".bpmn", ".form", ".report", ".roles", ".access",
            ".dsm", ".schema", ".table", ".view", ".csvim", ".csv", ".glue", ".print", ".extension");

    private final List<IntentTargetGenerator> generators;
    private final IRepository repository;

    public IntentGenerationService(List<IntentTargetGenerator> generators, IRepository repository) {
        this.generators = generators;
        this.repository = repository;
    }

    /** Maps a generated model file's extension to its {@code .settings} generation-recipe key. */
    private static final Map<String, String> EXTENSION_TO_RECIPE =
            Map.of(".model", "model", ".glue", "glue", ".form", "form", ".report", "report");

    /**
     * The order in which the model-to-code recipes run (model first - it creates the entities/repos).
     */
    private static final List<String> RECIPE_ORDER = List.of("model", "glue", "form", "report");

    /**
     * Outcome of a generation pass: the files emitted, the stale files scrubbed, and the model-to-code
     * generation plan (which template + parameters to run against each generated model file, from the
     * project's {@code .settings}). The editor's Generate replays the plan via the generation service.
     *
     * @param written bare names of the model files this pass produced
     * @param scrubbed bare names of previously generated files removed because the intent no longer
     *        declares their slice
     * @param codeGenerations ordered plan entries, each {@code {path, templateId, parameters}}
     */
    public record GenerationResult(List<String> written, List<String> scrubbed, List<Map<String, Object>> codeGenerations) {
    }

    /**
     * Parse the given intent YAML and (re)generate every model artefact in the target project,
     * scrubbing stale intent-owned files afterwards.
     *
     * @param yaml the raw {@code .intent} document
     * @param projectRoot repository path of the target project root, e.g.
     *        {@code /users/admin/workspace/my-library}
     * @param projectName the target project name
     * @param workspaceName the workspace the project lives in (used for cross-model projection paths)
     * @param fallbackName base name used for single-file outputs when the YAML omits {@code name:} -
     *        conventionally the intent file's base name
     * @return the files written and scrubbed
     * @throws org.eclipse.dirigible.components.intent.parser.IntentValidationException if the document
     *         has structural problems
     */
    public GenerationResult generate(String yaml, String projectRoot, String projectName, String workspaceName, String fallbackName) {
        IntentModel model = IntentParser.parse(yaml);
        IntentGenerationContext context =
                new IntentGenerationContext(model, projectRoot, projectName, workspaceName, fallbackName, repository);
        context.setSettings(loadOrScaffoldSettings(context));
        LOGGER.info("Generating model files for intent [{}] under [{}] via {} generator(s)", IntentNaming.baseName(context), projectRoot,
                generators.size());
        for (IntentTargetGenerator generator : generators) {
            try {
                generator.generate(context);
            } catch (IntentValidationException e) {
                // A fatal authoring error the developer must fix (e.g. an unresolvable cross-model
                // dependency) - surface it to the caller (-> 422), do NOT isolate it like a generator bug.
                throw e;
            } catch (RuntimeException e) {
                LOGGER.error("Intent generator [{}] failed for project [{}]", generator.name(), projectName, e);
            }
        }
        List<String> scrubbed = scrubStaleModelFiles(projectRoot, context.getWrittenFileNames());
        List<Map<String, Object>> plan = buildCodeGenerationPlan(context.getSettings(), context.getWrittenFileNames());
        return new GenerationResult(new ArrayList<>(context.getWrittenFileNames()), scrubbed, plan);
    }

    /**
     * The model-to-code plan: for each generated model file whose type has a recipe in the
     * {@code .settings}, an entry naming the template + parameters to run against it. Ordered so the
     * full-stack {@code model} runs first. Replayed by the editor's Generate via the generation
     * service.
     */
    private List<Map<String, Object>> buildCodeGenerationPlan(IntentSettings settings, Set<String> written) {
        List<Map<String, Object>> plan = new ArrayList<>();
        for (String recipeKey : RECIPE_ORDER) {
            IntentSettings.Recipe recipe = settings.getGeneration()
                                                   .get(recipeKey);
            if (recipe == null || recipe.getTemplateId() == null || recipe.getTemplateId()
                                                                          .isBlank()) {
                continue;
            }
            for (String fileName : written) {
                int dot = fileName.lastIndexOf('.');
                if (dot < 0 || !recipeKey.equals(EXTENSION_TO_RECIPE.get(fileName.substring(dot)))) {
                    continue;
                }
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("path", fileName);
                entry.put("templateId", recipe.getTemplateId());
                entry.put("parameters", recipe.getParameters());
                plan.add(entry);
            }
        }
        return plan;
    }

    /**
     * Load the project's {@code <intent>.settings} if it exists; otherwise scaffold the initial version
     * (default template recipes + per-artefact override stubs) and write it. The settings file is
     * developer-owned: it is written once and never overwritten or scrubbed afterwards, so manual edits
     * (template choices, {@code generate:false} overrides) survive regeneration.
     */
    private IntentSettings loadOrScaffoldSettings(IntentGenerationContext context) {
        String fileName = IntentNaming.baseName(context) + ".settings";
        String path = context.getProjectRoot() + "/" + fileName;
        var resource = repository.getResource(path);
        if (resource.exists()) {
            try {
                return IntentSettings.parse(new String(resource.getContent(), java.nio.charset.StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                LOGGER.error("Failed to parse [{}] - falling back to defaults (not overwriting your file)", fileName, e);
                return IntentSettings.scaffold(context.getModel());
            }
        }
        IntentSettings settings = IntentSettings.scaffold(context.getModel());
        context.writeModelFile(fileName, settings.toJson());
        LOGGER.info("Scaffolded initial settings [{}/{}]", context.getProjectRoot(), fileName);
        return settings;
    }

    /**
     * Remove intent-owned model files at the project root that are not part of the current output set.
     */
    private List<String> scrubStaleModelFiles(String projectRoot, Set<String> keep) {
        List<String> scrubbed = new ArrayList<>();
        ICollection project = repository.getCollection(projectRoot);
        if (!project.exists()) {
            return scrubbed;
        }
        for (String fileName : project.getResourcesNames()) {
            if (keep.contains(fileName) || !isIntentOwned(fileName)) {
                continue;
            }
            try {
                repository.removeResource(projectRoot + "/" + fileName);
                scrubbed.add(fileName);
                LOGGER.info("Scrubbed stale intent output [{}/{}]", projectRoot, fileName);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to scrub stale intent output [{}/{}]", projectRoot, fileName, e);
            }
        }
        return scrubbed;
    }

    private static boolean isIntentOwned(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && INTENT_OWNED_EXTENSIONS.contains(fileName.substring(dot));
    }
}
