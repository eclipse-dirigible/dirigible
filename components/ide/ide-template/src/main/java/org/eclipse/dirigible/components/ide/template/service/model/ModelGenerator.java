/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import org.eclipse.dirigible.commons.api.helpers.NamingHelper;
import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateMetadataSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMap;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMaps;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.strOr;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.truthy;

/**
 * Renders a template's sources against a model.
 *
 * <p>
 * There are two shapes of model. An <b>entity model</b> - an {@code .edm} projection or an intent
 * glue file - drives {@link #generateFiles}, which partitions its entities and renders one file per
 * entity of the partition a source names. Everything else - a form, a report, a mapping - drives
 * {@link #generateGeneric}, which renders each source once against the whole model.
 *
 * <p>
 * The partitions are computed once, before any source is rendered, and that ordering is
 * contractual: rendering a settings or report collection rewrites those entities' layout and
 * perspective, so a partition computed lazily would see a different model than one computed
 * eagerly.
 */
@Component
class ModelGenerator {

    /** The action that emits a template source unchanged. */
    private static final String ACTION_COPY = "copy";

    /** The action that renders a template source. */
    private static final String ACTION_GENERATE = "generate";

    /** The action that emits a translation catalog. */
    private static final String ACTION_TRANSLATE = "translate";

    /** The identifier of the perspective a settings entity is moved to. */
    private static final String SETTINGS_PERSPECTIVE = "Settings";

    /** The identifier of the perspective a report entity is moved to. */
    private static final String REPORTS_PERSPECTIVE = "Reports";

    /** The renderer. */
    private final ModelTemplateRenderer renderer;

    /** The glue generator. */
    private final GlueGenerator glueGenerator;

    /**
     * Instantiates a new model generator.
     *
     * @param renderer the renderer
     */
    ModelGenerator(ModelTemplateRenderer renderer) {
        this.renderer = renderer;
        this.glueGenerator = new GlueGenerator(renderer);
    }

