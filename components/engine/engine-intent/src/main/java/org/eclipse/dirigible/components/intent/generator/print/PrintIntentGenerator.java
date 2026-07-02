/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.print;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * Emits one {@code <Entity>.print} standard print template per document (header-items) entity — the
 * document-template DSL counterpart of the entity's Harmonia document view. The template is an
 * authoring artifact at the project root: on publish the {@code PrintTemplateSynchronizer} seeds it
 * into the CMS under {@code Templates/<Entity>/Print/<lang>/} where business users download and
 * upload customizations through the Documents perspective; the generated file is only the
 * never-customized default.
 *
 * <p>
 * Document masters are detected exactly like the EDM generator's document layout: an entity is a
 * document master when it is the composition parent of a child entity named {@code *Item}.
 *
 * <p>
 * The data contract the template binds against (assembled by the Print action from the already
 * loaded page state): a {@code document} object with the header entity's PascalCase properties
 * (relation FKs already resolved to display labels by the client) and an {@code items} list with
 * the line-item properties.
 */
@Component
@Order(800)
public class PrintIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintIntentGenerator.class);

    @Override
    public String name() {
        return "print";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        Map<EntityIntent, EntityIntent> masters = documentMasters(model);
        for (Map.Entry<EntityIntent, EntityIntent> master : masters.entrySet()) {
            String fileName = master.getKey()
                                    .getName()
                    + ".print";
            context.writeModelFile(fileName, buildTemplate(master.getKey(), master.getValue()));
            LOGGER.debug("Generated standard print template [{}]", fileName);
        }
    }

    /**
     * The document masters of the model: entities that are the composition parent of a {@code *Item}
     * child, mapped to that items entity (first {@code *Item} child wins, in declaration order — the
     * same convention the EDM generator uses for the document layout).
     *
     * @param model the parsed intent model
     * @return master entity to items entity, in declaration order
     */
    static Map<EntityIntent, EntityIntent> documentMasters(IntentModel model) {
        Map<String, EntityIntent> byName = new LinkedHashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Map<EntityIntent, EntityIntent> masters = new LinkedHashMap<>();
        for (EntityIntent child : model.getEntities()) {
            String childName = child.getName();
            if (childName == null || !childName.endsWith("Item")) {
                continue;
            }
            for (RelationIntent relation : child.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.isComposition() && relation.getTo() != null) {
                    EntityIntent parent = byName.get(relation.getTo());
                    if (parent != null && !masters.containsKey(parent)) {
                        masters.put(parent, child);
                    }
                    break;
                }
            }
        }
        return masters;
    }

    /**
     * Builds the standard template for one document master.
     *
     * @param master the header entity
     * @param items the line-items entity
     * @return the {@code .print} template source
     */
    static String buildTemplate(EntityIntent master, EntityIntent items) {
        String label = IntentNaming.humanize(master.getName());
        StringBuilder template = new StringBuilder(4096);
        template.append("<!-- Standard print template for ")
                .append(label)
                .append(", generated from the intent model.\n")
                .append("     The published copy is seeded into the CMS under Templates/")
                .append(master.getName())
                .append("/Print/<lang>/ where it\n")
                .append("     can be customized (download/upload) through the Documents perspective. -->\n");
        template.append("<document id=\"")
                .append(IntentNaming.upperSnake(master.getName())
                                    .toLowerCase()
                                    .replace('_', '-'))
                .append("-print\">\n");
        template.append("    <page width=\"595\" height=\"842\" padding=\"40\">\n\n");

        // header: humanized title + the document number (when a documentTitle field exists)
        template.append("        <header>\n");
        template.append("            <text align=\"right\" style=\"title\">")
                .append(escape(label))
                .append("</text>\n");
        FieldIntent number = documentTitleField(master);
        if (number != null) {
            template.append("            <text align=\"right\" style=\"subtitle\">{{document.")
                    .append(IntentNaming.pascalCase(number.getName()))
                    .append("}}</text>\n");
        }
        template.append("            <line/>\n");
        template.append("        </header>\n\n");

        // header fields: plain non-aggregate fields + to-one relations (labels resolved client-side)
        template.append("        <section>\n");
        for (FieldIntent field : master.getFields()) {
            if (field.isPrimaryKey() || field.isAggregate() || field == number) {
                continue;
            }
            appendField(template, "            ", field.getName());
        }
        for (RelationIntent relation : master.getRelations()) {
            if (isToOne(relation)) {
                appendField(template, "            ", relation.getName());
            }
        }
        template.append("        </section>\n\n");

        // line items
        template.append("        <table source=\"items\">\n");
        List<FieldIntent> itemFields = itemColumns(items);
        for (int i = 0; i < itemFields.size(); i++) {
            FieldIntent field = itemFields.get(i);
            String width = i == 0 ? "3*" : "*";
            String align = isNumeric(field) ? " align=\"right\"" : "";
            template.append("            <column width=\"")
                    .append(width)
                    .append("\"")
                    .append(align)
                    .append(" label=\"")
                    .append(escape(IntentNaming.humanize(field.getName())))
                    .append("\">{{")
                    .append(IntentNaming.pascalCase(field.getName()))
                    .append("}}</column>\n");
        }
        template.append("        </table>\n\n");

        // totals: aggregate fields, the last one emphasized as the document total
        List<FieldIntent> aggregates = new ArrayList<>();
        for (FieldIntent field : master.getFields()) {
            if (field.isAggregate()) {
                aggregates.add(field);
            }
        }
        if (!aggregates.isEmpty()) {
            template.append("        <section align=\"right\">\n");
            FieldIntent total = totalField(aggregates);
            for (FieldIntent aggregate : aggregates) {
                if (aggregate == total) {
                    continue;
                }
                appendField(template, "            ", aggregate.getName());
            }
            template.append("            <total align=\"right\">{{document.")
                    .append(IntentNaming.pascalCase(total.getName()))
                    .append("}}</total>\n");
            template.append("        </section>\n\n");
        }

        template.append("        <footer>\n");
        template.append("            <text align=\"center\">")
                .append(escape(label))
                .append(" {{document.")
                .append(number != null ? IntentNaming.pascalCase(number.getName()) : "Id")
                .append("}}</text>\n");
        template.append("        </footer>\n\n");
        template.append("    </page>\n");
        template.append("</document>\n");
        return template.toString();
    }

    private static void appendField(StringBuilder template, String indent, String name) {
        template.append(indent)
                .append("<field label=\"")
                .append(escape(IntentNaming.humanize(name)))
                .append("\">{{document.")
                .append(IntentNaming.pascalCase(name))
                .append("}}</field>\n");
    }

    /** The line-item columns: every field except the PK; the master FK relation never shows. */
    private static List<FieldIntent> itemColumns(EntityIntent items) {
        List<FieldIntent> columns = new ArrayList<>();
        for (FieldIntent field : items.getFields()) {
            if (!field.isPrimaryKey()) {
                columns.add(field);
            }
        }
        return columns;
    }

    private static FieldIntent documentTitleField(EntityIntent master) {
        for (FieldIntent field : master.getFields()) {
            if (field.isDocumentTitle()) {
                return field;
            }
        }
        return null;
    }

    /** The field named {@code total} when present, else the last aggregate. */
    private static FieldIntent totalField(List<FieldIntent> aggregates) {
        for (FieldIntent aggregate : aggregates) {
            if ("total".equalsIgnoreCase(aggregate.getName())) {
                return aggregate;
            }
        }
        return aggregates.get(aggregates.size() - 1);
    }

    private static boolean isToOne(RelationIntent relation) {
        return ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind())) && !relation.isComposition();
    }

    private static boolean isNumeric(FieldIntent field) {
        return switch (field.getType() == null ? "" : field.getType()) {
            case "integer", "int", "long", "decimal", "double" -> true;
            default -> false;
        };
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
