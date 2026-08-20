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

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@code .edm} must be a lossless source for the derived {@code .model} (#6826). Intent
 * Generate writes {@code <name>.edm} and a complete {@code <name>.model} together; the loss used to
 * happen when the {@code .edm} was used to rebuild the {@code .model} - opening a diagram and
 * saving it, which regenerates the model from the {@code .edm} via {@code transform-edm.js},
 * dropped every structured (List/Map) attribute (rollupGuard, checks, uniqueConstraints, ...)
 * because they never reached the {@code .edm}.
 *
 * <p>
 * This drives the real round-trip over HTTP, no browser: generate from an intent that exercises the
 * structured values, capture the intent's {@code .model} (the oracle), then save the {@code .edm}
 * back through the workspace API (which fires the same {@code ide-workspace-on-save} transform the
 * editor's save does) and assert the regenerated {@code .model} carries every structured value
 * intact. A structural diff of every entity/property key against the oracle then makes the
 * guarantee complete: a future structured attribute forgotten in the generator's set is dropped
 * from the {@code .edm} and caught here as a missing key, and one written but not parsed back is
 * caught as a value mismatch - so a structured attribute cannot ship without {@code .edm}
 * serialization support.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EdmModelRoundTripIT extends IntegrationTest {

    private static final String PROJECT = "edm-roundtrip-test";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String EDM_PUT_URL = "/services/ide/workspaces/" + WORKSPACE + "/" + PROJECT + "/rt.edm";

    // A same-model intent whose entities exercise the structured (List/Map) values #6826 was losing:
    // labelParts + uniqueConstraints + relatedEntities on Category, lookupColumns on Product's FK,
    // checks on Booking, and rollupGuard on Booking (the child of the capacity-bearing seat roll-up).
    private static final String INTENT_YAML = """
            name: rt
            entities:
              - name: Category
                label: "{code} - {title}"
                unique:
                  - { fields: [code, title], message: "Code and title must be unique together" }
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: code,  type: string,  length: 20 }
                  - { name: title, type: string,  length: 100 }
                related:
                  - { entity: Product }

              - name: Product
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string,  length: 100 }
                  - { name: price, type: decimal }
                relations:
                  - { name: Category, kind: manyToOne, to: Category, show: [code, title] }

              - name: Event
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: name,       type: string,  length: 100 }
                  - { name: capacity,   type: integer }
                  - { name: seatsTaken, type: integer }
                  - { name: seatsFree,  type: integer }

              - name: Booking
                checks:
                  - { kind: exactlyOne, fields: [seats, waitlistSeats], message: "Either a seat or a waitlist seat" }
                fields:
                  - { name: id,            type: integer, primaryKey: true, generated: true }
                  - { name: seats,         type: integer }
                  - { name: waitlistSeats, type: integer }
                relations:
                  - { name: Event, kind: manyToOne, to: Event, composition: true }

            rollups:
              - { name: eventSeats, entity: Booking, via: Event, field: seatsTaken,
                  op: sum, of: seats, capacity: capacity, balance: seatsFree }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void structured_values_survive_the_edm_to_model_round_trip() {
        writeIntent(INTENT_YAML);

        // 1. Generate: writes rt.edm AND the complete rt.model (the oracle).
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));

        String edm = contentOf("rt.edm");
        JsonObject modelFromIntent = parseModel(contentOf("rt.model"));

        // The generator must have written the structured values into the .edm (as JSON attributes),
        // otherwise there is nothing for the transform to read back. uniqueConstraints is excluded here:
        // it is owned by the composite-unique-key feature, which emits it as a <constraints> section, not
        // a JSON attribute.
        for (String key : new String[] {"rollupGuard", "checks", "labelParts", "relatedEntities", "lookupColumns"}) {
            assertTrue(edm.contains(key + "=\""), "the .edm must carry the structured value [" + key + "] as an attribute");
        }

        // 2. Rebuild the .model FROM the .edm the way the editor's save does: delete the oracle, then
        // PUT the .edm back through the workspace API (fires the ide-workspace-on-save transform). The
        // regenerated rt.model existing at all proves the transform ran.
        byte[] edmBytes = resource("rt.edm").getContent();
        resource("rt.model").delete();
        restAssuredExecutor.execute(() -> given().contentType("application/octet-stream")
                                                 .body(edmBytes)
                                                 .when()
                                                 .put(EDM_PUT_URL)
                                                 .then()
                                                 .statusCode(200));

        assertTrue(resource("rt.model").exists(), "the .edm -> .model transform must have regenerated rt.model");
        JsonObject modelFromEdm = parseModel(contentOf("rt.model"));

        // 3. Every structured value present in the intent's .model must survive intact in the one
        // rebuilt from the .edm - the acceptance criterion "the .model generated from the .edm equals
        // the .model from intent" for exactly the values that were being dropped.
        assertEntityStructuredEquals(modelFromIntent, modelFromEdm, "Category", "labelParts");
        assertEntityStructuredEquals(modelFromIntent, modelFromEdm, "Category", "relatedEntities");
        assertEntityStructuredEquals(modelFromIntent, modelFromEdm, "Booking", "checks");
        assertEntityStructuredEquals(modelFromIntent, modelFromEdm, "Booking", "rollupGuard");
        assertPropertyStructuredEquals(modelFromIntent, modelFromEdm, "Product", "Category", "lookupColumns");

        // uniqueConstraints is owned by the composite-unique-key feature (a <constraints> section, not a
        // JSON attribute); this fix must not double it. Guard against the duplication that would occur if
        // both mechanisms wrote it: exactly one entry after the round-trip.
        JsonElement uniqueConstraints = entity(modelFromEdm, "Category").get("uniqueConstraints");
        assertNotNull(uniqueConstraints, "uniqueConstraints must survive the round-trip (via the <constraints> feature)");
        assertEquals(1, uniqueConstraints.getAsJsonArray()
                                         .size(),
                "uniqueConstraints must appear exactly once - not duplicated by both the feature and this fix");

        // The headline symptom: rollupGuard is a non-empty object on Booking after the round-trip.
        JsonElement rollupGuard = entity(modelFromEdm, "Booking").get("rollupGuard");
        assertNotNull(rollupGuard, "rollupGuard must survive the save-regenerate cycle");
        assertTrue(rollupGuard.isJsonObject() && rollupGuard.getAsJsonObject()
                                                            .size() > 0,
                "rollupGuard must be a populated object, not an empty or stringified value");

        // 4. The real "future attributes cannot ship without .edm serialization" guard: every
        // entity/property key the intent's .model carries must survive intact in the one rebuilt from the
        // .edm. A new structured attribute forgotten in the generator's set is dropped from the .edm and
        // caught here as a MISSING key; one written but not parsed back is caught as a VALUE mismatch
        // (object vs JSON string). Comparing parsed structure - not pattern-matching for {/[ - means a
        // scalar that legitimately IS a JSON string (e.g. widgetDependsOnValueCases) is compared
        // value-to-value and never falsely flagged.
        assertEveryKeySurvives(modelFromIntent, modelFromEdm);
    }

    private void assertEntityStructuredEquals(JsonObject fromIntent, JsonObject fromEdm, String entityName, String key) {
        JsonElement expected = entity(fromIntent, entityName).get(key);
        assertNotNull(expected, "fixture precondition: [" + entityName + "] should declare [" + key + "] in the intent's .model");
        JsonElement actual = entity(fromEdm, entityName).get(key);
        assertNotNull(actual, "[" + entityName + "] lost [" + key + "] in the .edm -> .model round-trip");
        assertEquals(expected, actual, "[" + entityName + "]." + key + " changed across the .edm -> .model round-trip");
    }

    private void assertPropertyStructuredEquals(JsonObject fromIntent, JsonObject fromEdm, String entityName, String propertyName,
            String key) {
        JsonElement expected = property(entity(fromIntent, entityName), propertyName).get(key);
        assertNotNull(expected,
                "fixture precondition: [" + entityName + "." + propertyName + "] should declare [" + key + "] in the intent's .model");
        JsonElement actual = property(entity(fromEdm, entityName), propertyName).get(key);
        assertNotNull(actual, "[" + entityName + "." + propertyName + "] lost [" + key + "] in the .edm -> .model round-trip");
        assertEquals(expected, actual, "[" + entityName + "." + propertyName + "]." + key + " changed across the round-trip");
    }

    /**
     * Keys another mechanism owns and round-trips differently, so they are compared by their own tests
     * rather than here: {@code uniqueConstraints} is emitted and rebuilt by the composite-unique-key
     * feature's {@code <constraints>} section (a shape without {@code properties}), and this fix
     * deliberately does not touch it (see {@code EdmIntentGenerator.STRUCTURED_ATTRIBUTES} and
     * {@code transform-edm.js} {@code ENTITY_STRUCTURED}). Its non-duplication is asserted separately
     * above.
     */
    private static final java.util.Set<String> ROUND_TRIP_EXCEPTIONS = java.util.Set.of("uniqueConstraints");

    /**
     * Every entity/property key in the intent's .model must survive intact in the one rebuilt from the
     * .edm.
     */
    private void assertEveryKeySurvives(JsonObject fromIntent, JsonObject fromEdm) {
        for (JsonElement e : fromIntent.getAsJsonObject("model")
                                       .getAsJsonArray("entities")) {
            JsonObject oracle = e.getAsJsonObject();
            String name = oracle.get("name")
                                .getAsString();
            JsonObject actual = entity(fromEdm, name);
            for (Map.Entry<String, JsonElement> member : oracle.entrySet()) {
                String key = member.getKey();
                if (ROUND_TRIP_EXCEPTIONS.contains(key)) {
                    continue;
                }
                if ("properties".equals(key)) {
                    assertPropertiesSurvive(member.getValue()
                                                  .getAsJsonArray(),
                            actual, name);
                    continue;
                }
                assertTrue(actual.has(key), "[" + name + "] lost attribute [" + key + "] in the .edm -> .model round-trip");
                assertEquals(member.getValue(), actual.get(key), "[" + name + "]." + key + " changed across the .edm -> .model round-trip");
            }
        }
    }

    private void assertPropertiesSurvive(JsonArray oracleProperties, JsonObject actualEntity, String entityName) {
        for (JsonElement pe : oracleProperties) {
            JsonObject oracleProperty = pe.getAsJsonObject();
            String propertyName = oracleProperty.get("name")
                                                .getAsString();
            JsonObject actualProperty = property(actualEntity, propertyName);
            for (Map.Entry<String, JsonElement> member : oracleProperty.entrySet()) {
                String key = member.getKey();
                if (ROUND_TRIP_EXCEPTIONS.contains(key)) {
                    continue;
                }
                assertTrue(actualProperty.has(key),
                        "[" + entityName + "." + propertyName + "] lost attribute [" + key + "] in the round-trip");
                assertEquals(member.getValue(), actualProperty.get(key),
                        "[" + entityName + "." + propertyName + "]." + key + " changed across the round-trip");
            }
        }
    }

    private JsonObject entity(JsonObject model, String name) {
        for (JsonElement e : model.getAsJsonObject("model")
                                  .getAsJsonArray("entities")) {
            JsonObject entity = e.getAsJsonObject();
            if (name.equals(entity.get("name")
                                  .getAsString())) {
                return entity;
            }
        }
        throw new AssertionError("entity [" + name + "] not found in the model");
    }

    private JsonObject property(JsonObject entity, String name) {
        for (JsonElement p : entity.getAsJsonArray("properties")) {
            JsonObject property = p.getAsJsonObject();
            if (name.equals(property.get("name")
                                    .getAsString())) {
                return property;
            }
        }
        throw new AssertionError("property [" + name + "] not found on entity [" + entity.get("name")
                                                                                         .getAsString()
                + "]");
    }

    private JsonObject parseModel(String json) {
        return JsonParser.parseString(json)
                         .getAsJsonObject();
    }

    private void writeIntent(String yaml) {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private IResource resource(String fileName) {
        return repository.getResource(PROJECT_PATH + "/" + fileName);
    }

    private String contentOf(String fileName) {
        return new String(resource(fileName).getContent(), StandardCharsets.UTF_8);
    }

    @AfterEach
    void cleanup() {
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }
}
