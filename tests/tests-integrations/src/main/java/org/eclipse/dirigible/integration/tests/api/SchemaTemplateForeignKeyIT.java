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
import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Renders the real {@code application.schema.template} through the application's own
 * {@link VelocityGenerationEngine} bean against models whose relationship graph contains cycles - a
 * self-reference ({@code Employee.manager -> Employee}) and a mutual {@code A <-> B} reference - to
 * prove the foreign-key resolution:
 * <ul>
 * <li><b>terminates</b> (a hard {@link Timeout} fails the build rather than hanging it if the
 * template ever loops) - the referenced table/PK are resolved by a build-once {@code name -> model}
 * map lookup, not by a recursive or unbounded scan, so a reference cycle can never loop;</li>
 * <li>emits <b>valid JSON</b> (parsed by Gson); and</li>
 * <li>resolves each foreign key's {@code referencedTable}/{@code referencedColumns} <b>exactly
 * once</b> (single emission).</li>
 * </ul>
 * These are the cases the shipped models never exercised, which is why the self-reference
 * dangling-comma bug went unnoticed.
 */
class SchemaTemplateForeignKeyIT extends IntegrationTest {

    /** Classpath location of the template under test (shipped in template-application-schema). */
    private static final String TEMPLATE_LOCATION = "/META-INF/dirigible/template-application-schema/data/application.schema.template";

    private static final Gson GSON = new Gson();

    @Autowired
    private VelocityGenerationEngine velocityGenerationEngine;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void selfReferencingAndMutualForeignKeysResolveWithoutLooping() throws Exception {
        // Self-reference: Employee.manager -> Employee (the original dangling-comma bug).
        String selfReference = render(List.of(entity("Employee", "EMPLOYEES_EMPLOYEE", pk("Id", "EMPLOYEE_ID"),
                fk("Manager", "EMPLOYEE_MANAGER", "Employee_Employee", "Employee"))));

        assertNotNull(GSON.fromJson(selfReference, JsonObject.class), "self-reference output must be valid JSON");
        assertTrue(selfReference.contains("\"referencedTable\": \"EMPLOYEES_EMPLOYEE\""), "self-reference must resolve its own table");
        assertTrue(selfReference.contains("\"referencedColumns\": \"EMPLOYEE_ID\""), "self-reference must resolve its own primary key");
        assertEquals(1, count(selfReference, "\"referencedTable\""), "the single foreign key must resolve exactly once");

        // Mutual cycle: A -> B and B -> A.
        String mutual = render(List.of(entity("A", "A_TABLE", pk("Id", "A_ID"), fk("B", "A_B", "A_B", "B")),
                entity("B", "B_TABLE", pk("Id", "B_ID"), fk("A", "B_A", "B_A", "A"))));

        assertNotNull(GSON.fromJson(mutual, JsonObject.class), "mutual-reference output must be valid JSON");
        assertTrue(mutual.contains("\"referencedTable\": \"B_TABLE\""), "A.B must resolve to B");
        assertTrue(mutual.contains("\"referencedTable\": \"A_TABLE\""), "B.A must resolve to A");
        assertEquals(2, count(mutual, "\"referencedTable\""), "each of the two foreign keys resolves exactly once");
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

    @SafeVarargs
    private static Map<String, Object> entity(String name, String dataName, Map<String, Object>... properties) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("name", name);
        model.put("dataName", dataName);
        model.put("type", "PRIMARY");
        model.put("properties", new ArrayList<>(Arrays.asList(properties)));
        return model;
    }

    private static Map<String, Object> pk(String name, String dataName) {
        Map<String, Object> property = baseProperty(name, dataName);
        property.put("dataPrimaryKey", Boolean.TRUE);
        property.put("dataAutoIncrement", Boolean.TRUE);
        property.put("dataNotNull", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> fk(String name, String dataName, String relationshipName, String relationshipEntityName) {
        Map<String, Object> property = baseProperty(name, dataName);
        property.put("relationshipName", relationshipName);
        property.put("relationshipEntityName", relationshipEntityName);
        return property;
    }

    private static Map<String, Object> baseProperty(String name, String dataName) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", dataName);
        property.put("dataType", "INTEGER");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }

    private static int count(String haystack, String needle) {
        int occurrences = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            occurrences++;
        }
        return occurrences;
    }
}