    /**
     * Renders each source once against the whole model - the path for a form, a report or a mapping.
     *
     * @param model the model
     * @param parameters the generation parameters
     * @param sources the template sources
     * @return the generated files
     * @throws IOException when a template is missing or rendering fails
     */
    List<GeneratedFile> generateGeneric(Map<String, Object> model, Map<String, Object> parameters,
            List<GenerationTemplateMetadataSource> sources) throws IOException {
        String filePath = str(parameters, "filePath");
        boolean isForm = filePath != null && filePath.endsWith(".form");
        boolean isReport = filePath != null && filePath.endsWith(".report");
        if (isForm) {
            ModelTranslations.migrateForm(model, str(parameters, "fileName"));
        } else if (isReport) {
            ModelTranslations.migrateReport(model);
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.putAll(model);
        context.putAll(parameters);
        context.put("tprefix", ModelTranslations.catalogPrefix(filePath));
        ModelValues.clean(context);

        List<GeneratedFile> files = new ArrayList<>();
        for (GenerationTemplateMetadataSource source : sources) {
            String content = renderer.readTemplate(source.getLocation());
            if (ACTION_COPY.equals(source.getAction())) {
                files.add(copied(source, content, parameters));
            } else if (ACTION_TRANSLATE.equals(source.getAction())) {
                files.add(genericCatalog(content, model, parameters, str(context, "tprefix"), isForm, isReport));
            } else {
                files.add(new GeneratedFile(source.getLocation(), renderer.renderPath(source.getLocation(), source.getRename(), parameters),
                        renderer.render(source, content, context)));
            }
        }
        return files;
    }

    /**
     * Renders the sources of an entity model.
     *
     * @param model the entity model
     * @param parameters the generation parameters
     * @param sources the template sources
     * @return the generated files
     * @throws IOException when a template is missing or rendering fails
     */
    List<GeneratedFile> generateFiles(Map<String, Object> model, Map<String, Object> parameters,
            List<GenerationTemplateMetadataSource> sources) throws IOException {
        mergeExtensionEntities(model);
        List<Map<String, Object>> entities = asMaps(model.get("entities"));
        annotateDocumentModels(entities);
        annotateGuardedRollups(entities);
        applyDefaultEntityLabels(entities);
        attachReportFilters(entities);
        Map<String, List<Map<String, Object>>> collections = partition(entities);

        List<GeneratedFile> files = new ArrayList<>();
        for (GenerationTemplateMetadataSource source : sources) {
            String content = renderer.readTemplate(source.getLocation());
            parameters.put("tprefix", ModelTranslations.catalogPrefix(str(parameters, "filePath")));
            if (ACTION_COPY.equals(source.getAction())) {
                files.add(copied(source, content, parameters));
            } else if (ACTION_GENERATE.equals(source.getAction())) {
                files.addAll(generateSource(source, content, model, parameters, collections));
            } else if (ACTION_TRANSLATE.equals(source.getAction())) {
                files.add(entityCatalog(content, model, parameters));
            }
        }
        return files;
    }

    /**
     * Renders one generating source, dispatching on the collection it names.
     *
     * @param source the template source
     * @param content the template content
     * @param model the model
     * @param parameters the generation parameters
     * @param collections the entity partitions
     * @return the generated files
     * @throws IOException when rendering fails
     */
    private List<GeneratedFile> generateSource(GenerationTemplateMetadataSource source, String content, Map<String, Object> model,
            Map<String, Object> parameters, Map<String, List<Map<String, Object>>> collections) throws IOException {
        String collection = source.getCollection();
        if (collection == null) {
            return List.of(wholeModel(source, content, model, parameters));
        }
        List<Map<String, Object>> entities = collections.get(collection);
        if (entities != null) {
            return generateCollection(source, content, entities, parameters);
        }
        if ("uiNavigations".equals(collection)) {
            return navigations(source, content, model, parameters);
        }
        if ("adminModel".equals(collection)) {
            return List.of(adminModel(source, content, model, parameters));
        }
        if (GlueGenerator.handles(collection)) {
            return glueGenerator.generate(collection, source, content, model, parameters);
        }
        return List.of(wholeModel(source, content, model, parameters));
    }

    /**
     * Emits a source unchanged, with only its path rendered.
     *
     * @param source the template source
     * @param content the template content
     * @param parameters the generation parameters
     * @return the generated file
     * @throws IOException when rendering the path fails
     */
    private GeneratedFile copied(GenerationTemplateMetadataSource source, String content, Map<String, Object> parameters)
            throws IOException {
        return new GeneratedFile(source.getLocation(), renderer.renderPath(source.getLocation(), source.getRename(), parameters), content);
    }

    /**
     * Renders a source once against the whole entity list - the fallback for a source that names no
     * collection.
     *
     * @param source the template source
     * @param content the template content
     * @param model the model
     * @param parameters the generation parameters
     * @return the generated file
     * @throws IOException when rendering fails
     */
    private GeneratedFile wholeModel(GenerationTemplateMetadataSource source, String content, Map<String, Object> model,
            Map<String, Object> parameters) throws IOException {
        parameters.put("models", model.get("entities"));
        ModelValues.clean(parameters);
        return new GeneratedFile(source.getLocation(), renderer.renderPath(source.getLocation(), source.getRename(), parameters),
                renderer.render(source, content, parameters));
    }

    /**
     * Renders one file per entity of a collection.
     *
     * @param source the template source
     * @param content the template content
     * @param collection the entities
     * @param parameters the generation parameters
     * @return the generated files
     * @throws IOException when rendering fails
     */
    private List<GeneratedFile> generateCollection(GenerationTemplateMetadataSource source, String content,
            List<Map<String, Object>> collection, Map<String, Object> parameters) throws IOException {
        List<GeneratedFile> files = new ArrayList<>();
        for (Map<String, Object> entity : collection) {
            // A settings or report entity is rendered under a fixed perspective and without a layout,
            // whatever the model declared. This rewrites the entity itself, so a later source sees the
            // moved perspective too.
            String type = str(entity, "type");
            if ("SETTING".equals(type) || "REPORT".equals(type)) {
                entity.remove("layoutType");
                entity.put("perspectiveName", "SETTING".equals(type) ? SETTINGS_PERSPECTIVE : REPORTS_PERSPECTIVE);
                entity.remove("perspectiveLabel");
                entity.remove("navigationPath");
            }
            Map<String, Object> context = new LinkedHashMap<>();
            context.putAll(entity);
            context.putAll(parameters);
            if (!"SETTING".equals(type)) {
                context.put("perspectiveViews", perspectiveViews(context, entity, source, collection));
            }
            ModelValues.clean(context);
            files.add(new GeneratedFile(source.getLocation(), renderer.renderPath(source.getLocation(), source.getRename(), context),
                    renderer.render(source, content, context)));
        }
        return files;
    }

    /**
     * Resolves the views registered under an entity's perspective, which its navigation renders.
     *
     * @param context the template context
     * @param entity the entity
     * @param source the template source
     * @param collection the entities being rendered
     * @return the view names
     * @throws IOException when the perspective was never collected
     */
    private static List<Object> perspectiveViews(Map<String, Object> context, Map<String, Object> entity,
            GenerationTemplateMetadataSource source, List<Map<String, Object>> collection) throws IOException {
        String perspectiveName = str(entity, "perspectiveName");
        Map<String, Object> perspectives = asMap(context.get("perspectives"));
        Map<String, Object> perspective = perspectives == null ? null : asMap(perspectives.get(perspectiveName));
        if (perspective == null) {
            throw new IOException("Entity [" + str(entity, "name") + "] names the perspective [" + perspectiveName
                    + "], which no entity of the model contributed.");
        }
        List<Object> views = ModelValues.asList(perspective.get("views"));
        if ("uiManageMasterModels".equals(source.getCollection()) || "uiListMasterModels".equals(source.getCollection())) {
            // A master layout additionally registers a details view per master sharing its perspective.
            for (Map<String, Object> sibling : collection) {
                if (perspectiveName != null && perspectiveName.equals(str(sibling, "perspectiveName"))) {
                    views.add(str(sibling, "name") + "-details");
                }
            }
        }
        return views;
    }

    /**
     * Renders one file per navigation entry of the model.
     *
     * @param source the template source
     * @param content the template content
     * @param model the model
     * @param parameters the generation parameters
     * @return the generated files
     * @throws IOException when rendering fails
     */
    private List<GeneratedFile> navigations(GenerationTemplateMetadataSource source, String content, Map<String, Object> model,
            Map<String, Object> parameters) throws IOException {
        List<GeneratedFile> files = new ArrayList<>();
        for (Map<String, Object> navigation : asMaps(model.get("navigations"))) {
            Map<String, Object> context = ModelValues.copy(parameters);
            context.put("navId", navigation.get("id"));
            context.put("navLabel", navigation.get("label"));
            context.put("navHeader", navigation.get("header"));
            context.put("navExpanded", navigation.get("expanded"));
            context.put("navOrder", navigation.get("order"));
            context.put("navIcon", navigation.get("icon"));
            context.put("navRole", navigation.get("role"));
            ModelValues.clean(context);
            files.add(new GeneratedFile(source.getLocation(), renderer.renderPath(source.getLocation(), source.getRename(), context),
                    renderer.render(source, content, context)));
        }
        return files;
    }

    /**
     * Renders the administration surface - one file per model rather than per entity, with every
     * editable entity's shape baked in as JSON.
     *
     * <p>
     * The administration pages talk to each entity's own controller, so an administrative write fires
     * the same events, checks and audit stamping an application write does. A relation is rendered as a
     * lookup resolving the identifier to its label, because a raw identifier input is how an
     * administrator corrupts data; there is deliberately no inline creation - this surface edits what
     * exists.
     *
     * @param source the template source
     * @param content the template content
     * @param model the model
     * @param parameters the generation parameters
     * @return the generated file
     * @throws IOException when rendering fails
     */
    private GeneratedFile adminModel(GenerationTemplateMetadataSource source, String content, Map<String, Object> model,
            Map<String, Object> parameters) throws IOException {
        List<Object> adminEntities = new ArrayList<>();
        for (Map<String, Object> entity : asMaps(model.get("entities"))) {
            if ("PROJECTION".equals(str(entity, "type")) || strOr(entity, "layoutType", "").startsWith("REPORT")) {
                continue;
            }
            Map<String, Object> descriptor = new LinkedHashMap<>();
            descriptor.put("name", entity.get("name"));
            descriptor.put("label", strOr(entity, "menuLabel", str(entity, "name")));
            descriptor.put("api",
                    "/services/java/" + str(parameters, "projectName") + "/gen/" + str(parameters, "javaGenFolderName") + "/api/"
                            + NamingHelper.sanitizeJavaIdentifier(str(entity, "perspectiveName")) + "/" + str(entity, "name")
                            + "Controller");
            List<Object> primaryKeys = ModelValues.asList(entity.get("primaryKeys"));
            descriptor.put("pk", primaryKeys.isEmpty() ? "Id" : primaryKeys.get(0));
            List<Object> properties = new ArrayList<>();
            for (Map<String, Object> property : asMaps(entity.get("properties"))) {
                properties.add(adminProperty(property));
            }
            descriptor.put("properties", properties);
            adminEntities.add(descriptor);
        }
        Map<String, Object> context = ModelValues.copy(parameters);
        context.put("adminEntitiesJson", JavaScriptJson.compact(adminEntities));
        ModelValues.clean(context);
        return new GeneratedFile(source.getLocation(), renderer.renderPath(source.getLocation(), source.getRename(), context),
                renderer.render(source, content, context));
    }

    /**
     * Describes one property for the administration surface.
     *
     * @param property the property
     * @return the descriptor
     */
    private static Map<String, Object> adminProperty(Map<String, Object> property) {
        String typescriptType = str(property, "dataTypeTypescript");
        String auditType = str(property, "auditType");
        boolean audit = auditType != null && !"NONE".equals(auditType);
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("name", property.get("name"));
        descriptor.put("type", "number".equals(typescriptType) ? "number" : ("boolean".equals(typescriptType) ? "boolean" : "string"));
        descriptor.put("required", Boolean.TRUE.equals(property.get("isRequiredProperty")));
        // A system or derived column stays visible but is not editable.
        descriptor.put("readonly",
                Boolean.TRUE.equals(property.get("dataAutoIncrement")) || Boolean.TRUE.equals(property.get("isCalculatedProperty"))
                        || Boolean.TRUE.equals(property.get("isReadOnlyProperty")) || audit);
        descriptor.put("pk", Boolean.TRUE.equals(property.get("dataPrimaryKey")));
        descriptor.put("fk", property.get("relationshipEntityName"));
        // Every property carries a lookup URL key, empty unless the property is a relation, so this
        // tests the value rather than the key's presence.
        if (truthy(property, "widgetDropdownControllerUrl")) {
            Map<String, Object> lookup = new LinkedHashMap<>();
            lookup.put("url", property.get("widgetDropdownControllerUrl"));
            lookup.put("key", strOr(property, "widgetDropDownKey", "Id"));
            lookup.put("label", strOr(property, "widgetDropDownValue", "Name"));
            descriptor.put("lookup", lookup);
        } else {
            descriptor.put("lookup", null);
        }
        return descriptor;
    }

    /**
     * Builds the catalog of a form, report or mapping model.
     *
     * @param content the catalog template
     * @param model the model
     * @param parameters the generation parameters
     * @param prefix the catalog prefix
     * @param isForm whether the model is a form
     * @param isReport whether the model is a report
     * @return the generated catalog
     */
    private static GeneratedFile genericCatalog(String content, Map<String, Object> model, Map<String, Object> parameters, String prefix,
            boolean isForm, boolean isReport) {
        Map<String, Object> catalog = ModelJson.parseObject(content);
        if (isReport) {
            catalog.put("t", ModelTranslations.reportTranslations(model));
        } else {
            Map<String, Object> metadata = asMap(model.get("metadata"));
            if (isForm && metadata != null && metadata.get("successMsg") != null) {
                Map<String, Object> dialogs = asMap(catalog.get("dialogs"));
                if (dialogs != null) {
                    dialogs.put("successMsg", metadata.get("successMsg"));
                }
            }
            catalog.put("t", ModelTranslations.formTranslations(model));
        }
        return catalogFile(prefix, catalog, str(parameters, "filePath"));
    }

    /**
     * Builds the catalog of an entity model.
     *
     * @param content the catalog template
     * @param model the model
     * @param parameters the generation parameters
     * @return the generated catalog
     */
    private static GeneratedFile entityCatalog(String content, Map<String, Object> model, Map<String, Object> parameters) {
        Map<String, Object> catalog = ModelJson.parseObject(content);
        ModelTranslations.fillEntityCatalog(model, catalog);
        return catalogFile(str(parameters, "tprefix"), catalog, str(parameters, "filePath"));
    }

    /**
     * Wraps a catalog under its prefix and writes it to the catalog path.
     *
     * @param prefix the catalog prefix
     * @param catalog the catalog
     * @param filePath the model file path
     * @return the generated catalog
     */
    private static GeneratedFile catalogFile(String prefix, Map<String, Object> catalog, String filePath) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put(prefix, catalog);
        return new GeneratedFile(null, ModelTranslations.catalogPath(filePath), JavaScriptJson.pretty(root));
    }

