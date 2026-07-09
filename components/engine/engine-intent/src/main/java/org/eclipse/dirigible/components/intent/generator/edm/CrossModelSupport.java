/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Resolves the facts a consuming model needs about a cross-model relation's target entity: the
 * perspective it lives under in its owner model (drives the dropdown's REST URL), its table name
 * and primary-key column (drive the projection entity + the cross-model foreign key in the {@code
 * .schema}), and its key / label fields (drive the dropdown).
 *
 * <p>
 * Resolution is <b>order-independent</b> and reads a <b>real</b> model from two equally valid
 * sources: the owner's WORKSPACE {@code .model} (a locally-developed dependency generated this
 * cycle), else its PUBLISHED {@code .model} in the registry. The registry is not a mere fallback -
 * a prebuilt, prepackaged npm-module dependency ({@code uoms}, {@code currencies}, ...) ships
 * <b>only</b> in the registry and is never in the workspace, so the registry read is a first-class
 * source. This makes the outcome immune to the alphabetical "generate all" order (e.g.
 * {@code sales-invoices} generated before its {@code uoms} leaf).
 *
 * <p>
 * If neither source has the model, resolution <b>fails loudly</b> with an
 * {@link IntentValidationException} - it does NOT guess a perspective from the naming convention,
 * because a wrong guess for a setting target (Settings vs the entity name) silently produced a dead
 * dropdown / 404 controller URL. Generate the dependency, or install/publish its prebuilt module,
 * first.
 */
