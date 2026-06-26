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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.generator.TriggerSupport;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits {@code <intent>.edm} (XML) and its JSON twin {@code <intent>.model} for every intent that
 * declares one or more entities. The pair is the canonical entity-data-model file consumed by the
 * EDM editor in the IDE and by the downstream "Generate from EDM" template engine, which turns the
 * model into UI / Java / SQL artefacts in a second step. The intent layer never emits those
 * second-stage artefacts itself - that contract belongs to the existing template generators.
 *
 * <p>
 * Conventions mirrored from real EDM documents (see
 * {@code tests-integrations/.../DependsOnScenariosTestProject/sales-order.edm}):
 * <ul>
 * <li>{@code dataName} is prefixed with the intent name: {@code <INTENT>_<ENTITY>} (e.g.
 * {@code ORDERS_COUNTRY}). This keeps physical table names unique across projects and away from SQL
 * reserved words like {@code ORDER}; the {@code .report} and {@code .csvim} generators use the same
 * convention so all three artefacts agree on the table name.</li>
 * <li>A {@code manyToOne}/{@code oneToOne} relation marked {@code composition: true} is a
 * <b>composition</b>: the FK property carries the {@code relationship*} attributes, the owning
 * entity becomes {@code DEPENDENT} with {@code MANAGE_DETAILS} layout and inherits the perspective
 * of its (transitively resolved) composition parent (and the FK is NOT NULL). Every other relation
 * is an <b>association</b>: a plain DROPDOWN property whose FK is NOT NULL when {@code required},
 * and the entity stays {@code PRIMARY} with its own perspective. Composition is opt-in (matching
 * the Dirigible convention, where it is an explicit {@code relationshipType="COMPOSITION"});
 * {@code required} alone only means the FK column is NOT NULL.</li>
 * <li>Dropdown key / value and the relation's {@code referencedProperty} are derived from the
 * target entity's actual fields (its primary key and its {@code name}-like field), never
 * hardcoded.</li>
 * <li>The {@code .model} JSON carries {@code entities} / {@code perspectives} /
 * {@code navigations}; relations appear only in the XML as {@code <relation>} elements interleaved
 * with their owning {@code <entity>}.</li>
 * </ul>
 * An {@code mxGraphModel} diagram block IS emitted with a deterministic grid layout: the EDM editor
 * renders the canvas exclusively by decoding {@code mxGraphModel}, so without it the editor opens
 * empty. See {@link #appendMxGraphModel}.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(200)
public class EdmIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdmIntentGenerator.class);

    private static final String DEFAULT_ICON = "/services/web/resources/unicons/file.svg";

    /** Id of the shell-provided perspective every {@code SETTING} entity is grouped under. */
    private static final String SETTINGS_PERSPECTIVE = "Settings";

    @Override
    public String name() {
        return "edm";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getEntities()
                 .isEmpty()) {
            return;
        }
        String baseName = IntentNaming.baseName(context);
        EdmDocument document = buildDocument(model, baseName);
        context.writeModelFile(baseName + ".model", JsonHelper.toJson(document.modelJson));
        context.writeModelFile(baseName + ".edm", renderEdmXml(document));
    }

    /** The two views over one model tree: the {@code .model} JSON root and the XML extras. */
    private static final class EdmDocument {
        /** Root of the {@code .model} JSON: {@code {model: {entities, perspectives, navigations}}}. */
        final Map<String, Object> modelJson = new LinkedHashMap<>();
        /** Top-level {@code <relation>} elements per owning entity name, XML-only. */
        final Map<String, List<Map<String, Object>>> relationsByEntity = new LinkedHashMap<>();
    }

    private static EdmDocument buildDocument(IntentModel model, String intentName) {
        List<EntityIntent> entities = model.getEntities();
        Map<String, EntityIntent> byName = indexEntities(entities);
        Map<String, String> compositionParents = computeCompositionParents(entities);
        Set<String> settingEntities = settingEntities(entities);
        Set<String> triggerTargets = TriggerSupport.triggerTargetEntities(model);

        EdmDocument document = new EdmDocument();
        List<Map<String, Object>> entityList = new ArrayList<>();
        List<Map<String, Object>> perspectiveList = new ArrayList<>();
        String tablePrefix = IntentNaming.upperSnake(intentName);
        int perspectiveOrder = 1;

        for (EntityIntent entity : entities) {
            String name = entity.getName();
            if (name == null || name.isBlank()) {
                LOGGER.warn("Skipping unnamed entity in intent");
                continue;
            }
            boolean setting = settingEntities.contains(name);
            boolean dependent = !setting && compositionParents.containsKey(name);
            // A setting lives under the global Settings perspective (provided by the shell); it does not
            // own a generated perspective.
            String perspective = perspectiveFor(name, compositionParents, settingEntities);
            Map<String, Object> entityMap = entityDefaults(name, entity.getDescription(), entity.getIcon(), dependent, setting, perspective,
                    tablePrefix, perspectiveOrder);
            // dashboard: false excludes the entity from the home dashboard tiles (settings are excluded
            // anyway by their type); carried on the .model entity, read by the Harmonia dashboard.
            if (entity.isDashboardExcluded()) {
                entityMap.put("dashboardWidget", "false");
            }
            if (!dependent && !setting) {
                perspectiveList.add(perspectiveEntry(name, perspectiveOrder, iconUrl(entity.getIcon())));
                perspectiveOrder++;
            }

            List<Map<String, Object>> properties = new ArrayList<>();
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() == null || field.getName()
                                                    .isBlank()) {
                    continue;
                }
                properties.add(propertyMap(name, field));
            }
            // An entity a process starts on create carries a ProcessId back-reference (the runtime
            // listener/handler writes the started process-instance id here). See TriggerSupport.
            if (triggerTargets.contains(name)) {
                properties.add(processIdProperty(name));
            }
            List<Map<String, Object>> relations = new ArrayList<>();
            boolean compositionAssigned = false;
            for (RelationIntent relation : entity.getRelations()) {
                if (!"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                    continue;
                }
                if (relation.getName() == null || relation.getTo() == null) {
                    continue;
                }
                boolean composition = !compositionAssigned && relation.isComposition();
                compositionAssigned |= composition;
                EntityIntent target = byName.get(relation.getTo());
                String targetPerspective = perspectiveFor(relation.getTo(), compositionParents, settingEntities);
                properties.add(relationProperty(name, relation, target, composition, targetPerspective));
                relations.add(relationLink(name, relation, target, compositionParents, settingEntities));
            }
            entityMap.put("properties", properties);
            entityList.add(entityMap);
            if (!relations.isEmpty()) {
                document.relationsByEntity.put(name, relations);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        // Model-level caption for the generated app (the Harmonia shell title / sidebar header). The
        // intent's `name` (humanised) is more meaningful than the raw project name; the `description`
        // rides along as a subtitle/tooltip. Both are ignored by tooling that only reads entities.
        body.put("title", IntentNaming.humanize(model.getName()));
        if (model.getDescription() != null && !model.getDescription()
                                                    .isBlank()) {
            body.put("description", model.getDescription());
        }
        body.put("entities", entityList);
        body.put("perspectives", perspectiveList);
        body.put("navigations", new ArrayList<>());
        // Process glue (triggers, decision resolvers) is NOT part of the EDM - it lives in the
        // sibling <intent>.glue file (GlueIntentGenerator). The EDM keeps only the persisted
        // ProcessId column it adds to a trigger-target entity.
        document.modelJson.put("model", body);
        return document;
    }

    /** Names of entities declared as settings / nomenclature ({@code kind: setting}). */
    private static Set<String> settingEntities(List<EntityIntent> entities) {
        Set<String> settings = new LinkedHashSet<>();
        for (EntityIntent entity : entities) {
            if (entity.getName() != null && entity.isSetting()) {
                settings.add(entity.getName());
            }
        }
        return settings;
    }

    /**
     * The perspective an entity (or relation target) lives under: the global {@code Settings}
     * perspective for a setting entity, otherwise its composition-resolved perspective.
     */
    private static String perspectiveFor(String entityName, Map<String, String> compositionParents, Set<String> settingEntities) {
        if (settingEntities.contains(entityName)) {
            return SETTINGS_PERSPECTIVE;
        }
        return resolvePerspective(entityName, compositionParents);
    }

    private static Map<String, EntityIntent> indexEntities(List<EntityIntent> entities) {
        Map<String, EntityIntent> index = new HashMap<>();
        for (EntityIntent entity : entities) {
            if (entity.getName() != null) {
                index.put(entity.getName(), entity);
            }
        }
        return index;
    }

    /**
     * Map each entity to its composition parent: the target of its first {@code composition: true}
     * {@code manyToOne} / {@code oneToOne} relation. Entities present as keys are DEPENDENT; their
     * perspective is the parent's, resolved transitively by {@link #resolvePerspective}.
     */
    private static Map<String, String> computeCompositionParents(List<EntityIntent> entities) {
        Map<String, String> parents = new LinkedHashMap<>();
        for (EntityIntent entity : entities) {
            if (entity.getName() == null) {
                continue;
            }
            for (RelationIntent relation : entity.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.isComposition() && relation.getTo() != null) {
                    parents.put(entity.getName(), relation.getTo());
                    break;
                }
            }
        }
        return parents;
    }

    /**
     * Follow the composition-parent chain to the first PRIMARY entity; that entity's name is the
     * perspective every entity in the chain lives under. A cycle (mutually required relations) falls
     * back to the starting entity itself.
     */
    private static String resolvePerspective(String entityName, Map<String, String> compositionParents) {
        String current = entityName;
        Set<String> visited = new LinkedHashSet<>();
        while (compositionParents.containsKey(current)) {
            if (!visited.add(current)) {
                LOGGER.warn("Composition cycle detected at entity [{}] - keeping its own perspective", entityName);
                return entityName;
            }
            current = compositionParents.get(current);
        }
        return current;
    }

    private static final String UNICONS_BASE = "/services/web/resources/unicons/";

    /**
     * Resolve an intent icon name to a unicons SVG URL (for the AngularJS perspective); blank →
     * default.
     */
    private static String iconUrl(String icon) {
        if (icon == null || icon.isBlank()) {
            return DEFAULT_ICON;
        }
        String n = icon.trim();
        return (n.startsWith("/") || n.startsWith("http")) ? n : UNICONS_BASE + n + ".svg";
    }

    /** The raw icon name (a Lucide icon name for the Harmonia sidebar); blank → a neutral default. */
    private static String iconName(String icon) {
        return (icon == null || icon.isBlank()) ? "list" : icon.trim();
    }

    private static Map<String, Object> perspectiveEntry(String name, int order, String icon) {
        Map<String, Object> perspective = new LinkedHashMap<>();
        perspective.put("name", name);
        perspective.put("label", name);
        perspective.put("icon", icon);
        perspective.put("order", Integer.toString(order));
        return perspective;
    }

    private static Map<String, Object> entityDefaults(String name, String description, String icon, boolean dependent, boolean setting,
            String perspective, String tablePrefix, int order) {
        String dataName = tablePrefix + "_" + IntentNaming.upperSnake(name);
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", name);
        entity.put("dataName", dataName);
        entity.put("dataCount", "SELECT COUNT(*) AS COUNT FROM \"${tablePrefix}" + dataName + "\"");
        entity.put("dataQuery", "");
        entity.put("type", setting ? "SETTING" : (dependent ? "DEPENDENT" : "PRIMARY"));
        entity.put("title", name);
        entity.put("caption", "Manage entity " + name);
        entity.put("description", description != null && !description.isBlank() ? description : "Manage entity " + name);
        entity.put("tooltip", name);
        entity.put("icon", iconUrl(icon));
        // Raw icon name for the Harmonia sidebar (Lucide). Defaults to "list" when unset.
        entity.put("iconName", iconName(icon));
        entity.put("menuKey", name.toLowerCase(Locale.ROOT));
        // Navigation label: humanized + pluralized so the menu reads naturally
        // (SalesInvoice -> "Sales Invoices", Book -> "Books").
        entity.put("menuLabel", IntentNaming.pluralize(IntentNaming.humanize(name)));
        entity.put("menuIndex", "100");
        entity.put("layoutType", dependent ? "MANAGE_DETAILS" : "MANAGE_MASTER");
        entity.put("perspectiveName", perspective);
        entity.put("perspectiveLabel", perspective);
        entity.put("perspectiveHeader", "");
        entity.put("perspectiveIcon", iconUrl(icon));
        entity.put("perspectiveOrder", Integer.toString(order));
        entity.put("perspectiveNavId", "");
        entity.put("perspectiveRole", "");
        entity.put("generateReport", "false");
        entity.put("generateDefaultRoles", "false");
        return entity;
    }

    private static Map<String, Object> propertyMap(String entityName, FieldIntent field) {
        String column = IntentNaming.upperSnake(entityName) + "_" + IntentNaming.upperSnake(field.getName());
        String dataType = mapDataType(field.getType());
        Map<String, Object> p = new LinkedHashMap<>();
        // Property names are PascalCase (Dirigible convention); the physical column dataName
        // stays UPPER_SNAKE, derived from the authored field name above.
        p.put("name", IntentNaming.pascalCase(field.getName()));
        p.put("description", field.getDescription() == null ? "" : field.getDescription());
        p.put("tooltip", "");
        p.put("dataName", column);
        p.put("dataType", dataType);
        p.put("dataNullable", field.isRequired() || field.isPrimaryKey() ? "false" : "true");
        if (field.isPrimaryKey()) {
            p.put("dataPrimaryKey", "true");
        } else if (field.isRequired()) {
            // The generated REST controller's required-value validation keys on isRequiredProperty
            // (not dataNullable); set it so a required field is actually validated. PKs are excluded
            // (auto-generated).
            p.put("isRequiredProperty", "true");
        }
        // Auto-increment is a DB identity/sequence - valid only on integer columns (a VARCHAR/uuid
        // AUTO_INCREMENT is rejected by the database). The parser already enforces integer primary keys;
        // this keeps the generator correct on its own.
        if (field.isPrimaryKey() && field.isGenerated() && ("INTEGER".equals(dataType) || "BIGINT".equals(dataType))) {
            p.put("dataAutoIncrement", "true");
        }
        Integer length = fieldLength(field);
        if (length != null && length > 0) {
            p.put("dataLength", length.toString());
        }
        if ("DECIMAL".equals(dataType)) {
            p.put("dataPrecision", "16");
            p.put("dataScale", "2");
        }
        if (field.getDefaultValue() != null && !field.getDefaultValue()
                                                     .isBlank()) {
            p.put("dataDefaultValue", field.getDefaultValue());
        }
        p.put("auditType", "NONE");
        p.put("widgetType", widgetForType(dataType));
        p.put("widgetSize", "");
        p.put("widgetLength", length == null ? "20" : length.toString());
        p.put("widgetIsMajor", "true");
        return p;
    }

    /**
     * The {@code ProcessId} back-reference property added to an entity that a process starts on create.
     * A plain VARCHAR holding the started process-instance id; the runtime trigger handler writes it.
     * Not a major widget - it is system-managed, not user input.
     */
    private static Map<String, Object> processIdProperty(String entityName) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", "ProcessId");
        p.put("description", "Process instance started for this record");
        p.put("tooltip", "");
        p.put("dataName", IntentNaming.upperSnake(entityName) + "_" + IntentNaming.upperSnake("ProcessId"));
        p.put("dataType", "VARCHAR");
        p.put("dataNullable", "true");
        p.put("dataLength", "100");
        p.put("auditType", "NONE");
        p.put("widgetType", "TEXTBOX");
        p.put("widgetSize", "");
        p.put("widgetLength", "100");
        p.put("widgetIsMajor", "false");
        return p;
    }

    /**
     * FK property added to the owning entity for a {@code manyToOne}/{@code oneToOne} relation. Renders
     * as a DROPDOWN keyed by the target entity's actual primary-key field and labelled by its
     * {@code name}-like field. Only the entity's composition relation (its first
     * {@code composition: true} to-one) carries the {@code relationship*} attributes that make the EDM
     * editor treat the owner as a detail of the target - every other relation stays a plain association
     * (NOT NULL when {@code required}), mirroring how the EDM editor writes multi-FK entities.
     */
    private static Map<String, Object> relationProperty(String ownerEntity, RelationIntent relation, EntityIntent target,
            boolean composition, String targetPerspective) {
        String column = IntentNaming.upperSnake(ownerEntity) + "_" + IntentNaming.upperSnake(relation.getName());
        FieldIntent targetPk = primaryKeyOf(target);
        String fkType = targetPk == null ? "INTEGER" : mapDataType(targetPk.getType());
        boolean oneToOne = "oneToOne".equals(relation.getKind());
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", IntentNaming.pascalCase(relation.getName()));
        p.put("description", relation.getDescription() == null ? "" : relation.getDescription());
        p.put("tooltip", "");
        p.put("dataName", column);
        p.put("dataType", fkType);
        // A composition FK is always NOT NULL (the detail cannot exist without its parent), even if
        // `required` was not also set; otherwise nullability follows `required`.
        boolean notNull = relation.isRequired() || composition;
        p.put("dataNullable", notNull ? "false" : "true");
        if (notNull) {
            p.put("isRequiredProperty", "true");
        }
        if ("VARCHAR".equals(fkType) && targetPk != null) {
            Integer length = fieldLength(targetPk);
            if (length != null && length > 0) {
                p.put("dataLength", length.toString());
            }
        }
        p.put("auditType", "NONE");
        // Relationship metadata the generation reads (Dirigible .model convention): composition vs
        // association + cardinality (composition 1_n; association n_1 for manyToOne, 1_1 for oneToOne);
        // relationshipName is the FK constraint name <owner>_<target>; relationshipEntityName and
        // relationshipEntityPerspectiveName drive the dropdown's data-service URL
        // (api/<perspective>/<entity>Service.ts) and the create-detail dialog.
        p.put("relationshipType", composition ? "COMPOSITION" : "ASSOCIATION");
        p.put("relationshipCardinality", composition ? "1_n" : (oneToOne ? "1_1" : "n_1"));
        p.put("relationshipName", ownerEntity + "_" + relation.getTo());
        p.put("relationshipEntityName", relation.getTo());
        p.put("relationshipEntityPerspectiveName", targetPerspective);
        p.put("relationshipEntityPerspectiveLabel", "Entities");
        p.put("widgetType", "DROPDOWN");
        p.put("widgetSize", "");
        p.put("widgetLength", "20");
        p.put("widgetIsMajor", "true");
        p.put("widgetDropDownKey", keyFieldName(target));
        p.put("widgetDropDownValue", labelFieldName(target));
        return p;
    }

    /**
     * Top-level {@code <relation>} element interleaved with its owning {@code <entity>} in the XML.
     * {@code relationshipEntityPerspectiveName} is the target's <i>resolved</i> perspective - for a
     * dependent target that is its composition parent's perspective, mirroring how the EDM editor
     * writes these links.
     */
    private static Map<String, Object> relationLink(String ownerEntity, RelationIntent relation, EntityIntent target,
            Map<String, String> compositionParents, Set<String> settingEntities) {
        Map<String, Object> link = new LinkedHashMap<>();
        String linkName = ownerEntity + "_" + IntentNaming.pascalCase(relation.getName());
        link.put("name", linkName);
        link.put("type", "relation");
        link.put("entity", ownerEntity);
        link.put("relationName", linkName);
        link.put("relationshipEntityPerspectiveName", perspectiveFor(relation.getTo(), compositionParents, settingEntities));
        link.put("relationshipEntityPerspectiveLabel", "Entities");
        link.put("property", IntentNaming.pascalCase(relation.getName()));
        link.put("referenced", relation.getTo());
        link.put("referencedProperty", keyFieldName(target));
        return link;
    }

    /** The target entity's primary-key field, or null when the target is unknown or has no PK. */
    private static FieldIntent primaryKeyOf(EntityIntent entity) {
        if (entity == null) {
            return null;
        }
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() && field.getName() != null) {
                return field;
            }
        }
        return null;
    }

    /**
     * The dropdown key: the target's actual PK field name (PascalCase); {@code Id} as a last resort.
     */
    private static String keyFieldName(EntityIntent target) {
        FieldIntent pk = primaryKeyOf(target);
        return pk == null ? "Id" : IntentNaming.pascalCase(pk.getName());
    }

    /**
     * The dropdown label: the target's {@code name} field (case-insensitive), else its first
     * string-typed field, else its PK.
     */
    private static String labelFieldName(EntityIntent target) {
        if (target == null) {
            return "Name";
        }
        for (FieldIntent field : target.getFields()) {
            if (field.getName() != null && "name".equalsIgnoreCase(field.getName())) {
                return IntentNaming.pascalCase(field.getName());
            }
        }
        for (FieldIntent field : target.getFields()) {
            if (field.getName() != null && "VARCHAR".equals(mapDataType(field.getType())) && !field.isPrimaryKey()) {
                return IntentNaming.pascalCase(field.getName());
            }
        }
        return keyFieldName(target);
    }

    /** Declared length, with type-derived defaults ({@code uuid} -> 36). */
    private static Integer fieldLength(FieldIntent field) {
        if (field.getLength() != null) {
            return field.getLength();
        }
        if (field.getType() != null && "uuid".equalsIgnoreCase(field.getType())) {
            return 36;
        }
        return defaultLength(mapDataType(field.getType()));
    }

    private static String mapDataType(String type) {
        if (type == null) {
            return "VARCHAR";
        }
        switch (type.toLowerCase(Locale.ROOT)) {
            case "integer":
            case "int":
                return "INTEGER";
            case "long":
                return "BIGINT";
            case "decimal":
            case "double":
                return "DECIMAL";
            case "boolean":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "timestamp":
                return "TIMESTAMP";
            case "text":
                return "CLOB";
            case "uuid":
            case "string":
            default:
                return "VARCHAR";
        }
    }

    private static String widgetForType(String dataType) {
        switch (dataType) {
            case "INTEGER":
            case "BIGINT":
            case "DECIMAL":
                return "NUMBER";
            case "DATE":
                return "DATE";
            case "TIMESTAMP":
                return "DATETIME-LOCAL";
            case "BOOLEAN":
                return "CHECKBOX";
            case "CLOB":
                return "TEXTAREA";
            case "VARCHAR":
            default:
                return "TEXTBOX";
        }
    }

    private static Integer defaultLength(String dataType) {
        switch (dataType) {
            case "VARCHAR":
                return 100;
            case "DECIMAL":
                return 16;
            case "INTEGER":
            case "BIGINT":
                return 20;
            default:
                return null;
        }
    }

    /**
     * Render the EDM XML shape: entities with their relations interleaved, the perspectives and
     * navigations blocks, then the {@code mxGraphModel} diagram. The mxGraphModel is mandatory, not
     * optional: the EDM editor renders the canvas <b>exclusively</b> by decoding {@code mxGraphModel}
     * (see {@code editor-entity/js/editor.js} - {@code codec.decode(... getElementsByTagName(
     * 'mxGraphModel')[0] ...)}); without it the editor opens to an empty canvas. The diagram is laid
     * out deterministically in a grid so re-generation is byte-stable.
     */
    @SuppressWarnings("unchecked")
    private static String renderEdmXml(EdmDocument document) {
        Map<String, Object> body = (Map<String, Object>) document.modelJson.get("model");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) body.get("entities");
        List<Map<String, Object>> perspectives = (List<Map<String, Object>>) body.get("perspectives");

        StringBuilder sb = new StringBuilder(8192);
        sb.append("<model>\n");
        sb.append(" <entities>\n");
        for (Map<String, Object> entity : entities) {
            sb.append("  <entity");
            List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.getOrDefault("properties", List.of());
            for (Map.Entry<String, Object> attr : entity.entrySet()) {
                if ("properties".equals(attr.getKey())) {
                    continue;
                }
                appendAttribute(sb, attr.getKey(), attr.getValue());
            }
            sb.append(">\n");
            for (Map<String, Object> property : properties) {
                sb.append("   <property");
                for (Map.Entry<String, Object> attr : property.entrySet()) {
                    appendAttribute(sb, attr.getKey(), attr.getValue());
                }
                sb.append("></property>\n");
            }
            sb.append("  </entity>\n");
            List<Map<String, Object>> relations = document.relationsByEntity.get(entity.get("name"));
            if (relations != null) {
                for (Map<String, Object> relation : relations) {
                    sb.append("  <relation");
                    for (Map.Entry<String, Object> attr : relation.entrySet()) {
                        appendAttribute(sb, attr.getKey(), attr.getValue());
                    }
                    sb.append(">\n  </relation>\n");
                }
            }
        }
        sb.append(" </entities>\n");
        sb.append(" <perspectives>\n");
        for (Map<String, Object> perspective : perspectives) {
            sb.append("  <perspective><name>")
              .append(escapeXmlText(perspective.get("name")))
              .append("</name><label>")
              .append(escapeXmlText(perspective.get("label")))
              .append("</label><icon>")
              .append(escapeXmlText(perspective.get("icon")))
              .append("</icon><order>")
              .append(escapeXmlText(perspective.get("order")))
              .append("</order></perspective>\n");
        }
        sb.append(" </perspectives>\n");
        sb.append(" <navigations>\n");
        sb.append(" </navigations>\n");
        appendMxGraphModel(sb, document, entities);
        sb.append("</model>\n");
        return sb.toString();
    }

    /** Entity box width and row heights for the deterministic grid layout. */
    private static final int CELL_WIDTH = 200;
    private static final int TITLE_HEIGHT = 28;
    private static final int ROW_HEIGHT = 26;
    private static final int GRID_COLUMNS = 3;
    private static final int COLUMN_GAP = 80;
    private static final int ROW_GAP = 40;
    private static final int GRID_ORIGIN = 20;

    /**
     * Append the {@code mxGraphModel} the EDM editor decodes to render the canvas: one
     * {@code style="entity"} vertex per entity carrying an {@code <Entity>} value, a child vertex per
     * property carrying a {@code <Property>} value, and one edge per foreign-key relation linking the
     * owner's FK property to the target entity's primary-key property. Entities are placed in a fixed
     * grid so the output is deterministic across regenerations.
     */
    private static void appendMxGraphModel(StringBuilder sb, EdmDocument document, List<Map<String, Object>> entities) {
        // Pre-compute cell ids and the per-entity primary-key property cell id (edge targets).
        Map<String, String> entityCellId = new HashMap<>();
        Map<String, Map<String, String>> propertyCellId = new HashMap<>();
        Map<String, String> pkCellIdByEntity = new HashMap<>();
        for (Map<String, Object> entity : entities) {
            String name = (String) entity.get("name");
            String entCell = "ent_" + sanitizeId(name);
            entityCellId.put(name, entCell);
            Map<String, String> props = new LinkedHashMap<>();
            List<Map<String, Object>> properties = propertiesOf(entity);
            for (Map<String, Object> property : properties) {
                String propName = (String) property.get("name");
                String propCell = entCell + "_p_" + sanitizeId(propName);
                props.put(propName, propCell);
                if ("true".equals(property.get("dataPrimaryKey")) && !pkCellIdByEntity.containsKey(name)) {
                    pkCellIdByEntity.put(name, propCell);
                }
            }
            propertyCellId.put(name, props);
        }

        sb.append(" <mxGraphModel>\n  <root>\n");
        sb.append("   <mxCell id=\"0\"/>\n");
        sb.append("   <mxCell id=\"1\" parent=\"0\"/>\n");

        int[] columnY = new int[GRID_COLUMNS];
        for (int i = 0; i < GRID_COLUMNS; i++) {
            columnY[i] = GRID_ORIGIN;
        }
        int index = 0;
        for (Map<String, Object> entity : entities) {
            String name = (String) entity.get("name");
            List<Map<String, Object>> properties = propertiesOf(entity);
            int column = index % GRID_COLUMNS;
            int x = GRID_ORIGIN + column * (CELL_WIDTH + COLUMN_GAP);
            int y = columnY[column];
            int height = TITLE_HEIGHT + ROW_HEIGHT * Math.max(properties.size(), 1);
            columnY[column] = y + height + ROW_GAP;
            index++;

            sb.append("   <mxCell id=\"")
              .append(entityCellId.get(name))
              .append("\" style=\"entity\" parent=\"1\" vertex=\"1\">\n");
            appendEntityValue(sb, entity);
            sb.append("    <mxGeometry x=\"")
              .append(x)
              .append("\" y=\"")
              .append(y)
              .append("\" width=\"")
              .append(CELL_WIDTH)
              .append("\" height=\"")
              .append(height)
              .append("\" as=\"geometry\"><mxRectangle width=\"")
              .append(CELL_WIDTH)
              .append("\" height=\"")
              .append(TITLE_HEIGHT)
              .append("\" as=\"alternateBounds\"/></mxGeometry>\n");
            sb.append("   </mxCell>\n");

            int propIndex = 0;
            for (Map<String, Object> property : properties) {
                sb.append("   <mxCell id=\"")
                  .append(propertyCellId.get(name)
                                        .get(property.get("name")))
                  .append("\" parent=\"")
                  .append(entityCellId.get(name))
                  .append("\" vertex=\"1\" connectable=\"0\">\n");
                appendPropertyValue(sb, property);
                sb.append("    <mxGeometry y=\"")
                  .append(TITLE_HEIGHT + propIndex * ROW_HEIGHT)
                  .append("\" width=\"")
                  .append(CELL_WIDTH)
                  .append("\" height=\"")
                  .append(ROW_HEIGHT)
                  .append("\" as=\"geometry\"/>\n");
                sb.append("   </mxCell>\n");
                propIndex++;
            }
        }

        // Edges: owner FK property -> target entity primary-key property.
        for (Map.Entry<String, List<Map<String, Object>>> entry : document.relationsByEntity.entrySet()) {
            String owner = entry.getKey();
            for (Map<String, Object> relation : entry.getValue()) {
                String property = (String) relation.get("property");
                String referenced = (String) relation.get("referenced");
                String source = propertyCellId.getOrDefault(owner, Map.of())
                                              .get(property);
                String target = pkCellIdByEntity.get(referenced);
                if (source == null || target == null) {
                    continue;
                }
                sb.append("   <mxCell id=\"edge_")
                  .append(sanitizeId(owner))
                  .append("_")
                  .append(sanitizeId(property))
                  .append("\" parent=\"1\" source=\"")
                  .append(source)
                  .append("\" target=\"")
                  .append(target)
                  .append("\" edge=\"1\"><mxGeometry relative=\"1\" as=\"geometry\"/></mxCell>\n");
            }
        }

        sb.append("  </root>\n </mxGraphModel>\n");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> propertiesOf(Map<String, Object> entity) {
        return (List<Map<String, Object>>) entity.getOrDefault("properties", List.of());
    }

    /**
     * The {@code <Entity>} cell value. {@code type="Entity"} is the constant cell-kind marker the
     * editor keys on; the PRIMARY/DEPENDENT distinction is carried in {@code entityType} (omitted for
     * PRIMARY), matching the editor's own serializer.
     */
    private static void appendEntityValue(StringBuilder sb, Map<String, Object> entity) {
        sb.append("    <Entity");
        appendAttribute(sb, "name", entity.get("name"));
        // PRIMARY is the editor's default (omitted); DEPENDENT and SETTING are carried explicitly so
        // the EDM editor restyles the cell and the template engine routes the entity correctly.
        Object entityType = entity.get("type");
        if ("DEPENDENT".equals(entityType) || "SETTING".equals(entityType)) {
            appendAttribute(sb, "entityType", entityType);
        }
        for (String key : new String[] {"dataName", "dataCount", "dataQuery", "title", "caption", "description", "tooltip", "menuKey",
                "menuLabel", "layoutType", "perspectiveName"}) {
            if (entity.get(key) != null) {
                appendAttribute(sb, key, entity.get(key));
            }
        }
        appendAttribute(sb, "type", "Entity");
        sb.append(" as=\"value\"/>\n");
    }

    /** The {@code <Property>} cell value - the property's attributes verbatim. */
    private static void appendPropertyValue(StringBuilder sb, Map<String, Object> property) {
        sb.append("    <Property");
        for (Map.Entry<String, Object> attr : property.entrySet()) {
            appendAttribute(sb, attr.getKey(), attr.getValue());
        }
        sb.append(" as=\"value\"/>\n");
    }

    /** mxGraph cell ids must be attribute-safe and stable; keep only word characters. */
    private static String sanitizeId(String raw) {
        return raw == null ? "" : raw.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static void appendAttribute(StringBuilder sb, String key, Object value) {
        sb.append(' ')
          .append(key)
          .append("=\"")
          .append(escapeXml(value == null ? "" : value.toString()))
          .append("\"");
    }

    private static String escapeXmlText(Object value) {
        return escapeXml(value == null ? "" : value.toString());
    }

    private static String escapeXml(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

}
