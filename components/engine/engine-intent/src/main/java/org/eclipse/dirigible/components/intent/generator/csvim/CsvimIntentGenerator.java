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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.eclipse.dirigible.components.intent.model.SeedIntent;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits two files per {@link SeedIntent}:
 * <ul>
 * <li>{@code gen/<seed>.csvim} - a CSVIM declaration the platform's {@code CsvimSynchronizer}
 * consumes. References the sibling CSV file by registry-relative path.</li>
 * <li>{@code gen/<seed>.csv} - the CSV body. Header row carries the entity's {@code dataName}
 * columns (upper-snake of the field names), row order matches the entity's declared field order so
 * row authors can omit fields and the column still maps unambiguously.</li>
 * </ul>
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
        IRepository repository = context.getRepository();
        String genRoot = context.getGenRoot();
        Set<String> seenFiles = new HashSet<>();
        for (SeedIntent seed : model.getSeeds()) {
            if (seed.getName() == null || seed.getName()
                                              .isBlank()) {
                LOGGER.warn("Skipping unnamed seed in intent [{}]", context.getIntent()
                                                                           .getName());
                continue;
            }
            EntityIntent entity = entitiesByName.get(seed.getEntity());
            if (entity == null) {
                LOGGER.warn("Seed [{}] references unknown entity [{}] - skipping", seed.getName(), seed.getEntity());
                continue;
            }
            String fileName = seed.getName();
            if (!seenFiles.add(fileName)) {
                LOGGER.warn("Duplicate seed [{}] in intent [{}] - keeping the first occurrence", seed.getName(), context.getIntent()
                                                                                                                        .getName());
                continue;
            }
            List<FieldIntent> orderedFields = orderedFieldsOf(entity);
            String csv = renderCsv(orderedFields, entity, seed);
            String csvim = renderCsvim(seed, entity, fileName);
            writeResource(repository, genRoot + "/" + fileName + ".csv", csv);
            writeResource(repository, genRoot + "/" + fileName + ".csvim", csvim);
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

    private static String renderCsv(List<FieldIntent> fields, EntityIntent entity, SeedIntent seed) {
        StringBuilder sb = new StringBuilder(256);
        String entityDataName = toUpperSnake(entity.getName());
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(FIELD_DELIM);
            }
            sb.append(entityDataName)
              .append('_')
              .append(toUpperSnake(fields.get(i)
                                         .getName()));
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
            sb.append('\n');
        }
        return sb.toString();
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

    private static String renderCsvim(SeedIntent seed, EntityIntent entity, String fileName) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("table", toUpperSnake(entity.getName()));
        entry.put("schema", seed.getSchema() == null || seed.getSchema()
                                                            .isBlank() ? DEFAULT_SCHEMA : seed.getSchema());
        entry.put("file", "/" + fileNameOnly(fileName) + ".csv");
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
        return out.toString()
                  .toUpperCase(Locale.ROOT);
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