public final class CrossModelSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossModelSupport.class);
    private static final Gson GSON = new Gson();

    private CrossModelSupport() {}

    /**
     * Everything the consuming generator needs about a cross-model target.
     *
     * @param resolved whether the owner model was found and parsed (false → convention fallbacks)
     * @param perspectiveName the target's perspective in its owner model ({@code Settings} for a
     *        setting, else the entity name)
     * @param tableDataName the target's physical table name (owner's {@code dataName})
     * @param keyField the target's primary-key property name (PascalCase)
     * @param keyColumn the target's primary-key column name
     * @param labelField the target's label property name (PascalCase) for the dropdown value
     * @param fkType the JDBC type of the foreign-key column (the target PK's type)
     * @param propertyNames the target's property names (PascalCase), for validating references to its
     *        properties (a {@code dependsOn} {@code valueFrom}/{@code filterBy}); {@code null} when the
     *        model was not resolved (convention fallback) - callers then skip the check
     */
    public record TargetInfo(boolean resolved, String perspectiveName, String tableDataName, String keyField, String keyColumn,
            String labelField, String fkType, java.util.Set<String> propertyNames, String hierarchyProperty) {
    }

    @SuppressWarnings("unchecked")
    public static TargetInfo resolve(IntentGenerationContext context, UsesIntent uses, String targetEntity) {
        String alias = uses.getModel();
        String project = uses.resolveProject();
        // Naming-convention DEFAULTS for the within-model sub-fields (table/key column) a found model may
        // omit - NOT a substitute for a missing model (we fail loudly for that, below).
        TargetInfo defaults = convention(alias, targetEntity);
        if (context == null || context.getRepository() == null || context.getProjectRoot() == null) {
            return defaults; // no repository to read from (e.g. a unit test) - cannot resolve against a real model
        }
        IRepository repository = context.getRepository();
        // Order-INDEPENDENT resolution against a REAL model, from two equally valid sources:
        // 1. the sibling's WORKSPACE .model - a locally-developed dependency generated this cycle; and
        // 2. its PUBLISHED .model in the registry - which is ALSO where a prebuilt, prepackaged npm-module
        // dependency (uoms, currencies, ...) lives: such a dependency is NEVER in the workspace, only
        // in the registry. So the registry read is a first-class source, not merely a fallback.
        // Workspace wins when present (local dev overrides the published copy); otherwise the registry.
        // This makes the result independent of project generation order (the alphabetical "generate all"
        // trap where `sales-invoices` is generated before its `uoms` leaf).
        String workspacePath = siblingModelPath(context.getProjectRoot(), project, alias);
        TargetInfo fromWorkspace = readTarget(repository, workspacePath, targetEntity, defaults);
        if (fromWorkspace != null) {
            return fromWorkspace;
        }
        String registryPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project + "/" + alias + ".model";
        TargetInfo fromRegistry = readTarget(repository, registryPath, targetEntity, defaults);
        if (fromRegistry != null) {
            return fromRegistry;
        }
        // Fail LOUDLY. We never guess a perspective from the naming convention: guessing wrong for a
        // setting target (Settings vs the entity name) silently produces a dead dropdown / 404 controller
        // URL - the bug this replaced. A cross-model dependency must resolve against a real model.
        throw new IntentValidationException(List.of("Cross-model relation target [" + targetEntity + "] (model alias [" + alias
                + "], project [" + project + "]) cannot be resolved: no model found in the workspace [" + workspacePath
                + "] or the registry [" + registryPath + "]. Generate the [" + alias
                + "] model first, or install/publish its prebuilt module so its .model is in the registry."));
    }

    /**
     * Read the cross-model target entity's facts from a {@code .model} resource, or {@code null} when
     * the resource is absent, unparseable, or does not contain the target entity (so the caller can try
     * the next source).
     */
    @SuppressWarnings("unchecked")
    private static TargetInfo readTarget(IRepository repository, String modelPath, String targetEntity, TargetInfo fallback) {
        if (modelPath == null) {
            return null;
        }
        IResource resource = repository.getResource(modelPath);
        if (!resource.exists()) {
            return null;
        }
        try {
            String content = new String(resource.getContent(), StandardCharsets.UTF_8);
            Map<String, Object> root = GSON.fromJson(content, Map.class);
            Map<String, Object> body = (Map<String, Object>) root.get("model");
            List<Map<String, Object>> entities = body == null ? null : (List<Map<String, Object>>) body.get("entities");
            if (entities == null) {
                return null;
            }
            for (Map<String, Object> entity : entities) {
                if (!targetEntity.equals(entity.get("name"))) {
                    continue;
                }
                String perspective = str(entity.get("perspectiveName"), targetEntity);
                String tableDataName = str(entity.get("dataName"), fallback.tableDataName());
                List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.get("properties");
                String keyField = "Id";
                String keyColumn = fallback.keyColumn();
                String fkType = "INTEGER";
                String labelField = "Name";
                java.util.Set<String> propertyNames = null;
                if (properties != null) {
                    propertyNames = new java.util.LinkedHashSet<>();
                    for (Map<String, Object> p : properties) {
                        if ("true".equals(String.valueOf(p.get("dataPrimaryKey")))) {
                            keyField = str(p.get("name"), keyField);
                            keyColumn = str(p.get("dataName"), keyColumn);
                            fkType = str(p.get("dataType"), fkType);
                        }
                        String propertyName = str(p.get("name"), null);
                        if (propertyName != null) {
                            propertyNames.add(propertyName);
                        }
                    }
                    labelField = labelField(properties, keyField);
                }
                String hierarchyProperty = str(entity.get("hierarchyProperty"), null);
                return new TargetInfo(true, perspective, tableDataName, keyField, keyColumn, labelField, fkType, propertyNames,
                        hierarchyProperty);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to read owner model [{}] for cross-model target [{}]", modelPath, targetEntity, e);
        }
        return null;
    }

    /**
     * The label property: the target's {@code Name} field, else its first non-PK string field, else the
     * key.
     */
    private static String labelField(List<Map<String, Object>> properties, String keyField) {
        for (Map<String, Object> p : properties) {
            if ("Name".equalsIgnoreCase(String.valueOf(p.get("name")))) {
                return str(p.get("name"), "Name");
            }
        }
        for (Map<String, Object> p : properties) {
            boolean pk = "true".equals(String.valueOf(p.get("dataPrimaryKey")));
            if (!pk && "VARCHAR".equals(String.valueOf(p.get("dataType")))) {
                return str(p.get("name"), keyField);
            }
        }
        return keyField;
    }

    /**
     * Deterministic fallbacks matching the Dirigible naming convention an intent owner would produce:
     * table {@code <ALIAS>_<ENTITY>}, PK column {@code <ENTITY>_ID}, integer key {@code Id}, label
     * {@code Name}, and (lacking the owner model) a PRIMARY-style perspective equal to the entity name.
     */
    private static TargetInfo convention(String alias, String targetEntity) {
        String table = IntentNaming.upperSnake(alias) + "_" + IntentNaming.upperSnake(targetEntity);
        String keyColumn = IntentNaming.upperSnake(targetEntity) + "_ID";
        return new TargetInfo(false, targetEntity, table, "Id", keyColumn, "Name", "INTEGER", null, null);
    }

    /**
     * The repository path of a sibling project's {@code <alias>.model}. {@code projectRoot} is
     * {@code /users/<user>/<workspace>/<thisProject>}; the owner is a sibling under the same workspace.
     */
    private static String siblingModelPath(String projectRoot, String project, String alias) {
        int lastSlash = projectRoot.lastIndexOf('/');
        if (lastSlash <= 0) {
            return null;
        }
        String workspaceRoot = projectRoot.substring(0, lastSlash);
        return workspaceRoot + "/" + project + "/" + alias + ".model";
    }

    private static String str(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String s = String.valueOf(value);
        return s.isBlank() ? fallback : s;
    }

    /** Lower-cased helper retained for symmetry with callers that key on the alias. */
    static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
