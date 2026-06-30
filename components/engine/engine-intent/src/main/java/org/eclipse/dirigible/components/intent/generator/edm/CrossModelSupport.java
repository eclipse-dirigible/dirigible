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
import org.eclipse.dirigible.repository.api.IRepository;
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
 * When the owner model has already been generated (the recommended leaf-first order), every fact is
 * read from its {@code .model} - exact and order-independent. When it has not (the owner is
 * generated later), the resolution falls back to the deterministic Dirigible naming convention and
 * assumes a PRIMARY (own-perspective) target; the dropdown then resolves only once the owner is
 * generated and published, which the generator logs.
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
     */
    public record TargetInfo(boolean resolved, String perspectiveName, String tableDataName, String keyField, String keyColumn,
            String labelField, String fkType) {
    }

    @SuppressWarnings("unchecked")
    public static TargetInfo resolve(IntentGenerationContext context, UsesIntent uses, String targetEntity) {
        String alias = uses.getModel();
        String project = uses.resolveProject();
        TargetInfo fallback = convention(alias, targetEntity);
        if (context == null || context.getRepository() == null || context.getProjectRoot() == null) {
            return fallback;
        }
        String modelPath = siblingModelPath(context.getProjectRoot(), project, alias);
        if (modelPath == null) {
            return fallback;
        }
        IRepository repository = context.getRepository();
        IResource resource = repository.getResource(modelPath);
        if (!resource.exists()) {
            LOGGER.info(
                    "Cross-model target [{}] of model [{}] not yet generated at [{}] - using convention fallbacks; regenerate after [{}] is generated",
                    targetEntity, alias, modelPath, alias);
            return fallback;
        }
        try {
            String content = new String(resource.getContent(), StandardCharsets.UTF_8);
            Map<String, Object> root = GSON.fromJson(content, Map.class);
            Map<String, Object> body = (Map<String, Object>) root.get("model");
            List<Map<String, Object>> entities = body == null ? null : (List<Map<String, Object>>) body.get("entities");
            if (entities == null) {
                return fallback;
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
                if (properties != null) {
                    for (Map<String, Object> p : properties) {
                        if ("true".equals(String.valueOf(p.get("dataPrimaryKey")))) {
                            keyField = str(p.get("name"), keyField);
                            keyColumn = str(p.get("dataName"), keyColumn);
                            fkType = str(p.get("dataType"), fkType);
                        }
                    }
                    labelField = labelField(properties, keyField);
                }
                return new TargetInfo(true, perspective, tableDataName, keyField, keyColumn, labelField, fkType);
            }
            LOGGER.warn("Cross-model target entity [{}] not found in owner model [{}] - using convention fallbacks", targetEntity,
                    modelPath);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to read owner model [{}] for cross-model target [{}] - using convention fallbacks", modelPath, targetEntity,
                    e);
        }
        return fallback;
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
        return new TargetInfo(false, targetEntity, table, "Id", keyColumn, "Name", "INTEGER");
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
