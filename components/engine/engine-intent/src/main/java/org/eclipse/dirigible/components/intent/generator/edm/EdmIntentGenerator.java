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
 * {@code ORDERS_COUNTRY}, codbex-style). This keeps physical table names unique across projects and
 * away from SQL reserved words like {@code ORDER}; the {@code .report} and {@code .csvim}
 * generators use the same convention so all three artefacts agree on the table name.</li>
 * <li>A {@code required} {@code manyToOne}/{@code oneToOne} relation is a <b>composition</b>: the
 * FK property carries the {@code relationship*} attributes, the owning entity becomes
 * {@code DEPENDENT} with {@code MANAGE_DETAILS} layout and inherits the perspective of its
 * (transitively resolved) composition parent. An optional relation is an <b>association</b>: a
 * plain DROPDOWN property, the entity stays {@code PRIMARY} with its own perspective.</li>
 * <li>Dropdown key / value and the relation's {@code referencedProperty} are derived from the
 * target entity's actual fields (its primary key and its {@code name}-like field), never
 * hardcoded.</li>
 * <li>The {@code .model} JSON carries {@code entities} / {@code perspectives} /
 * {@code navigations}; relations appear only in the XML as {@code <relation>} elements interleaved
 * with their owning {@code <entity>}.</li>
 * </ul>
 * No {@code mxGraphModel} diagram block is emitted - the EDM editor lays out a missing diagram on
 * first open, which keeps the output deterministic across regenerations.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(200)
