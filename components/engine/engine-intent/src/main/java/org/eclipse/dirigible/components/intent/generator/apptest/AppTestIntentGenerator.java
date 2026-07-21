/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.apptest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.model.CheckIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.SeedIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Emits one {@code <name>.test} manifest per module — the app-integration-test counterpart of the
 * generated application. The manifest is a JSON description of WHAT the generated app promises
 * (entities, layouts, per-field metadata, seeds, languages, REST + shell coordinates); the generic
 * Playwright runner {@code @aerokit/test} reads it and knows HOW to verify each promise against the
 * generated Harmonia UI and the reused REST controllers. So one runner, versioned with the
 * templates whose markup it drives, checks every generated app the same way — no per-entity spec
 * files to drift.
 *
 * <p>
 * The manifest is intent-owned and scrub-managed like every other model file (extension
 * {@code .test} — a free extension; JS {@code *.test.js} files carry extension {@code js}, so there
 * is no clash). It is regenerated on every Generate
 * ({@link IntentGenerationContext#writeModelFile(String, String)}), never hand-edited: the
 * per-module {@code test/} harness (a few files at the repo root) references it but does not own
 * it.
 *
 * <p>
 * Runs at {@code @Order(900)} — after the {@code EdmIntentGenerator} (200) has written the
 * {@code .model}, whose resolved per-entity metadata (perspective → REST path, {@code dataName} →
 * table, {@code menuLabel} → plural label, layout type, nav group, {@code multilingual}) is the
 * same source the Harmonia templates consume; this generator reads it back rather than re-deriving
 * it. The logical per-field/relation data (types, required, unique, length, major) comes straight
 * from the intent model.
 */
@Component
@Order(900)
public class AppTestIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppTestIntentGenerator.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
                                                      .disableHtmlEscaping()
                                                      .create();

    @Override
    public String name() {
        return "test";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        String baseName = IntentNaming.baseName(context);
        Map<String, Map<String, Object>> edmEntities = readModelEntities(context, baseName);
        if (edmEntities.isEmpty()) {
            LOGGER.debug("Skipping app-test manifest for [{}] — no .model entities to describe", baseName);
            return;
        }

        Map<String, Object> manifest = buildManifest(baseName, context.getProjectName(), model, edmEntities, context);
        context.writeModelFile(baseName + ".test", GSON.toJson(manifest) + "\n");
        LOGGER.debug("Generated app-test manifest [{}.test]", baseName);
    }

    /**
     * Assembles the manifest map from the intent model and the EDM-derived per-entity metadata — the
     * pure, repository-free core of this generator (so it is unit-testable independently of the
     * workspace I/O).
     *
     * @param baseName the intent base name (module id + web/gen folder)
     * @param project the workspace project folder
     * @param model the parsed intent model (logical field/relation data)
     * @param edmEntities the {@code .model} entities indexed by name (resolved perspective, table,
     *        labels, layout, nav group, multilingual)
     * @return the ordered manifest map ready to serialize as {@code <name>.test}
     */
    public static Map<String, Object> buildManifest(String baseName, String project, IntentModel model,
            Map<String, Map<String, Object>> edmEntities) {
        return buildManifest(baseName, project, model, edmEntities, null);
    }

    /**
     * The full variant carrying the generation context, which cross-model relation resolution needs (a
     * {@code null} context falls back to the naming-convention target coordinates - unit tests).
     */
    public static Map<String, Object> buildManifest(String baseName, String project, IntentModel model,
            Map<String, Map<String, Object>> edmEntities, IntentGenerationContext context) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("module", baseName);
        manifest.put("standaloneShell", "/services/web/" + project + "/gen/" + baseName + "/index.html");
        manifest.put("restBase", "/services/java/" + project + "/gen/" + sanitizeJavaIdentifier(baseName) + "/api");
        manifest.put("idProperty", idProperty(edmEntities));
        manifest.put("languages", languages(model));

        List<Map<String, Object>> entities = new ArrayList<>();
        for (EntityIntent entity : model.getEntities()) {
            Map<String, Object> edm = edmEntities.get(entity.getName());
            if (edm == null) {
                continue;
            }
            // A composition detail child is exercised through its master, not as its own list; a
            // cross-model projection has no local table or UI to drive.
            if ("MANAGE_DETAILS".equals(string(edm.get("layoutType"))) || "PROJECTION".equals(string(edm.get("type")))) {
                continue;
            }
            entities.add(entityManifest(entity, edm, model, context, edmEntities));
        }
        manifest.put("entities", entities);
        return manifest;
    }

    private static Map<String, Object> entityManifest(EntityIntent entity, Map<String, Object> edm, IntentModel model,
            IntentGenerationContext context, Map<String, Map<String, Object>> edmEntities) {
        Map<String, Object> out = new LinkedHashMap<>();
        String name = entity.getName();
        out.put("name", name);
        out.put("label", stringOr(edm.get("entityLabel"), IntentNaming.humanize(name)));
        out.put("labelPlural", stringOr(edm.get("menuLabel"), IntentNaming.pluralize(IntentNaming.humanize(name))));
        out.put("layout", layout(string(edm.get("layoutType"))));
        out.put("route", "#/" + name);
        out.put("navGroup", string(edm.get("perspectiveNavId")));
        out.put("api", "/" + sanitizeJavaIdentifier(string(edm.get("perspectiveName"))) + "/" + name + "Controller");
        out.put("table", string(edm.get("dataName")));

        // A hierarchical entity renders its list as a tree (role=treeitem, no table/columnheaders),
        // so the runner must branch on it.
        if (entity.getHierarchy() != null && !entity.getHierarchy()
                                                    .isBlank()) {
            out.put("hierarchy", true);
        }
        boolean multilingual = "true".equals(string(edm.get("multilingual")));
        if (multilingual) {
            out.put("multilingual", true);
            Map<String, Object> sample = multilingualSample(entity, model);
            if (sample != null) {
                out.put("multilingualSample", sample);
            }
        }
        if (hasSeed(model, name)) {
            out.put("expectSeedData", true);
        }
        // personal (my) surface: a `personal: true` to-one relation makes the entity a personal
        // root - the generator emits an ADDITIONAL scoped <Entity>MyController whose contract the
        // runner's my flow drives: reads filtered to the identity-mapped user, the owner FK forced
        // server-side, sensitive fields stripped from the wire, foreign rows 404.
        for (RelationIntent relation : entity.getRelations()) {
            if (!relation.isPersonal()) {
                continue;
            }
            Map<String, Object> personal = new LinkedHashMap<>();
            personal.put("api", "/" + sanitizeJavaIdentifier(string(edm.get("perspectiveName"))) + "/" + name + "MyController");
            personal.put("owner", IntentNaming.pascalCase(relation.getName()));
            List<String> sensitive = new ArrayList<>();
            for (FieldIntent field : entity.getFields()) {
                if (field.isSensitive()) {
                    sensitive.add(IntentNaming.pascalCase(field.getName()));
                }
            }
            if (!sensitive.isEmpty()) {
                personal.put("sensitive", sensitive);
            }
            out.put("personal", personal);
            break;
        }
        // exactlyOne checks: exactly one of the named fields may be non-null - a sample record
        // filling all of them is rejected with 400, so the runner keeps only the first
        List<List<String>> exactlyOne = new ArrayList<>();
        for (CheckIntent check : entity.getChecks() == null ? List.<CheckIntent>of() : entity.getChecks()) {
            if ("exactlyOne".equals(check.getKind()) && check.getFields() != null && !check.getFields()
                                                                                           .isEmpty()) {
                exactlyOne.add(check.getFields()
                                    .stream()
                                    .map(IntentNaming::pascalCase)
                                    .toList());
            }
        }
        if (!exactlyOne.isEmpty()) {
            out.put("exactlyOne", exactlyOne);
        }
        out.put("fields", fields(entity));
        List<Map<String, Object>> relations = relations(entity, model, context, edmEntities);
        if (!relations.isEmpty()) {
            out.put("relations", relations);
        }
        return out;
    }

    /** Non-PK, non-generated fields, with the metadata the runner needs to fill and assert them. */
    private static List<Map<String, Object>> fields(EntityIntent entity) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() || field.isGenerated()) {
                continue;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("name", IntentNaming.pascalCase(field.getName()));
            out.put("type", logicalType(field.getType()));
            if (field.isRequired()) {
                out.put("required", true);
            }
            if (field.isUnique()) {
                out.put("unique", true);
            }
            if (field.getLength() != null) {
                out.put("length", field.getLength());
            }
            // Read-only must mirror the generated form exactly, or the runner waits forever on an
            // input that is not there: an author-marked field and a uuid render in the read-only
            // details block (no #f_<Name> input), a calculated field renders as a non-editable
            // input, an aggregate renders in the document totals footer, and a dependsOn field is
            // auto-populated by its trigger relation's watcher (the runner must not fill it).
            if (field.isReadOnly() || "uuid".equalsIgnoreCase(field.getType()) || field.isCalculated() || field.isAggregate()
                    || field.getDependsOn() != null) {
                out.put("readOnly", true);
            }
            out.put("major", field.isMajor());
            fields.add(out);
        }
        return fields;
    }

    /**
     * The user-pickable to-one relations rendered as dropdowns. A cross-model relation's target lives
     * in another module — its option rows are resolved through an {@code apiAbsolute} controller URL
     * (the same owner-project coordinates the generated dropdown uses), so the runner can fill the
     * required FK without the target being in this manifest. A {@code function: EntityStatus} relation
     * is marked {@code entityStatus} — it renders as a status pill / is excluded from the editable
     * inputs by the form templates, and its value comes from the {@code init:} DB default, so the
     * runner must neither pick nor post it.
     */
    private static List<Map<String, Object>> relations(EntityIntent entity, IntentModel model, IntentGenerationContext context,
            Map<String, Map<String, Object>> edmEntities) {
        Map<String, UsesIntent> usesByAlias = new LinkedHashMap<>();
        for (UsesIntent uses : model.getUses()) {
            if (uses.getModel() != null) {
                usesByAlias.put(uses.getModel(), uses);
            }
        }
        List<Map<String, Object>> relations = new ArrayList<>();
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (!toOne || relation.getTo() == null) {
                continue;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("name", IntentNaming.pascalCase(relation.getName()));
            out.put("kind", relation.getKind());
            out.put("to", relation.getTo());
            if (relation.isRequired() || relation.isComposition()) {
                out.put("required", true);
            }
            out.put("widget", "dropdown");
            if (relation.isEntityStatus()) {
                out.put("entityStatus", true);
            }
            // dependsOn cascade: the option list narrows to target rows whose filterBy equals the
            // trigger sibling's value - the runner must pick MATCHING samples (the dependent row
            // first, then its FK as the trigger's sample), not independent first rows.
            if (relation.getDependsOn() != null) {
                Map<String, Object> dependsOn = new LinkedHashMap<>();
                dependsOn.put("relation", IntentNaming.pascalCase(relation.getDependsOn()
                                                                          .getRelation()));
                if (relation.getDependsOn()
                            .getFilterBy() != null) {
                    dependsOn.put("filterBy", IntentNaming.pascalCase(relation.getDependsOn()
                                                                              .getFilterBy()));
                }
                out.put("dependsOn", dependsOn);
            }
            // where: static option filter - only matching target rows are offered as options
            if (relation.getWhere() != null && relation.getWhere()
                                                       .size() == 1) {
                Map.Entry<String, Object> condition = relation.getWhere()
                                                              .entrySet()
                                                              .iterator()
                                                              .next();
                Map<String, Object> where = new LinkedHashMap<>();
                where.put("by", IntentNaming.pascalCase(condition.getKey()));
                where.put("value", condition.getValue());
                out.put("where", where);
            }
            if (relation.isCrossModel()) {
                UsesIntent uses = usesByAlias.get(relation.getModel());
                if (uses == null) {
                    continue;
                }
                CrossModelSupport.TargetInfo info;
                try {
                    info = CrossModelSupport.resolve(context, uses, relation.getTo());
                } catch (RuntimeException ex) {
                    // the EDM generator (order 200) fails loudly for a truly unresolvable target;
                    // reaching here means a degraded context - omit the relation rather than emit a
                    // guessed URL
                    LOGGER.warn("Omitting cross-model relation [{}] of [{}] from the app-test manifest - target unresolved",
                            relation.getName(), entity.getName(), ex);
                    continue;
                }
                out.put("crossModel", true);
                out.put("apiAbsolute", "/services/java/" + uses.resolveProject() + "/gen/" + sanitizeJavaIdentifier(uses.getModel())
                        + "/api/" + sanitizeJavaIdentifier(info.perspectiveName()) + "/" + relation.getTo() + "Controller");
                out.put("labelFrom", info.labelField());
                // leafOnly: the generated validation rejects a non-leaf target - the runner must
                // pick a row no other row references via the target's hierarchy edge
                if (relation.isLeafOnly() && info.hierarchyProperty() != null) {
                    out.put("leafOnly", Map.of("hierarchyProperty", info.hierarchyProperty()));
                }
            } else {
                // relative controller path of the same-model target - resolvable even when the
                // target is a composition detail (excluded from this manifest's entities list)
                Map<String, Object> targetEdm = edmEntities.get(relation.getTo());
                if (targetEdm != null) {
                    out.put("api",
                            "/" + sanitizeJavaIdentifier(string(targetEdm.get("perspectiveName"))) + "/" + relation.getTo() + "Controller");
                }
                out.put("labelFrom", labelFieldOf(relation.getTo(), model));
                if (relation.isLeafOnly()) {
                    for (EntityIntent target : model.getEntities()) {
                        if (relation.getTo()
                                    .equals(target.getName())
                                && target.getHierarchy() != null) {
                            out.put("leafOnly", Map.of("hierarchyProperty", IntentNaming.pascalCase(target.getHierarchy())));
                        }
                    }
                }
            }
            relations.add(out);
        }
        return relations;
    }

    /**
     * The label property of a relation target: its {@code name} field PascalCased, else {@code Name}.
     */
    private static String labelFieldOf(String targetName, IntentModel model) {
        for (EntityIntent entity : model.getEntities()) {
            if (!targetName.equals(entity.getName())) {
                continue;
            }
            for (FieldIntent field : entity.getFields()) {
                if ("name".equalsIgnoreCase(field.getName())) {
                    return IntentNaming.pascalCase(field.getName());
                }
            }
            for (FieldIntent field : entity.getFields()) {
                if (isStringType(field.getType()) && !field.isPrimaryKey()) {
                    return IntentNaming.pascalCase(field.getName());
                }
            }
        }
        return "Name";
    }

    /**
     * A concrete {@code {language, base, translated}} sample for the multilingual read overlay, derived
     * from the entity's inline base + {@code language:} seeds — or null when it cannot be derived
     * (file-backed seeds), in which case the runner simply skips the translation assertion.
     */
    private static Map<String, Object> multilingualSample(EntityIntent entity, IntentModel model) {
        SeedIntent base = null;
        SeedIntent translated = null;
        for (SeedIntent seed : model.getSeeds()) {
            if (!entity.getName()
                       .equals(seed.getEntity())
                    || seed.getRows() == null || seed.getRows()
                                                     .isEmpty()) {
                continue;
            }
            if (seed.isLanguageSeed()) {
                if (translated == null) {
                    translated = seed;
                }
            } else if (base == null) {
                base = seed;
            }
        }
        if (base == null || translated == null) {
            return null;
        }
        String key = firstTranslatableKey(entity, base.getRows()
                                                      .get(0));
        if (key == null) {
            return null;
        }
        Object baseId = base.getRows()
                            .get(0)
                            .get("id");
        Object baseValue = base.getRows()
                               .get(0)
                               .get(key);
        Object translatedValue = null;
        for (Map<String, Object> row : translated.getRows()) {
            if (baseId != null && baseId.equals(row.get("id"))) {
                translatedValue = row.get(key);
                break;
            }
        }
        if (baseValue == null || translatedValue == null) {
            return null;
        }
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("language", translated.getLanguage());
        sample.put("base", baseValue);
        sample.put("translated", translatedValue);
        return sample;
    }

    private static String firstTranslatableKey(EntityIntent entity, Map<String, Object> row) {
        for (FieldIntent field : entity.getFields()) {
            if (!field.isPrimaryKey() && isStringType(field.getType()) && row.containsKey(field.getName())) {
                return field.getName();
            }
        }
        return null;
    }

    private static boolean hasSeed(IntentModel model, String entityName) {
        for (SeedIntent seed : model.getSeeds()) {
            if (entityName.equals(seed.getEntity()) && !seed.isLanguageSeed()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> languages(IntentModel model) {
        List<String> languages = model.getLanguages();
        return (languages == null || languages.isEmpty()) ? List.of("en") : languages;
    }

    /** The manifest's shared primary-key property name, taken from the model's PK column. */
    private static String idProperty(Map<String, Map<String, Object>> edmEntities) {
        for (Map<String, Object> entity : edmEntities.values()) {
            Object properties = entity.get("properties");
            if (!(properties instanceof List<?> list)) {
                continue;
            }
            for (Object property : list) {
                if (property instanceof Map<?, ?> map && "true".equals(String.valueOf(map.get("dataPrimaryKey")))) {
                    String name = String.valueOf(map.get("name"));
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
        }
        return "Id";
    }

    /** The runner's layout token from the EDM layout type. */
    private static String layout(String layoutType) {
        return switch (layoutType == null ? "" : layoutType) {
            case "MANAGE_DOCUMENT" -> "document";
            // the view family replaces the table page - the runner must not expect columns/rows
            case "MANAGE_CALENDAR" -> "calendar";
            case "MANAGE_SLOTS" -> "slots";
            default -> "manage-list";
        };
    }

    /** Maps an intent logical field type to the small set the runner's sample generator understands. */
    private static String logicalType(String type) {
        return switch (type == null ? "string" : type) {
            case "text", "uuid" -> "string";
            case "int", "long" -> "integer";
            case "double" -> "decimal";
            default -> type;
        };
    }

    private static boolean isStringType(String type) {
        return "string".equals(type) || "text".equals(type) || "uuid".equals(type);
    }

    /**
     * Reads back the {@code .model} written by the EDM generator and indexes its entities by name.
     * Returns an empty map when the model is absent or unreadable (nothing to describe).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> readModelEntities(IntentGenerationContext context, String baseName) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        try {
            IRepository repository = context.getRepository();
            IResource resource = repository.getResource(context.getProjectRoot() + "/" + baseName + ".model");
            if (!resource.exists()) {
                return byName;
            }
            String json = new String(resource.getContent(), StandardCharsets.UTF_8);
            Map<String, Object> document = GSON.fromJson(json, Map.class);
            Object modelNode = document.get("model");
            Map<String, Object> modelMap = (modelNode instanceof Map) ? (Map<String, Object>) modelNode : document;
            Object entities = modelMap.get("entities");
            if (entities instanceof List<?> list) {
                for (Object entity : list) {
                    if (entity instanceof Map<?, ?> map) {
                        Object name = map.get("name");
                        if (name != null) {
                            byName.put(String.valueOf(name), (Map<String, Object>) map);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to read the .model for the app-test manifest of [{}]; skipping", baseName, e);
        }
        return byName;
    }

    /** Mirrors the template engine's {@code sanitizeJavaIdentifier} so REST paths match the backend. */
    private static String sanitizeJavaIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return "_";
        }
        String sanitized = name.toLowerCase()
                               .replaceAll("[^a-z0-9_]", "_");
        return Character.isDigit(sanitized.charAt(0)) ? "_" + sanitized : sanitized;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String stringOr(Object value, String fallback) {
        String string = string(value);
        return string.isBlank() ? fallback : string;
    }
}
