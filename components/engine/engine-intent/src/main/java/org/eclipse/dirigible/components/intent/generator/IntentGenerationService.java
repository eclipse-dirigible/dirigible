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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.ide.template.service.model.ModelGenerationService;
import org.eclipse.dirigible.components.intent.LoggedValue;
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
            ".dsm", ".schema", ".table", ".view", ".csvim", ".csv", ".glue", ".print", ".extension", ".test");

    private final List<IntentTargetGenerator> generators;
    private final IRepository repository;
    private final ModelGenerationService modelGenerationService;

    public IntentGenerationService(List<IntentTargetGenerator> generators, IRepository repository,
            ModelGenerationService modelGenerationService) {
        this.generators = generators;
        this.repository = repository;
        this.modelGenerationService = modelGenerationService;
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
     * generations it ran (which template + parameters against which generated model file, from the
     * project's {@code .settings}) with the outcome of each.
     *
     * @param written bare names of the model files this pass produced
     * @param scrubbed bare names of previously generated files removed because the intent no longer
     *        declares their slice
     * @param codeGenerations ordered entries, each {@code {path, templateId, parameters, generated}}
     *        plus an {@code error} when that one failed - the pass runs them itself, so this is a
     *        report of what it did, not a plan for the caller to replay
     * @param issues non-fatal generation issues (glue that could not be emitted) - the pass still
     *        succeeded, but a caller should surface these so the drop is not silent (dirigible #6360)
     */
    public record GenerationResult(List<String> written, List<String> scrubbed, List<Map<String, Object>> codeGenerations,
            List<String> issues) {
    }

    /**
     * Parse the given intent YAML and (re)generate every model artefact in the target project,
     * scrubbing stale intent-owned files afterwards.
     *
     * @param yaml the raw {@code .intent} document
     * @param projectRoot repository path of the target project root, e.g.
     *        {@code /users/admin/workspace/my-library}
     * @param projectName the target project name
     * @param workspaceName the workspace the project lives in (generation-target identity; no generator
     *        reads it - see {@code IntentGenerationContext#getWorkspaceName()})
     * @param fallbackName base name used for single-file outputs when the YAML omits {@code name:} -
     *        conventionally the intent file's base name
     * @return the files written and scrubbed
     * @throws org.eclipse.dirigible.components.intent.parser.IntentValidationException if the document
     *         has structural problems
     */
    public GenerationResult generate(String yaml, String projectRoot, String projectName, String workspaceName, String fallbackName) {
        return generate(yaml, projectRoot, projectName, workspaceName, fallbackName, false);
    }

    /**
     * The same pass, optionally as the declared BOOTSTRAP of a mutual cross-model cycle (dirigible
     * #6539): a {@code generates} whose target model has not been generated yet is skipped, and named
     * in the returned issues, instead of failing the whole Generate. Nothing else is relaxed - see
     * {@link BootstrapRequiredException}.
     *
     * @param yaml the raw {@code .intent} document
     * @param projectRoot repository path of the target project root
     * @param projectName the target project name
     * @param workspaceName the workspace the project lives in
     * @param fallbackName base name used for single-file outputs when the YAML omits {@code name:}
     * @param bootstrap whether an unresolvable cross-model create-from may be skipped
     * @return the files written and scrubbed
     * @throws org.eclipse.dirigible.components.intent.parser.IntentValidationException if the document
     *         has structural problems
     */
    public GenerationResult generate(String yaml, String projectRoot, String projectName, String workspaceName, String fallbackName,
            boolean bootstrap) {
        IntentModel model = IntentParser.parse(yaml);
        IntentGenerationContext context =
                new IntentGenerationContext(model, projectRoot, projectName, workspaceName, fallbackName, repository, false, bootstrap);
        context.setSettings(loadOrScaffoldSettings(context));
        LOGGER.info("Generating model files for intent [{}] under [{}] via {} generator(s)", LoggedValue.of(IntentNaming.baseName(context)),
                LoggedValue.of(projectRoot), generators.size());
        // The shape this project declared BEFORE the pass. Compared against what the pass writes, it is
        // what makes a removal visible - see the impact report below.
        String modelFileName = IntentNaming.baseName(context) + ".model";
        Map<String, Set<String>> shapeBefore = CrossModelImpactSupport.readShape(repository, projectRoot + "/" + modelFileName);
        for (IntentTargetGenerator generator : generators) {
            try {
                generator.generate(context);
            } catch (IntentValidationException e) {
                // A fatal authoring error the developer must fix (e.g. an unresolvable cross-model
                // dependency) - surface it to the caller (-> 422), do NOT isolate it like a generator bug.
                throw e;
            } catch (RuntimeException e) {
                LOGGER.error("Intent generator [{}] failed for project [{}]", generator.name(), LoggedValue.of(projectName), e);
            }
        }
        // A member this pass dropped invalidates the committed generated code of every project that
        // references it cross-model - and nothing else will notice until javac does, in one of THOSE
        // projects (dirigible #6422). Name the regeneration set here, while the removal is being made.
        try {
            CrossModelImpactSupport.reportRemovals(context, shapeBefore, modelFileName);
        } catch (RuntimeException e) {
            LOGGER.error("Cross-model impact report failed for project [{}]", LoggedValue.of(projectName), e);
        }
        List<String> scrubbed = scrubStaleModelFiles(projectRoot, context.getWrittenFileNames());
        List<Map<String, Object>> plan = buildCodeGenerationPlan(context.getSettings(), context.getWrittenFileNames());
        runCodeGenerations(plan, workspaceName, projectName);
        // The developer-facing warnings carry BOTH kinds: the actionable issues and the advisories.
        // Only the assistant's dry run keeps them apart (see dryRun below).
        List<String> warnings = new ArrayList<>(context.getIssues());
        warnings.addAll(context.getAdvisories());
        return new GenerationResult(new ArrayList<>(context.getWrittenFileNames()), scrubbed, plan, warnings);
    }

    /**
     * Run the generation pass for VALIDATION only: every generator executes and reports against the
     * same context a real Generate would get, but nothing is written and nothing is scrubbed (dirigible
     * #6956). This is the band the parser legitimately cannot see - a document that parses but whose
     * generation would drop glue, or would be refused by a generation-time check.
     *
     * <p>
     * A generator throwing {@link IntentValidationException} - fatal on a real Generate (422) - is
     * collected here instead: the caller wants the complete list of what generation would object to,
     * not the first objection.
     *
     * <p>
     * The project coordinates may all be {@code null} - a proposal that belongs to no project yet, the
     * AI assistant's case. Reads then find nothing (developer-owned files are treated as absent), and
     * cross-model references resolve by naming convention rather than against a real owner model
     * ({@code CrossModelSupport} skips its strict checks for unresolved targets) - deliberately, so a
     * dependency that only exists in some workspace never produces a false "cannot be resolved" issue
     * for the repair loop to burn a round on.
     *
     * @param yaml the raw {@code .intent} document; must already parse (the caller handles
     *        {@link IntentValidationException} from the parse separately)
     * @param projectRoot repository path of the target project root, or {@code null} when the document
     *        belongs to no project
     * @param projectName the target project name, or {@code null}
     * @param workspaceName the workspace, or {@code null}
     * @param fallbackName base name for single-file outputs when the YAML omits {@code name:}
     * @return the actionable issues a real generation pass would report - advisories that no change to
     *         this document can address are deliberately excluded
     */
    public List<String> dryRun(String yaml, String projectRoot, String projectName, String workspaceName, String fallbackName) {
        IntentModel model = IntentParser.parse(yaml);
        IntentGenerationContext context =
                new IntentGenerationContext(model, projectRoot, projectName, workspaceName, fallbackName, repository, true);
        context.setSettings(loadSettingsReadOnly(context));
        for (IntentTargetGenerator generator : generators) {
            try {
                generator.generate(context);
            } catch (IntentValidationException e) {
                e.getIssues()
                 .forEach(context::addIssue);
            } catch (RuntimeException e) {
                // The same isolation the real pass applies to a generator bug: one broken slice must
                // not silence what the other generators have to say about the document.
                LOGGER.error("Intent generator [{}] failed during a dry run", generator.name(), e);
            }
        }
        return context.getIssues();
    }

    /**
     * Validation-only pass for a document with no project context - the AI assistant's proposals.
     *
     * @param yaml the raw {@code .intent} document
     * @return the actionable issues a real generation pass would report
     */
    public List<String> dryRun(String yaml) {
        return dryRun(yaml, null, null, null, "app");
    }

    /**
     * Run the model-to-code plan, one generation per generated model file, in the recipe order.
     *
     * <p>
     * Each entry records its own outcome ({@code generated}, and {@code error} when it failed) rather
     * than aborting the pass: the model files are already on disk at this point, so a template that
     * cannot render is a partial result the caller has to be told about - not a reason to report the
     * whole Generate as failed. A failure is isolated to its entry, so one broken recipe does not cost
     * the others their code.
     *
     * @param plan the plan entries, each mutated with its outcome
     * @param workspaceName the workspace the project lives in
     * @param projectName the project the models were written into
     */
    private void runCodeGenerations(List<Map<String, Object>> plan, String workspaceName, String projectName) {
        for (Map<String, Object> entry : plan) {
            String path = String.valueOf(entry.get("path"));
            String templateId = String.valueOf(entry.get("templateId"));
            // A copy: the pipeline derives its whole parameter graph into the map it is handed, and this
            // entry goes back to the caller as the recipe it was, not as the derivation.
            @SuppressWarnings("unchecked")
            Map<String, Object> recipeParameters = (Map<String, Object>) entry.get("parameters");
            Map<String, Object> parameters = recipeParameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(recipeParameters);
            try {
                modelGenerationService.generate(workspaceName, projectName, path, templateId, parameters);
                entry.put("generated", Boolean.TRUE);
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Failed to generate code from [{}/{}] with template [{}]", LoggedValue.of(projectName), LoggedValue.of(path),
                        LoggedValue.of(templateId), e);
                entry.put("generated", Boolean.FALSE);
                entry.put("error", e.getMessage());
            }
        }
    }

    /**
     * The model-to-code plan: for each generated model file whose type has a recipe in the
     * {@code .settings}, an entry naming the template + parameters to run against it. Ordered so the
     * full-stack {@code model} runs first, since it creates the entities and repositories the rest
     * builds on.
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
        IntentSettings existing = readSettings(context, fileName);
        if (existing != null) {
            return existing;
        }
        IntentSettings settings = IntentSettings.scaffold(context.getModel());
        context.writeModelFile(fileName, settings.toJson());
        LOGGER.info("Scaffolded initial settings [{}/{}]", LoggedValue.of(context.getProjectRoot()), LoggedValue.of(fileName));
        return settings;
    }

    /**
     * The settings a dry run works with: the project's real {@code .settings} when there is a project
     * to read them from, else an in-memory scaffold. Never writes - the scaffold-and-write of a first
     * real Generate is exactly the side effect a dry run must not have.
     */
    private IntentSettings loadSettingsReadOnly(IntentGenerationContext context) {
        IntentSettings existing =
                context.getProjectRoot() == null ? null : readSettings(context, IntentNaming.baseName(context) + ".settings");
        return existing != null ? existing : IntentSettings.scaffold(context.getModel());
    }

    /**
     * The parsed project settings, or {@code null} when absent (or unreadable - defaults then apply).
     */
    private IntentSettings readSettings(IntentGenerationContext context, String fileName) {
        var resource = repository.getResource(context.getProjectRoot() + "/" + fileName);
        if (!resource.exists()) {
            return null;
        }
        try {
            return IntentSettings.parse(new String(resource.getContent(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse [{}] - falling back to defaults (not overwriting your file)", LoggedValue.of(fileName), e);
            return IntentSettings.scaffold(context.getModel());
        }
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
                LOGGER.info("Scrubbed stale intent output [{}/{}]", LoggedValue.of(projectRoot), LoggedValue.of(fileName));
            } catch (RuntimeException e) {
                LOGGER.error("Failed to scrub stale intent output [{}/{}]", LoggedValue.of(projectRoot), LoggedValue.of(fileName), e);
            }
        }
        return scrubbed;
    }

    private static boolean isIntentOwned(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && INTENT_OWNED_EXTENSIONS.contains(fileName.substring(dot));
    }

}
