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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.dirigible.components.intent.LoggedValue;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Names the projects a generation pass just invalidated: when this model stops declaring an entity
 * or a property, every OTHER model that references it cross-model is left holding generated code
 * that still dereferences the member (dirigible #6422).
 *
 * <p>
 * <b>Why the owner side has to say it.</b> A consumer's cross-model references are checked when the
 * CONSUMER is generated - against the owner's {@code .model}, through
 * {@link org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport}. That check is
 * useless here, because the consumer is not regenerated: it did not change. The owner's own pass is
 * green, the consumer's committed {@code gen/} is now wrong, and nothing connects the two until
 * {@code javac} runs - in a different project, on a different day. Client Java compiles in one
 * all-or-nothing batch, so the blast radius of that one stale reference is every module's beans,
 * not just the consumer's.
 *
 * <p>
 * <b>What it reports.</b> The removal, plus the projects that must be regenerated because of it -
 * the regeneration set, discoverable at the moment the removal is made. Reported as non-fatal
 * generation issues ({@link IntentGenerationContext#addIssue(String)}): the owner's intent is
 * legitimate and its generation succeeds, so this is a warning, never a 422.
 *
 * <p>
 * <b>Scope of the scan.</b> Only when something was actually removed (the overwhelmingly common
 * pass removes nothing and pays one file read). Candidates are the sibling projects of the same
 * workspace plus the published projects in the registry - the same two sources, workspace-wins,
 * that cross-model resolution reads; a consumer that lives in neither is invisible to any
 * generation-time check.
 */
final class CrossModelImpactSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossModelImpactSupport.class);
    private static final Gson GSON = new Gson();

    private CrossModelImpactSupport() {}

    /**
     * One member this pass stopped declaring.
     *
     * @param entity the entity name
     * @param property the property name, or {@code null} when the whole entity is gone
     */
    record Removal(String entity, String property) {

        String describe() {
            return property == null ? "entity [" + entity + "]" : "[" + entity + "." + property + "]";
        }
    }

    /**
     * The entity → property-names shape of a generated {@code .model}, or an empty map when the file is
     * absent or unreadable (a first-ever generation removes nothing).
     */
    static Map<String, Set<String>> readShape(IRepository repository, String modelPath) {
        if (repository == null || modelPath == null) {
            return Map.of();
        }
        IResource resource = repository.getResource(modelPath);
        if (!resource.exists()) {
            return Map.of();
        }
        try {
            return parseShape(new String(resource.getContent(), StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            LOGGER.warn("Could not read the previous shape of [{}] - skipping the cross-model impact report", LoggedValue.of(modelPath), e);
            return Map.of();
        }
    }

    /** The entity → property-names shape of a {@code .model} document. */
    @SuppressWarnings("unchecked")
    static Map<String, Set<String>> parseShape(String modelJson) {
        Map<String, Set<String>> shape = new LinkedHashMap<>();
        if (modelJson == null || modelJson.isBlank()) {
            return shape;
        }
        Map<String, Object> root = GSON.fromJson(modelJson, Map.class);
        Map<String, Object> body = root == null ? null : (Map<String, Object>) root.get("model");
        List<Map<String, Object>> entities = body == null ? null : (List<Map<String, Object>>) body.get("entities");
        if (entities == null) {
            return shape;
        }
        for (Map<String, Object> entity : entities) {
            Object name = entity.get("name");
            if (name == null) {
                continue;
            }
            Set<String> properties = new LinkedHashSet<>();
            List<Map<String, Object>> declared = (List<Map<String, Object>>) entity.get("properties");
            if (declared != null) {
                for (Map<String, Object> property : declared) {
                    Object propertyName = property.get("name");
                    if (propertyName != null) {
                        properties.add(String.valueOf(propertyName));
                    }
                }
            }
            shape.put(String.valueOf(name), properties);
        }
        return shape;
    }

    /**
     * What {@code after} no longer declares. An entity that disappeared is reported once, as the entity
     * - not once per property it took with it.
     */
    static List<Removal> removals(Map<String, Set<String>> before, Map<String, Set<String>> after) {
        List<Removal> removals = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entity : before.entrySet()) {
            Set<String> current = after.get(entity.getKey());
            if (current == null) {
                removals.add(new Removal(entity.getKey(), null));
                continue;
            }
            for (String property : entity.getValue()) {
                if (!current.contains(property)) {
                    removals.add(new Removal(entity.getKey(), property));
                }
            }
        }
        return removals;
    }

    /**
     * The projects, among the given candidates, that hold a cross-model relation to {@code entity} in
     * model {@code ownerModel} of project {@code ownerProject} - i.e. those whose generated code
     * dereferences the entity and must be regenerated. Sorted, so the message is stable.
     *
     * @param ownerModel the owner's model alias (its {@code .model} base name)
     * @param ownerProject the project owning that model
     * @param entity the affected entity
     * @param candidateIntents candidate project name → its raw {@code .intent} document
     * @return the affected project names
     */
    static List<String> consumers(String ownerModel, String ownerProject, String entity, Map<String, String> candidateIntents) {
        List<String> consumers = new ArrayList<>();
        for (Map.Entry<String, String> candidate : candidateIntents.entrySet()) {
            IntentModel model;
            try {
                model = IntentParser.parse(candidate.getValue());
            } catch (RuntimeException e) {
                // A candidate we cannot parse is somebody else's problem - it must not fail this pass.
                LOGGER.debug("Skipping unparseable intent of project [{}] while reporting cross-model impact",
                        LoggedValue.of(candidate.getKey()), e);
                continue;
            }
            if (declaresUse(model, ownerModel, ownerProject) && relatesTo(model, ownerModel, entity)) {
                consumers.add(candidate.getKey());
            }
        }
        java.util.Collections.sort(consumers);
        return consumers;
    }

    /** Whether the model declares a {@code uses:} entry resolving to the owner's model and project. */
    private static boolean declaresUse(IntentModel model, String ownerModel, String ownerProject) {
        for (UsesIntent uses : model.getUses()) {
            if (ownerModel.equals(uses.getModel()) && ownerProject.equals(uses.resolveProject())) {
                return true;
            }
        }
        return false;
    }

    /** Whether any entity of the model holds a relation to {@code entity} through the owner's alias. */
    private static boolean relatesTo(IntentModel model, String ownerModel, String entity) {
        for (EntityIntent candidate : model.getEntities()) {
            for (RelationIntent relation : candidate.getRelations()) {
                if (ownerModel.equals(relation.getModel()) && entity.equals(relation.getTo())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Compare the shape captured before the pass against the one it just wrote and, for every removed
     * member that some other project references cross-model, add a generation issue naming the projects
     * to regenerate.
     *
     * @param context the pass context (source of the repository, project and issue sink)
     * @param before the shape captured before the generators ran, from
     *        {@link #readShape(IRepository, String)}
     * @param modelFileName the bare name of the generated {@code .model}
     */
    static void reportRemovals(IntentGenerationContext context, Map<String, Set<String>> before, String modelFileName) {
        if (before.isEmpty() || context.getRepository() == null) {
            return;
        }
        String modelPath = context.getProjectRoot() + "/" + modelFileName;
        List<Removal> removals = removals(before, readShape(context.getRepository(), modelPath));
        if (removals.isEmpty()) {
            return;
        }
        String ownerModel = modelFileName.substring(0, modelFileName.lastIndexOf('.'));
        Map<String, String> candidates = candidateIntents(context);
        if (candidates.isEmpty()) {
            return;
        }
        for (Removal removal : removals) {
            List<String> consumers = consumers(ownerModel, context.getProjectName(), removal.entity(), candidates);
            if (consumers.isEmpty()) {
                continue;
            }
            String issue = "Model [" + ownerModel + "] no longer declares " + removal.describe()
                    + ", but the generated code of these projects still dereferences it cross-model and will not compile until they are regenerated: "
                    + consumers + " (client Java compiles as one batch, so a single stale reference stops every module's beans)";
            LOGGER.warn(LoggedValue.of(issue));
            context.addIssue(issue);
        }
    }

    /**
     * Every project that could be a consumer, as project name → its {@code .intent} document: the
     * sibling projects of this workspace, then the published projects in the registry (a prebuilt,
     * prepackaged module dependency ships only there). The workspace copy wins, and this project itself
     * is never a candidate.
     */
    private static Map<String, String> candidateIntents(IntentGenerationContext context) {
        Map<String, String> candidates = new TreeMap<>();
        String projectRoot = context.getProjectRoot();
        int lastSlash = projectRoot.lastIndexOf('/');
        if (lastSlash > 0) {
            collectIntents(context, projectRoot.substring(0, lastSlash), candidates);
        }
        collectIntents(context, IRepositoryStructure.PATH_REGISTRY_PUBLIC, candidates);
        return candidates;
    }

    /** Add the {@code .intent} of every project directly under {@code root} that is not yet known. */
    private static void collectIntents(IntentGenerationContext context, String root, Map<String, String> candidates) {
        try {
            ICollection collection = context.getRepository()
                                            .getCollection(root);
            if (!collection.exists()) {
                return;
            }
            for (ICollection project : collection.getCollections()) {
                if (project.getName()
                           .equals(context.getProjectName())
                        || candidates.containsKey(project.getName())) {
                    continue;
                }
                for (String fileName : project.getResourcesNames()) {
                    if (fileName.endsWith(".intent")) {
                        candidates.put(project.getName(), new String(project.getResource(fileName)
                                                                            .getContent(),
                                StandardCharsets.UTF_8));
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            // Best-effort discovery: an unreadable neighbour must never fail the owner's generation.
            LOGGER.warn("Could not scan [{}] for cross-model consumers", LoggedValue.of(root), e);
        }
    }
}
