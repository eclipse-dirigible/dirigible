/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.entity;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
 * Emits a {@code <EntityName>Entity.ts} decorator-driven TypeScript file under {@code gen/} for
 * every entity declared in an intent. The output matches the canonical Dirigible entity shape (see
 * {@code tests/tests-integrations/src/main/resources/typescript/CustomerEntity.ts}) so that the
 * existing {@code EntitySynchronizer} (file extension {@code Entity.ts}) picks the files up on the
 * next reconciliation cycle and projects them into the Hibernate dynamic-map store.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output. Generators must not introduce
 * timestamps or stable-but-version-sensitive headers.
 */
@Component
@Order(100)
public class EntityIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityIntentGenerator.class);

    /** SQL column-name length cap for the auto-derived names. Keeps Oracle / older RDBMS happy. */
    private static final int MAX_COLUMN_NAME_LENGTH = 30;

    @Override
    public String name() {
        return "entity";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getEntities()
                 .isEmpty()) {
            return;
        }
        IRepository repository = context.getRepository();
        String genRoot = context.getGenRoot();
        Set<String> seenFqns = new HashSet<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() == null || entity.getName()
                                                  .isBlank()) {
                LOGGER.warn("Skipping unnamed entity in intent [{}]", context.getIntent()
                                                                             .getName());
                continue;
            }
            String fileName = entity.getName() + "Entity.ts";
            String path = genRoot + "/" + fileName;
            if (!seenFqns.add(path)) {
                LOGGER.warn("Duplicate entity [{}] in intent [{}] - keeping the first occurrence", entity.getName(), context.getIntent()
                                                                                                                            .getName());
                continue;
            }
            String source = render(entity);
            writeResource(repository, path, source);
        }
    }

    private static String render(EntityIntent entity) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated from .intent - do not edit by hand. Edit the .intent and republish.\n");
        sb.append("@Entity(\"")
          .append(entity.getName())
          .append("\")\n");
        sb.append("@Table(\"")
          .append(toUpperSnake(entity.getName()))
          .append("\")\n");
        sb.append("export class ")
          .append(entity.getName())
          .append(" {\n");
        for (int i = 0; i < entity.getFields()
                                  .size(); i++) {
            FieldIntent field = entity.getFields()
                                      .get(i);
            appendField(sb, entity, field);
            if (i < entity.getFields()
                          .size()
                    - 1
                    || !entity.getRelations()
                              .isEmpty()) {
                sb.append('\n');
            }
        }
        for (int i = 0; i < entity.getRelations()
                                  .size(); i++) {
            RelationIntent relation = entity.getRelations()
                                            .get(i);
            appendRelation(sb, relation);
            if (i < entity.getRelations()
                          .size()
                    - 1) {
                sb.append('\n');
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, EntityIntent entity, FieldIntent field) {
        if (field.getName() == null || field.getName()
                                            .isBlank()) {
            return;
        }
        if (field.isPrimaryKey()) {
            sb.append("    @Id()\n");
        }
        if (field.isGenerated()) {
            sb.append("    @Generated(\"sequence\")\n");
        }
        sb.append("    @Column({ name: \"")
          .append(toColumnName(entity.getName(), field.getName()))
          .append("\", type: \"")
          .append(mapType(field.getType()))
          .append("\"");
        if (field.getLength() != null && field.getLength() > 0) {
            sb.append(", length: ")
              .append(field.getLength());
        }
        if (field.isRequired() || field.isPrimaryKey()) {
            sb.append(", nullable: false");
        }
        if (field.getDefaultValue() != null && !field.getDefaultValue()
                                                     .isBlank()) {
            sb.append(", defaultValue: \"")
              .append(field.getDefaultValue())
              .append("\"");
        }
        sb.append(" })\n");
        sb.append("    public ")
          .append(field.getName())
          .append(": ")
          .append(mapTsType(field.getType()))
          .append(";\n");
    }

    private static void appendRelation(StringBuilder sb, RelationIntent relation) {
        if (relation.getName() == null || relation.getTo() == null) {
            return;
        }
        String kind = relation.getKind() == null ? "manyToOne" : relation.getKind();
        switch (kind) {
            case "oneToMany":
                sb.append("    @OneToMany(() => ")
                  .append(relation.getTo())
                  .append(", { joinColumn: \"")
                  .append(toColumnName(relation.getTo(), "id"))
                  .append("\" })\n");
                sb.append("    public ")
                  .append(relation.getName())
                  .append(": ")
                  .append(relation.getTo())
                  .append("[];\n");
                break;
            case "manyToOne":
            default:
                sb.append("    @ManyToOne(() => ")
                  .append(relation.getTo())
                  .append(", { joinColumn: \"")
                  .append(toColumnName(relation.getTo(), "id"))
                  .append("\" })\n");
                sb.append("    public ")
                  .append(relation.getName())
                  .append(": ")
                  .append(relation.getTo())
                  .append(";\n");
                break;
        }
    }

    /**
     * Map an intent logical field type to a Dirigible SDK column type string. Unknown types fall back
     * to {@code string} - generators must never crash on a typo; the engine logs a warning instead.
     */
    private static String mapType(String type) {
        if (type == null) {
            return "string";
        }
        switch (type.toLowerCase(Locale.ROOT)) {
            case "integer":
            case "int":
                return "integer";
            case "long":
                return "long";
            case "decimal":
            case "double":
                return "double";
            case "boolean":
                return "boolean";
            case "date":
                return "date";
            case "uuid":
                return "string";
            case "text":
                return "string";
            case "string":
            default:
                return "string";
        }
    }

    /**
     * Map the intent logical type to the TypeScript property type.
     */
    private static String mapTsType(String type) {
        if (type == null) {
            return "string";
        }
        switch (type.toLowerCase(Locale.ROOT)) {
            case "integer":
            case "int":
            case "long":
            case "decimal":
            case "double":
                return "number";
            case "boolean":
                return "boolean";
            case "date":
                return "Date";
            case "uuid":
            case "text":
            case "string":
            default:
                return "string";
        }
    }

    /**
     * Convert a camelCase or PascalCase identifier to UPPER_SNAKE_CASE for the column name. Capped at
     * {@link #MAX_COLUMN_NAME_LENGTH} characters.
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
     * Produce a stable column name of the form {@code <ENTITY>_<FIELD>}, snake-cased and capped.
     */
    private static String toColumnName(String entityName, String fieldName) {
        String raw = toUpperSnake(entityName) + "_" + toUpperSnake(fieldName);
        if (raw.length() <= MAX_COLUMN_NAME_LENGTH) {
            return raw;
        }
        return raw.substring(0, MAX_COLUMN_NAME_LENGTH);
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
