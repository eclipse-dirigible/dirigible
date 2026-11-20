/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.data.structures.domain.Schema;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintCheck;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintUnique;
import org.eclipse.dirigible.components.data.structures.synchronizer.SchemasSynchronizer;
import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * A composite business key survives the whole way down: the real
 * {@code application.schema.template} renders it into the {@code .schema}, and the real
 * {@link SchemasSynchronizer} reads that back as the constraint the table is created with (#6763).
 *
 * <p>
 * The two halves are asserted together on purpose. A key that renders into JSON nobody parses, or a
 * parser waiting for a shape nothing emits, both look perfectly healthy in isolation - and the
 * business rule the author wrote silently constrains nothing either way.
 */
class SchemaTemplateUniqueConstraintIT extends IntegrationTest {

    /** Classpath location of the template under test (shipped in template-application-schema). */
    private static final String TEMPLATE_LOCATION = "/META-INF/dirigible/template-application-schema/data/application.schema.template";

    private static final Gson GSON = new Gson();

    @Autowired
    private VelocityGenerationEngine velocityGenerationEngine;

    @Autowired
    private SchemasSynchronizer schemasSynchronizer;

    @Test
    void aCompositeKeyIsEmittedAndParsedBackAsTheTablesConstraint() throws Exception {
        String schema = render(List.of(entityWithKey()));

        assertNotNull(GSON.fromJson(schema, JsonObject.class), "the emitted schema must be valid JSON");

        Table table = onlyTable(schema);
        List<TableConstraintUnique> keys = table.getConstraints()
                                                .getUniqueIndexes();
        assertEquals(1, keys.size(), "the declared key must reach the table the synchronizer creates");
        assertEquals("TenantApplication_Tenant_Application", keys.get(0)
                                                                 .getName());
        assertEquals(List.of("TENANT_APPLICATION_TENANT", "TENANT_APPLICATION_APPLICATION"), Arrays.asList(keys.get(0)
                                                                                                               .getColumns()),
                "the emitted schema keeps the authored order, so regenerating an unchanged model produces an unchanged file");
    }

    @Test
    void aTableWithoutAKeyEmitsNoConstraintsAtAll() throws Exception {
        Map<String, Object> plain = entityWithKey();
        plain.remove("uniqueConstraints");

        String schema = render(List.of(plain));

        assertNotNull(GSON.fromJson(schema, JsonObject.class), "the emitted schema must still be valid JSON");
        assertFalse(schema.contains("uniqueIndexes"), "an entity that declares no key must not emit an empty constraints block");
        assertTrue(onlyTable(schema).getConstraints()
                                    .getUniqueIndexes()
                                    .isEmpty());
    }

    /**
     * #8794: a multilingual entity's language table holds one translation per record and language, its
     * code in lower case - a unique key over {@code Id} and {@code Language} and a check, both reaching
     * the table the synchronizer creates.
     */
    @Test
    void aLanguageTableDeclaresOneLowerCaseTranslationPerRecordAndLanguage() throws Exception {
        String schema = render(List.of(multilingualEntity()));

        assertNotNull(GSON.fromJson(schema, JsonObject.class), "the emitted schema must be valid JSON");
        Table languageTable = tableNamed(schema, "UNIT_LANG");
        List<TableConstraintUnique> keys = languageTable.getConstraints()
                                                        .getUniqueIndexes();
        assertEquals(1, keys.size(), "one translation per record and language");
        assertEquals("UNIT_LANG_UNIQUE", keys.get(0)
                                             .getName());
        assertEquals(List.of("Id", "Language"), Arrays.asList(keys.get(0)
                                                                  .getColumns()));
        List<TableConstraintCheck> checks = languageTable.getConstraints()
                                                         .getChecks();
        assertEquals(1, checks.size(), "the code in lower case - without it the key lets bg and BG both in");
        assertEquals("UNIT_LANG_LOWER", checks.get(0)
                                              .getName());
        assertEquals("\"Language\" = LOWER(\"Language\")", checks.get(0)
                                                                 .getExpression());
        assertTrue(tableNamed(schema, "UNIT").getConstraints()
                                             .getChecks()
                                             .isEmpty(),
                "the base table declares nothing new");
    }

    private Table tableNamed(String schema, String name) {
        return schemasSynchronizer.parseSchema("/unique-it/application.schema", schema)
                                  .getTables()
                                  .stream()
                                  .filter(table -> name.equals(table.getName()))
                                  .findFirst()
                                  .orElseThrow(() -> new AssertionError("no table [" + name + "] in " + schema));
    }

    private Table onlyTable(String schema) {
        Schema parsed = schemasSynchronizer.parseSchema("/unique-it/application.schema", schema);
        assertEquals(1, parsed.getTables()
                              .size());
        return parsed.getTables()
                     .get(0);
    }

    private String render(List<Map<String, Object>> models) throws Exception {
        String template;
        try (InputStream in = getClass().getResourceAsStream(TEMPLATE_LOCATION)) {
            assertNotNull(in, "template resource not found on classpath: " + TEMPLATE_LOCATION);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("models", models);
        parameters.put("tablePrefix", "");
        parameters.put("dataSource", "DefaultDB");
        byte[] out = velocityGenerationEngine.generate(parameters, TEMPLATE_LOCATION, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /**
     * The shape {@code EdmIntentGenerator} puts on the entity for {@code unique: [{fields: [...]}]}.
     */
    private static Map<String, Object> entityWithKey() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("name", "TenantApplication");
        model.put("dataName", "TENANT_APPLICATION");
        model.put("type", "PRIMARY");
        model.put("properties", new ArrayList<>(Arrays.asList(pk("Id", "TENANT_APPLICATION_ID"),
                column("Tenant", "TENANT_APPLICATION_TENANT"), column("Application", "TENANT_APPLICATION_APPLICATION"))));
        Map<String, Object> constraint = new LinkedHashMap<>();
        constraint.put("name", "TenantApplication_Tenant_Application");
        constraint.put("columns", List.of(Map.of("name", "TENANT_APPLICATION_TENANT"), Map.of("name", "TENANT_APPLICATION_APPLICATION")));
        constraint.put("message", "This application is already provisioned for the tenant");
        model.put("uniqueConstraints", new ArrayList<>(List.of(constraint)));
        return model;
    }

    /** A multilingual entity with one translatable property, as the EDM generator shapes it. */
    private static Map<String, Object> multilingualEntity() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("name", "Unit");
        model.put("dataName", "UNIT");
        model.put("type", "PRIMARY");
        model.put("multilingual", "true");
        Map<String, Object> name = column("Name", "UNIT_NAME");
        name.put("dataType", "VARCHAR");
        name.put("dataLength", "100");
        model.put("properties", new ArrayList<>(Arrays.asList(pk("Id", "UNIT_ID"), name)));
        return model;
    }

    private static Map<String, Object> pk(String name, String dataName) {
        Map<String, Object> property = column(name, dataName);
        property.put("dataPrimaryKey", Boolean.TRUE);
        property.put("dataAutoIncrement", Boolean.TRUE);
        property.put("dataNotNull", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> column(String name, String dataName) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", dataName);
        property.put("dataType", "INTEGER");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }
}