public class EdmIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdmIntentGenerator.class);

    private static final String DEFAULT_ICON = "/services/web/resources/unicons/file.svg";

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
            boolean dependent = compositionParents.containsKey(name);
            String perspective = resolvePerspective(name, compositionParents);
            Map<String, Object> entityMap =
                    entityDefaults(name, entity.getDescription(), dependent, perspective, tablePrefix, perspectiveOrder);
            if (!dependent) {
                perspectiveList.add(perspectiveEntry(name, perspectiveOrder));
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
            List<Map<String, Object>> relations = new ArrayList<>();
            boolean compositionAssigned = false;
            for (RelationIntent relation : entity.getRelations()) {
                if (!"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                    continue;
                }
                if (relation.getName() == null || relation.getTo() == null) {
                    continue;
                }
                boolean composition = !compositionAssigned && relation.isRequired();
                compositionAssigned |= composition;
                EntityIntent target = byName.get(relation.getTo());
                properties.add(relationProperty(name, relation, target, composition));
                relations.add(relationLink(name, relation, target, compositionParents));
            }
            entityMap.put("properties", properties);
            entityList.add(entityMap);
            if (!relations.isEmpty()) {
                document.relationsByEntity.put(name, relations);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("entities", entityList);
        body.put("perspectives", perspectiveList);
        body.put("navigations", new ArrayList<>());
        document.modelJson.put("model", body);
        return document;
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
     * Map each entity to its composition parent: the target of its first {@code required}
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
                if (toOne && relation.isRequired() && relation.getTo() != null) {
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

    private static Map<String, Object> perspectiveEntry(String name, int order) {
        Map<String, Object> perspective = new LinkedHashMap<>();
        perspective.put("name", name);
        perspective.put("label", name);
        perspective.put("icon", DEFAULT_ICON);
        perspective.put("order", Integer.toString(order));
        return perspective;
    }

    private static Map<String, Object> entityDefaults(String name, String description, boolean dependent, String perspective,
            String tablePrefix, int order) {
        String dataName = tablePrefix + "_" + IntentNaming.upperSnake(name);
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", name);
        entity.put("dataName", dataName);
        entity.put("dataCount", "SELECT COUNT(*) AS COUNT FROM \"${tablePrefix}" + dataName + "\"");
        entity.put("dataQuery", "");
        entity.put("type", dependent ? "DEPENDENT" : "PRIMARY");
        entity.put("title", name);
        entity.put("caption", "Manage entity " + name);
        entity.put("description", description != null && !description.isBlank() ? description : "Manage entity " + name);
        entity.put("tooltip", name);
        entity.put("icon", DEFAULT_ICON);
        entity.put("menuKey", name.toLowerCase(Locale.ROOT));
        entity.put("menuLabel", name);
        entity.put("menuIndex", "100");
        entity.put("layoutType", dependent ? "MANAGE_DETAILS" : "MANAGE_MASTER");
        entity.put("perspectiveName", perspective);
        entity.put("perspectiveLabel", perspective);
        entity.put("perspectiveHeader", "");
        entity.put("perspectiveIcon", DEFAULT_ICON);
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
        p.put("name", field.getName());
        p.put("description", field.getDescription() == null ? "" : field.getDescription());
        p.put("tooltip", "");
        p.put("dataName", column);
        p.put("dataType", dataType);
        p.put("dataNullable", field.isRequired() || field.isPrimaryKey() ? "false" : "true");
        if (field.isPrimaryKey()) {
            p.put("dataPrimaryKey", "true");
        }
        if (field.isPrimaryKey() && field.isGenerated()) {
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
        p.put("widgetType", widgetForType(dataType));
        p.put("widgetSize", "");
        p.put("widgetLength", length == null ? "20" : length.toString());
        p.put("widgetIsMajor", "true");
        return p;
    }

    /**
     * FK property added to the owning entity for a {@code manyToOne}/{@code oneToOne} relation. Renders
     * as a DROPDOWN keyed by the target entity's actual primary-key field and labelled by its
     * {@code name}-like field. Only the entity's composition relation (its first {@code required}
     * to-one) carries the {@code relationship*} attributes that make the EDM editor treat the owner as
     * a detail of the target - further required relations stay plain NOT NULL associations, mirroring
     * how the EDM editor writes multi-FK entities.
     */
    private static Map<String, Object> relationProperty(String ownerEntity, RelationIntent relation, EntityIntent target,
            boolean composition) {
        String column = IntentNaming.upperSnake(ownerEntity) + "_" + IntentNaming.upperSnake(relation.getName());
        FieldIntent targetPk = primaryKeyOf(target);
        String fkType = targetPk == null ? "INTEGER" : mapDataType(targetPk.getType());
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", relation.getName());
        p.put("description", relation.getDescription() == null ? "" : relation.getDescription());
        p.put("tooltip", "");
        p.put("dataName", column);
        p.put("dataType", fkType);
        p.put("dataNullable", relation.isRequired() ? "false" : "true");
        if ("VARCHAR".equals(fkType) && targetPk != null) {
            Integer length = fieldLength(targetPk);
            if (length != null && length > 0) {
                p.put("dataLength", length.toString());
            }
        }
        if (composition) {
            p.put("relationshipType", "COMPOSITION");
            p.put("relationshipCardinality", "1_n");
            p.put("relationshipName", relation.getName());
        }
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
            Map<String, String> compositionParents) {
        Map<String, Object> link = new LinkedHashMap<>();
        String linkName = ownerEntity + "_" + relation.getName();
        link.put("name", linkName);
        link.put("type", "relation");
        link.put("entity", ownerEntity);
        link.put("relationName", linkName);
        link.put("relationshipEntityPerspectiveName", resolvePerspective(relation.getTo(), compositionParents));
        link.put("relationshipEntityPerspectiveLabel", "Entities");
        link.put("property", relation.getName());
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

    /** The dropdown key: the target's actual PK field name; {@code Id} only as a last resort. */
    private static String keyFieldName(EntityIntent target) {
        FieldIntent pk = primaryKeyOf(target);
        return pk == null ? "Id" : pk.getName();
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
                return field.getName();
            }
        }
        for (FieldIntent field : target.getFields()) {
            if (field.getName() != null && "VARCHAR".equals(mapDataType(field.getType())) && !field.isPrimaryKey()) {
                return field.getName();
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
     * Render the EDM XML shape: entities with their relations interleaved, then the perspectives and
     * navigations blocks, mirroring documents the EDM editor itself writes (minus the
     * {@code mxGraphModel} diagram, which the editor recreates).
     */
    @SuppressWarnings("unchecked")
    private static String renderEdmXml(EdmDocument document) {
        Map<String, Object> body = (Map<String, Object>) document.modelJson.get("model");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) body.get("entities");
        List<Map<String, Object>> perspectives = (List<Map<String, Object>>) body.get("perspectives");

        StringBuilder sb = new StringBuilder(4096);
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
        sb.append("</model>\n");
        return sb.toString();
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
