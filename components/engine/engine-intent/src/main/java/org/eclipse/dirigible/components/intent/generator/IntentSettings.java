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

import org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport.Resolver;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@code <intent>.settings} document: the per-project recipe for turning the generated model
 * files into code, plus per-artefact overrides. Scaffolded once by the generation pass when absent
 * and then owned by the developer (loaded and respected, never overwritten or scrubbed) - so it is
 * the place to pin template choices, parameters, and "don't generate this, I wrote it by hand"
 * decisions.
 * <ul>
 * <li>{@code generation} - keyed by model type ({@code model} / {@code glue} / {@code form} /
 * {@code report}); each entry is the template id + parameters the Generate button replays to
 * produce code (the same templates/params used by hand today). Consumed in phase 2 (Generate
 * chaining).</li>
 * <li>{@code overrides} - per category ({@code triggers} / {@code resolvers} / {@code forms}) a map
 * of artefact name to {@code {generate: true|false}}; {@code false} means the generator skips that
 * artefact so an existing hand-written one is used (the BPMN/DAO still reference it).</li>
 * <li>{@code userTasks.candidateGroupsExtra} - extra candidate groups appended to every generated
 * user task (defaults to {@code ADMINISTRATOR} so an administrator can always claim).</li>
 * </ul>
 * Parsed with a plain {@link Gson} (not the platform {@code JsonHelper}, which would null out
 * un-{@code @Expose}d fields).
 */
public final class IntentSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
                                                      .disableHtmlEscaping()
                                                      .create();

    /** One model-to-code template invocation. */
    public static final class Recipe {
        private String templateId;
        private Map<String, Object> parameters = new LinkedHashMap<>();

        Recipe(String templateId, Map<String, Object> parameters) {
            this.templateId = templateId;
            this.parameters = parameters;
        }

        public String getTemplateId() {
            return templateId;
        }

        public Map<String, Object> getParameters() {
            return parameters == null ? Map.of() : parameters;
        }
    }

    /** Per-artefact override - currently just whether the generator should emit it. */
    public static final class ArtefactOverride {
        private Boolean generate;

        ArtefactOverride(Boolean generate) {
            this.generate = generate;
        }

        public boolean isGenerate() {
            return generate == null || generate;
        }
    }

    /** User-task generation options. */
    public static final class UserTasks {
        private List<String> candidateGroupsExtra = new ArrayList<>();
    }

    private Map<String, Recipe> generation = new LinkedHashMap<>();
    private Map<String, Map<String, ArtefactOverride>> overrides = new LinkedHashMap<>();
    private UserTasks userTasks = new UserTasks();

    /** Parse a settings document; tolerant of missing sections. */
    public static IntentSettings parse(String json) {
        IntentSettings settings = GSON.fromJson(json, IntentSettings.class);
        if (settings == null) {
            settings = new IntentSettings();
        }
        return settings;
    }

    /**
     * Build the initial settings for a model: the default template recipes plus a {@code generate:true}
     * entry per discoverable trigger / resolver / form (so the developer sees the full editable list)
     * and {@code ADMINISTRATOR} as an extra user-task candidate group.
     */
    public static IntentSettings scaffold(IntentModel model) {
        IntentSettings settings = new IntentSettings();
        settings.generation.put("model", new Recipe("template-application-angular-java/template/template.js",
                orderedMap("tablePrefix", "", "dataSource", "DefaultDB")));
        settings.generation.put("glue", new Recipe("template-application-events-java/template/template.js", new LinkedHashMap<>()));
        settings.generation.put("form", new Recipe("template-form-builder-angularjs/template/template.js", new LinkedHashMap<>()));
        settings.generation.put("report",
                new Recipe("template-application-ui-angular-java/template/template-report-file.js", new LinkedHashMap<>()));

        Map<String, ArtefactOverride> triggers = new LinkedHashMap<>();
        for (ProcessIntent process : model.getProcesses()) {
            if (TriggerSupport.onCreateEntity(process) != null && process.getName() != null) {
                triggers.put(process.getName(), new ArtefactOverride(true));
            }
        }
        Map<String, ArtefactOverride> resolvers = new LinkedHashMap<>();
        for (Resolver resolver : ProcessResolverSupport.resolvers(model)) {
            resolvers.put(resolver.handler(), new ArtefactOverride(true));
        }
        Map<String, ArtefactOverride> forms = new LinkedHashMap<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() != null) {
                forms.put(form.getName(), new ArtefactOverride(true));
            }
        }
        if (!triggers.isEmpty()) {
            settings.overrides.put("triggers", triggers);
        }
        if (!resolvers.isEmpty()) {
            settings.overrides.put("resolvers", resolvers);
        }
        if (!forms.isEmpty()) {
            settings.overrides.put("forms", forms);
        }
        settings.userTasks.candidateGroupsExtra.add("ADMINISTRATOR");
        return settings;
    }

    /** Whether the generator should emit the named artefact in the given category (default true). */
    public boolean shouldGenerate(String category, String name) {
        Map<String, ArtefactOverride> categoryOverrides = overrides.get(category);
        if (categoryOverrides == null) {
            return true;
        }
        ArtefactOverride override = categoryOverrides.get(name);
        return override == null || override.isGenerate();
    }

    /** Extra candidate groups to append to every generated user task. */
    public List<String> candidateGroupsExtra() {
        return userTasks == null || userTasks.candidateGroupsExtra == null ? List.of() : userTasks.candidateGroupsExtra;
    }

    /** The model-to-code recipes, keyed by model type. Used by the Generate-chaining step. */
    public Map<String, Recipe> getGeneration() {
        return generation == null ? Map.of() : generation;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    private static Map<String, Object> orderedMap(String... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