    /**
     * Folds every extension entity's properties into the entity it extends, then drops the extensions.
     *
     * <p>
     * An extension owns no table: its fields become real columns on the base entity, so the whole
     * downstream pipeline treats them natively - no join, and they are filterable, sortable and
     * formable like any other column. A property is merged unless it is a primary key, an audit column,
     * or a name the base already defines: the base always wins, an extension can only add. An extension
     * whose base is not in this model - which is what generating the extension's own project looks like
     * - is simply dropped.
     *
     * @param model the model
     */
    private static void mergeExtensionEntities(Map<String, Object> model) {
        List<Map<String, Object>> entities = asMaps(model.get("entities"));
        List<Map<String, Object>> extensions = new ArrayList<>();
        for (Map<String, Object> entity : entities) {
            if ("EXTENSION".equals(str(entity, "type"))) {
                extensions.add(entity);
            }
        }
        if (extensions.isEmpty()) {
            return;
        }
        for (Map<String, Object> extension : extensions) {
            Map<String, Object> base = null;
            String baseName = str(extension, "extensionReferencedEntity");
            for (Map<String, Object> candidate : entities) {
                if (baseName != null && baseName.equals(str(candidate, "name")) && !"EXTENSION".equals(str(candidate, "type"))) {
                    base = candidate;
                    break;
                }
            }
            if (base == null) {
                continue;
            }
            if (!(base.get("properties") instanceof List)) {
                base.put("properties", new ArrayList<>());
            }
            List<Object> baseProperties = ModelValues.asList(base.get("properties"));
            Set<String> existing = new HashSet<>();
            for (Map<String, Object> property : asMaps(base.get("properties"))) {
                existing.add(str(property, "name"));
            }
            for (Map<String, Object> property : asMaps(extension.get("properties"))) {
                String auditType = str(property, "auditType");
                if ("true".equals(property.get("dataPrimaryKey")) || (auditType != null && !"NONE".equals(auditType))
                        || existing.contains(str(property, "name"))) {
                    continue;
                }
                baseProperties.add(ModelJson.deepCopy(property));
                existing.add(str(property, "name"));
            }
        }
        List<Object> remaining = new ArrayList<>();
        for (Map<String, Object> entity : entities) {
            if (!"EXTENSION".equals(str(entity, "type"))) {
                remaining.add(entity);
            }
        }
        model.put("entities", remaining);
    }

