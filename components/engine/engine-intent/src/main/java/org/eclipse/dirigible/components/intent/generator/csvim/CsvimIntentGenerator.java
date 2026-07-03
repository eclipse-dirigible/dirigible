/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.csvim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.eclipse.dirigible.components.intent.model.SeedIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits two files per {@link SeedIntent}:
 * <ul>
 * <li>{@code <seed>.csvim} - a CSVIM declaration the platform's {@code CsvimSynchronizer} consumes.
 * The {@code file} path is project-qualified ({@code /<project>/<seed>.csv}) because
 * {@code CsvimProcessor} resolves it against {@code /registry/public}, exactly like the existing
 * {@code CsvimIT} fixtures do.</li>
 * <li>{@code <seed>.csv} - the CSV body. Header row carries the entity's column names (upper-snake
 * of the field names prefixed with the entity name); row order matches the entity's declared field
 * order so row authors can omit fields and the column still maps unambiguously.</li>
 * </ul>
 *
 * <p>
 * The {@code table} value comes from {@link IntentNaming#tableName} - the same intent-prefixed name
 * the {@code .edm} declares as {@code dataName}, so the import targets the table the downstream
 * template will create. Note the table only exists after "Generate from EDM" output is published;
 * until then the CSVIM import is retried by its own synchronizer.
 *
 * <p>
 * Defaults match the existing {@code CsvimIT} sample: {@code header: true},
 * {@code useHeaderNames: true}, {@code delimField: ","}, {@code delimEnclosing: "\""},
 * {@code distinguishEmptyFromNull: true}, {@code version: "1.0"}. Schema defaults to {@code PUBLIC}
 * when the seed does not override it.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(700)
public class CsvimIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvimIntentGenerator.class);

    private static final String DEFAULT_SCHEMA = "PUBLIC";
    private static final String FIELD_DELIM = ",";
    private static final String QUOTE_DELIM = "\"";

    @Override
    public String name() {
        return "csvim";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getSeeds()
                 .isEmpty()) {
            return;
        }
        Map<String, EntityIntent> entitiesByName = indexEntities(model);
        Set<String> seenFiles = new HashSet<>();
        for (SeedIntent seed : model.getSeeds()) {
            if (seed.getName() == null || seed.getName()
                                              .isBlank()) {
                LOGGER.warn("Skipping unnamed seed in intent [{}]", IntentNaming.baseName(context));
                continue;
            }
            EntityIntent entity = entitiesByName.get(seed.getEntity());
            if (entity == null) {
                LOGGER.warn("Seed [{}] references unknown entity [{}] - skipping", seed.getName(), seed.getEntity());
                continue;
            }
            String fileName = fileNameOnly(seed.getName());
            if (!seenFiles.add(fileName)) {
                LOGGER.warn("Duplicate seed [{}] in intent [{}] - keeping the first occurrence", seed.getName(),
                        IntentNaming.baseName(context));
                continue;
            }
            // A file seed references an AUTHORED CSV (large data sets - countries, currencies, ...):
            // only the .csvim is generated, pointing at the developer-owned file. Otherwise the CSV
            // body is generated from the inline rows - into the sibling <TABLE>_LANG table for a
            // language seed (`language: bg`, translations for a multilingual entity), else the base.
            if (!seed.isFileSeed()) {
                String csv = seed.isLanguageSeed() ? renderLanguageCsv(entity, seed) : renderCsv(orderedFieldsOf(entity), entity, seed);
                context.writeModelFile(fileName + ".csv", csv);
            }
            context.writeModelFile(fileName + ".csvim", renderCsvim(context, seed, entity, fileName));
        }
    }

    private static Map<String, EntityIntent> indexEntities(IntentModel model) {
        Map<String, EntityIntent> index = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                index.put(entity.getName(), entity);
            }
        }
        return index;
    }

    /**
     * Return the entity's fields in declaration order, skipping unnamed ones. Field order in the CSV
     * header matches this list, so row authors get a predictable mapping even when they omit optional
     * columns.
     */
    private static List<FieldIntent> orderedFieldsOf(EntityIntent entity) {
        List<FieldIntent> ordered = new ArrayList<>();
        for (FieldIntent field : entity.getFields()) {
            if (field.getName() != null && !field.getName()
                                                 .isBlank()) {
                ordered.add(field);
            }
        }
        return ordered;
    }

    /**
     * Test seam: render a seed's CSV body without a generation context - dispatching between the base
     * and the language-table shape exactly like {@link #generate}. Never use in production code.
     */
    static String renderCsvForTest(EntityIntent entity, SeedIntent seed) {
        return seed.isLanguageSeed() ? renderLanguageCsv(entity, seed) : renderCsv(orderedFieldsOf(entity), entity, seed);
    }

    private static String renderCsv(List<FieldIntent> fields, EntityIntent entity, SeedIntent seed) {
        StringBuilder sb = new StringBuilder(256);
        String entityDataName = IntentNaming.upperSnake(entity.getName());
        // A seed row may also set a to-one relation's FK by the relation's authored name (e.g.
        // `Country: 34` on a City row). Only relations actually referenced by a row become columns, so
        // relation-free seeds keep their exact previous shape.
        List<String> relationColumns = referencedRelationColumns(entity, seed);
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(FIELD_DELIM);
            }
            sb.append(entityDataName)
              .append('_')
              .append(IntentNaming.upperSnake(fields.get(i)
                                                    .getName()));
        }
        for (String relation : relationColumns) {
            sb.append(FIELD_DELIM)
              .append(entityDataName)
              .append('_')
              .append(IntentNaming.upperSnake(relation));
        }
        sb.append('\n');
        for (Map<String, Object> row : seed.getRows()) {
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    sb.append(FIELD_DELIM);
                }
                Object value = row.get(fields.get(i)
                                             .getName());
                sb.append(formatCell(value));
            }
            for (String relation : relationColumns) {
                sb.append(FIELD_DELIM)
                  .append(formatCell(row.get(relation)));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * The entity's to-one relations referenced by at least one seed row (by authored relation name), in
     * declaration order. Each becomes an FK column {@code <ENTITY>_<RELATION>} - the same
     * {@code dataName} the EDM generator gives the FK property.
     */
    private static List<String> referencedRelationColumns(EntityIntent entity, SeedIntent seed) {
        List<String> columns = new ArrayList<>();
        for (RelationIntent relation : entity.getRelations()) {
            String name = relation.getName();
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (name == null || !toOne) {
                continue;
            }
            for (Map<String, Object> row : seed.getRows()) {
                if (row.containsKey(name)) {
                    columns.add(name);
                    break;
                }
            }
        }
        return columns;
    }

    /**
     * CSV cell formatter. Null becomes empty; values containing the field delimiter, the quote
     * character, or a line break are quoted and inner quotes doubled. Everything else passes through as
     * {@link Object#toString()}.
     */
    private static String formatCell(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        boolean needsQuote = s.contains(FIELD_DELIM) || s.contains(QUOTE_DELIM) || s.contains("\n") || s.contains("\r");
        if (!needsQuote) {
            return s;
        }
        return QUOTE_DELIM + s.replace(QUOTE_DELIM, QUOTE_DELIM + QUOTE_DELIM) + QUOTE_DELIM;
    }

    /**
     * CSV body of a translation seed: {@code GUID,Id,<referenced translatable PascalCase columns>,
     * Language} - the shape of the schema-generated {@code
     *
    <TABLE>
     * _LANG} table. {@code GUID} is auto-numbered by row order, {@code Id} references the translated
     * base row, {@code Language} is the seed's constant code. Only translatable (string-typed, non-PK)
     * fields referenced by at least one row become columns.
     */
    private static String renderLanguageCsv(EntityIntent entity, SeedIntent seed) {
        List<FieldIntent> translatable = new ArrayList<>();
        for (FieldIntent field : orderedFieldsOf(entity)) {
            if (field.isPrimaryKey() || !isTranslatableType(field)) {
                continue;
            }
            for (Map<String, Object> row : seed.getRows()) {
                if (row.containsKey(field.getName())) {
                    translatable.add(field);
                    break;
                }
            }
        }
        String idField = primaryKeyName(entity);
        StringBuilder sb = new StringBuilder(256);
        sb.append("GUID")
          .append(FIELD_DELIM)
          .append("Id");
        for (FieldIntent field : translatable) {
            sb.append(FIELD_DELIM)
              .append(IntentNaming.pascalCase(field.getName()));
        }
        sb.append(FIELD_DELIM)
          .append("Language")
          .append('\n');
        int guid = 1;
        for (Map<String, Object> row : seed.getRows()) {
            sb.append(guid++)
              .append(FIELD_DELIM)
              .append(formatCell(row.get(idField)));
            for (FieldIntent field : translatable) {
                sb.append(FIELD_DELIM)
                  .append(formatCell(row.get(field.getName())));
            }
            sb.append(FIELD_DELIM)
              .append(formatCell(seed.getLanguage()))
              .append('\n');
        }
        return sb.toString();
    }

    /** Whether the field's logical type maps to a translatable (string) column. */
    private static boolean isTranslatableType(FieldIntent field) {
        String type = field.getType() == null ? "string"
                : field.getType()
                       .toLowerCase(java.util.Locale.ROOT);
        return "string".equals(type) || "text".equals(type);
    }

    /** The entity's primary-key field name ({@code id} by convention). */
    private static String primaryKeyName(EntityIntent entity) {
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() && field.getName() != null) {
                return field.getName();
            }
        }
        return "id";
    }

    private static String renderCsvim(IntentGenerationContext context, SeedIntent seed, EntityIntent entity, String fileName) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("table", IntentNaming.tableName(context, entity.getName()) + (seed.isLanguageSeed() ? "_LANG" : ""));
        entry.put("schema", seed.getSchema() == null || seed.getSchema()
                                                            .isBlank() ? DEFAULT_SCHEMA : seed.getSchema());
        String csvPath = seed.isFileSeed() ? seed.getFile()
                                                 .trim()
                : fileName + ".csv";
        entry.put("file", "/" + context.getProjectName() + "/" + csvPath);
        entry.put("header", true);
        entry.put("useHeaderNames", true);
        entry.put("delimField", FIELD_DELIM);
        entry.put("delimEnclosing", QUOTE_DELIM);
        entry.put("distinguishEmptyFromNull", true);
        entry.put("version", "1.0");
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("files", List.of(entry));
        return JsonHelper.toJson(document);
    }

    /**
     * Bare file name. Seed names should already be free of path separators, but normalize defensively.
     */
    private static String fileNameOnly(String name) {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? name : name.substring(slash + 1);
    }
}
