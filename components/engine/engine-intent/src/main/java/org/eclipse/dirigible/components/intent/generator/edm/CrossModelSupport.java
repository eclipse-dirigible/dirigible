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
     * @param propertyWidgets the target's property name → widget type (e.g. {@code Period} →
     *        {@code MONTH}) - the only place a consumer can learn a cross-model field's LOGICAL type
     *        ({@code month}/{@code week} are VARCHAR at the JDBC level); {@code null} when the model
     *        was not resolved - callers then fall back to the untyped behavior
     * @param statusProperty the entity's {@code function: EntityStatus} relation property (PascalCase)
     *        - the one carrying the {@code DOCUMENT_STATUS} widget; {@code null} when it has none or
     *        the model was not resolved. A cross-model {@code generates} SOURCE needs it to render the
     *        {@code sourceStatus} completion hook, which reads the same fact off
     *        {@code RelationIntent.isEntityStatus()} in the local case
     * @param propertyRelations the target's to-one property name → the entity it references (the
     *        owner's {@code relationshipEntityName}); {@code null} when the model was not resolved. A
     *        consumer that binds to a property of a FOREIGN entity as a foreign key (a roll-up over a
     *        cross-model child) needs it to check that the property really points at the entity it
     *        expects - a property that resolves but references something else would silently key the
     *        aggregate on the wrong rows
     * @param translatedProperties the target's translatable property names (PascalCase) - the columns
     *        its sibling <code>&lt;TABLE&gt;_LANG</code> table carries, empty when the target is not
     *        {@code multilingual} or the model was not resolved. A consumer reading the target's
     *        columns directly (a report SELECT) needs it to overlay the caller's language the way the
     *        target's own repository does
     */
    public record TargetInfo(boolean resolved, String perspectiveName, String tableDataName, String keyField, String keyColumn,
            String labelField, String fkType, java.util.Set<String> propertyNames, String hierarchyProperty, String identityProperty,
            java.util.Map<String, String> propertyWidgets, String statusProperty, java.util.Map<String, String> propertyRelations,
            java.util.Set<String> translatedProperties) {
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
     * Everything a {@code generates} computed-line
     * ({@link org.eclipse.dirigible.components.intent.model.GeneratesIntent#getItemLines()}) needs to
     * type its cells against a cross-model target's line-items child: the child entity name and the
     * perspective it lives under (== the master's, a document renders its items there), plus each
     * property's JDBC type / decimal scale and which properties are to-one relations. Types drive the
     * per-cell rendering (numeric {@code Calc} vs {@code {}} string interpolation vs foreign-key copy)
     * exactly as the local {@code FieldIntent} does for a same-model target.
     *
     * @param resolved whether the owner model was found, parsed AND contained a composition child of
     *        the master (false → nothing resolved; the caller treats the target as having no items
     *        child)
     */
    public record ItemsChildInfo(boolean resolved, String childEntity, String perspectiveName, Map<String, String> propertyTypes,
            Map<String, Integer> propertyScales, java.util.Set<String> relationProperties, Map<String, String> propertyWidgets) {
    }

    /**
     * Resolve the composition line-items child of a cross-model {@code generates} target master (e.g.
     * {@code SalesInvoiceItem} for {@code SalesInvoice}). Order-independent and read from the same two
     * sources as {@link #resolve} (workspace {@code .model}, else the published registry copy); fails
     * loudly the same way when neither is present so a computed-line create-from never silently drops
     * its lines. A resolved model that simply has no composition child of the master returns an
     * <b>unresolved</b> {@link ItemsChildInfo} (not an error) - the caller reports "no items child".
     */
    public static ItemsChildInfo resolveItemsChild(IntentGenerationContext context, UsesIntent uses, String masterEntity) {
        if (context == null || context.getRepository() == null || context.getProjectRoot() == null) {
            return new ItemsChildInfo(false, null, masterEntity, java.util.Map.of(), java.util.Map.of(), java.util.Set.of(),
                    java.util.Map.of()); // no repository (unit test)
        }
        String alias = uses.getModel();
        String project = uses.resolveProject();
        IRepository repository = context.getRepository();
        String workspacePath = siblingModelPath(context.getProjectRoot(), project, alias);
        ItemsChildInfo fromWorkspace = readItemsChild(repository, workspacePath, masterEntity);
        if (fromWorkspace != null) {
            return fromWorkspace;
        }
        String registryPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project + "/" + alias + ".model";
        ItemsChildInfo fromRegistry = readItemsChild(repository, registryPath, masterEntity);
        if (fromRegistry != null) {
            return fromRegistry;
        }
        throw new IntentValidationException(List.of("Cross-model generates target [" + masterEntity + "] (model alias [" + alias
                + "], project [" + project + "]) cannot be resolved for computed item lines: no model found in the workspace ["
                + workspacePath + "] or the registry [" + registryPath + "]. Generate the [" + alias
                + "] model first, or install/publish its prebuilt module so its .model is in the registry."));
    }

    /**
     * Everything a read-only related-records register needs about a REFERENCING entity owned by another
     * model: where its controller lives, which of its properties is the foreign key back to the
     * referenced entity, and the property metadata its columns render from.
     *
     * @param entity the referencing entity's name
     * @param perspectiveName the perspective it lives under in its owner model (drives its REST URL)
     * @param dataName its physical table name
     * @param primaryKey its primary-key property name (PascalCase)
     * @param fkProperty the property holding the foreign key back to the referenced entity
     * @param properties its property maps, verbatim from the owner's {@code .model}, each relation
     *        property additionally carrying {@code referencedModel} when ITS target is a projection of
     *        yet another model - so a column's label lookup resolves against the right project
     */
    public record RelatedSourceInfo(String entity, String perspectiveName, String dataName, String primaryKey, String fkProperty,
            List<Map<String, Object>> properties) {
    }

    /**
     * Resolve a cross-model related-records register's SOURCE - the entity that references the one
     * declaring the register. Read from the same two order-independent sources as {@link #resolve} (the
     * owner's workspace {@code .model}, else its published copy in the registry) and failing just as
     * loudly when neither has it: a register whose columns silently came out empty would look like a
     * feature that does not work rather than a dependency that was not generated.
     *
     * @param context the generation context; a context without a repository (a unit test) yields
     *        {@code null} - there is nothing to resolve against
     * @param uses the {@code uses:} entry naming the owner model
     * @param sourceEntity the referencing entity
     * @param referencedEntity the entity declaring the register (the relation's target)
     * @param via the source's relation to filter by, or null to take its only one
     * @return the resolved source, or null when there is no repository to read from
     */
    public static RelatedSourceInfo resolveRelatedSource(IntentGenerationContext context, UsesIntent uses, String sourceEntity,
            String referencedEntity, String via) {
        if (context == null || context.getRepository() == null || context.getProjectRoot() == null) {
            return null;
        }
        String alias = uses.getModel();
        String project = uses.resolveProject();
        IRepository repository = context.getRepository();
        String workspacePath = siblingModelPath(context.getProjectRoot(), project, alias);
        RelatedSourceInfo fromWorkspace = readRelatedSource(repository, workspacePath, sourceEntity, referencedEntity, via);
        if (fromWorkspace != null) {
            return fromWorkspace;
        }
        String registryPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project + "/" + alias + ".model";
        RelatedSourceInfo fromRegistry = readRelatedSource(repository, registryPath, sourceEntity, referencedEntity, via);
        if (fromRegistry != null) {
            return fromRegistry;
        }
        throw new IntentValidationException(List.of("Related register source [" + sourceEntity + "] (model alias [" + alias + "], project ["
                + project + "]) cannot be resolved: no model found in the workspace [" + workspacePath + "] or the registry ["
                + registryPath + "]. Generate the [" + alias
                + "] model first, or install/publish its prebuilt module so its .model is in the registry."));
    }

    /**
     * Read the register's source entity from a {@code .model} resource, or {@code null} when the
     * resource is absent, unparseable or does not declare the entity (so the caller tries the next
     * source). A resolved model that declares the entity but no matching foreign key is an authoring
     * error, reported as such rather than by falling through to the next source.
     */
    @SuppressWarnings("unchecked")
    private static RelatedSourceInfo readRelatedSource(IRepository repository, String modelPath, String sourceEntity,
            String referencedEntity, String via) {
        if (modelPath == null) {
            return null;
        }
        IResource resource = repository.getResource(modelPath);
        if (!resource.exists()) {
            return null;
        }
        List<Map<String, Object>> entities;
        try {
            String content = new String(resource.getContent(), StandardCharsets.UTF_8);
            Map<String, Object> root = GSON.fromJson(content, Map.class);
            Map<String, Object> body = (Map<String, Object>) root.get("model");
            entities = body == null ? null : (List<Map<String, Object>>) body.get("entities");
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to read owner model [{}] for related register source [{}]", modelPath, sourceEntity, e);
            return null;
        }
        if (entities == null) {
            return null;
        }
        Map<String, Object> source = null;
        for (Map<String, Object> entity : entities) {
            if (sourceEntity.equals(entity.get("name"))) {
                source = entity;
                break;
            }
        }
        if (source == null) {
            return null;
        }
        List<Map<String, Object>> properties = (List<Map<String, Object>>) source.get("properties");
        if (properties == null) {
            properties = List.of();
        }
        String fkProperty = relatedForeignKey(properties, sourceEntity, referencedEntity, via);
        String primaryKey = "Id";
        for (Map<String, Object> property : properties) {
            if ("true".equals(String.valueOf(property.get("dataPrimaryKey")))) {
                primaryKey = str(property.get("name"), primaryKey);
                break;
            }
        }
        return new RelatedSourceInfo(sourceEntity, str(source.get("perspectiveName"), sourceEntity), str(source.get("dataName"), null),
                primaryKey, fkProperty, withProjectionOwners(properties, entities));
    }

    /**
     * The source property holding the foreign key back at the referenced entity: the one named by
     * {@code via:}, or - when the source references it exactly once - that single property. Anything
     * else fails loudly; the cross-model half of the check the parser runs on a same-model source.
     */
    private static String relatedForeignKey(List<Map<String, Object>> properties, String sourceEntity, String referencedEntity,
            String via) {
        List<String> candidates = new java.util.ArrayList<>();
        for (Map<String, Object> property : properties) {
            if (referencedEntity.equals(str(property.get("relationshipEntityName"), null))) {
                candidates.add(str(property.get("name"), null));
            }
        }
        if (via != null && !via.isBlank()) {
            String wanted = IntentNaming.pascalCase(via);
            if (candidates.contains(wanted)) {
                return wanted;
            }
            throw new IntentValidationException(List.of("Related register source [" + sourceEntity + "] via [" + via
                    + "] is not one of its relations targeting [" + referencedEntity + "] " + candidates));
        }
        if (candidates.isEmpty()) {
            throw new IntentValidationException(List.of("Related register source [" + sourceEntity + "] declares no relation targeting ["
                    + referencedEntity + "], so there is nothing to list"));
        }
        if (candidates.size() > 1) {
            throw new IntentValidationException(List.of("Related register source [" + sourceEntity + "] references [" + referencedEntity
                    + "] through " + candidates.size() + " relations " + candidates + " - name the one to list through with via:"));
        }
        return candidates.get(0);
    }

    /**
     * Stamps {@code referencedModel} on every relation property whose own target is a PROJECTION of the
     * owner model - i.e. an entity of a THIRD model. Without it a consumer would resolve that column's
     * label lookup against the source's project and hit a controller that does not exist there.
     */
    private static List<Map<String, Object>> withProjectionOwners(List<Map<String, Object>> properties,
            List<Map<String, Object>> entities) {
        Map<String, String> projectionOwners = new java.util.LinkedHashMap<>();
        for (Map<String, Object> entity : entities) {
            String owner = str(entity.get("projectionReferencedModel"), null);
            String name = str(entity.get("name"), null);
            if (owner != null && name != null && "PROJECTION".equals(str(entity.get("type"), null))) {
                projectionOwners.put(name, owner);
            }
        }
        List<Map<String, Object>> stamped = new java.util.ArrayList<>(properties.size());
        for (Map<String, Object> property : properties) {
            String owner = projectionOwners.get(str(property.get("relationshipEntityName"), null));
            if (owner == null) {
                stamped.add(property);
                continue;
            }
            Map<String, Object> copy = new java.util.LinkedHashMap<>(property);
            copy.put("referencedModel", owner);
            stamped.add(copy);
        }
        return stamped;
    }

    /**
     * Read the target master's composition line-items child from a {@code .model} resource, or
     * {@code null} when the resource is absent / unparseable (so the caller tries the next source). A
     * resource that parses but has no composition child of the master returns a resolved-but-empty
     * {@link ItemsChildInfo} so the caller stops looking and reports "no items child".
     */
    @SuppressWarnings("unchecked")
    private static ItemsChildInfo readItemsChild(IRepository repository, String modelPath, String masterEntity) {
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
                List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.get("properties");
                if (properties == null) {
                    continue;
                }
                boolean isChild = false;
                for (Map<String, Object> p : properties) {
                    if ("COMPOSITION".equals(String.valueOf(p.get("relationshipType")))
                            && masterEntity.equals(String.valueOf(p.get("relationshipEntityName")))) {
                        isChild = true;
                        break;
                    }
                }
                if (!isChild) {
                    continue;
                }
                String childEntity = str(entity.get("name"), null);
                String perspective = str(entity.get("perspectiveName"), masterEntity);
                Map<String, String> propertyTypes = new java.util.LinkedHashMap<>();
                Map<String, Integer> propertyScales = new java.util.LinkedHashMap<>();
                java.util.Set<String> relationProperties = new java.util.LinkedHashSet<>();
                Map<String, String> propertyWidgets = new java.util.LinkedHashMap<>();
                for (Map<String, Object> p : properties) {
                    String propertyName = str(p.get("name"), null);
                    if (propertyName == null) {
                        continue;
                    }
                    propertyTypes.put(propertyName, str(p.get("dataType"), "VARCHAR"));
                    Object scale = p.get("dataScale");
                    if (scale != null) {
                        try {
                            propertyScales.put(propertyName, (int) Double.parseDouble(String.valueOf(scale)));
                        } catch (NumberFormatException ignore) {
                            // a non-numeric scale is meaningless - leave it unscaled (default applies)
                        }
                    }
                    String relationshipType = str(p.get("relationshipType"), null);
                    if (relationshipType != null && !"null".equals(relationshipType)) {
                        relationProperties.add(propertyName);
                    }
                    String widget = str(p.get("widgetType"), null);
                    if (widget != null) {
                        propertyWidgets.put(propertyName, widget);
                    }
                }
                return new ItemsChildInfo(true, childEntity, perspective, propertyTypes, propertyScales, relationProperties,
                        propertyWidgets);
            }
            // The model resolved but declares no composition child of the master - stop looking here.
            return new ItemsChildInfo(false, null, masterEntity, java.util.Map.of(), java.util.Map.of(), java.util.Set.of(),
                    java.util.Map.of());
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to read owner model [{}] for cross-model items child of [{}]", modelPath, masterEntity, e);
        }
        return null;
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
                java.util.Map<String, String> propertyWidgets = null;
                java.util.Map<String, String> propertyRelations = null;
                String statusProperty = null;
                if (properties != null) {
                    propertyNames = new java.util.LinkedHashSet<>();
                    propertyWidgets = new java.util.LinkedHashMap<>();
                    propertyRelations = new java.util.LinkedHashMap<>();
                    for (Map<String, Object> p : properties) {
                        if ("true".equals(String.valueOf(p.get("dataPrimaryKey")))) {
                            keyField = str(p.get("name"), keyField);
                            keyColumn = str(p.get("dataName"), keyColumn);
                            fkType = str(p.get("dataType"), fkType);
                        }
                        String propertyName = str(p.get("name"), null);
                        if (propertyName != null) {
                            propertyNames.add(propertyName);
                            String widget = str(p.get("widgetType"), null);
                            if (widget != null) {
                                propertyWidgets.put(propertyName, widget);
                            }
                            String references = str(p.get("relationshipEntityName"), null);
                            if (references != null) {
                                propertyRelations.put(propertyName, references);
                            }
                            // The EntityStatus FK is the one the edm generator gave the DOCUMENT_STATUS
                            // widget (EdmIntentGenerator: isEntityStatus() ? DOCUMENT_STATUS : DROPDOWN),
                            // so the owner's .model carries the fact without a new attribute.
                            if ("DOCUMENT_STATUS".equals(widget)) {
                                statusProperty = propertyName;
                            }
                        }
                    }
                    labelField = labelField(properties, keyField);
                }
                String hierarchyProperty = str(entity.get("hierarchyProperty"), null);
                String identityProperty = str(entity.get("identityProperty"), null);
                return new TargetInfo(true, perspective, tableDataName, keyField, keyColumn, labelField, fkType, propertyNames,
                        hierarchyProperty, identityProperty, propertyWidgets, statusProperty, propertyRelations,
                        translatedProperties("true".equals(String.valueOf(entity.get("multilingual"))), properties));
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to read owner model [{}] for cross-model target [{}]", modelPath, targetEntity, e);
        }
        return null;
    }

    /**
     * The properties a multilingual entity's sibling <code>&lt;TABLE&gt;_LANG</code> table carries a
     * column for - mirroring exactly what the schema template emits there: the character-typed
     * properties that are neither the primary key, nor a foreign key, nor calculated, nor an audit
     * column. A consumer that reads the base table directly can only overlay a property that actually
     * has a language column.
     *
     * @param multilingual whether the entity keeps per-language values at all
     * @param properties the entity's model properties
     * @return the translatable property names, empty when there is no language table
     */
    private static java.util.Set<String> translatedProperties(boolean multilingual, List<Map<String, Object>> properties) {
        if (!multilingual || properties == null) {
            return java.util.Set.of();
        }
        java.util.Set<String> translated = new java.util.LinkedHashSet<>();
        for (Map<String, Object> property : properties) {
            String name = str(property.get("name"), null);
            String dataType = str(property.get("dataType"), "");
            boolean character = "VARCHAR".equals(dataType) || "CHAR".equals(dataType) || "CLOB".equals(dataType);
            boolean audit = !"NONE".equals(str(property.get("auditType"), "NONE"));
            if (name == null || !character || audit || "true".equals(String.valueOf(property.get("dataPrimaryKey")))
                    || "true".equals(String.valueOf(property.get("isCalculatedProperty")))
                    || property.get("relationshipEntityName") != null) {
                continue;
            }
            translated.add(name);
        }
        return translated;
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
        return new TargetInfo(false, targetEntity, table, "Id", keyColumn, "Name", "INTEGER", null, null, null, null, null, null,
                java.util.Set.of());
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