    /**
     * Gives every entity a human-readable singular label. An intent-generated model bakes one; a
     * hand-authored model carries none, and without this the templates' label placeholder would reach
     * the output verbatim whenever the runtime catalog lookup missed.
     *
     * @param entities the entities
     */
    private static void applyDefaultEntityLabels(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            String label = str(entity, "entityLabel");
            if (label == null || label.isBlank()) {
                entity.put("entityLabel", NamingHelper.humanizeIdentifier(str(entity, "name")));
            }
        }
    }

    /**
     * Attaches each report filter to the report it filters.
     *
     * @param entities the entities
     */
    private static void attachReportFilters(List<Map<String, Object>> entities) {
        for (Map<String, Object> filter : entities) {
            if (!"FILTER".equals(str(filter, "type"))) {
                continue;
            }
            String reportName = null;
            for (Map<String, Object> property : asMaps(filter.get("properties"))) {
                if ("ASSOCIATION".equals(str(property, "relationshipType")) && "1_1".equals(str(property, "relationshipCardinality"))) {
                    reportName = str(property, "relationshipEntityName");
                    break;
                }
            }
            if (reportName == null) {
                continue;
            }
            for (Map<String, Object> report : entities) {
                if ("REPORT".equals(str(report, "type")) && reportName.equals(str(report, "name"))) {
                    report.put("filter", filter);
                    break;
                }
            }
        }
    }

    /**
     * Annotates a document master and its line-items child with what their repositories need to keep
     * the document totals consistent within the same request, rather than through asynchronous roll-up
     * listeners.
     *
     * @param entities the entities
     */
    private static void annotateDocumentModels(List<Map<String, Object>> entities) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        for (Map<String, Object> entity : entities) {
            byName.put(str(entity, "name"), entity);
        }
        for (Map<String, Object> master : entities) {
            if (!"MANAGE_DOCUMENT".equals(str(master, "layoutType")) || !truthy(master, "documentItemsEntity")) {
                continue;
            }
            Map<String, Object> child = byName.get(str(master, "documentItemsEntity"));
            if (child == null) {
                continue;
            }
            String fkProperty = str(child, "masterEntityId");
            List<Object> masterKeys = ModelValues.asList(master.get("primaryKeys"));
            String masterPk = masterKeys.isEmpty() ? "Id" : String.valueOf(masterKeys.get(0));
            Set<String> childFieldNames = new HashSet<>();
            for (Map<String, Object> property : asMaps(child.get("properties"))) {
                childFieldNames.add(str(property, "name"));
            }
            List<Object> fields = new ArrayList<>();
            for (Map<String, Object> property : asMaps(master.get("properties"))) {
                if ("true".equals(property.get("aggregate")) && childFieldNames.contains(str(property, "name"))) {
                    Map<String, Object> field = new LinkedHashMap<>();
                    field.put("field", property.get("name"));
                    fields.add(field);
                }
            }
            if (fields.isEmpty() || fkProperty == null) {
                continue;
            }
            Map<String, Object> documentMaster = new LinkedHashMap<>();
            documentMaster.put("childEntity", child.get("name"));
            documentMaster.put("javaChildPerspective", NamingHelper.sanitizeJavaIdentifier(str(child, "perspectiveName")));
            documentMaster.put("fkProperty", fkProperty);
            documentMaster.put("masterPk", masterPk);
            documentMaster.put("fields", fields);
            master.put("documentMaster", documentMaster);
            Map<String, Object> documentItem = new LinkedHashMap<>();
            documentItem.put("parentEntity", master.get("name"));
            documentItem.put("javaParentPerspective", NamingHelper.sanitizeJavaIdentifier(str(master, "perspectiveName")));
            documentItem.put("fkProperty", fkProperty);
            child.put("documentItem", documentItem);
        }
    }

    /**
     * Sanitizes the parent perspective of a capacity guard into the package segment the repository
     * imports the parent from.
     *
     * @param entities the entities
     */
    private static void annotateGuardedRollups(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            Map<String, Object> rollupGuard = asMap(entity.get("rollupGuard"));
            if (rollupGuard != null && truthy(rollupGuard, "parentPerspective")) {
                rollupGuard.put("parentPerspective", NamingHelper.sanitizeJavaIdentifier(str(rollupGuard, "parentPerspective")));
            }
        }
    }

    /**
     * Partitions the entities into the collections the template sources name.
     *
     * @param entities the entities
     * @return the partitions, keyed by collection name
     */
    private static Map<String, List<Map<String, Object>>> partition(List<Map<String, Object>> entities) {
        Map<String, List<Map<String, Object>>> collections = new LinkedHashMap<>();
        collections.put("models", select(entities, e -> !"REPORT".equals(str(e, "type")) && !"FILTER".equals(str(e, "type"))));
        collections.put("apiModels", select(entities, e -> !"PROJECTION".equals(str(e, "type"))));
        collections.put("daoModels", select(entities, e -> !"PROJECTION".equals(str(e, "type"))));
        collections.put("generateReportModels", select(entities, e -> "true".equals(e.get("generateReport"))));
        List<Map<String, Object>> reportModels = select(entities, e -> "REPORT".equals(str(e, "type")));
        collections.put("reportModels", reportModels);
        collections.put("feedModels", select(entities, e -> truthy(e, "feedUrl")));

        collections.put("uiManageModels", select(entities, layout("MANAGE", "PRIMARY")));
        collections.put("uiListModels", select(entities, layout("LIST", "PRIMARY")));
        collections.put("uiSettingModels", select(entities, e -> "SETTING".equals(str(e, "type"))));
        collections.put("uiManageMasterModels", select(entities, layout("MANAGE_MASTER", "PRIMARY")));
        collections.put("uiListMasterModels", select(entities, layout("LIST_MASTER", "PRIMARY")));
        collections.put("uiManageDetailsModels", select(entities, layout("MANAGE_DETAILS", "DEPENDENT")));
        collections.put("uiListDetailsModels", select(entities, layout("LIST_DETAILS", "DEPENDENT")));
        // A document master owns a line-items child: a header form, an inline items table and a totals
        // footer. The items child stays an ordinary dependent detail.
        collections.put("uiDocumentModels", select(entities, layout("MANAGE_DOCUMENT", "PRIMARY")));
        collections.put("uiCalendarModels", select(entities, layout("MANAGE_CALENDAR", "PRIMARY")));
        collections.put("uiSlotsModels", select(entities, layout("MANAGE_SLOTS", "PRIMARY")));

        // The personal surface: entities the logged-in user owns, either through a direct personal
        // relation or through the scope inherited from their composition parent.
        collections.put("personalModels", select(entities, e -> e.get("personalProperty") != null || e.get("personalParent") != null));
        // Roots only: these get list pages and shell perspectives, while a child is reached through
        // its parent's panels rather than through navigation.
        collections.put("personalRootModels", select(entities, e -> e.get("personalProperty") != null));
        collections.put("personalCalendarModels",
                select(entities, e -> "MANAGE_CALENDAR".equals(str(e, "layoutType")) && e.get("personalProperty") != null));
        collections.put("personalListModels",
                select(entities, e -> e.get("personalProperty") != null && !"MANAGE_CALENDAR".equals(str(e, "layoutType"))));
        collections.put("personalDocumentModels",
                select(entities, e -> "MANAGE_DOCUMENT".equals(str(e, "layoutType")) && e.get("personalProperty") != null));
        // A document root uses the document layout instead of the plain form, so it is excluded here
        // while still getting its controller, list and perspective from the collections above.
        collections.put("personalFormModels", select(entities, e -> (e.get("personalProperty") != null || e.get("personalParent") != null)
                && !("MANAGE_DOCUMENT".equals(str(e, "layoutType")) && e.get("personalProperty") != null)));

        // The external-partner mirror of the personal surface, registered on its own extension point.
        collections.put("partnerModels", select(entities, e -> e.get("partnerProperty") != null || e.get("partnerParent") != null));
        collections.put("partnerRootModels", select(entities, e -> e.get("partnerProperty") != null));
        collections.put("partnerDocumentModels",
                select(entities, e -> "MANAGE_DOCUMENT".equals(str(e, "layoutType")) && e.get("partnerProperty") != null));
        collections.put("partnerFormModels", select(entities, e -> (e.get("partnerProperty") != null || e.get("partnerParent") != null)
                && !("MANAGE_DOCUMENT".equals(str(e, "layoutType")) && e.get("partnerProperty") != null)));

        collections.put("uiReportChartModels", select(reportModels, e -> !"REPORT_TABLE".equals(str(e, "layoutType"))));
        collections.put("uiReportTableModels", select(reportModels, e -> "REPORT_TABLE".equals(str(e, "layoutType"))));
        return collections;
    }

    /**
     * A predicate matching an entity's layout and type.
     *
     * @param layoutType the layout
     * @param type the type
     * @return the predicate
     */
    private static Predicate<Map<String, Object>> layout(String layoutType, String type) {
        return entity -> layoutType.equals(str(entity, "layoutType")) && type.equals(str(entity, "type"));
    }

    /**
     * Selects the entities matching a predicate, preserving model order.
     *
     * @param entities the entities
     * @param predicate the predicate
     * @return the matching entities
     */
    private static List<Map<String, Object>> select(List<Map<String, Object>> entities, Predicate<Map<String, Object>> predicate) {
        List<Map<String, Object>> selected = new ArrayList<>();
        for (Map<String, Object> entity : entities) {
            if (predicate.test(entity)) {
                selected.add(entity);
            }
        }
        return selected;
    }

}
