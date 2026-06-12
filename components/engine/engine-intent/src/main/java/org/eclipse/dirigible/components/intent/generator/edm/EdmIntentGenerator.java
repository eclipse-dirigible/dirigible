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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits {@code gen/<intent>.edm} (XML) and its JSON twin {@code gen/<intent>.model} for every
 * intent that declares one or more entities. The pair is the canonical entity-data-model file
 * consumed by the EDM editor in the IDE and by the downstream "Generate from EDM" template engine,
 * which turns the model into UI / Java / SQL artefacts in a second step. The intent layer never
 * emits those second-stage artefacts itself - that contract belongs to the existing template
 * generators.
 *
 * <p>
 * The intent JSON is intentionally narrower than the EDM XML attribute surface. Everything the EDM
 * editor expects but the intent omits (icons, menu keys, layout type, perspective metadata, widget
 * type) is filled with conservative defaults derived from the entity / field name:
 * <ul>
 * <li>{@code dataName} = upper-snake of the name</li>
 * <li>{@code icon} / {@code perspectiveIcon} =
 * {@code /services/web/resources/unicons/file.svg}</li>
 * <li>{@code type} = {@code PRIMARY}, or {@code DEPENDENT} if another entity owns it via a
 * {@code manyToOne}</li>
 * <li>{@code layoutType} = {@code MANAGE_MASTER} / {@code MANAGE_DETAILS} matching the above</li>
 * <li>{@code widgetType} = derived from field type (TEXTBOX / NUMBER / DATE / CHECKBOX); FK
 * properties get DROPDOWN</li>
 * </ul>
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
        Map<String, Object> root = buildModel(model);
        String baseName = baseName(context);
        String genRoot = context.getGenRoot();
        IRepository repo = context.getRepository();
        writeResource(repo, genRoot + "/" + baseName + ".model", JsonHelper.toJson(root));
        writeResource(repo, genRoot + "/" + baseName + ".edm", renderEdmXml(root));
    }

    /**
     * Build the typed map that mirrors the canonical {@code .model} JSON shape. Both the JSON
     * serializer and the XML renderer consume this same tree, so the two on-disk formats can never
     * drift.
     */
    private static Map<String, Object> buildModel(IntentModel model) {
        List<EntityIntent> entities = model.getEntities();
        Set<String> dependentEntities = computeDependents(entities);

        List<Map<String, Object>> entityList = new ArrayList<>();
        List<Map<String, Object>> relationList = new ArrayList<>();
        int order = 1;

        for (EntityIntent entity : entities) {
            String name = entity.getName();
            if (name == null || name.isBlank()) {
                LOGGER.warn("Skipping unnamed entity in intent");
                continue;
            }
            boolean dependent = dependentEntities.contains(name);
            Map<String, Object> entityMap = entityDefaults(name, entity.getDescription(), dependent, order++);

            List<Map<String, Object>> properties = new ArrayList<>();
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() == null || field.getName()
                                                    .isBlank()) {
                    continue;
                }
                properties.add(propertyMap(name, field));
            }
            for (RelationIntent relation : entity.getRelations()) {
                if (!"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                    continue;
                }
                if (relation.getName() == null || relation.getTo() == null) {
                    continue;
                }
                properties.add(relationProperty(name, relation));
                relationList.add(relationLink(name, relation));
            }
            entityMap.put("properties", properties);
            entityList.add(entityMap);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("entities", entityList);
        if (!relationList.isEmpty()) {
            body.put("relations", relationList);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("model", body);
        return root;
    }

    /**
     * Build the set of entity names that are owned by another entity through an outgoing
     * {@code manyToOne}. Used to decide PRIMARY vs DEPENDENT and MASTER vs DETAILS layouts.
     */
    private static Set<String> computeDependents(List<EntityIntent> entities) {
        Map<String, Boolean> result = new HashMap<>();
        for (EntityIntent entity : entities) {
            for (RelationIntent relation : entity.getRelations()) {
                if ("manyToOne".equals(relation.getKind()) && entity.getName() != null) {
                    result.put(entity.getName(), true);
                }
            }
        }
        return result.keySet();
    }

    private static Map<String, Object> entityDefaults(String name, String description, boolean dependent, int order) {
        String dataName = toUpperSnake(name);
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
        entity.put("perspectiveName", name);
        entity.put("perspectiveLabel", name);
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
        String column = toUpperSnake(entityName) + "_" + toUpperSnake(field.getName());
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
        Integer length = field.getLength() != null ? field.getLength() : defaultLength(dataType);
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
     * as a DROPDOWN bound to the target entity's Id/Name.
     */
    private static Map<String, Object> relationProperty(String ownerEntity, RelationIntent relation) {
        String column = toUpperSnake(ownerEntity) + "_" + toUpperSnake(relation.getName());
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", relation.getName());
        p.put("description", relation.getDescription() == null ? "" : relation.getDescription());
        p.put("tooltip", "");
        p.put("dataName", column);
        p.put("dataType", "INTEGER");
        p.put("dataNullable", relation.isRequired() ? "false" : "true");
        p.put("relationshipType", "COMPOSITION");
        p.put("relationshipCardinality", "1_n");
        p.put("relationshipName", relation.getName());
        p.put("widgetType", "DROPDOWN");
        p.put("widgetSize", "");
        p.put("widgetLength", "20");
        p.put("widgetIsMajor", "true");
        p.put("widgetDropDownKey", "Id");
        p.put("widgetDropDownValue", "Name");
        return p;
    }

    /**
     * Top-level {@code <relation>} link that the EDM editor uses to render arrows on the canvas.
     */
    private static Map<String, Object> relationLink(String ownerEntity, RelationIntent relation) {
        Map<String, Object> link = new LinkedHashMap<>();
        String linkName = ownerEntity + "_" + relation.getName();
        link.put("name", linkName);
        link.put("type", "relation");
        link.put("entity", ownerEntity);
        link.put("relationName", linkName);
        link.put("relationshipEntityPerspectiveName", relation.getTo());
        link.put("relationshipEntityPerspectiveLabel", "Entities");
        link.put("property", relation.getName());
        link.put("referenced", relation.getTo());
        link.put("referencedProperty", "Id");
        return link;
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
     * Camel-/Pascal-case to upper snake. Handles {@code IDValue} -> {@code ID_VALUE}.
     */
    private static String toUpperSnake(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(name.length() + 8);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) {
                out.append('_');
            }
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }

    /**
     * Render the typed model tree as the EDM XML shape. The XML is deliberately minimal - the EDM
     * editor accepts files without the {@code <ui>} or {@code <model>}-wrapped extension blocks and
     * fills its own defaults on first edit.
     */
    @SuppressWarnings("unchecked")
    private static String renderEdmXml(Map<String, Object> root) {
        Map<String, Object> body = (Map<String, Object>) root.get("model");
        List<Map<String, Object>> entities =
                body == null ? Collections.emptyList() : (List<Map<String, Object>>) body.getOrDefault("entities", Collections.emptyList());
        List<Map<String, Object>> relations = body == null ? Collections.emptyList()
                : (List<Map<String, Object>>) body.getOrDefault("relations", Collections.emptyList());

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<model>\n");
        sb.append("  <entities>\n");
        for (Map<String, Object> entity : entities) {
            sb.append("    <entity");
            List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.getOrDefault("properties", Collections.emptyList());
            for (Map.Entry<String, Object> attr : entity.entrySet()) {
                if ("properties".equals(attr.getKey())) {
                    continue;
                }
                appendAttribute(sb, attr.getKey(), attr.getValue());
            }
            sb.append(">\n");
            for (Map<String, Object> property : properties) {
                sb.append("      <property");
                for (Map.Entry<String, Object> attr : property.entrySet()) {
                    appendAttribute(sb, attr.getKey(), attr.getValue());
                }
                sb.append("></property>\n");
            }
            sb.append("    </entity>\n");
        }
        sb.append("  </entities>\n");
        for (Map<String, Object> relation : relations) {
            sb.append("  <relation");
            for (Map.Entry<String, Object> attr : relation.entrySet()) {
                appendAttribute(sb, attr.getKey(), attr.getValue());
            }
            sb.append("></relation>\n");
        }
        sb.append("</model>\n");
        return sb.toString();
    }

    private static void appendAttribute(StringBuilder sb, String key, Object value) {
        sb.append(' ')
          .append(key)
          .append("=\"")
          .append(escapeXmlAttribute(value == null ? "" : value.toString()))
          .append("\"");
    }

    private static String escapeXmlAttribute(String raw) {
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

    private static String baseName(IntentGenerationContext context) {
        String intentName = context.getIntent()
                                   .getName();
        if (intentName != null && !intentName.isBlank()) {
            return intentName;
        }
        String project = context.getProjectName();
        return project.isEmpty() ? "intent" : project;
    }

    private static void writeResource(IRepository repository, String path, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(bytes);
        } else {
            repository.createResource(path, bytes);
        }
    }
}
